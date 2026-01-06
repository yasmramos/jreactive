package com.reactive.core;

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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for Completable type covering all operators
 * and edge cases for maximum code coverage.
 */
@DisplayName("Completable Comprehensive Tests")
public class CompletableComprehensiveTest {

    // ==================== CREATION OPERATORS ====================
    
    @Nested
    @DisplayName("Creation Operators")
    class CreationOperators {
        
        @Test
        @DisplayName("complete should complete immediately")
        void completeShouldCompleteImmediately() {
            AtomicBoolean completed = new AtomicBoolean(false);
            
            Completable.complete()
                .subscribe(() -> completed.set(true));
            
            assertTrue(completed.get());
        }
        
        @Test
        @DisplayName("error should emit error")
        void errorShouldEmitError() {
            AtomicReference<Throwable> error = new AtomicReference<>();
            RuntimeException expected = new RuntimeException("Test error");
            
            Completable.error(expected)
                .subscribe(() -> {}, error::set);
            
            assertEquals(expected, error.get());
        }
        
        @Test
        @DisplayName("fromAction should execute action and complete")
        void fromActionShouldExecuteActionAndComplete() {
            AtomicBoolean actionExecuted = new AtomicBoolean(false);
            AtomicBoolean completed = new AtomicBoolean(false);
            
            Completable.fromAction(() -> actionExecuted.set(true))
                .subscribe(() -> completed.set(true));
            
            assertTrue(actionExecuted.get());
            assertTrue(completed.get());
        }
        
        @Test
        @DisplayName("fromAction should emit error when action throws")
        void fromActionShouldEmitErrorWhenActionThrows() {
            AtomicReference<Throwable> error = new AtomicReference<>();
            
            Completable.fromAction(() -> {
                throw new RuntimeException("Action error");
            }).subscribe(() -> {}, error::set);
            
            assertEquals("Action error", error.get().getMessage());
        }
        
        @Test
        @DisplayName("fromCallable should execute callable and complete")
        void fromCallableShouldExecuteCallableAndComplete() {
            AtomicBoolean completed = new AtomicBoolean(false);
            
            Completable.fromCallable(() -> null)
                .subscribe(() -> completed.set(true));
            
            assertTrue(completed.get());
        }
        
        @Test
        @DisplayName("fromCallable should emit error when callable throws")
        void fromCallableShouldEmitErrorWhenCallableThrows() {
            AtomicReference<Throwable> error = new AtomicReference<>();
            
            Completable.fromCallable(() -> {
                throw new RuntimeException("Callable error");
            }).subscribe(() -> {}, error::set);
            
            assertEquals("Callable error", error.get().getMessage());
        }
        
        @Test
        @DisplayName("fromObservable should complete when Observable completes")
        void fromObservableShouldCompleteWhenObservableCompletes() {
            AtomicBoolean completed = new AtomicBoolean(false);
            
            Completable.fromObservable(Observable.just(1, 2, 3))
                .subscribe(() -> completed.set(true));
            
            assertTrue(completed.get());
        }
        
        @Test
        @DisplayName("fromObservable should ignore values")
        void fromObservableShouldIgnoreValues() {
            AtomicBoolean completed = new AtomicBoolean(false);
            
            // Values are emitted but ignored
            Completable.fromObservable(Observable.just("a", "b", "c"))
                .subscribe(() -> completed.set(true));
            
            assertTrue(completed.get());
        }
        
        @Test
        @DisplayName("fromObservable should propagate error")
        void fromObservableShouldPropagateError() {
            AtomicReference<Throwable> error = new AtomicReference<>();
            
            Completable.fromObservable(Observable.error(new RuntimeException("Observable error")))
                .subscribe(() -> {}, error::set);
            
            assertEquals("Observable error", error.get().getMessage());
        }
        
        @Test
        @DisplayName("fromSingle should complete when Single succeeds")
        void fromSingleShouldCompleteWhenSingleSucceeds() {
            AtomicBoolean completed = new AtomicBoolean(false);
            
            Completable.fromSingle(Single.just(42))
                .subscribe(() -> completed.set(true));
            
            assertTrue(completed.get());
        }
        
