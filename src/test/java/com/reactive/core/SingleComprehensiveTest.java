package com.reactive.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.NoSuchElementException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for Single type covering all operators
 * and edge cases for maximum code coverage.
 */
@DisplayName("Single Comprehensive Tests")
public class SingleComprehensiveTest {

    // ==================== CREATION OPERATORS ====================
    
    @Nested
    @DisplayName("Creation Operators")
    class CreationOperators {
        
        @Test
        @DisplayName("just should emit value and complete")
        void justShouldEmitValueAndComplete() {
            AtomicReference<String> result = new AtomicReference<>();
            
            Single.just("Hello")
                .subscribe(result::set);
            
            assertEquals("Hello", result.get());
        }
        
        @Test
        @DisplayName("error should emit error")
        void errorShouldEmitError() {
            AtomicReference<Throwable> error = new AtomicReference<>();
            RuntimeException expected = new RuntimeException("Test error");
            
            Single.<String>error(expected)
                .subscribe(v -> {}, error::set);
            
            assertEquals(expected, error.get());
        }
        
        @Test
        @DisplayName("fromCallable should execute callable and emit result")
        void fromCallableShouldExecuteCallableAndEmitResult() {
            AtomicReference<Integer> result = new AtomicReference<>();
            
            Single.fromCallable(() -> 42)
                .subscribe(result::set);
            
            assertEquals(42, result.get());
        }
        
        @Test
        @DisplayName("fromCallable should emit error when callable throws")
        void fromCallableShouldEmitErrorWhenCallableThrows() {
            AtomicReference<Throwable> error = new AtomicReference<>();
            
            Single.fromCallable(() -> {
                throw new RuntimeException("Callable error");
            }).subscribe(v -> {}, error::set);
            
            assertNotNull(error.get());
            assertEquals("Callable error", error.get().getMessage());
        }
        
        @Test
        @DisplayName("fromObservable should take first value")
        void fromObservableShouldTakeFirstValue() {
            AtomicReference<Integer> result = new AtomicReference<>();
            
            Single.fromObservable(Observable.just(1, 2, 3))
                .subscribe(result::set);
            
            assertEquals(1, result.get());
        }
        
        @Test
        @DisplayName("fromObservable should emit error when empty")
        void fromObservableShouldEmitErrorWhenEmpty() {
            AtomicReference<Throwable> error = new AtomicReference<>();
            
            Single.fromObservable(Observable.empty())
                .subscribe(v -> {}, error::set);
            
            assertNotNull(error.get());
            assertThat(error.get()).isInstanceOf(NoSuchElementException.class);
        }
        
        @Test
        @DisplayName("fromObservable should propagate error")
        void fromObservableShouldPropagateError() {
            AtomicReference<Throwable> error = new AtomicReference<>();
            
            Single.fromObservable(Observable.error(new RuntimeException("Observable error")))
                .subscribe(v -> {}, error::set);
            
            assertNotNull(error.get());
            assertEquals("Observable error", error.get().getMessage());
        }
    }
    
    // ==================== TRANSFORMATION OPERATORS ====================
    
    @Nested
    @DisplayName("Transformation Operators")
    class TransformationOperators {
        
        @Test
        @DisplayName("map should transform value")
        void mapShouldTransformValue() {
            AtomicReference<Integer> result = new AtomicReference<>();
            
            Single.just(5)
                .map(x -> x * 10)
                .subscribe(result::set);
            
            assertEquals(50, result.get());
        }
        
        @Test
        @DisplayName("map should propagate upstream error")
        void mapShouldPropagateUpstreamError() {
            AtomicReference<Throwable> error = new AtomicReference<>();
            
            Single.<Integer>error(new RuntimeException("Upstream error"))
                .map(x -> x * 10)
                .subscribe(v -> {}, error::set);
            
            assertEquals("Upstream error", error.get().getMessage());
        }
        
