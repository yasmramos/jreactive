package com.reactive.operators;

import com.reactive.core.Disposable;
import com.reactive.core.Observable;
import com.reactive.core.Observer;
import com.reactive.core.ObservableSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for standalone operator classes in com.reactive.operators package.
 * These tests directly test the operator classes to achieve code coverage.
 */
@DisplayName("Standalone Operators Tests")
public class StandaloneOperatorsTest {

    // Helper method to create ObservableSource from values
    // Now we can use Observable.just() directly since Observable implements ObservableSource!
    @SafeVarargs
    private static <T> ObservableSource<T> sourceOf(T... values) {
        // This works because Observable now implements ObservableSource
        return Observable.just(values);
    }
    
    // Helper method to create an error source
    private static <T> ObservableSource<T> errorSource(Throwable error) {
        return observer -> {
            observer.onSubscribe(Disposable.empty());
            observer.onError(error);
        };
    }
    
    // Helper method to create an empty source
    private static <T> ObservableSource<T> emptySource() {
        return observer -> {
            observer.onSubscribe(Disposable.empty());
            observer.onComplete();
        };
    }

    // ==================== OBSERVABLE MAP ====================
    
    @Nested
    @DisplayName("ObservableMap")
    class ObservableMapTests {
        
        @Test
        @DisplayName("Should transform values correctly")
        void shouldTransformValuesCorrectly() {
            List<Integer> results = new ArrayList<>();
            AtomicBoolean completed = new AtomicBoolean(false);
            
            ObservableSource<Integer> source = sourceOf(1, 2, 3);
            ObservableMap<Integer, Integer> mapped = new ObservableMap<>(source, x -> x * 2);
            
            mapped.subscribe(new Observer<Integer>() {
                @Override
                public void onSubscribe(Disposable d) {}
                
                @Override
                public void onNext(Integer value) {
                    results.add(value);
                }
                
                @Override
                public void onError(Throwable e) {}
                
                @Override
                public void onComplete() {
                    completed.set(true);
                }
            });
            
            assertThat(results).containsExactly(2, 4, 6);
            assertTrue(completed.get());
        }
        
        @Test
        @DisplayName("Should propagate onSubscribe")
        void shouldPropagateOnSubscribe() {
            AtomicReference<Disposable> receivedDisposable = new AtomicReference<>();
            Disposable expectedDisposable = Disposable.empty();
            
            ObservableSource<Integer> source = observer -> observer.onSubscribe(expectedDisposable);
            ObservableMap<Integer, Integer> mapped = new ObservableMap<>(source, x -> x * 2);
            
            mapped.subscribe(new Observer<Integer>() {
                @Override
                public void onSubscribe(Disposable d) {
                    receivedDisposable.set(d);
                }
                
                @Override
                public void onNext(Integer value) {}
                
                @Override
                public void onError(Throwable e) {}
                
                @Override
                public void onComplete() {}
            });
            
            assertSame(expectedDisposable, receivedDisposable.get());
        }
        
        @Test
        @DisplayName("Should emit error when mapper throws")
        void shouldEmitErrorWhenMapperThrows() {
            AtomicReference<Throwable> error = new AtomicReference<>();
            
            ObservableSource<Integer> source = sourceOf(1, 2, 3);
            ObservableMap<Integer, Integer> mapped = new ObservableMap<>(source, x -> {
                if (x == 2) throw new RuntimeException("Mapper error");
                return x * 2;
            });
            
            mapped.subscribe(new Observer<Integer>() {
                @Override
                public void onSubscribe(Disposable d) {}
                
                @Override
                public void onNext(Integer value) {}
                
                @Override
                public void onError(Throwable e) {
                    error.set(e);
                }
                
                @Override
                public void onComplete() {}
            });
            
            assertThat(error.get()).isNotNull();
            assertThat(error.get().getMessage()).isEqualTo("Mapper error");
        }
        
        @Test
        @DisplayName("Should propagate error from source")
        void shouldPropagateErrorFromSource() {
            AtomicReference<Throwable> error = new AtomicReference<>();
            RuntimeException sourceError = new RuntimeException("Source error");
            
            ObservableSource<Integer> source = errorSource(sourceError);
            ObservableMap<Integer, Integer> mapped = new ObservableMap<>(source, x -> x * 2);
            
            mapped.subscribe(new Observer<Integer>() {
                @Override
                public void onSubscribe(Disposable d) {}
                
                @Override
                public void onNext(Integer value) {}
                
                @Override
                public void onError(Throwable e) {
                    error.set(e);
                }
                
                @Override
                public void onComplete() {}
            });
            
            assertSame(sourceError, error.get());
        }
        
        @Test
        @DisplayName("Should handle empty source")
        void shouldHandleEmptySource() {
            List<Integer> results = new ArrayList<>();
            AtomicBoolean completed = new AtomicBoolean(false);
            
            ObservableSource<Integer> source = emptySource();
            ObservableMap<Integer, Integer> mapped = new ObservableMap<>(source, x -> x * 2);
            
            mapped.subscribe(new Observer<Integer>() {
                @Override
                public void onSubscribe(Disposable d) {}
                
                @Override
                public void onNext(Integer value) {
                    results.add(value);
                }
                
                @Override
                public void onError(Throwable e) {}
                
                @Override
                public void onComplete() {
                    completed.set(true);
                }
            });
            
            assertThat(results).isEmpty();
            assertTrue(completed.get());
        }
    }

