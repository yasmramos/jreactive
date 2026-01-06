package com.reactive.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.NoSuchElementException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for Maybe type covering all operators
 * and edge cases for maximum code coverage.
 */
@DisplayName("Maybe Comprehensive Tests")
public class MaybeComprehensiveTest {

    // ==================== CREATION OPERATORS ====================
    
    @Nested
    @DisplayName("Creation Operators")
    class CreationOperators {
        
        @Test
        @DisplayName("just should emit value")
        void justShouldEmitValue() {
            AtomicReference<String> result = new AtomicReference<>();
            AtomicBoolean completed = new AtomicBoolean(false);
            
            Maybe.just("Hello")
                .subscribe(result::set, e -> {}, () -> completed.set(true));
            
            assertEquals("Hello", result.get());
            assertFalse(completed.get()); // Just emits success, not complete
        }
        
        @Test
        @DisplayName("empty should complete without value")
        void emptyShouldCompleteWithoutValue() {
            AtomicReference<String> result = new AtomicReference<>();
            AtomicBoolean completed = new AtomicBoolean(false);
            
            Maybe.<String>empty()
                .subscribe(result::set, e -> {}, () -> completed.set(true));
            
            assertNull(result.get());
            assertTrue(completed.get());
        }
        
        @Test
        @DisplayName("error should emit error")
        void errorShouldEmitError() {
            AtomicReference<Throwable> error = new AtomicReference<>();
            RuntimeException expected = new RuntimeException("Test error");
            
            Maybe.<String>error(expected)
                .subscribe(v -> {}, error::set, () -> {});
            
            assertEquals(expected, error.get());
        }
        
        @Test
        @DisplayName("fromCallable should emit non-null value")
        void fromCallableShouldEmitNonNullValue() {
            AtomicReference<Integer> result = new AtomicReference<>();
            
            Maybe.fromCallable(() -> 42)
                .subscribe(result::set);
            
            assertEquals(42, result.get());
        }
        
        @Test
        @DisplayName("fromCallable should complete empty for null value")
        void fromCallableShouldCompleteEmptyForNullValue() {
            AtomicBoolean completed = new AtomicBoolean(false);
            AtomicReference<Integer> result = new AtomicReference<>();
            
            Maybe.<Integer>fromCallable(() -> null)
                .subscribe(result::set, e -> {}, () -> completed.set(true));
            
            assertNull(result.get());
            assertTrue(completed.get());
        }
        
        @Test
        @DisplayName("fromCallable should emit error when callable throws")
        void fromCallableShouldEmitErrorWhenCallableThrows() {
            AtomicReference<Throwable> error = new AtomicReference<>();
            
            Maybe.fromCallable(() -> {
                throw new RuntimeException("Callable error");
            }).subscribe(v -> {}, error::set, () -> {});
            
            assertEquals("Callable error", error.get().getMessage());
        }
        
        @Test
        @DisplayName("fromSingle should convert Single to Maybe")
        void fromSingleShouldConvertSingleToMaybe() {
            AtomicReference<Integer> result = new AtomicReference<>();
            
            Maybe.fromSingle(Single.just(42))
                .subscribe(result::set);
            
            assertEquals(42, result.get());
        }
        
        @Test
        @DisplayName("fromSingle should propagate error")
        void fromSingleShouldPropagateError() {
            AtomicReference<Throwable> error = new AtomicReference<>();
            
            Maybe.fromSingle(Single.<Integer>error(new RuntimeException("Single error")))
                .subscribe(v -> {}, error::set, () -> {});
            
            assertEquals("Single error", error.get().getMessage());
        }
        
        @Test
        @DisplayName("fromObservable should take first value")
        void fromObservableShouldTakeFirstValue() {
            AtomicReference<Integer> result = new AtomicReference<>();
            
            Maybe.fromObservable(Observable.just(1, 2, 3))
                .subscribe(result::set);
            
            assertEquals(1, result.get());
        }
        