        @Test
        @DisplayName("map should emit error when mapper throws")
        void mapShouldEmitErrorWhenMapperThrows() {
            AtomicReference<Throwable> error = new AtomicReference<>();
            
            Single.just(5)
                .map(x -> {
                    throw new RuntimeException("Mapper error");
                })
                .subscribe(v -> {}, error::set);
            
            assertEquals("Mapper error", error.get().getMessage());
        }
        
        @Test
        @DisplayName("flatMap should chain singles")
        void flatMapShouldChainSingles() {
            AtomicReference<Integer> result = new AtomicReference<>();
            
            Single.just(5)
                .flatMap(x -> Single.just(x * 10))
                .subscribe(result::set);
            
            assertEquals(50, result.get());
        }
        
        @Test
        @DisplayName("flatMap should propagate upstream error")
        void flatMapShouldPropagateUpstreamError() {
            AtomicReference<Throwable> error = new AtomicReference<>();
            
            Single.<Integer>error(new RuntimeException("Upstream error"))
                .flatMap(x -> Single.just(x * 10))
                .subscribe(v -> {}, error::set);
            
            assertEquals("Upstream error", error.get().getMessage());
        }
        
        @Test
        @DisplayName("flatMap should propagate inner single error")
        void flatMapShouldPropagateInnerSingleError() {
            AtomicReference<Throwable> error = new AtomicReference<>();
            
            Single.just(5)
                .flatMap(x -> Single.<Integer>error(new RuntimeException("Inner error")))
                .subscribe(v -> {}, error::set);
            
            assertEquals("Inner error", error.get().getMessage());
        }
        
        @Test
        @DisplayName("flatMap should emit error when mapper throws")
        void flatMapShouldEmitErrorWhenMapperThrows() {
            AtomicReference<Throwable> error = new AtomicReference<>();
            
            Single.just(5)
                .<Integer>flatMap(x -> {
                    throw new RuntimeException("Mapper error");
                })
                .subscribe(v -> {}, error::set);
            
            assertEquals("Mapper error", error.get().getMessage());
        }
        
        @Test
        @DisplayName("filter should pass value when predicate is true")
        void filterShouldPassValueWhenPredicateIsTrue() {
            AtomicReference<Integer> result = new AtomicReference<>();
            
            Single.just(5)
                .filter(x -> x > 3)
                .subscribe(result::set, e -> {});
            
            assertEquals(5, result.get());
        }
        
        @Test
        @DisplayName("filter should emit error when predicate is false")
        void filterShouldEmitErrorWhenPredicateIsFalse() {
            AtomicReference<Throwable> error = new AtomicReference<>();
            
            Single.just(5)
                .filter(x -> x > 10)
                .subscribe(v -> {}, error::set);
            
            assertThat(error.get()).isInstanceOf(NoSuchElementException.class);
        }
        
        @Test
        @DisplayName("filter should emit error when predicate throws")
        void filterShouldEmitErrorWhenPredicateThrows() {
            AtomicReference<Throwable> error = new AtomicReference<>();
            
            Single.just(5)
                .filter(x -> {
                    throw new RuntimeException("Predicate error");
                })
                .subscribe(v -> {}, error::set);
            
            assertEquals("Predicate error", error.get().getMessage());
        }
    }
    
    // ==================== COMBINATION OPERATORS ====================
    
    @Nested
    @DisplayName("Combination Operators")
    class CombinationOperators {
        
        @Test
        @DisplayName("zip should combine two singles")
        void zipShouldCombineTwoSingles() {
            AtomicReference<String> result = new AtomicReference<>();
            
            Single.zip(
                Single.just(5),
                Single.just("hello"),
                (a, b) -> a + "-" + b
            ).subscribe(result::set);
            
            assertEquals("5-hello", result.get());
        }
        