    // ==================== OBSERVABLE FILTER ====================
    
    @Nested
    @DisplayName("ObservableFilter")
    class ObservableFilterTests {
        
        @Test
        @DisplayName("Should filter values correctly")
        void shouldFilterValuesCorrectly() {
            List<Integer> results = new ArrayList<>();
            AtomicBoolean completed = new AtomicBoolean(false);
            
            ObservableSource<Integer> source = sourceOf(1, 2, 3, 4, 5, 6);
            ObservableFilter<Integer> filtered = new ObservableFilter<>(source, x -> x % 2 == 0);
            
            filtered.subscribe(new Observer<Integer>() {
                @Override
                public void onSubscribe(Disposable d) {}
                
                @Override
                public void onNext(Integer value) {
                    results.add(value);
                }
                
                @Override
                public void onError(Throwable e) {}
                
                @Override
                public void onComplete() {
                    completed.set(true);
                }
            });
            
            assertThat(results).containsExactly(2, 4, 6);
            assertTrue(completed.get());
        }
        
        @Test
        @DisplayName("Should propagate onSubscribe")
        void shouldPropagateOnSubscribe() {
            AtomicReference<Disposable> receivedDisposable = new AtomicReference<>();
            Disposable expectedDisposable = Disposable.empty();
            
            ObservableSource<Integer> source = observer -> observer.onSubscribe(expectedDisposable);
            ObservableFilter<Integer> filtered = new ObservableFilter<>(source, x -> true);
            
            filtered.subscribe(new Observer<Integer>() {
                @Override
                public void onSubscribe(Disposable d) {
                    receivedDisposable.set(d);
                }
                
                @Override
                public void onNext(Integer value) {}
                
                @Override
                public void onError(Throwable e) {}
                
                @Override
                public void onComplete() {}
            });
            
            assertSame(expectedDisposable, receivedDisposable.get());
        }
        
        @Test
        @DisplayName("Should emit error when predicate throws")
        void shouldEmitErrorWhenPredicateThrows() {
            AtomicReference<Throwable> error = new AtomicReference<>();
            
            ObservableSource<Integer> source = sourceOf(1, 2, 3);
            ObservableFilter<Integer> filtered = new ObservableFilter<>(source, x -> {
                if (x == 2) throw new RuntimeException("Predicate error");
                return x % 2 == 0;
            });
            
            filtered.subscribe(new Observer<Integer>() {
                @Override
                public void onSubscribe(Disposable d) {}
                
                @Override
                public void onNext(Integer value) {}
                
                @Override
                public void onError(Throwable e) {
                    error.set(e);
                }
                
                @Override
                public void onComplete() {}
            });
            
            assertThat(error.get()).isNotNull();
            assertThat(error.get().getMessage()).isEqualTo("Predicate error");
        }
        
        @Test
        @DisplayName("Should propagate error from source")
        void shouldPropagateErrorFromSource() {
            AtomicReference<Throwable> error = new AtomicReference<>();
            RuntimeException sourceError = new RuntimeException("Source error");
            
            ObservableSource<Integer> source = errorSource(sourceError);
            ObservableFilter<Integer> filtered = new ObservableFilter<>(source, x -> true);
            
            filtered.subscribe(new Observer<Integer>() {
                @Override
                public void onSubscribe(Disposable d) {}
                
                @Override
                public void onNext(Integer value) {}
                
                @Override
                public void onError(Throwable e) {
                    error.set(e);
                }
                
                @Override
                public void onComplete() {}
            });
            
            assertSame(sourceError, error.get());
        }
    }

    // ==================== OBSERVABLE FLATMAP ====================
    
    @Nested
    @DisplayName("ObservableFlatMap")
    class ObservableFlatMapTests {
        
        @Test
        @DisplayName("Should flatten values correctly")
        void shouldFlattenValuesCorrectly() throws InterruptedException {
            List<Integer> results = Collections.synchronizedList(new ArrayList<>());
            CountDownLatch latch = new CountDownLatch(1);
            
            ObservableSource<Integer> source = sourceOf(1, 2, 3);
            ObservableFlatMap<Integer, Integer> flatMapped = new ObservableFlatMap<>(source, 
                x -> sourceOf(x * 10, x * 10 + 1));
            
            flatMapped.subscribe(new Observer<Integer>() {
                @Override
                public void onSubscribe(Disposable d) {}
                
                @Override
                public void onNext(Integer value) {
                    results.add(value);
                }
                
                @Override
                public void onError(Throwable e) {
                    latch.countDown();
                }
                
                @Override
                public void onComplete() {
                    latch.countDown();
                }
            });
            
            assertTrue(latch.await(2, TimeUnit.SECONDS));
            assertThat(results).containsExactlyInAnyOrder(10, 11, 20, 21, 30, 31);
        }
        