        @Test
        @DisplayName("fromObservable should complete empty when source is empty")
        void fromObservableShouldCompleteEmptyWhenSourceIsEmpty() {
            AtomicBoolean completed = new AtomicBoolean(false);
            
            Maybe.fromObservable(Observable.empty())
                .subscribe(v -> {}, e -> {}, () -> completed.set(true));
            
            assertTrue(completed.get());
        }
        
        @Test
        @DisplayName("fromObservable should propagate error")
        void fromObservableShouldPropagateError() {
            AtomicReference<Throwable> error = new AtomicReference<>();
            
            Maybe.fromObservable(Observable.error(new RuntimeException("Observable error")))
                .subscribe(v -> {}, error::set, () -> {});
            
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
            
            Maybe.just(5)
                .map(x -> x * 10)
                .subscribe(result::set);
            
            assertEquals(50, result.get());
        }
        
        @Test
        @DisplayName("map should propagate empty")
        void mapShouldPropagateEmpty() {
            AtomicBoolean completed = new AtomicBoolean(false);
            
            Maybe.<Integer>empty()
                .map(x -> x * 10)
                .subscribe(v -> {}, e -> {}, () -> completed.set(true));
            
            assertTrue(completed.get());
        }
        
        @Test
        @DisplayName("map should propagate error")
        void mapShouldPropagateError() {
            AtomicReference<Throwable> error = new AtomicReference<>();
            
            Maybe.<Integer>error(new RuntimeException("Upstream error"))
                .map(x -> x * 10)
                .subscribe(v -> {}, error::set, () -> {});
            
            assertEquals("Upstream error", error.get().getMessage());
        }
        
        @Test
        @DisplayName("map should emit error when mapper throws")
        void mapShouldEmitErrorWhenMapperThrows() {
            AtomicReference<Throwable> error = new AtomicReference<>();
            
            Maybe.just(5)
                .map(x -> {
                    throw new RuntimeException("Mapper error");
                })
                .subscribe(v -> {}, error::set, () -> {});
            
            assertEquals("Mapper error", error.get().getMessage());
        }
        
        @Test
        @DisplayName("flatMap should chain maybes")
        void flatMapShouldChainMaybes() {
            AtomicReference<Integer> result = new AtomicReference<>();
            
            Maybe.just(5)
                .flatMap(x -> Maybe.just(x * 10))
                .subscribe(result::set);
            
            assertEquals(50, result.get());
        }
        
        @Test
        @DisplayName("flatMap should propagate empty from upstream")
        void flatMapShouldPropagateEmptyFromUpstream() {
            AtomicBoolean completed = new AtomicBoolean(false);
            
            Maybe.<Integer>empty()
                .flatMap(x -> Maybe.just(x * 10))
                .subscribe(v -> {}, e -> {}, () -> completed.set(true));
            
            assertTrue(completed.get());
        }
        
        @Test
        @DisplayName("flatMap should propagate empty from inner")
        void flatMapShouldPropagateEmptyFromInner() {
            AtomicBoolean completed = new AtomicBoolean(false);
            
            Maybe.just(5)
                .flatMap(x -> Maybe.<Integer>empty())
                .subscribe(v -> {}, e -> {}, () -> completed.set(true));
            
            assertTrue(completed.get());
        }
        
        @Test
        @DisplayName("flatMap should emit error when mapper throws")
        void flatMapShouldEmitErrorWhenMapperThrows() {
            AtomicReference<Throwable> error = new AtomicReference<>();
            
            Maybe.just(5)
                .<Integer>flatMap(x -> {
                    throw new RuntimeException("Mapper error");
                })
                .subscribe(v -> {}, error::set, () -> {});
            
            assertEquals("Mapper error", error.get().getMessage());
        }
        
        @Test
        @DisplayName("filter should pass value when predicate is true")
        void filterShouldPassValueWhenPredicateIsTrue() {
            AtomicReference<Integer> result = new AtomicReference<>();
            
            Maybe.just(5)
                .filter(x -> x > 3)
                .subscribe(result::set);
            
            assertEquals(5, result.get());
        }
        
