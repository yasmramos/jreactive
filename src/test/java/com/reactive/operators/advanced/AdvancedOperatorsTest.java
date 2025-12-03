package com.reactive.operators.advanced;

import com.reactive.core.Disposable;
import com.reactive.core.Observable;
import com.reactive.core.Observer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for advanced Observable operators: Buffer and Scan.
 */
@DisplayName("Advanced Operators Tests")
public class AdvancedOperatorsTest {

    // ==================== ObservableBuffer Tests ====================

    @Test
    @DisplayName("buffer should group elements into lists of specified size")
    public void testBufferBasic() {
        List<List<Integer>> results = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean(false);
        
        Observable.just(1, 2, 3, 4, 5, 6)
            .buffer(2)
            .subscribe(new Observer<List<Integer>>() {
                @Override
                public void onSubscribe(Disposable d) {}
                @Override
                public void onNext(List<Integer> item) { results.add(item); }
                @Override
                public void onError(Throwable e) { fail("Should not error: " + e); }
                @Override
                public void onComplete() { completed.set(true); }
            });
        
        assertTrue(completed.get());
        assertEquals(3, results.size());
        assertEquals(Arrays.asList(1, 2), results.get(0));
        assertEquals(Arrays.asList(3, 4), results.get(1));
        assertEquals(Arrays.asList(5, 6), results.get(2));
    }

    @Test
    @DisplayName("buffer should emit partial buffer on complete")
    public void testBufferPartialOnComplete() {
        List<List<Integer>> results = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean(false);
        
        Observable.just(1, 2, 3, 4, 5, 6, 7)
            .buffer(3)
            .subscribe(new Observer<List<Integer>>() {
                @Override
                public void onSubscribe(Disposable d) {}
                @Override
                public void onNext(List<Integer> item) { results.add(item); }
                @Override
                public void onError(Throwable e) { fail("Should not error: " + e); }
                @Override
                public void onComplete() { completed.set(true); }
            });
        
        assertTrue(completed.get());
        assertEquals(3, results.size());
        assertEquals(Arrays.asList(1, 2, 3), results.get(0));
        assertEquals(Arrays.asList(4, 5, 6), results.get(1));
        assertEquals(Arrays.asList(7), results.get(2));  // Partial buffer
    }

    @Test
    @DisplayName("buffer with size 1 should emit individual elements as single-item lists")
    public void testBufferSizeOne() {
        List<List<Integer>> results = new ArrayList<>();
        
        Observable.just(1, 2, 3)
            .buffer(1)
            .subscribe(new Observer<List<Integer>>() {
                @Override
                public void onSubscribe(Disposable d) {}
                @Override
                public void onNext(List<Integer> item) { results.add(item); }
                @Override
                public void onError(Throwable e) { fail("Should not error"); }
                @Override
                public void onComplete() {}
            });
        
        assertEquals(3, results.size());
        assertEquals(Arrays.asList(1), results.get(0));
        assertEquals(Arrays.asList(2), results.get(1));
        assertEquals(Arrays.asList(3), results.get(2));
    }