        @Test
        @DisplayName("Should emit error when mapper throws")
        void shouldEmitErrorWhenMapperThrows() throws InterruptedException {
            AtomicReference<Throwable> error = new AtomicReference<>();
            CountDownLatch latch = new CountDownLatch(1);
            
            ObservableSource<Integer> source = sourceOf(1, 2, 3);
            ObservableFlatMap<Integer, Integer> flatMapped = new ObservableFlatMap<>(source, x -> {
                if (x == 2) throw new RuntimeException("Mapper error");
                return sourceOf(x * 10);
            });
            
            flatMapped.subscribe(new Observer<Integer>() {
                @Override
                public void onSubscribe(Disposable d) {}
                
                @Override
                public void onNext(Integer value) {}
                
                @Override
                public void onError(Throwable e) {
                    error.set(e);
                    latch.countDown();
                }
                
                @Override
                public void onComplete() {
                    latch.countDown();
                }
            });
            
            assertTrue(latch.await(2, TimeUnit.SECONDS));
            assertThat(error.get()).isNotNull();
            assertThat(error.get().getMessage()).isEqualTo("Mapper error");
        }
        
        @Test
        @DisplayName("Should propagate error from inner observable")
        void shouldPropagateErrorFromInnerObservable() throws InterruptedException {
            AtomicReference<Throwable> error = new AtomicReference<>();
            CountDownLatch latch = new CountDownLatch(1);
            RuntimeException innerError = new RuntimeException("Inner error");
            
            ObservableSource<Integer> source = sourceOf(1, 2);
            ObservableFlatMap<Integer, Integer> flatMapped = new ObservableFlatMap<>(source, x -> {
                if (x == 2) {
                    return errorSource(innerError);
                }
                return sourceOf(x * 10);
            });
            
            flatMapped.subscribe(new Observer<Integer>() {
                @Override
                public void onSubscribe(Disposable d) {}
                
                @Override
                public void onNext(Integer value) {}
                
                @Override
                public void onError(Throwable e) {
                    error.set(e);
                    latch.countDown();
                }
                
                @Override
                public void onComplete() {
                    latch.countDown();
                }
            });
            
            assertTrue(latch.await(2, TimeUnit.SECONDS));
            assertThat(error.get()).isEqualTo(innerError);
        }
        
        @Test
        @DisplayName("Should propagate error from source")
        void shouldPropagateErrorFromSource() throws InterruptedException {
            AtomicReference<Throwable> error = new AtomicReference<>();
            CountDownLatch latch = new CountDownLatch(1);
            RuntimeException sourceError = new RuntimeException("Source error");
            
            ObservableSource<Integer> source = errorSource(sourceError);
            ObservableFlatMap<Integer, Integer> flatMapped = new ObservableFlatMap<>(source, 
                x -> sourceOf(x * 10));
            
            flatMapped.subscribe(new Observer<Integer>() {
                @Override
                public void onSubscribe(Disposable d) {}
                
                @Override
                public void onNext(Integer value) {}
                
                @Override
                public void onError(Throwable e) {
                    error.set(e);
                    latch.countDown();
                }
                
                @Override
                public void onComplete() {
                    latch.countDown();
                }
            });
            
            assertTrue(latch.await(2, TimeUnit.SECONDS));
            assertSame(sourceError, error.get());
        }
        
        @Test
        @DisplayName("Should handle empty inner observables")
        void shouldHandleEmptyInnerObservables() throws InterruptedException {
            List<Integer> results = Collections.synchronizedList(new ArrayList<>());
            CountDownLatch latch = new CountDownLatch(1);
            
            ObservableSource<Integer> source = sourceOf(1, 2, 3);
            ObservableFlatMap<Integer, Integer> flatMapped = new ObservableFlatMap<>(source, 
                x -> emptySource());
            
            flatMapped.subscribe(new Observer<Integer>() {
                @Override
                public void onSubscribe(Disposable d) {}
                
                @Override
                public void onNext(Integer value) {
                    results.add(value);
                }
                
                @Override
                public void onError(Throwable e) {
                    latch.countDown();
                }
                
                @Override
                public void onComplete() {
                    latch.countDown();
                }
            });
            
            assertTrue(latch.await(2, TimeUnit.SECONDS));
            assertThat(results).isEmpty();
        }
    }

    // ==================== OBSERVABLE CONCATMAP ====================
    
    @Nested
    @DisplayName("ObservableConcatMap")
    class ObservableConcatMapTests {
        
        @Test
        @DisplayName("Should concat values in order")
        void shouldConcatValuesInOrder() throws InterruptedException {
            List<Integer> results = Collections.synchronizedList(new ArrayList<>());
            CountDownLatch latch = new CountDownLatch(1);
            
            ObservableSource<Integer> source = sourceOf(1, 2, 3);
            ObservableConcatMap<Integer, Integer> concatMapped = new ObservableConcatMap<>(source, 
                x -> sourceOf(x * 10, x * 10 + 1));
            
            concatMapped.subscribe(new Observer<Integer>() {
                @Override
                public void onSubscribe(Disposable d) {}
                
                @Override
                public void onNext(Integer value) {
                    results.add(value);
                }
                
                @Override
                public void onError(Throwable e) {
                    latch.countDown();
                }
                
                @Override
                public void onComplete() {
                    latch.countDown();
                }
            });
            
            assertTrue(latch.await(2, TimeUnit.SECONDS));
            // ConcatMap should maintain order
            assertThat(results).containsExactly(10, 11, 20, 21, 30, 31);
        }
        