        @Test
        @DisplayName("filter should complete empty when predicate is false")
        void filterShouldCompleteEmptyWhenPredicateIsFalse() {
            AtomicBoolean completed = new AtomicBoolean(false);
            AtomicReference<Integer> result = new AtomicReference<>();
            
            Maybe.just(5)
                .filter(x -> x > 10)
                .subscribe(result::set, e -> {}, () -> completed.set(true));
            
            assertNull(result.get());
            assertTrue(completed.get());
        }
        
        @Test
        @DisplayName("filter should propagate empty")
        void filterShouldPropagateEmpty() {
            AtomicBoolean completed = new AtomicBoolean(false);
            
            Maybe.<Integer>empty()
                .filter(x -> x > 3)
                .subscribe(v -> {}, e -> {}, () -> completed.set(true));
            
            assertTrue(completed.get());
        }
        
        @Test
        @DisplayName("filter should emit error when predicate throws")
        void filterShouldEmitErrorWhenPredicateThrows() {
            AtomicReference<Throwable> error = new AtomicReference<>();
            
            Maybe.just(5)
                .filter(x -> {
                    throw new RuntimeException("Predicate error");
                })
                .subscribe(v -> {}, error::set, () -> {});
            
            assertEquals("Predicate error", error.get().getMessage());
        }
    }
    
    // ==================== CONDITIONAL OPERATORS ====================
    
    @Nested
    @DisplayName("Conditional Operators")
    class ConditionalOperators {
        
        @Test
        @DisplayName("switchIfEmpty should not switch when value exists")
        void switchIfEmptyShouldNotSwitchWhenValueExists() {
            AtomicReference<Integer> result = new AtomicReference<>();
            
            Maybe.just(5)
                .switchIfEmpty(Maybe.just(99))
                .subscribe(result::set);
            
            assertEquals(5, result.get());
        }
        
        @Test
        @DisplayName("switchIfEmpty should switch when empty")
        void switchIfEmptyShouldSwitchWhenEmpty() {
            AtomicReference<Integer> result = new AtomicReference<>();
            
            Maybe.<Integer>empty()
                .switchIfEmpty(Maybe.just(99))
                .subscribe(result::set);
            
            assertEquals(99, result.get());
        }
        
        @Test
        @DisplayName("switchIfEmpty should propagate error")
        void switchIfEmptyShouldPropagateError() {
            AtomicReference<Throwable> error = new AtomicReference<>();
            
            Maybe.<Integer>error(new RuntimeException("Error"))
                .switchIfEmpty(Maybe.just(99))
                .subscribe(v -> {}, error::set, () -> {});
            
            assertEquals("Error", error.get().getMessage());
        }
        
        @Test
        @DisplayName("defaultIfEmpty with value should emit default when empty")
        void defaultIfEmptyWithValueShouldEmitDefaultWhenEmpty() {
            AtomicReference<Integer> result = new AtomicReference<>();
            
            Maybe.<Integer>empty()
                .defaultIfEmpty(42)
                .subscribe(result::set);
            
            assertEquals(42, result.get());
        }
        
        @Test
        @DisplayName("defaultIfEmpty with value should emit original when present")
        void defaultIfEmptyWithValueShouldEmitOriginalWhenPresent() {
            AtomicReference<Integer> result = new AtomicReference<>();
            
            Maybe.just(5)
                .defaultIfEmpty(42)
                .subscribe(result::set);
            
            assertEquals(5, result.get());
        }
        
        @Test
        @DisplayName("defaultIfEmpty with supplier should work")
        void defaultIfEmptyWithSupplierShouldWork() {
            AtomicReference<Integer> result = new AtomicReference<>();
            
            Maybe.<Integer>empty()
                .defaultIfEmpty(() -> 42)
                .subscribe(result::set);
            
            assertEquals(42, result.get());
        }
        
        @Test
        @DisplayName("defaultIfEmpty with supplier should emit error when supplier throws")
        void defaultIfEmptyWithSupplierShouldEmitErrorWhenSupplierThrows() {
            AtomicReference<Throwable> error = new AtomicReference<>();
            
            Maybe.<Integer>empty()
                .defaultIfEmpty(() -> {
                    throw new RuntimeException("Supplier error");
                })
                .subscribe(v -> {}, error::set);
            
            assertEquals("Supplier error", error.get().getMessage());
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
            
            Maybe.<Integer>error(new RuntimeException("Error"))
                .onErrorReturn(e -> -1)
                .subscribe(result::set);
            
            assertEquals(-1, result.get());
        }
        