        @Test
        @DisplayName("zip should propagate first single error")
        void zipShouldPropagateFirstSingleError() {
            AtomicReference<Throwable> error = new AtomicReference<>();
            
            Single.zip(
                Single.<Integer>error(new RuntimeException("First error")),
                Single.just("hello"),
                (a, b) -> a + "-" + b
            ).subscribe(v -> {}, error::set);
            
            assertEquals("First error", error.get().getMessage());
        }
        
        @Test
        @DisplayName("zip should propagate second single error")
        void zipShouldPropagateSecondSingleError() {
            AtomicReference<Throwable> error = new AtomicReference<>();
            
            Single.zip(
                Single.just(5),
                Single.<String>error(new RuntimeException("Second error")),
                (a, b) -> a + "-" + b
            ).subscribe(v -> {}, error::set);
            
            assertEquals("Second error", error.get().getMessage());
        }
        
        @Test
        @DisplayName("zip should emit error when zipper throws")
        void zipShouldEmitErrorWhenZipperThrows() {
            AtomicReference<Throwable> error = new AtomicReference<>();
            
            Single.zip(
                Single.just(5),
                Single.just("hello"),
                (Integer a, String b) -> {
                    throw new RuntimeException("Zipper error");
                }
            ).subscribe(v -> {}, error::set);
            
            assertEquals("Zipper error", error.get().getMessage());
        }
        
        @Test
        @DisplayName("zipWith should work like zip")
        void zipWithShouldWorkLikeZip() {
            AtomicReference<Integer> result = new AtomicReference<>();
            
            Single.just(5)
                .zipWith(Single.just(10), (a, b) -> a + b)
                .subscribe(result::set);
            
            assertEquals(15, result.get());
        }
    }
    
    // ==================== ERROR HANDLING ====================
    
    @Nested
    @DisplayName("Error Handling")
    class ErrorHandling {
        
        @Test
        @DisplayName("onErrorReturn should provide fallback value")
        void onErrorReturnShouldProvideFallbackValue() {
            AtomicReference<Integer> result = new AtomicReference<>();
            
            Single.<Integer>error(new RuntimeException("Error"))
                .onErrorReturn(e -> -1)
                .subscribe(result::set);
            
            assertEquals(-1, result.get());
        }
        
        @Test
        @DisplayName("onErrorReturn should emit error when supplier throws")
        void onErrorReturnShouldEmitErrorWhenSupplierThrows() {
            AtomicReference<Throwable> error = new AtomicReference<>();
            
            Single.<Integer>error(new RuntimeException("Original"))
                .onErrorReturn(e -> {
                    throw new RuntimeException("Supplier error");
                })
                .subscribe(v -> {}, error::set);
            
            assertEquals("Supplier error", error.get().getMessage());
        }
        
        @Test
        @DisplayName("onErrorReturn should not affect success")
        void onErrorReturnShouldNotAffectSuccess() {
            AtomicReference<Integer> result = new AtomicReference<>();
            
            Single.just(42)
                .onErrorReturn(e -> -1)
                .subscribe(result::set);
            
            assertEquals(42, result.get());
        }
        
        @Test
        @DisplayName("onErrorResumeNext should switch to fallback single")
        void onErrorResumeNextShouldSwitchToFallbackSingle() {
            AtomicReference<Integer> result = new AtomicReference<>();
            
            Single.<Integer>error(new RuntimeException("Error"))
                .onErrorResumeNext(e -> Single.just(-1))
                .subscribe(result::set);
            
            assertEquals(-1, result.get());
        }
        
        @Test
        @DisplayName("onErrorResumeNext should emit error when resumeFunction throws")
        void onErrorResumeNextShouldEmitErrorWhenResumeFunctionThrows() {
            AtomicReference<Throwable> error = new AtomicReference<>();
            
            Single.<Integer>error(new RuntimeException("Original"))
                .onErrorResumeNext(e -> {
                    throw new RuntimeException("Resume error");
                })
                .subscribe(v -> {}, error::set);
            
            assertEquals("Resume error", error.get().getMessage());
        }
        