        @Test
        @DisplayName("Should emit error when mapper throws")
        void shouldEmitErrorWhenMapperThrows() throws InterruptedException {
            AtomicReference<Throwable> error = new AtomicReference<>();
            CountDownLatch latch = new CountDownLatch(1);
            
            ObservableSource<Integer> source = sourceOf(1, 2, 3);
            ObservableConcatMap<Integer, Integer> concatMapped = new ObservableConcatMap<>(source, x -> {
                if (x == 2) throw new RuntimeException("Mapper error");
                return sourceOf(x * 10);
            });
            
            concatMapped.subscribe(new Observer<Integer>() {
                @Override
                public void onSubscribe(Disposable d) {}
                
                @Override
                public void onNext(Integer value) {}
                
                @Override
                public void onError(Throwable e) {
                    error.set(e);
                    latch.countDown();
                }
                
                @Override
                public void onComplete() {
                    latch.countDown();
                }
            });
            
            assertTrue(latch.await(2, TimeUnit.SECONDS));
            assertThat(error.get()).isNotNull();
            assertThat(error.get().getMessage()).isEqualTo("Mapper error");
        }
    }

    // ==================== OBSERVABLE TAKE ====================
    
    @Nested
    @DisplayName("ObservableTake")
    class ObservableTakeTests {
        
        @Test
        @DisplayName("Should take first N values")
        void shouldTakeFirstNValues() {
            List<Integer> results = new ArrayList<>();
            AtomicBoolean completed = new AtomicBoolean(false);
            
            ObservableSource<Integer> source = sourceOf(1, 2, 3, 4, 5);
            ObservableTake<Integer> taken = new ObservableTake<>(source, 3);
            
            taken.subscribe(new Observer<Integer>() {
                @Override
                public void onSubscribe(Disposable d) {}
                
                @Override
                public void onNext(Integer value) {
                    results.add(value);
                }
                
                @Override
                public void onError(Throwable e) {}
                
                @Override
                public void onComplete() {
                    completed.set(true);
                }
            });
            
            assertThat(results).containsExactly(1, 2, 3);
            assertTrue(completed.get());
        }
        
        @Test
        @DisplayName("Should handle source with fewer items than take count")
        void shouldHandleFewerItems() {
            List<Integer> results = new ArrayList<>();
            AtomicBoolean completed = new AtomicBoolean(false);
            
            ObservableSource<Integer> source = sourceOf(1, 2);
            ObservableTake<Integer> taken = new ObservableTake<>(source, 5);
            
            taken.subscribe(new Observer<Integer>() {
                @Override
                public void onSubscribe(Disposable d) {}
                
                @Override
                public void onNext(Integer value) {
                    results.add(value);
                }
                
                @Override
                public void onError(Throwable e) {}
                
                @Override
                public void onComplete() {
                    completed.set(true);
                }
            });
            
            assertThat(results).containsExactly(1, 2);
            assertTrue(completed.get());
        }
        
        @Test
        @DisplayName("Should propagate error")
        void shouldPropagateError() {
            AtomicReference<Throwable> error = new AtomicReference<>();
            RuntimeException sourceError = new RuntimeException("Source error");
            
            ObservableSource<Integer> source = errorSource(sourceError);
            ObservableTake<Integer> taken = new ObservableTake<>(source, 5);
            
            taken.subscribe(new Observer<Integer>() {
                @Override
                public void onSubscribe(Disposable d) {}
                
                @Override
                public void onNext(Integer value) {}
                
                @Override
                public void onError(Throwable e) {
                    error.set(e);
                }
                
                @Override
                public void onComplete() {}
            });
            
            assertSame(sourceError, error.get());
        }
        
        @Test
        @DisplayName("Should take zero items")
        void shouldTakeZeroItems() {
            List<Integer> results = new ArrayList<>();
            AtomicBoolean completed = new AtomicBoolean(false);
            
            ObservableSource<Integer> source = sourceOf(1, 2, 3);
            ObservableTake<Integer> taken = new ObservableTake<>(source, 0);
            
            taken.subscribe(new Observer<Integer>() {
                @Override
                public void onSubscribe(Disposable d) {}
                
                @Override
                public void onNext(Integer value) {
                    results.add(value);
                }
                
                @Override
                public void onError(Throwable e) {}
                
                @Override
                public void onComplete() {
                    completed.set(true);
                }
            });
            
            assertThat(results).isEmpty();
            assertTrue(completed.get());
        }
    }

    // ==================== OBSERVABLE SKIP ====================
    
    @Nested
    @DisplayName("ObservableSkip")
    class ObservableSkipTests {
        