        @Test
        @DisplayName("fromSingle should propagate error")
        void fromSingleShouldPropagateError() {
            AtomicReference<Throwable> error = new AtomicReference<>();
            
            Completable.fromSingle(Single.<Integer>error(new RuntimeException("Single error")))
                .subscribe(() -> {}, error::set);
            
            assertEquals("Single error", error.get().getMessage());
        }
        
        @Test
        @DisplayName("fromMaybe should complete when Maybe succeeds")
        void fromMaybeShouldCompleteWhenMaybeSucceeds() {
            AtomicBoolean completed = new AtomicBoolean(false);
            
            Completable.fromMaybe(Maybe.just(42))
                .subscribe(() -> completed.set(true));
            
            assertTrue(completed.get());
        }
        
        @Test
        @DisplayName("fromMaybe should complete when Maybe is empty")
        void fromMaybeShouldCompleteWhenMaybeIsEmpty() {
            AtomicBoolean completed = new AtomicBoolean(false);
            
            Completable.fromMaybe(Maybe.empty())
                .subscribe(() -> completed.set(true));
            
            assertTrue(completed.get());
        }
        
        @Test
        @DisplayName("fromMaybe should propagate error")
        void fromMaybeShouldPropagateError() {
            AtomicReference<Throwable> error = new AtomicReference<>();
            
            Completable.fromMaybe(Maybe.<Integer>error(new RuntimeException("Maybe error")))
                .subscribe(() -> {}, error::set);
            
            assertEquals("Maybe error", error.get().getMessage());
        }
    }
    
    // ==================== COMBINATION OPERATORS ====================
    
    @Nested
    @DisplayName("Combination Operators")
    class CombinationOperators {
        
        @Test
        @DisplayName("andThen with Completable should chain sequentially")
        void andThenWithCompletableShouldChainSequentially() {
            List<Integer> order = new ArrayList<>();
            AtomicBoolean completed = new AtomicBoolean(false);
            
            Completable.fromAction(() -> order.add(1))
                .andThen(Completable.fromAction(() -> order.add(2)))
                .subscribe(() -> completed.set(true));
            
            assertTrue(completed.get());
            assertThat(order).containsExactly(1, 2);
        }
        
        @Test
        @DisplayName("andThen should propagate first Completable error")
        void andThenShouldPropagateFirstCompletableError() {
            AtomicReference<Throwable> error = new AtomicReference<>();
            
            Completable.error(new RuntimeException("First error"))
                .andThen(Completable.complete())
                .subscribe(() -> {}, error::set);
            
            assertEquals("First error", error.get().getMessage());
        }
        
        @Test
        @DisplayName("andThen with Observable should chain after completion")
        void andThenWithObservableShouldChainAfterCompletion() {
            List<Integer> results = new ArrayList<>();
            AtomicBoolean completed = new AtomicBoolean(false);
            
            Completable.complete()
                .andThen(Observable.just(1, 2, 3))
                .subscribe(
                    results::add,
                    e -> {},
                    () -> completed.set(true)
                );
            
            assertTrue(completed.get());
            assertThat(results).containsExactly(1, 2, 3);
        }
        
        @Test
        @DisplayName("andThen with Single should chain after completion")
        void andThenWithSingleShouldChainAfterCompletion() {
            AtomicReference<Integer> result = new AtomicReference<>();
            
            Completable.complete()
                .andThen(Single.just(42))
                .subscribe(result::set);
            
            assertEquals(42, result.get());
        }
        
        @Test
        @DisplayName("andThen with Maybe should chain after completion")
        void andThenWithMaybeShouldChainAfterCompletion() {
            AtomicReference<Integer> result = new AtomicReference<>();
            
            Completable.complete()
                .andThen(Maybe.just(42))
                .subscribe(result::set);
            
            assertEquals(42, result.get());
        }
        