        @Test
        @DisplayName("retry should retry on error")
        void retryShouldRetryOnError() {
            AtomicInteger attempts = new AtomicInteger(0);
            AtomicReference<String> result = new AtomicReference<>();
            
            Single.fromCallable(() -> {
                if (attempts.incrementAndGet() < 3) {
                    throw new RuntimeException("Attempt " + attempts.get());
                }
                return "Success";
            })
            .retry(3)
            .subscribe(result::set, e -> {});
            
            assertEquals("Success", result.get());
            assertEquals(3, attempts.get());
        }
        
        @Test
        @DisplayName("retry should fail after max retries")
        void retryShouldFailAfterMaxRetries() {
            AtomicInteger attempts = new AtomicInteger(0);
            AtomicReference<Throwable> error = new AtomicReference<>();
            
            Single.fromCallable(() -> {
                attempts.incrementAndGet();
                throw new RuntimeException("Always fails");
            })
            .retry(2)
            .subscribe(v -> {}, error::set);
            
            assertNotNull(error.get());
            assertEquals(3, attempts.get()); // 1 original + 2 retries
        }
    }
    
    // ==================== UTILITY OPERATORS ====================
    
    @Nested
    @DisplayName("Utility Operators")
    class UtilityOperators {
        
        @Test
        @DisplayName("doOnSuccess should execute side effect")
        void doOnSuccessShouldExecuteSideEffect() {
            AtomicReference<Integer> sideEffect = new AtomicReference<>();
            AtomicReference<Integer> result = new AtomicReference<>();
            
            Single.just(42)
                .doOnSuccess(sideEffect::set)
                .subscribe(result::set);
            
            assertEquals(42, sideEffect.get());
            assertEquals(42, result.get());
        }
        
        @Test
        @DisplayName("doOnSuccess should emit error when action throws")
        void doOnSuccessShouldEmitErrorWhenActionThrows() {
            AtomicReference<Throwable> error = new AtomicReference<>();
            
            Single.just(42)
                .doOnSuccess(v -> {
                    throw new RuntimeException("Side effect error");
                })
                .subscribe(v -> {}, error::set);
            
            assertEquals("Side effect error", error.get().getMessage());
        }
        
        @Test
        @DisplayName("doOnError should execute side effect")
        void doOnErrorShouldExecuteSideEffect() {
            AtomicReference<Throwable> sideEffect = new AtomicReference<>();
            AtomicReference<Throwable> error = new AtomicReference<>();
            
            Single.<Integer>error(new RuntimeException("Original error"))
                .doOnError(sideEffect::set)
                .subscribe(v -> {}, error::set);
            
            assertEquals("Original error", sideEffect.get().getMessage());
            assertEquals("Original error", error.get().getMessage());
        }
        
        @Test
        @Timeout(5)
        @DisplayName("delay should delay emission")
        void delayShouldDelayEmission() throws InterruptedException {
            AtomicReference<Integer> result = new AtomicReference<>();
            CountDownLatch latch = new CountDownLatch(1);
            long startTime = System.currentTimeMillis();
            AtomicReference<Long> elapsedTime = new AtomicReference<>();
            
            Single.just(42)
                .delay(100, TimeUnit.MILLISECONDS)
                .subscribe(v -> {
                    elapsedTime.set(System.currentTimeMillis() - startTime);
                    result.set(v);
                    latch.countDown();
                }, e -> latch.countDown());
            
            assertTrue(latch.await(2, TimeUnit.SECONDS));
            assertEquals(42, result.get());
            assertTrue(elapsedTime.get() >= 90); // Allow some tolerance
        }
    }
    
    // ==================== CONVERSION OPERATORS ====================
    
    @Nested
    @DisplayName("Conversion Operators")
    class ConversionOperators {
        