        @Test
        @DisplayName("Should skip first N values")
        void shouldSkipFirstNValues() {
            List<Integer> results = new ArrayList<>();
            AtomicBoolean completed = new AtomicBoolean(false);
            
            ObservableSource<Integer> source = sourceOf(1, 2, 3, 4, 5);
            ObservableSkip<Integer> skipped = new ObservableSkip<>(source, 2);
            
            skipped.subscribe(new Observer<Integer>() {
                @Override
                public void onSubscribe(Disposable d) {}
                
                @Override
                public void onNext(Integer value) {
                    results.add(value);
                }
                
                @Override
                public void onError(Throwable e) {}
                
                @Override
                public void onComplete() {
                    completed.set(true);
                }
            });
            
            assertThat(results).containsExactly(3, 4, 5);
            assertTrue(completed.get());
        }
        
        @Test
        @DisplayName("Should handle skipping more than available")
        void shouldHandleSkippingMoreThanAvailable() {
            List<Integer> results = new ArrayList<>();
            AtomicBoolean completed = new AtomicBoolean(false);
            
            ObservableSource<Integer> source = sourceOf(1, 2);
            ObservableSkip<Integer> skipped = new ObservableSkip<>(source, 5);
            
            skipped.subscribe(new Observer<Integer>() {
                @Override
                public void onSubscribe(Disposable d) {}
                
                @Override
                public void onNext(Integer value) {
                    results.add(value);
                }
                
                @Override
                public void onError(Throwable e) {}
                
                @Override
                public void onComplete() {
                    completed.set(true);
                }
            });
            
            assertThat(results).isEmpty();
            assertTrue(completed.get());
        }
        
        @Test
        @DisplayName("Should propagate error")
        void shouldPropagateError() {
            AtomicReference<Throwable> error = new AtomicReference<>();
            RuntimeException sourceError = new RuntimeException("Source error");
            
            ObservableSource<Integer> source = errorSource(sourceError);
            ObservableSkip<Integer> skipped = new ObservableSkip<>(source, 2);
            
            skipped.subscribe(new Observer<Integer>() {
                @Override
                public void onSubscribe(Disposable d) {}
                
                @Override
                public void onNext(Integer value) {}
                
                @Override
                public void onError(Throwable e) {
                    error.set(e);
                }
                
                @Override
                public void onComplete() {}
            });
            
            assertSame(sourceError, error.get());
        }
    }

    // ==================== OBSERVABLE DISTINCTUNTILCHANGED ====================
    
    @Nested
    @DisplayName("ObservableDistinctUntilChanged")
    class ObservableDistinctUntilChangedTests {
        
        @Test
        @DisplayName("Should emit consecutive distinct values only")
        void shouldEmitConsecutiveDistinctValuesOnly() {
            List<Integer> results = new ArrayList<>();
            AtomicBoolean completed = new AtomicBoolean(false);
            
            ObservableSource<Integer> source = sourceOf(1, 1, 2, 2, 1, 1, 3);
            ObservableDistinctUntilChanged<Integer> distinctUntilChanged = 
                new ObservableDistinctUntilChanged<>(source);
            
            distinctUntilChanged.subscribe(new Observer<Integer>() {
                @Override
                public void onSubscribe(Disposable d) {}
                
                @Override
                public void onNext(Integer value) {
                    results.add(value);
                }
                
                @Override
                public void onError(Throwable e) {}
                
                @Override
                public void onComplete() {
                    completed.set(true);
                }
            });
            
            assertThat(results).containsExactly(1, 2, 1, 3);
            assertTrue(completed.get());
        }
        
        @Test
        @DisplayName("Should emit all values when all are different")
        void shouldEmitAllWhenAllDifferent() {
            List<Integer> results = new ArrayList<>();
            AtomicBoolean completed = new AtomicBoolean(false);
            
            ObservableSource<Integer> source = sourceOf(1, 2, 3, 4, 5);
            ObservableDistinctUntilChanged<Integer> distinctUntilChanged = 
                new ObservableDistinctUntilChanged<>(source);
            
            distinctUntilChanged.subscribe(new Observer<Integer>() {
                @Override
                public void onSubscribe(Disposable d) {}
                
                @Override
                public void onNext(Integer value) {
                    results.add(value);
                }
                
                @Override
                public void onError(Throwable e) {}
                
                @Override
                public void onComplete() {
                    completed.set(true);
                }
            });
            
            assertThat(results).containsExactly(1, 2, 3, 4, 5);
            assertTrue(completed.get());
        }
        
        @Test
        @DisplayName("Should propagate error")
        void shouldPropagateError() {
            AtomicReference<Throwable> error = new AtomicReference<>();
            RuntimeException sourceError = new RuntimeException("Source error");
            
            ObservableSource<Integer> source = errorSource(sourceError);
            ObservableDistinctUntilChanged<Integer> distinctUntilChanged = 
                new ObservableDistinctUntilChanged<>(source);
            
            distinctUntilChanged.subscribe(new Observer<Integer>() {
                @Override
                public void onSubscribe(Disposable d) {}
                
                @Override
                public void onNext(Integer value) {}
                
                @Override
                public void onError(Throwable e) {
                    error.set(e);
                }
                
                @Override
                public void onComplete() {}
            });
            
            assertSame(sourceError, error.get());
        }
    }