        @Test
        @DisplayName("merge should execute completables concurrently")
        void mergeShouldExecuteCompletablesConcurrently() {
            AtomicBoolean completed = new AtomicBoolean(false);
            List<Integer> order = Collections.synchronizedList(new ArrayList<>());
            
            Completable.merge(
                Completable.fromAction(() -> order.add(1)),
                Completable.fromAction(() -> order.add(2)),
                Completable.fromAction(() -> order.add(3))
            ).subscribe(() -> completed.set(true));
            
            assertTrue(completed.get());
            assertEquals(3, order.size());
        }
        
        @Test
        @DisplayName("merge with empty array should complete immediately")
        void mergeWithEmptyArrayShouldCompleteImmediately() {
            AtomicBoolean completed = new AtomicBoolean(false);
            
            Completable.merge().subscribe(() -> completed.set(true));
            
            assertTrue(completed.get());
        }
        
        @Test
        @DisplayName("merge should propagate first error")
        void mergeShouldPropagateFirstError() {
            AtomicReference<Throwable> error = new AtomicReference<>();
            
            Completable.merge(
                Completable.complete(),
                Completable.error(new RuntimeException("Merge error")),
                Completable.complete()
            ).subscribe(() -> {}, error::set);
            
            assertEquals("Merge error", error.get().getMessage());
        }
        
        @Test
        @DisplayName("concat should execute completables sequentially")
        void concatShouldExecuteCompletablesSequentially() {
            List<Integer> order = new ArrayList<>();
            AtomicBoolean completed = new AtomicBoolean(false);
            
            Completable.concat(
                Completable.fromAction(() -> order.add(1)),
                Completable.fromAction(() -> order.add(2)),
                Completable.fromAction(() -> order.add(3))
            ).subscribe(() -> completed.set(true));
            
            assertTrue(completed.get());
            assertThat(order).containsExactly(1, 2, 3);
        }
        
        @Test
        @DisplayName("concat with empty array should complete immediately")
        void concatWithEmptyArrayShouldCompleteImmediately() {
            AtomicBoolean completed = new AtomicBoolean(false);
            
            Completable.concat().subscribe(() -> completed.set(true));
            
            assertTrue(completed.get());
        }
        
        @Test
        @DisplayName("concat should stop on first error")
        void concatShouldStopOnFirstError() {
            List<Integer> order = new ArrayList<>();
            AtomicReference<Throwable> error = new AtomicReference<>();
            
            Completable.concat(
                Completable.fromAction(() -> order.add(1)),
                Completable.error(new RuntimeException("Concat error")),
                Completable.fromAction(() -> order.add(3)) // Should not execute
            ).subscribe(() -> {}, error::set);
            
            assertEquals("Concat error", error.get().getMessage());
            assertThat(order).containsExactly(1);
        }
    }
    
    // ==================== ERROR HANDLING ====================
    
    @Nested
    @DisplayName("Error Handling")
    class ErrorHandling {
        
        @Test
        @DisplayName("onErrorResumeNext should switch to fallback")
        void onErrorResumeNextShouldSwitchToFallback() {
            AtomicBoolean fallbackExecuted = new AtomicBoolean(false);
            AtomicBoolean completed = new AtomicBoolean(false);
            
            Completable.error(new RuntimeException("Error"))
                .onErrorResumeNext(e -> Completable.fromAction(() -> fallbackExecuted.set(true)))
                .subscribe(() -> completed.set(true));
            
            assertTrue(fallbackExecuted.get());
            assertTrue(completed.get());
        }
        
        @Test
        @DisplayName("onErrorResumeNext should not affect success")
        void onErrorResumeNextShouldNotAffectSuccess() {
            AtomicBoolean fallbackExecuted = new AtomicBoolean(false);
            AtomicBoolean completed = new AtomicBoolean(false);
            
            Completable.complete()
                .onErrorResumeNext(e -> Completable.fromAction(() -> fallbackExecuted.set(true)))
                .subscribe(() -> completed.set(true));
            
            assertFalse(fallbackExecuted.get());
            assertTrue(completed.get());
        }
        