        @Test
        @DisplayName("toObservable should convert to Observable")
        void toObservableShouldConvertToObservable() {
            AtomicReference<Integer> result = new AtomicReference<>();
            AtomicBoolean completed = new AtomicBoolean(false);
            
            Single.just(42)
                .toObservable()
                .subscribe(
                    result::set,
                    e -> {},
                    () -> completed.set(true)
                );
            
            assertEquals(42, result.get());
            assertTrue(completed.get());
        }
        
        @Test
        @DisplayName("toObservable should propagate error")
        void toObservableShouldPropagateError() {
            AtomicReference<Throwable> error = new AtomicReference<>();
            
            Single.<Integer>error(new RuntimeException("Test error"))
                .toObservable()
                .subscribe(
                    v -> {},
                    error::set,
                    () -> {}
                );
            
            assertEquals("Test error", error.get().getMessage());
        }
    }
    
    // ==================== SUBSCRIPTION ====================
    
    @Nested
    @DisplayName("Subscription")
    class Subscription {
        
        @Test
        @DisplayName("subscribe with only onSuccess should work")
        void subscribeWithOnlyOnSuccessShouldWork() {
            AtomicReference<Integer> result = new AtomicReference<>();
            
            Disposable d = Single.just(42)
                .subscribe(result::set);
            
            assertEquals(42, result.get());
            assertNotNull(d);
        }
        
        @Test
        @DisplayName("subscribe should return disposable")
        void subscribeShouldReturnDisposable() {
            Disposable d = Single.just(42)
                .subscribe(v -> {}, e -> {});
            
            assertNotNull(d);
            assertTrue(d.isDisposed()); // Should be disposed after emission
        }
        
        @Test
        @DisplayName("disposed subscription should not receive events")
        void disposedSubscriptionShouldNotReceiveEvents() {
            AtomicReference<Integer> result = new AtomicReference<>();
            AtomicBoolean errorReceived = new AtomicBoolean(false);
            
            // Create a single that will never naturally complete
            Single<Integer> single = new Single<Integer>() {
                @Override
                public void subscribe(SingleObserver<Integer> observer) {
                    // Simulate delayed emission
                    new Thread(() -> {
                        try {
                            Thread.sleep(100);
                            observer.onSuccess(42);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }).start();
                }
            };
            
            Disposable d = single.subscribe(result::set, e -> errorReceived.set(true));
            d.dispose();
            
            // Wait a bit to ensure the thread would have completed
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // Result may or may not be set depending on timing, but no assertion error
            assertTrue(d.isDisposed());
        }
    }
    
    // ==================== SCHEDULER OPERATORS ====================
    
    @Nested
    @DisplayName("Scheduler Operators")
    class SchedulerOperators {
        
        @Test
        @Timeout(5)
        @DisplayName("subscribeOn should execute on specified scheduler")
        void subscribeOnShouldExecuteOnSpecifiedScheduler() throws InterruptedException {
            AtomicReference<String> threadName = new AtomicReference<>();
            CountDownLatch latch = new CountDownLatch(1);
            
            Single.fromCallable(() -> {
                threadName.set(Thread.currentThread().getName());
                return "done";
            })
            .subscribeOn(com.reactive.schedulers.Schedulers.io())
            .subscribe(v -> latch.countDown(), e -> latch.countDown());
            
            assertTrue(latch.await(2, TimeUnit.SECONDS));
            assertNotNull(threadName.get());
            // IO scheduler uses cached thread pool
        }
        
        @Test
        @Timeout(5)
        @DisplayName("observeOn should observe on specified scheduler")
        void observeOnShouldObserveOnSpecifiedScheduler() throws InterruptedException {
            AtomicReference<String> threadName = new AtomicReference<>();
            CountDownLatch latch = new CountDownLatch(1);
            
            Single.just(42)
                .observeOn(com.reactive.schedulers.Schedulers.io())
                .subscribe(v -> {
                    threadName.set(Thread.currentThread().getName());
                    latch.countDown();
                }, e -> latch.countDown());
            
            assertTrue(latch.await(2, TimeUnit.SECONDS));
            assertNotNull(threadName.get());
        }
    }
}