    // ==================== OBSERVABLE DOONSUBSCRIBE ====================
    
    @Nested
    @DisplayName("ObservableDoOnSubscribe")
    class ObservableDoOnSubscribeTests {
        
        @Test
        @DisplayName("Should call action on subscribe")
        void shouldCallActionOnSubscribe() {
            AtomicBoolean actionCalled = new AtomicBoolean(false);
            AtomicReference<Disposable> actionDisposable = new AtomicReference<>();
            
            ObservableSource<Integer> source = sourceOf(1, 2, 3);
            ObservableDoOnSubscribe<Integer> doOnSubscribe = new ObservableDoOnSubscribe<>(source, d -> {
                actionCalled.set(true);
                actionDisposable.set(d);
            });
            
            doOnSubscribe.subscribe(new Observer<Integer>() {
                @Override
                public void onSubscribe(Disposable d) {}
                
                @Override
                public void onNext(Integer value) {}
                
                @Override
                public void onError(Throwable e) {}
                
                @Override
                public void onComplete() {}
            });
            
            assertTrue(actionCalled.get());
            assertNotNull(actionDisposable.get());
        }
        
        @Test
        @DisplayName("Should emit values correctly after action")
        void shouldEmitValuesCorrectlyAfterAction() {
            List<Integer> results = new ArrayList<>();
            AtomicBoolean completed = new AtomicBoolean(false);
            
            ObservableSource<Integer> source = sourceOf(1, 2, 3);
            ObservableDoOnSubscribe<Integer> doOnSubscribe = new ObservableDoOnSubscribe<>(source, d -> {});
            
            doOnSubscribe.subscribe(new Observer<Integer>() {
                @Override
                public void onSubscribe(Disposable d) {}
                
                @Override
                public void onNext(Integer value) {
                    results.add(value);
                }
                
                @Override
                public void onError(Throwable e) {}
                
                @Override
                public void onComplete() {
                    completed.set(true);
                }
            });
            
            assertThat(results).containsExactly(1, 2, 3);
            assertTrue(completed.get());
        }
    }

    // ==================== OBSERVABLE DOONCOMPLETE ====================
    
    @Nested
    @DisplayName("ObservableDoOnComplete")
    class ObservableDoOnCompleteTests {
        
        @Test
        @DisplayName("Should call action on complete")
        void shouldCallActionOnComplete() {
            AtomicBoolean actionCalled = new AtomicBoolean(false);
            
            ObservableSource<Integer> source = sourceOf(1, 2, 3);
            ObservableDoOnComplete<Integer> doOnComplete = new ObservableDoOnComplete<>(source, () -> {
                actionCalled.set(true);
            });
            
            doOnComplete.subscribe(new Observer<Integer>() {
                @Override
                public void onSubscribe(Disposable d) {}
                
                @Override
                public void onNext(Integer value) {}
                
                @Override
                public void onError(Throwable e) {}
                
                @Override
                public void onComplete() {}
            });
            
            assertTrue(actionCalled.get());
        }
        
        @Test
        @DisplayName("Should not call action on error")
        void shouldNotCallActionOnError() {
            AtomicBoolean actionCalled = new AtomicBoolean(false);
            RuntimeException sourceError = new RuntimeException("Source error");
            
            ObservableSource<Integer> source = errorSource(sourceError);
            ObservableDoOnComplete<Integer> doOnComplete = new ObservableDoOnComplete<>(source, () -> {
                actionCalled.set(true);
            });
            
            doOnComplete.subscribe(new Observer<Integer>() {
                @Override
                public void onSubscribe(Disposable d) {}
                
                @Override
                public void onNext(Integer value) {}
                
                @Override
                public void onError(Throwable e) {}
                
                @Override
                public void onComplete() {}
            });
            
            assertFalse(actionCalled.get());
        }
    }

    // ==================== OBSERVABLE DOONERROR ====================
    
    @Nested
    @DisplayName("ObservableDoOnError")
    class ObservableDoOnErrorTests {
        
        @Test
        @DisplayName("Should call action on error")
        void shouldCallActionOnError() {
            AtomicReference<Throwable> actionError = new AtomicReference<>();
            RuntimeException sourceError = new RuntimeException("Source error");
            
            ObservableSource<Integer> source = errorSource(sourceError);
            ObservableDoOnError<Integer> doOnError = new ObservableDoOnError<>(source, e -> {
                actionError.set(e);
            });
            
            doOnError.subscribe(new Observer<Integer>() {
                @Override
                public void onSubscribe(Disposable d) {}
                
                @Override
                public void onNext(Integer value) {}
                
                @Override
                public void onError(Throwable e) {}
                
                @Override
                public void onComplete() {}
            });
            
            assertSame(sourceError, actionError.get());
        }
        