        @Test
        @DisplayName("onErrorResumeNext should emit error when resumeFunction throws")
        void onErrorResumeNextShouldEmitErrorWhenResumeFunctionThrows() {
            AtomicReference<Throwable> error = new AtomicReference<>();
            
            Completable.error(new RuntimeException("Original"))
                .onErrorResumeNext(e -> {
                    throw new RuntimeException("Resume error");
                })
                .subscribe(() -> {}, error::set);
            
            assertEquals("Resume error", error.get().getMessage());
        }
        
        @Test
        @DisplayName("onErrorComplete should complete on error")
        void onErrorCompleteShouldCompleteOnError() {
            AtomicBoolean completed = new AtomicBoolean(false);
            AtomicReference<Throwable> error = new AtomicReference<>();
            
            Completable.error(new RuntimeException("Error"))
                .onErrorComplete()
                .subscribe(() -> completed.set(true), error::set);
            
            assertTrue(completed.get());
            assertNull(error.get());
        }
        
        @Test
        @DisplayName("onErrorComplete should not affect success")
        void onErrorCompleteShouldNotAffectSuccess() {
            AtomicBoolean completed = new AtomicBoolean(false);
            
            Completable.complete()
                .onErrorComplete()
                .subscribe(() -> completed.set(true));
            
            assertTrue(completed.get());
        }
        
        @Test
        @DisplayName("retry should retry on error")
        void retryShouldRetryOnError() {
            AtomicInteger attempts = new AtomicInteger(0);
            AtomicBoolean completed = new AtomicBoolean(false);
            
            Completable.fromAction(() -> {
                if (attempts.incrementAndGet() < 3) {
                    throw new RuntimeException("Attempt " + attempts.get());
                }
            })
            .retry(3)
            .subscribe(() -> completed.set(true), e -> {});
            
            assertTrue(completed.get());
            assertEquals(3, attempts.get());
        }
        
        @Test
        @DisplayName("retry should fail after max retries")
        void retryShouldFailAfterMaxRetries() {
            AtomicInteger attempts = new AtomicInteger(0);
            AtomicReference<Throwable> error = new AtomicReference<>();
            
            Completable.fromAction(() -> {
                attempts.incrementAndGet();
                throw new RuntimeException("Always fails");
            })
            .retry(2)
            .subscribe(() -> {}, error::set);
            
            assertNotNull(error.get());
            assertEquals(3, attempts.get()); // 1 original + 2 retries
        }
    }
    
    // ==================== UTILITY OPERATORS ====================
    
    @Nested
    @DisplayName("Utility Operators")
    class UtilityOperators {
        
        @Test
        @DisplayName("doOnComplete should execute side effect")
        void doOnCompleteShouldExecuteSideEffect() {
            AtomicBoolean sideEffect = new AtomicBoolean(false);
            AtomicBoolean completed = new AtomicBoolean(false);
            
            Completable.complete()
                .doOnComplete(() -> sideEffect.set(true))
                .subscribe(() -> completed.set(true));
            
            assertTrue(sideEffect.get());
            assertTrue(completed.get());
        }
        
        @Test
        @DisplayName("doOnComplete should ignore exceptions in action")
        void doOnCompleteShouldIgnoreExceptionsInAction() {
            AtomicBoolean completed = new AtomicBoolean(false);
            
            Completable.complete()
                .doOnComplete(() -> {
                    throw new RuntimeException("Side effect error");
                })
                .subscribe(() -> completed.set(true));
            
            assertTrue(completed.get()); // Still completes
        }
        
        @Test
        @DisplayName("doOnError should execute side effect")
        void doOnErrorShouldExecuteSideEffect() {
            AtomicReference<Throwable> sideEffect = new AtomicReference<>();
            AtomicReference<Throwable> error = new AtomicReference<>();
            
            Completable.error(new RuntimeException("Original error"))
                .doOnError(sideEffect::set)
                .subscribe(() -> {}, error::set);
            
            assertEquals("Original error", sideEffect.get().getMessage());
            assertEquals("Original error", error.get().getMessage());
        }
        