        @Test
        @DisplayName("onErrorReturn should not affect success")
        void onErrorReturnShouldNotAffectSuccess() {
            AtomicReference<Integer> result = new AtomicReference<>();
            
            Maybe.just(42)
                .onErrorReturn(e -> -1)
                .subscribe(result::set);
            
            assertEquals(42, result.get());
        }
        
        @Test
        @DisplayName("onErrorReturn should not affect empty")
        void onErrorReturnShouldNotAffectEmpty() {
            AtomicBoolean completed = new AtomicBoolean(false);
            
            Maybe.<Integer>empty()
                .onErrorReturn(e -> -1)
                .subscribe(v -> {}, e -> {}, () -> completed.set(true));
            
            assertTrue(completed.get());
        }
        
        @Test
        @DisplayName("onErrorReturn should emit error when supplier throws")
        void onErrorReturnShouldEmitErrorWhenSupplierThrows() {
            AtomicReference<Throwable> error = new AtomicReference<>();
            
            Maybe.<Integer>error(new RuntimeException("Original"))
                .onErrorReturn(e -> {
                    throw new RuntimeException("Supplier error");
                })
                .subscribe(v -> {}, error::set, () -> {});
            
            assertEquals("Supplier error", error.get().getMessage());
        }
        
        @Test
        @DisplayName("onErrorResumeNext should switch to fallback maybe")
        void onErrorResumeNextShouldSwitchToFallbackMaybe() {
            AtomicReference<Integer> result = new AtomicReference<>();
            
            Maybe.<Integer>error(new RuntimeException("Error"))
                .onErrorResumeNext(e -> Maybe.just(-1))
                .subscribe(result::set);
            
            assertEquals(-1, result.get());
        }
        
        @Test
        @DisplayName("onErrorResumeNext should emit error when resumeFunction throws")
        void onErrorResumeNextShouldEmitErrorWhenResumeFunctionThrows() {
            AtomicReference<Throwable> error = new AtomicReference<>();
            
            Maybe.<Integer>error(new RuntimeException("Original"))
                .onErrorResumeNext(e -> {
                    throw new RuntimeException("Resume error");
                })
                .subscribe(v -> {}, error::set, () -> {});
            
            assertEquals("Resume error", error.get().getMessage());
        }
        
        @Test
        @DisplayName("onErrorComplete should complete on error")
        void onErrorCompleteShouldCompleteOnError() {
            AtomicBoolean completed = new AtomicBoolean(false);
            AtomicReference<Throwable> error = new AtomicReference<>();
            
            Maybe.<Integer>error(new RuntimeException("Error"))
                .onErrorComplete()
                .subscribe(v -> {}, error::set, () -> completed.set(true));
            
            assertTrue(completed.get());
            assertNull(error.get());
        }
        
        @Test
        @DisplayName("onErrorComplete should not affect success")
        void onErrorCompleteShouldNotAffectSuccess() {
            AtomicReference<Integer> result = new AtomicReference<>();
            
            Maybe.just(42)
                .onErrorComplete()
                .subscribe(result::set);
            
            assertEquals(42, result.get());
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
            
            Maybe.just(42)
                .doOnSuccess(sideEffect::set)
                .subscribe(result::set);
            
            assertEquals(42, sideEffect.get());
            assertEquals(42, result.get());
        }
        
        @Test
        @DisplayName("doOnSuccess should emit error when action throws")
        void doOnSuccessShouldEmitErrorWhenActionThrows() {
            AtomicReference<Throwable> error = new AtomicReference<>();
            
            Maybe.just(42)
                .doOnSuccess(v -> {
                    throw new RuntimeException("Side effect error");
                })
                .subscribe(v -> {}, error::set, () -> {});
            
            assertEquals("Side effect error", error.get().getMessage());
        }
        