        @Test
        @DisplayName("Should not call action on complete")
        void shouldNotCallActionOnComplete() {
            AtomicBoolean actionCalled = new AtomicBoolean(false);
            
            ObservableSource<Integer> source = sourceOf(1, 2, 3);
            ObservableDoOnError<Integer> doOnError = new ObservableDoOnError<>(source, e -> {
                actionCalled.set(true);
            });
            
            doOnError.subscribe(new Observer<Integer>() {
                @Override
                public void onSubscribe(Disposable d) {}
                
                @Override
                public void onNext(Integer value) {}
                
                @Override
                public void onError(Throwable e) {}
                
                @Override
                public void onComplete() {}
            });
            
            assertFalse(actionCalled.get());
        }
    }

    // ==================== OBSERVABLE DOONNEXT ====================
    
    @Nested
    @DisplayName("ObservableDoOnNext")
    class ObservableDoOnNextTests {
        
        @Test
        @DisplayName("Should call action for each value")
        void shouldCallActionForEachValue() {
            List<Integer> actionValues = new ArrayList<>();
            
            ObservableSource<Integer> source = sourceOf(1, 2, 3);
            ObservableDoOnNext<Integer> doOnNext = new ObservableDoOnNext<>(source, v -> {
                actionValues.add(v);
            });
            
            doOnNext.subscribe(new Observer<Integer>() {
                @Override
                public void onSubscribe(Disposable d) {}
                
                @Override
                public void onNext(Integer value) {}
                
                @Override
                public void onError(Throwable e) {}
                
                @Override
                public void onComplete() {}
            });
            
            assertThat(actionValues).containsExactly(1, 2, 3);
        }
        
        @Test
        @DisplayName("Should emit values correctly after action")
        void shouldEmitValuesCorrectlyAfterAction() {
            List<Integer> results = new ArrayList<>();
            AtomicBoolean completed = new AtomicBoolean(false);
            
            ObservableSource<Integer> source = sourceOf(1, 2, 3);
            ObservableDoOnNext<Integer> doOnNext = new ObservableDoOnNext<>(source, v -> {});
            
            doOnNext.subscribe(new Observer<Integer>() {
                @Override
                public void onSubscribe(Disposable d) {}
                
                @Override
                public void onNext(Integer value) {
                    results.add(value);
                }
                
                @Override
                public void onError(Throwable e) {}
                
                @Override
                public void onComplete() {
                    completed.set(true);
                }
            });
            
            assertThat(results).containsExactly(1, 2, 3);
            assertTrue(completed.get());
        }
    }

    // ==================== OBSERVABLE DEFAULTIFEMPTY ====================
    
    @Nested
    @DisplayName("ObservableDefaultIfEmpty")
    class ObservableDefaultIfEmptyTests {
        
        @Test
        @DisplayName("Should emit default value for empty source")
        void shouldEmitDefaultValueForEmptySource() {
            List<Integer> results = new ArrayList<>();
            AtomicBoolean completed = new AtomicBoolean(false);
            
            ObservableSource<Integer> source = emptySource();
            ObservableDefaultIfEmpty<Integer> defaultIfEmpty = new ObservableDefaultIfEmpty<>(source, 42);
            
            defaultIfEmpty.subscribe(new Observer<Integer>() {
                @Override
                public void onSubscribe(Disposable d) {}
                
                @Override
                public void onNext(Integer value) {
                    results.add(value);
                }
                
                @Override
                public void onError(Throwable e) {}
                
                @Override
                public void onComplete() {
                    completed.set(true);
                }
            });
            
            assertThat(results).containsExactly(42);
            assertTrue(completed.get());
        }
        
        @Test
        @DisplayName("Should emit source values when not empty")
        void shouldEmitSourceValuesWhenNotEmpty() {
            List<Integer> results = new ArrayList<>();
            AtomicBoolean completed = new AtomicBoolean(false);
            
            ObservableSource<Integer> source = sourceOf(1, 2, 3);
            ObservableDefaultIfEmpty<Integer> defaultIfEmpty = new ObservableDefaultIfEmpty<>(source, 42);
            
            defaultIfEmpty.subscribe(new Observer<Integer>() {
                @Override
                public void onSubscribe(Disposable d) {}
                
                @Override
                public void onNext(Integer value) {
                    results.add(value);
                }
                
                @Override
                public void onError(Throwable e) {}
                
                @Override
                public void onComplete() {
                    completed.set(true);
                }
            });
            
            assertThat(results).containsExactly(1, 2, 3);
            assertTrue(completed.get());
        }
    }

    // ==================== OBSERVABLE LAST ====================
    
    @Nested
    @DisplayName("ObservableLast")
    class ObservableLastTests {
        
        @Test
        @DisplayName("Should emit only the last value")
        void shouldEmitOnlyLastValue() {
            List<Integer> results = new ArrayList<>();
            AtomicBoolean completed = new AtomicBoolean(false);
            
            ObservableSource<Integer> source = sourceOf(1, 2, 3, 4, 5);
            ObservableLast<Integer> last = new ObservableLast<>(source, null);
            
            last.subscribe(new Observer<Integer>() {
                @Override
                public void onSubscribe(Disposable d) {}
                
                @Override
                public void onNext(Integer value) {
                    results.add(value);
                }
                
                @Override
                public void onError(Throwable e) {}
                
                @Override
                public void onComplete() {
                    completed.set(true);
                }
            });
            
            assertThat(results).containsExactly(5);
            assertTrue(completed.get());
        }
        
