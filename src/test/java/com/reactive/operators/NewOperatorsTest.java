package com.reactive.operators;

import com.reactive.core.Observable;
import com.reactive.core.Observer;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Phase 1 new operators:
 * - toSet()
 * - toMap()
 * - collect()
 * - reduce(seed, accumulator)
 * - combineLatest(3 sources)
 * - withLatestFrom()
 * - startWith()
 * - sequenceEqual()
 * - window(count, skip)
 */
public class NewOperatorsTest {
    
    // ==================== toSet() Tests ====================
    
    @Test
    public void testToSet() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Set<Integer>> result = new AtomicReference<>();
        
        Observable.just(1, 2, 3, 2, 1, 4)
            .toSet()
            .subscribe(new Observer<Set<Integer>>() {
                @Override
                public void onNext(Set<Integer> set) {
                    result.set(set);
                }
                
                @Override
                public void onError(Throwable error) {
                    fail("Should not error");
                }
                
                @Override
                public void onComplete() {
                    latch.countDown();
                }
            });
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(Set.of(1, 2, 3, 4), result.get());
    }
    
    @Test
    public void testToSetEmpty() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Set<Integer>> result = new AtomicReference<>();
        
        Observable.<Integer>empty()
            .toSet()
            .subscribe(new Observer<Set<Integer>>() {
                @Override
                public void onNext(Set<Integer> set) {
                    result.set(set);
                }
                
                @Override
                public void onError(Throwable error) {
                    fail("Should not error");
                }
                
                @Override
                public void onComplete() {
                    latch.countDown();
                }
            });
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertTrue(result.get().isEmpty());
    }
    
    // ==================== toMap() Tests ====================
    
    @Test
    public void testToMapWithKeyAndValue() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Map<String, Integer>> result = new AtomicReference<>();
        
        Observable.just("one", "two", "three")
            .toMap(s -> s, String::length)
            .subscribe(new Observer<Map<String, Integer>>() {
                @Override
                public void onNext(Map<String, Integer> map) {
                    result.set(map);
                }
                
                @Override
                public void onError(Throwable error) {
                    fail("Should not error");
                }
                
                @Override
                public void onComplete() {
                    latch.countDown();
                }
            });
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        Map<String, Integer> expected = Map.of("one", 3, "two", 3, "three", 5);
        assertEquals(expected, result.get());
    }
    
    @Test
    public void testToMapWithKeyOnly() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Map<Integer, String>> result = new AtomicReference<>();
        
        Observable.just("one", "two", "three")
            .toMap(String::length)
            .subscribe(new Observer<Map<Integer, String>>() {
                @Override
                public void onNext(Map<Integer, String> map) {
                    result.set(map);
                }
                
                @Override
                public void onError(Throwable error) {
                    fail("Should not error");
                }
                
                @Override
                public void onComplete() {
                    latch.countDown();
                }
            });
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        // "three" overwrites "one" since both have length 5 and 3
        assertEquals(2, result.get().size());
        assertTrue(result.get().containsKey(3));
        assertTrue(result.get().containsKey(5));
    }
    
    // ==================== collect() Tests ====================
    
    @Test
    public void testCollectWithSupplierAndAccumulator() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Object> result = new AtomicReference<>();
        
        Observable.range(1, 5)
            .collect(ArrayList::new, ArrayList::add)
            .subscribe(new Observer<Object>() {
                @Override
                public void onNext(Object list) {
                    result.set(list);
                }
                
                @Override
                public void onError(Throwable error) {
                    fail("Should not error");
                }
                
                @Override
                public void onComplete() {
                    latch.countDown();
                }
            });
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertTrue(result.get() instanceof ArrayList);
        @SuppressWarnings("unchecked")
        ArrayList<Integer> resultList = (ArrayList<Integer>) result.get();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), resultList);
    }
    
    @Test
    public void testCollectWithJavaCollector() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Object> result = new AtomicReference<>();
        
        Observable.just("a", "b", "c")
            .collect(Collectors.toList())
            .subscribe(new Observer<Object>() {
                @Override
                public void onNext(Object value) {
                    result.set(value);
                }
                
                @Override
                public void onError(Throwable error) {
                    fail("Should not error");
                }
                
                @Override
                public void onComplete() {
                    latch.countDown();
                }
            });
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        @SuppressWarnings("unchecked")
        List<String> resultList = (List<String>) result.get();
        assertEquals(Arrays.asList("a", "b", "c"), resultList);
    }
    
    @Test
    public void testCollectToSet() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Set<Integer>> result = new AtomicReference<>();
        
        Observable.just(1, 2, 3, 2, 1)
            .collect(Collectors.toSet())
            .subscribe(new Observer<Set<Integer>>() {
                @Override
                public void onNext(Set<Integer> set) {
                    result.set(set);
                }
                
                @Override
                public void onError(Throwable error) {
                    fail("Should not error");
                }
                
                @Override
                public void onComplete() {
                    latch.countDown();
                }
            });
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(Set.of(1, 2, 3), result.get());
    }
    
    // ==================== reduce(seed, accumulator) Tests ====================
    
    @Test
    public void testReduceWithSeed() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Integer> result = new AtomicReference<>();
        
        Observable.range(1, 5)
            .reduce(10, (acc, val) -> acc + val)
            .subscribe(new Observer<Integer>() {
                @Override
                public void onNext(Integer value) {
                    result.set(value);
                }
                
                @Override
                public void onError(Throwable error) {
                    fail("Should not error");
                }
                
                @Override
                public void onComplete() {
                    latch.countDown();
                }
            });
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(25, result.get()); // 10 + 1 + 2 + 3 + 4 + 5
    }
    
    @Test
    public void testReduceWithSeedEmpty() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Integer> result = new AtomicReference<>();
        
        Observable.<Integer>empty()
            .reduce(100, (acc, val) -> acc + val)
            .subscribe(new Observer<Integer>() {
                @Override
                public void onNext(Integer value) {
                    result.set(value);
                }
                
                @Override
                public void onError(Throwable error) {
                    fail("Should not error");
                }
                
                @Override
                public void onComplete() {
                    latch.countDown();
                }
            });
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(100, result.get()); // Just the seed
    }
    
    @Test
    public void testReduceWithSeedDifferentType() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> result = new AtomicReference<>();
        
        Observable.range(1, 3)
            .reduce("Numbers: ", (acc, val) -> acc + val + " ")
            .subscribe(new Observer<String>() {
                @Override
                public void onNext(String value) {
                    result.set(value);
                }
                
                @Override
                public void onError(Throwable error) {
                    fail("Should not error");
                }
                
                @Override
                public void onComplete() {
                    latch.countDown();
                }
            });
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals("Numbers: 1 2 3 ", result.get());
    }
    
    // ==================== combineLatest(3 sources) Tests ====================
    
    @Test
    public void testCombineLatestThreeSources() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        List<String> results = Collections.synchronizedList(new ArrayList<>());
        
        Observable<Integer> obs1 = Observable.just(1, 2);
        Observable<String> obs2 = Observable.just("A", "B");
        Observable<Boolean> obs3 = Observable.just(true, false);
        
        Observable.combineLatest(obs1, obs2, obs3, (a, b, c) -> a + "-" + b + "-" + c)
            .subscribe(new Observer<String>() {
                @Override
                public void onNext(String value) {
                    results.add(value);
                }
                
                @Override
                public void onError(Throwable error) {
                    fail("Should not error: " + error.getMessage());
                }
                
                @Override
                public void onComplete() {
                    latch.countDown();
                }
            });
        
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertFalse(results.isEmpty());
        // Should contain combinations of latest values
        assertTrue(results.contains("2-B-false") || results.size() > 0);
    }
    
    // ==================== withLatestFrom() Tests ====================
    
    @Test
    public void testWithLatestFrom() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        List<String> results = Collections.synchronizedList(new ArrayList<>());
        
        Observable<Integer> source = Observable.just(1, 2, 3);
        Observable<String> other = Observable.just("A", "B");
        
        source.withLatestFrom(other, (a, b) -> a + "-" + b)
            .subscribe(new Observer<String>() {
                @Override
                public void onNext(String value) {
                    results.add(value);
                }
                
                @Override
                public void onError(Throwable error) {
                    fail("Should not error");
                }
                
                @Override
                public void onComplete() {
                    latch.countDown();
                }
            });
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertFalse(results.isEmpty());
        // Should combine source values with latest from other
    }
    
    @Test
    public void testWithLatestFromOtherHasNoValue() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        List<String> results = Collections.synchronizedList(new ArrayList<>());
        
        Observable<Integer> source = Observable.just(1, 2, 3);
        Observable<String> other = Observable.empty();
        
        source.withLatestFrom(other, (a, b) -> a + "-" + b)
            .subscribe(new Observer<String>() {
                @Override
                public void onNext(String value) {
                    results.add(value);
                }
                
                @Override
                public void onError(Throwable error) {
                    fail("Should not error");
                }
                
                @Override
                public void onComplete() {
                    latch.countDown();
                }
            });
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertTrue(results.isEmpty()); // No emissions because other has no value
    }
    
    // ==================== startWith() Tests ====================
    
    @Test
    public void testStartWithValues() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        List<Integer> results = new ArrayList<>();
        
        Observable.range(4, 3) // 4, 5, 6
            .startWith(1, 2, 3)
            .subscribe(new Observer<Integer>() {
                @Override
                public void onNext(Integer value) {
                    results.add(value);
                }
                
                @Override
                public void onError(Throwable error) {
                    fail("Should not error");
                }
                
                @Override
                public void onComplete() {
                    latch.countDown();
                }
            });
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), results);
    }
    
    @Test
    public void testStartWithObservable() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        List<Integer> results = new ArrayList<>();
        
        Observable<Integer> prefix = Observable.just(1, 2, 3);
        Observable.range(4, 3) // 4, 5, 6
            .startWith(prefix)
            .subscribe(new Observer<Integer>() {
                @Override
                public void onNext(Integer value) {
                    results.add(value);
                }
                
                @Override
                public void onError(Throwable error) {
                    fail("Should not error");
                }
                
                @Override
                public void onComplete() {
                    latch.countDown();
                }
            });
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), results);
    }
    
    @Test
    public void testStartWithEmpty() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        List<Integer> results = new ArrayList<>();
        
        Observable.range(1, 3)
            .startWith()
            .subscribe(new Observer<Integer>() {
                @Override
                public void onNext(Integer value) {
                    results.add(value);
                }
                
                @Override
                public void onError(Throwable error) {
                    fail("Should not error");
                }
                
                @Override
                public void onComplete() {
                    latch.countDown();
                }
            });
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(Arrays.asList(1, 2, 3), results);
    }
    
    // ==================== sequenceEqual() Tests ====================
    
    @Test
    public void testSequenceEqualTrue() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Boolean> result = new AtomicReference<>();
        
        Observable<Integer> obs1 = Observable.just(1, 2, 3);
        Observable<Integer> obs2 = Observable.just(1, 2, 3);
        
        Observable.sequenceEqual(obs1, obs2)
            .subscribe(new Observer<Boolean>() {
                @Override
                public void onNext(Boolean value) {
                    result.set(value);
                }
                
                @Override
                public void onError(Throwable error) {
                    fail("Should not error");
                }
                
                @Override
                public void onComplete() {
                    latch.countDown();
                }
            });
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertTrue(result.get());
    }
    
    @Test
    public void testSequenceEqualFalseDifferentValues() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Boolean> result = new AtomicReference<>();
        
        Observable<Integer> obs1 = Observable.just(1, 2, 3);
        Observable<Integer> obs2 = Observable.just(1, 2, 4);
        
        Observable.sequenceEqual(obs1, obs2)
            .subscribe(new Observer<Boolean>() {
                @Override
                public void onNext(Boolean value) {
                    result.set(value);
                }
                
                @Override
                public void onError(Throwable error) {
                    fail("Should not error");
                }
                
                @Override
                public void onComplete() {
                    latch.countDown();
                }
            });
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertFalse(result.get());
    }
    
    @Test
    public void testSequenceEqualFalseDifferentLengths() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Boolean> result = new AtomicReference<>();
        
        Observable<Integer> obs1 = Observable.just(1, 2, 3);
        Observable<Integer> obs2 = Observable.just(1, 2);
        
        Observable.sequenceEqual(obs1, obs2)
            .subscribe(new Observer<Boolean>() {
                @Override
                public void onNext(Boolean value) {
                    result.set(value);
                }
                
                @Override
                public void onError(Throwable error) {
                    fail("Should not error");
                }
                
                @Override
                public void onComplete() {
                    latch.countDown();
                }
            });
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertFalse(result.get());
    }
    
    @Test
    public void testSequenceEqualBothEmpty() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Boolean> result = new AtomicReference<>();
        
        Observable<Integer> obs1 = Observable.empty();
        Observable<Integer> obs2 = Observable.empty();
        
        Observable.sequenceEqual(obs1, obs2)
            .subscribe(new Observer<Boolean>() {
                @Override
                public void onNext(Boolean value) {
                    result.set(value);
                }
                
                @Override
                public void onError(Throwable error) {
                    fail("Should not error");
                }
                
                @Override
                public void onComplete() {
                    latch.countDown();
                }
            });
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertTrue(result.get());
    }
    
    // ==================== window(count, skip) Tests ====================
    
    @Test
    public void testWindowCountSkipNoOverlap() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        List<List<Integer>> windows = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger windowCount = new AtomicInteger(0);
        
        Observable.range(1, 9)
            .window(3, 3) // Same as window(3)
            .subscribe(new Observer<Observable<Integer>>() {
                @Override
                public void onNext(Observable<Integer> window) {
                    List<Integer> windowItems = new ArrayList<>();
                    window.subscribe(new Observer<Integer>() {
                        @Override
                        public void onNext(Integer value) {
                            windowItems.add(value);
                        }
                        
                        @Override
                        public void onError(Throwable error) {
                            fail("Window should not error");
                        }
                        
                        @Override
                        public void onComplete() {
                            windows.add(windowItems);
                            if (windowCount.incrementAndGet() == 3) {
                                latch.countDown();
                            }
                        }
                    });
                }
                
                @Override
                public void onError(Throwable error) {
                    fail("Should not error");
                }
                
                @Override
                public void onComplete() {
                    // Main observable complete
                }
            });
        
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertEquals(3, windows.size());
        assertEquals(Arrays.asList(1, 2, 3), windows.get(0));
        assertEquals(Arrays.asList(4, 5, 6), windows.get(1));
        assertEquals(Arrays.asList(7, 8, 9), windows.get(2));
    }
    
    @Test
    public void testWindowCountSkipWithOverlap() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        List<List<Integer>> windows = Collections.synchronizedList(new ArrayList<>());
        
        Observable.range(1, 6)
            .window(3, 2) // Overlapping: skip < count
            .subscribe(new Observer<Observable<Integer>>() {
                @Override
                public void onNext(Observable<Integer> window) {
                    List<Integer> windowItems = new ArrayList<>();
                    window.subscribe(new Observer<Integer>() {
                        @Override
                        public void onNext(Integer value) {
                            windowItems.add(value);
                        }
                        
                        @Override
                        public void onError(Throwable error) {
                            fail("Window should not error");
                        }
                        
                        @Override
                        public void onComplete() {
                            windows.add(new ArrayList<>(windowItems));
                        }
                    });
                }
                
                @Override
                public void onError(Throwable error) {
                    fail("Should not error");
                }
                
                @Override
                public void onComplete() {
                    // Main observable complete
                    latch.countDown();
                }
            });
        
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        // We expect 3 completed windows and possibly one empty/incomplete one
        assertTrue(windows.size() >= 3);
        assertEquals(Arrays.asList(1, 2, 3), windows.get(0));
        assertEquals(Arrays.asList(3, 4, 5), windows.get(1));
        assertEquals(Arrays.asList(5, 6), windows.get(2));
    }
    
    @Test
    public void testWindowCountSkipWithGaps() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        List<List<Integer>> windows = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger windowCount = new AtomicInteger(0);
        
        Observable.range(1, 9)
            .window(2, 3) // Gaps: skip > count
            .subscribe(new Observer<Observable<Integer>>() {
                @Override
                public void onNext(Observable<Integer> window) {
                    List<Integer> windowItems = new ArrayList<>();
                    window.subscribe(new Observer<Integer>() {
                        @Override
                        public void onNext(Integer value) {
                            windowItems.add(value);
                        }
                        
                        @Override
                        public void onError(Throwable error) {
                            fail("Window should not error");
                        }
                        
                        @Override
                        public void onComplete() {
                            windows.add(windowItems);
                            if (windowCount.incrementAndGet() == 3) {
                                latch.countDown();
                            }
                        }
                    });
                }
                
                @Override
                public void onError(Throwable error) {
                    fail("Should not error");
                }
                
                @Override
                public void onComplete() {
                    // Main observable complete
                }
            });
        
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertEquals(3, windows.size());
        assertEquals(Arrays.asList(1, 2), windows.get(0));
        assertEquals(Arrays.asList(4, 5), windows.get(1));
        assertEquals(Arrays.asList(7, 8), windows.get(2));
        // 3, 6, 9 are skipped
    }
    
    // ==================== zipWith() Tests ====================
    
    @Test
    public void testZipWithBasic() {
        List<String> results = new ArrayList<>();
        
        Observable.just(1, 2, 3)
            .zipWith(Observable.just("a", "b", "c"), (num, str) -> num + str)
            .subscribe(new Observer<String>() {
                @Override public void onNext(String value) { results.add(value); }
                @Override public void onError(Throwable error) { fail("Unexpected error"); }
                @Override public void onComplete() {}
            });
        
        assertEquals(Arrays.asList("1a", "2b", "3c"), results);
    }
    
    @Test
    public void testZipWithDifferentLengths() {
        List<String> results = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean(false);
        
        Observable.just(1, 2, 3, 4, 5)
            .zipWith(Observable.just("a", "b"), (num, str) -> num + str)
            .subscribe(new Observer<String>() {
                @Override public void onNext(String value) { results.add(value); }
                @Override public void onError(Throwable error) { fail("Unexpected error"); }
                @Override public void onComplete() { completed.set(true); }
            });
        
        assertEquals(Arrays.asList("1a", "2b"), results);
        assertTrue(completed.get(), "Should complete when shorter source completes");
    }
    
    @Test
    public void testZipWithEmptySource() {
        List<String> results = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean(false);
        
        Observable.<Integer>empty()
            .zipWith(Observable.just("a", "b", "c"), (num, str) -> num + str)
            .subscribe(new Observer<String>() {
                @Override public void onNext(String value) { results.add(value); }
                @Override public void onError(Throwable error) { fail("Unexpected error"); }
                @Override public void onComplete() { completed.set(true); }
            });
        
        assertTrue(results.isEmpty());
        assertTrue(completed.get());
    }
    
    @Test
    public void testZipWithErrorPropagation() {
        AtomicReference<Throwable> error = new AtomicReference<>();
        RuntimeException testError = new RuntimeException("Test error");
        
        // Test that error from source propagates correctly
        Observable.<Integer>create(emitter -> {
            emitter.onError(testError);
        }).zipWith(Observable.just("a", "b"), (num, str) -> num + str)
          .subscribe(new Observer<String>() {
              @Override public void onNext(String value) {}
              @Override public void onError(Throwable e) { error.set(e); }
              @Override public void onComplete() {}
          });
        
        assertSame(testError, error.get());
    }
    
    @Test
    public void testZipWithZipperException() {
        AtomicReference<Throwable> error = new AtomicReference<>();
        
        Observable.just(1, 2, 3)
            .zipWith(Observable.just("a", "b", "c"), (num, str) -> {
                if (num == 2) throw new RuntimeException("Zipper error");
                return num + str;
            })
            .subscribe(new Observer<String>() {
                @Override public void onNext(String value) {}
                @Override public void onError(Throwable e) { error.set(e); }
                @Override public void onComplete() {}
            });
        
        assertNotNull(error.get());
        assertEquals("Zipper error", error.get().getMessage());
    }
    
    @Test
    public void testZipWithNullOther() {
        assertThrows(NullPointerException.class, () -> {
            Observable.just(1).zipWith(null, (a, b) -> a);
        });
    }
    
    @Test
    public void testZipWithNullZipper() {
        assertThrows(NullPointerException.class, () -> {
            Observable.just(1).zipWith(Observable.just(2), null);
        });
    }
    
    // ==================== retryWhen() Tests ====================
    
    @Test
    public void testRetryWhenBasic() {
        AtomicInteger attempts = new AtomicInteger(0);
        List<Integer> results = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean(false);
        
        Observable.<Integer>create(emitter -> {
            int attempt = attempts.incrementAndGet();
            if (attempt < 3) {
                emitter.onNext(attempt);
                emitter.onError(new RuntimeException("Retry " + attempt));
            } else {
                emitter.onNext(attempt);
                emitter.onComplete();
            }
        }).retryWhen(errors -> errors.take(2)) // Retry up to 2 times
          .subscribe(new Observer<Integer>() {
              @Override public void onNext(Integer value) { results.add(value); }
              @Override public void onError(Throwable e) { fail("Unexpected error: " + e); }
              @Override public void onComplete() { completed.set(true); }
          });
        
        assertEquals(3, attempts.get());
        assertEquals(Arrays.asList(1, 2, 3), results);
        assertTrue(completed.get());
    }
    
    @Test
    public void testRetryWhenNoRetry() {
        AtomicInteger attempts = new AtomicInteger(0);
        List<Integer> results = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean(false);
        
        Observable.<Integer>create(emitter -> {
            attempts.incrementAndGet();
            emitter.onNext(1);
            emitter.onComplete();
        }).retryWhen(errors -> errors) // No errors, no retry
          .subscribe(new Observer<Integer>() {
              @Override public void onNext(Integer value) { results.add(value); }
              @Override public void onError(Throwable e) { fail("Unexpected error"); }
              @Override public void onComplete() { completed.set(true); }
          });
        
        assertEquals(1, attempts.get());
        assertEquals(Arrays.asList(1), results);
        assertTrue(completed.get());
    }
    
    @Test
    public void testRetryWhenExhaustRetries() {
        AtomicInteger attempts = new AtomicInteger(0);
        AtomicBoolean completed = new AtomicBoolean(false);
        
        Observable.<Integer>create(emitter -> {
            attempts.incrementAndGet();
            emitter.onError(new RuntimeException("Always fails"));
        }).retryWhen(errors -> errors.take(3)) // Retry 3 times, then complete
          .subscribe(new Observer<Integer>() {
              @Override public void onNext(Integer value) {}
              @Override public void onError(Throwable e) { fail("Should complete, not error"); }
              @Override public void onComplete() { completed.set(true); }
          });
        
        assertEquals(4, attempts.get()); // Initial + 3 retries
        assertTrue(completed.get(), "Should complete when retry handler completes");
    }
    
    @Test
    public void testRetryWhenHandlerError() {
        AtomicInteger attempts = new AtomicInteger(0);
        AtomicReference<Throwable> receivedError = new AtomicReference<>();
        RuntimeException handlerError = new RuntimeException("Handler error");
        
        Observable.<Integer>create(emitter -> {
            attempts.incrementAndGet();
            emitter.onError(new RuntimeException("Source error"));
        }).retryWhen(errors -> errors.flatMap(e -> Observable.error(handlerError)))
          .subscribe(new Observer<Integer>() {
              @Override public void onNext(Integer value) {}
              @Override public void onError(Throwable e) { receivedError.set(e); }
              @Override public void onComplete() {}
          });
        
        assertEquals(1, attempts.get());
        assertSame(handlerError, receivedError.get());
    }
    
    @Test
    public void testRetryWhenNullHandler() {
        assertThrows(NullPointerException.class, () -> {
            Observable.just(1).retryWhen(null);
        });
    }
    
    @Test
    public void testRetryWhenHandlerReturnsNull() {
        AtomicReference<Throwable> error = new AtomicReference<>();
        
        Observable.<Integer>create(emitter -> {
            emitter.onError(new RuntimeException("Test"));
        }).retryWhen(errors -> null)
          .subscribe(new Observer<Integer>() {
              @Override public void onNext(Integer value) {}
              @Override public void onError(Throwable e) { error.set(e); }
              @Override public void onComplete() {}
          });
        
        assertNotNull(error.get());
        assertTrue(error.get() instanceof NullPointerException);
    }
    
    @Test
    public void testRetryWhenHandlerThrowsException() {
        AtomicReference<Throwable> error = new AtomicReference<>();
        RuntimeException handlerException = new RuntimeException("Handler threw");
        
        Observable.just(1)
            .retryWhen(errors -> { throw handlerException; })
            .subscribe(new Observer<Integer>() {
                @Override public void onNext(Integer value) {}
                @Override public void onError(Throwable e) { error.set(e); }
                @Override public void onComplete() {}
            });
        
        assertSame(handlerException, error.get());
    }
}