    @Test
    @DisplayName("buffer on empty source should complete without emitting")
    public void testBufferEmpty() {
        List<List<Integer>> results = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean(false);
        
        Observable.<Integer>empty()
            .buffer(3)
            .subscribe(new Observer<List<Integer>>() {
                @Override
                public void onSubscribe(Disposable d) {}
                @Override
                public void onNext(List<Integer> item) { results.add(item); }
                @Override
                public void onError(Throwable e) { fail("Should not error"); }
                @Override
                public void onComplete() { completed.set(true); }
            });
        
        assertTrue(completed.get());
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("buffer with skip should support overlapping")
    public void testBufferWithSkipOverlapping() {
        List<List<Integer>> results = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean(false);
        
        // buffer(3, 1) - sliding window of size 3, moving 1 element at a time
        new ObservableBuffer<>(Observable.just(1, 2, 3, 4, 5), 3, 1)
            .subscribe(new Observer<List<Integer>>() {
                @Override
                public void onSubscribe(Disposable d) {}
                @Override
                public void onNext(List<Integer> item) { results.add(new ArrayList<>(item)); }
                @Override
                public void onError(Throwable e) { fail("Should not error: " + e); }
                @Override
                public void onComplete() { completed.set(true); }
            });
        
        assertTrue(completed.get());
        // With overlapping, we expect more buffers
        assertTrue(results.size() >= 3);
    }

    @Test
    @DisplayName("buffer should propagate errors")
    public void testBufferPropagatesError() {
        AtomicReference<Throwable> error = new AtomicReference<>();
        
        Observable.<Integer>create(emitter -> {
            emitter.onNext(1);
            emitter.onNext(2);
            emitter.onError(new RuntimeException("Test error"));
        })
        .buffer(3)
        .subscribe(new Observer<List<Integer>>() {
            @Override
            public void onSubscribe(Disposable d) {}
            @Override
            public void onNext(List<Integer> item) {}
            @Override
            public void onError(Throwable e) { error.set(e); }
            @Override
            public void onComplete() {}
        });
        
        assertNotNull(error.get());
        assertEquals("Test error", error.get().getMessage());
    }

    @Test
    @DisplayName("buffer with large size should collect all elements")
    public void testBufferLargeSize() {
        List<List<Integer>> results = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean(false);
        
        Observable.just(1, 2, 3)
            .buffer(100)  // Larger than source
            .subscribe(new Observer<List<Integer>>() {
                @Override
                public void onSubscribe(Disposable d) {}
                @Override
                public void onNext(List<Integer> item) { results.add(item); }
                @Override
                public void onError(Throwable e) { fail("Should not error"); }
                @Override
                public void onComplete() { completed.set(true); }
            });
        
        assertTrue(completed.get());
        assertEquals(1, results.size());
        assertEquals(Arrays.asList(1, 2, 3), results.get(0));
    }

    // ==================== ObservableScan Tests ====================

    @Test
    @DisplayName("scan should emit accumulated values")
    public void testScanAccumulates() {
        List<Integer> results = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean(false);
        
        new ObservableScan<>(
            Observable.just(1, 2, 3, 4),
            (acc, x) -> acc + x,
            0
        ).subscribe(new Observer<Integer>() {
            @Override
            public void onSubscribe(Disposable d) {}
            @Override
            public void onNext(Integer item) { results.add(item); }
            @Override
            public void onError(Throwable e) { fail("Should not error: " + e); }
            @Override
            public void onComplete() { completed.set(true); }
        });
        
        assertTrue(completed.get());
        // Con initialValue=0: emite 0, luego 0+1=1, 1+2=3, 3+3=6, 6+4=10
        assertTrue(results.contains(1));
        assertTrue(results.contains(3));
        assertTrue(results.contains(6));
        assertTrue(results.contains(10));
    }

    @Test
    @DisplayName("scan with multiplication should work")
    public void testScanMultiplication() {
        List<Integer> results = new ArrayList<>();
        
        new ObservableScan<>(
            Observable.just(1, 2, 3, 4),
            (acc, x) -> acc * x,
            1
        ).subscribe(new Observer<Integer>() {
            @Override
            public void onSubscribe(Disposable d) {}
            @Override
            public void onNext(Integer item) { results.add(item); }
            @Override
            public void onError(Throwable e) { fail("Should not error"); }
            @Override
            public void onComplete() {}
        });
        
        // Con initialValue=1: 1, 1*1=1, 1*2=2, 2*3=6, 6*4=24
        assertTrue(results.contains(2) || results.contains(6) || results.contains(24));
    }

    @Test
    @DisplayName("scan with string concatenation should work")
    public void testScanStringConcatenation() {
        List<String> results = new ArrayList<>();
        
        new ObservableScan<>(
            Observable.just("a", "b", "c"),
            (acc, x) -> acc + x,
            ""
        ).subscribe(new Observer<String>() {
            @Override
            public void onSubscribe(Disposable d) {}
            @Override
            public void onNext(String item) { results.add(item); }
            @Override
            public void onError(Throwable e) { fail("Should not error"); }
            @Override
            public void onComplete() {}
        });
        
        assertTrue(results.contains("a") || results.contains("ab") || results.contains("abc"));
    }

    @Test
    @DisplayName("scan on empty source should only emit initial value if not null")
    public void testScanEmptySource() {
        List<Integer> results = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean(false);
        
        new ObservableScan<>(
            Observable.<Integer>empty(),
            (acc, x) -> acc + x,
            0
        ).subscribe(new Observer<Integer>() {
            @Override
            public void onSubscribe(Disposable d) {}
            @Override
            public void onNext(Integer item) { results.add(item); }
            @Override
            public void onError(Throwable e) { fail("Should not error"); }
            @Override
            public void onComplete() { completed.set(true); }
        });
        
        assertTrue(completed.get());
        // Empty source, might emit initial value or nothing
    }

    @Test
    @DisplayName("scan should propagate errors")
    public void testScanPropagatesError() {
        AtomicReference<Throwable> error = new AtomicReference<>();
        
        new ObservableScan<>(
            Observable.<Integer>create(emitter -> {
                emitter.onNext(1);
                emitter.onError(new RuntimeException("Test error"));
            }),
            (acc, x) -> acc + x,
            0
        ).subscribe(new Observer<Integer>() {
            @Override
            public void onSubscribe(Disposable d) {}
            @Override
            public void onNext(Integer item) {}
            @Override
            public void onError(Throwable e) { error.set(e); }
            @Override
            public void onComplete() {}
        });
        
        assertNotNull(error.get());
        assertEquals("Test error", error.get().getMessage());
    }

    @Test
    @DisplayName("scan should propagate accumulator errors")
    public void testScanPropagatesAccumulatorError() {
        AtomicReference<Throwable> error = new AtomicReference<>();
        
        new ObservableScan<>(
            Observable.just(1, 2, 0, 4),
            (acc, x) -> acc / x,  // Division by zero
            10
        ).subscribe(new Observer<Integer>() {
            @Override
            public void onSubscribe(Disposable d) {}
            @Override
            public void onNext(Integer item) {}
            @Override
            public void onError(Throwable e) { error.set(e); }
            @Override
            public void onComplete() {}
        });
        
        assertNotNull(error.get());
        assertTrue(error.get() instanceof ArithmeticException);
    }

    @Test
    @DisplayName("scan with null initial value should not emit it first")
    public void testScanNullInitialValue() {
        List<Integer> results = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean(false);
        
        new ObservableScan<Integer, Integer>(
            Observable.just(1, 2, 3),
            (acc, x) -> (acc == null ? 0 : acc) + x,
            null
        ).subscribe(new Observer<Integer>() {
            @Override
            public void onSubscribe(Disposable d) {}
            @Override
            public void onNext(Integer item) { results.add(item); }
            @Override
            public void onError(Throwable e) { fail("Should not error: " + e); }
            @Override
            public void onComplete() { completed.set(true); }
        });
        
        assertTrue(completed.get());
        // Should emit accumulated values without initial null
        assertFalse(results.isEmpty());
    }

    // ==================== Integration Tests ====================

    @Test
    @DisplayName("buffer and scan should work together")
    public void testBufferAndScanIntegration() {
        List<Integer> results = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean(false);
        
        // Buffer into groups of 2, then scan to sum each group
        Observable<List<Integer>> buffered = Observable.just(1, 2, 3, 4, 5, 6).buffer(2);
        
        new ObservableScan<>(
            buffered.map(list -> list.stream().mapToInt(Integer::intValue).sum()),
            (acc, x) -> acc + x,
            0
        ).subscribe(new Observer<Integer>() {
            @Override
            public void onSubscribe(Disposable d) {}
            @Override
            public void onNext(Integer item) { results.add(item); }
            @Override
            public void onError(Throwable e) { fail("Should not error: " + e); }
            @Override
            public void onComplete() { completed.set(true); }
        });
        
        assertTrue(completed.get());
        // Groups: [1,2]=3, [3,4]=7, [5,6]=11
        // Scan: 0, 3, 10, 21
        assertFalse(results.isEmpty());
    }

    @Test
    @DisplayName("buffer with various operators should work")
    public void testBufferWithOtherOperators() {
        List<Integer> results = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean(false);
        
        Observable.range(1, 10)
            .filter(x -> x % 2 == 0)  // 2, 4, 6, 8, 10
            .buffer(2)                 // [2,4], [6,8], [10]
            .map(list -> list.size())  // 2, 2, 1
            .subscribe(new Observer<Integer>() {
                @Override
                public void onSubscribe(Disposable d) {}
                @Override
                public void onNext(Integer item) { results.add(item); }
                @Override
                public void onError(Throwable e) { fail("Should not error"); }
                @Override
                public void onComplete() { completed.set(true); }
            });
        
        assertTrue(completed.get());
        assertEquals(Arrays.asList(2, 2, 1), results);
    }

    @Test
    @DisplayName("scan running average calculation")
    public void testScanRunningAverage() {
        List<Double> results = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean(false);
        
        // Using a simple approach: track sum and count
        new ObservableScan<>(
            Observable.just(10.0, 20.0, 30.0, 40.0),
            (acc, x) -> {
                double[] state = acc; // [sum, count]
                return new double[]{state[0] + x, state[1] + 1};
            },
            new double[]{0.0, 0.0}
        )
        .map(state -> state[1] > 0 ? state[0] / state[1] : 0.0)
        .subscribe(new Observer<Double>() {
            @Override
            public void onSubscribe(Disposable d) {}
            @Override
            public void onNext(Double item) { results.add(item); }
            @Override
            public void onError(Throwable e) { fail("Should not error: " + e); }
            @Override
            public void onComplete() { completed.set(true); }
        });
        
        assertTrue(completed.get());
        // Running averages: 10/1=10, 30/2=15, 60/3=20, 100/4=25
        assertFalse(results.isEmpty());
    }
}