        @Test
        @DisplayName("Should handle single value")
        void shouldHandleSingleValue() {
            List<Integer> results = new ArrayList<>();
            AtomicBoolean completed = new AtomicBoolean(false);
            
            ObservableSource<Integer> source = sourceOf(42);
            ObservableLast<Integer> last = new ObservableLast<>(source, null);
            
            last.subscribe(new Observer<Integer>() {
                @Override
                public void onSubscribe(Disposable d) {}
                
                @Override
                public void onNext(Integer value) {
                    results.add(value);
                }
                
                @Override
                public void onError(Throwable e) {}
                
                @Override
                public void onComplete() {
                    completed.set(true);
                }
            });
            
            assertThat(results).containsExactly(42);
            assertTrue(completed.get());
        }
    }

    // ==================== OBSERVABLE ONERRORRETURN ====================
    
    @Nested
    @DisplayName("ObservableOnErrorReturn")
    class ObservableOnErrorReturnTests {
        
        @Test
        @DisplayName("Should return fallback value on error")
        void shouldReturnFallbackValueOnError() {
            List<Integer> results = new ArrayList<>();
            AtomicBoolean completed = new AtomicBoolean(false);
            RuntimeException sourceError = new RuntimeException("Source error");
            
            ObservableSource<Integer> source = errorSource(sourceError);
            ObservableOnErrorReturn<Integer> onErrorReturn = new ObservableOnErrorReturn<>(source, e -> -1);
            
            onErrorReturn.subscribe(new Observer<Integer>() {
                @Override
                public void onSubscribe(Disposable d) {}
                
                @Override
                public void onNext(Integer value) {
                    results.add(value);
                }
                
                @Override
                public void onError(Throwable e) {}
                
                @Override
                public void onComplete() {
                    completed.set(true);
                }
            });
            
            assertThat(results).containsExactly(-1);
            assertTrue(completed.get());
        }
        
        @Test
        @DisplayName("Should emit normal values when no error")
        void shouldEmitNormalValuesWhenNoError() {
            List<Integer> results = new ArrayList<>();
            AtomicBoolean completed = new AtomicBoolean(false);
            
            ObservableSource<Integer> source = sourceOf(1, 2, 3);
            ObservableOnErrorReturn<Integer> onErrorReturn = new ObservableOnErrorReturn<>(source, e -> -1);
            
            onErrorReturn.subscribe(new Observer<Integer>() {
                @Override
                public void onSubscribe(Disposable d) {}
                
                @Override
                public void onNext(Integer value) {
                    results.add(value);
                }
                
                @Override
                public void onError(Throwable e) {}
                
                @Override
                public void onComplete() {
                    completed.set(true);
                }
            });
            
            assertThat(results).containsExactly(1, 2, 3);
            assertTrue(completed.get());
        }
    }

    // ==================== OBSERVABLE ONERRORRESUMENEXT ====================
    
    @Nested
    @DisplayName("ObservableOnErrorResumeNext")
    class ObservableOnErrorResumeNextTests {
        
        @Test
        @DisplayName("Should resume with fallback observable on error")
        void shouldResumeWithFallbackOnError() {
            List<Integer> results = new ArrayList<>();
            AtomicBoolean completed = new AtomicBoolean(false);
            RuntimeException sourceError = new RuntimeException("Source error");
            
            ObservableSource<Integer> source = errorSource(sourceError);
            ObservableOnErrorResumeNext<Integer> onErrorResumeNext = 
                new ObservableOnErrorResumeNext<>(source, e -> sourceOf(10, 20, 30));
            
            onErrorResumeNext.subscribe(new Observer<Integer>() {
                @Override
                public void onSubscribe(Disposable d) {}
                
                @Override
                public void onNext(Integer value) {
                    results.add(value);
                }
                
                @Override
                public void onError(Throwable e) {}
                
                @Override
                public void onComplete() {
                    completed.set(true);
                }
            });
            
            assertThat(results).containsExactly(10, 20, 30);
            assertTrue(completed.get());
        }
        
        @Test
        @DisplayName("Should emit normal values when no error")
        void shouldEmitNormalValuesWhenNoError() {
            List<Integer> results = new ArrayList<>();
            AtomicBoolean completed = new AtomicBoolean(false);
            
            ObservableSource<Integer> source = sourceOf(1, 2, 3);
            ObservableOnErrorResumeNext<Integer> onErrorResumeNext = 
                new ObservableOnErrorResumeNext<>(source, e -> sourceOf(10, 20, 30));
            
            onErrorResumeNext.subscribe(new Observer<Integer>() {
                @Override
                public void onSubscribe(Disposable d) {}
                
                @Override
                public void onNext(Integer value) {
                    results.add(value);
                }
                
                @Override
                public void onError(Throwable e) {}
                
                @Override
                public void onComplete() {
                    completed.set(true);
                }
            });
            
            assertThat(results).containsExactly(1, 2, 3);
            assertTrue(completed.get());
        }
    }
}