        @Test
        @DisplayName("doOnError should not affect success")
        void doOnErrorShouldNotAffectSuccess() {
            AtomicReference<Throwable> sideEffect = new AtomicReference<>();
            AtomicBoolean completed = new AtomicBoolean(false);
            
            Completable.complete()
                .doOnError(sideEffect::set)
                .subscribe(() -> completed.set(true));
            
            assertNull(sideEffect.get());
            assertTrue(completed.get());
        }
        
        @Test
        @Timeout(5)
        @DisplayName("delay should delay completion")
        void delayShouldDelayCompletion() throws InterruptedException {
            AtomicBoolean completed = new AtomicBoolean(false);
            CountDownLatch latch = new CountDownLatch(1);
            long startTime = System.currentTimeMillis();
            AtomicReference<Long> elapsedTime = new AtomicReference<>();
            
            Completable.complete()
                .delay(100, TimeUnit.MILLISECONDS)
                .subscribe(() -> {
                    elapsedTime.set(System.currentTimeMillis() - startTime);
                    completed.set(true);
                    latch.countDown();
                }, e -> latch.countDown());
            
            assertTrue(latch.await(2, TimeUnit.SECONDS));
            assertTrue(completed.get());
            assertTrue(elapsedTime.get() >= 90);
        }
    }
    
    // ==================== CONVERSION OPERATORS ====================
    
    @Nested
    @DisplayName("Conversion Operators")
    class ConversionOperators {
        
        @Test
        @DisplayName("toObservable should create empty Observable")
        void toObservableShouldCreateEmptyObservable() {
            AtomicBoolean completed = new AtomicBoolean(false);
            List<Object> values = new ArrayList<>();
            
            Completable.complete()
                .<Object>toObservable()
                .subscribe(
                    values::add,
                    e -> {},
                    () -> completed.set(true)
                );
            
            assertTrue(completed.get());
            assertTrue(values.isEmpty());
        }
        
        @Test
        @DisplayName("toObservable should propagate error")
        void toObservableShouldPropagateError() {
            AtomicReference<Throwable> error = new AtomicReference<>();
            
            Completable.error(new RuntimeException("Test error"))
                .<Object>toObservable()
                .subscribe(v -> {}, error::set, () -> {});
            
            assertEquals("Test error", error.get().getMessage());
        }
    }
    
    // ==================== SUBSCRIPTION ====================
    
    @Nested
    @DisplayName("Subscription")
    class Subscription {
        
        @Test
        @DisplayName("subscribe with only onComplete should work")
        void subscribeWithOnlyOnCompleteShouldWork() {
            AtomicBoolean completed = new AtomicBoolean(false);
            
            Disposable d = Completable.complete()
                .subscribe(() -> completed.set(true));
            
            assertTrue(completed.get());
            assertNotNull(d);
        }
        
        @Test
        @DisplayName("disposed subscription should not receive events")
        void disposedSubscriptionShouldNotReceiveEvents() {
            AtomicBoolean completed = new AtomicBoolean(false);
            
            Disposable d = Completable.complete()
                .subscribe(() -> completed.set(true), e -> {});
            
            d.dispose();
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
            
            Completable.fromAction(() -> threadName.set(Thread.currentThread().getName()))
                .subscribeOn(com.reactive.schedulers.Schedulers.io())
                .subscribe(latch::countDown, e -> latch.countDown());
            
            assertTrue(latch.await(2, TimeUnit.SECONDS));
            assertNotNull(threadName.get());
        }
        
        @Test
        @Timeout(5)
        @DisplayName("observeOn should observe on specified scheduler")
        void observeOnShouldObserveOnSpecifiedScheduler() throws InterruptedException {
            AtomicReference<String> threadName = new AtomicReference<>();
            CountDownLatch latch = new CountDownLatch(1);
            
            Completable.complete()
                .observeOn(com.reactive.schedulers.Schedulers.io())
                .subscribe(() -> {
                    threadName.set(Thread.currentThread().getName());
                    latch.countDown();
                }, e -> latch.countDown());
            
            assertTrue(latch.await(2, TimeUnit.SECONDS));
            assertNotNull(threadName.get());
        }
    }
}