        @Test
        @DisplayName("doOnComplete should execute side effect")
        void doOnCompleteShouldExecuteSideEffect() {
            AtomicBoolean sideEffect = new AtomicBoolean(false);
            
            Maybe.empty()
                .doOnComplete(() -> sideEffect.set(true))
                .subscribe(v -> {}, e -> {}, () -> {});
            
            assertTrue(sideEffect.get());
        }
        
        @Test
        @Timeout(5)
        @DisplayName("delay should delay emission")
        void delayShouldDelayEmission() throws InterruptedException {
            AtomicReference<Integer> result = new AtomicReference<>();
            CountDownLatch latch = new CountDownLatch(1);
            long startTime = System.currentTimeMillis();
            AtomicReference<Long> elapsedTime = new AtomicReference<>();
            
            Maybe.just(42)
                .delay(100, TimeUnit.MILLISECONDS)
                .subscribe(v -> {
                    elapsedTime.set(System.currentTimeMillis() - startTime);
                    result.set(v);
                    latch.countDown();
                }, e -> latch.countDown(), latch::countDown);
            
            assertTrue(latch.await(2, TimeUnit.SECONDS));
            assertEquals(42, result.get());
            assertTrue(elapsedTime.get() >= 90);
        }
    }
    
    // ==================== CONVERSION OPERATORS ====================
    
    @Nested
    @DisplayName("Conversion Operators")
    class ConversionOperators {
        
        @Test
        @DisplayName("toObservable should convert value to Observable")
        void toObservableShouldConvertValueToObservable() {
            AtomicReference<Integer> result = new AtomicReference<>();
            AtomicBoolean completed = new AtomicBoolean(false);
            
            Maybe.just(42)
                .toObservable()
                .subscribe(result::set, e -> {}, () -> completed.set(true));
            
            assertEquals(42, result.get());
            assertTrue(completed.get());
        }
        
        @Test
        @DisplayName("toObservable should convert empty to empty Observable")
        void toObservableShouldConvertEmptyToEmptyObservable() {
            AtomicReference<Integer> result = new AtomicReference<>();
            AtomicBoolean completed = new AtomicBoolean(false);
            
            Maybe.<Integer>empty()
                .toObservable()
                .subscribe(result::set, e -> {}, () -> completed.set(true));
            
            assertNull(result.get());
            assertTrue(completed.get());
        }
        
        @Test
        @DisplayName("toObservable should propagate error")
        void toObservableShouldPropagateError() {
            AtomicReference<Throwable> error = new AtomicReference<>();
            
            Maybe.<Integer>error(new RuntimeException("Test error"))
                .toObservable()
                .subscribe(v -> {}, error::set, () -> {});
            
            assertEquals("Test error", error.get().getMessage());
        }
        
        @Test
        @DisplayName("toSingle should convert value to Single")
        void toSingleShouldConvertValueToSingle() {
            AtomicReference<Integer> result = new AtomicReference<>();
            
            Maybe.just(42)
                .toSingle()
                .subscribe(result::set);
            
            assertEquals(42, result.get());
        }
        
        @Test
        @DisplayName("toSingle should emit error when empty")
        void toSingleShouldEmitErrorWhenEmpty() {
            AtomicReference<Throwable> error = new AtomicReference<>();
            
            Maybe.<Integer>empty()
                .toSingle()
                .subscribe(v -> {}, error::set);
            
            assertThat(error.get()).isInstanceOf(NoSuchElementException.class);
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
            
            Disposable d = Maybe.just(42)
                .subscribe(result::set);
            
            assertEquals(42, result.get());
            assertNotNull(d);
        }
        
        @Test
        @DisplayName("disposed subscription should not receive events")
        void disposedSubscriptionShouldNotReceiveEvents() {
            AtomicReference<Integer> result = new AtomicReference<>();
            
            Disposable d = Maybe.just(42)
                .subscribe(result::set, e -> {}, () -> {});
            
            d.dispose();
            assertTrue(d.isDisposed());
        }
    }
}
