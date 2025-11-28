package com.reactive.core;

import org.junit.jupiter.api.Test;
import com.reactive.schedulers.Schedulers;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests para Maybe<T>
 */
public class MaybeTest {
    
    @Test
    public void testJust() {
        AtomicReference<Integer> result = new AtomicReference<>();
        AtomicBoolean completed = new AtomicBoolean(false);
        
        Maybe.just(42).subscribe(
            value -> result.set(value),
            error -> fail("No debería emitir error"),
            () -> completed.set(true)
        );
        
        assertEquals(42, result.get());
        assertFalse(completed.get()); // onSuccess no llama a onComplete
    }
    
    @Test
    public void testEmpty() {
        AtomicBoolean hasValue = new AtomicBoolean(false);
        AtomicBoolean completed = new AtomicBoolean(false);
        
        Maybe.empty().subscribe(
            value -> hasValue.set(true),
            error -> fail("No debería emitir error"),
            () -> completed.set(true)
        );
        
        assertFalse(hasValue.get());
        assertTrue(completed.get());
    }
    
    @Test
    public void testError() {
        AtomicReference<Throwable> error = new AtomicReference<>();
        
        Maybe.error(new RuntimeException("Test error")).subscribe(
            value -> fail("No debería emitir valor"),
            e -> error.set(e),
            () -> fail("No debería completar")
        );
        
        assertNotNull(error.get());
        assertEquals("Test error", error.get().getMessage());
    }
    
    @Test
    public void testFromCallableWithValue() {
        AtomicReference<String> result = new AtomicReference<>();
        
        Maybe.fromCallable(() -> "Hello").subscribe(
            value -> result.set(value),
            error -> fail("No debería emitir error"),
            () -> fail("No debería completar vacío")
        );
        
        assertEquals("Hello", result.get());
    }
    
    @Test
    public void testFromCallableWithNull() {
        AtomicBoolean completed = new AtomicBoolean(false);
        
        Maybe.fromCallable(() -> null).subscribe(
            value -> fail("No debería emitir valor"),
            error -> fail("No debería emitir error"),
            () -> completed.set(true)
        );
        
        assertTrue(completed.get());
    }
    
    @Test
    public void testFromSingle() {
        AtomicReference<Integer> result = new AtomicReference<>();
        
        Single<Integer> single = Single.just(42);
        Maybe.fromSingle(single).subscribe(
            value -> result.set(value),
            error -> fail("No debería emitir error"),
            () -> fail("No debería completar vacío")
        );
        
        assertEquals(42, result.get());
    }
    
    @Test
    public void testFromObservableWithValue() {
        AtomicReference<Integer> result = new AtomicReference<>();
        
        Observable<Integer> observable = Observable.just(1, 2, 3);
        Maybe.fromObservable(observable).subscribe(
            value -> result.set(value),
            error -> fail("No debería emitir error"),
            () -> fail("No debería completar vacío")
        );
        
        assertEquals(1, result.get());
    }
    
    @Test
    public void testFromObservableEmpty() {
        AtomicBoolean completed = new AtomicBoolean(false);
        
        Observable<Integer> observable = Observable.empty();
        Maybe.fromObservable(observable).subscribe(
            value -> fail("No debería emitir valor"),
            error -> fail("No debería emitir error"),
            () -> completed.set(true)
        );
        
        assertTrue(completed.get());
    }
    
    @Test
    public void testMap() {
        AtomicReference<Integer> result = new AtomicReference<>();
        
        Maybe.just(5)
            .map(x -> x * 2)
            .subscribe(
                value -> result.set(value),
                error -> fail("No debería emitir error"),
                () -> fail("No debería completar vacío")
            );
        
        assertEquals(10, result.get());
    }
    
    @Test
    public void testMapOnEmpty() {
        AtomicBoolean completed = new AtomicBoolean(false);
        
        Maybe.<Integer>empty()
            .map(x -> x * 2)
            .subscribe(
                value -> fail("No debería emitir valor"),
                error -> fail("No debería emitir error"),
                () -> completed.set(true)
            );
        
        assertTrue(completed.get());
    }
    
    @Test
    public void testFlatMap() {
        AtomicReference<String> result = new AtomicReference<>();
        
        Maybe.just(5)
            .flatMap(x -> Maybe.just("Value: " + x))
            .subscribe(
                value -> result.set(value),
                error -> fail("No debería emitir error"),
                () -> fail("No debería completar vacío")
            );
        
        assertEquals("Value: 5", result.get());
    }
    
    @Test
    public void testFlatMapToEmpty() {
        AtomicBoolean completed = new AtomicBoolean(false);
        
        Maybe.just(5)
            .flatMap(x -> Maybe.empty())
            .subscribe(
                value -> fail("No debería emitir valor"),
                error -> fail("No debería emitir error"),
                () -> completed.set(true)
            );
        
        assertTrue(completed.get());
    }
    
    @Test
    public void testFilter() {
        AtomicReference<Integer> result = new AtomicReference<>();
        
        Maybe.just(10)
            .filter(x -> x > 5)
            .subscribe(
                value -> result.set(value),
                error -> fail("No debería emitir error"),
                () -> fail("No debería completar vacío")
            );
        
        assertEquals(10, result.get());
    }
    
    @Test
    public void testFilterNotPass() {
        AtomicBoolean completed = new AtomicBoolean(false);
        
        Maybe.just(3)
            .filter(x -> x > 5)
            .subscribe(
                value -> fail("No debería emitir valor"),
                error -> fail("No debería emitir error"),
                () -> completed.set(true)
            );
        
        assertTrue(completed.get());
    }
    
    @Test
    public void testSwitchIfEmpty() {
        AtomicReference<Integer> result = new AtomicReference<>();
        
        Maybe.<Integer>empty()
            .switchIfEmpty(Maybe.just(42))
            .subscribe(
                value -> result.set(value),
                error -> fail("No debería emitir error"),
                () -> fail("No debería completar vacío")
            );
        
        assertEquals(42, result.get());
    }
    
    @Test
    public void testSwitchIfEmptyButHasValue() {
        AtomicReference<Integer> result = new AtomicReference<>();
        
        Maybe.just(10)
            .switchIfEmpty(Maybe.just(42))
            .subscribe(
                value -> result.set(value),
                error -> fail("No debería emitir error"),
                () -> fail("No debería completar vacío")
            );
        
        assertEquals(10, result.get());
    }
    
    @Test
    public void testDefaultIfEmpty() {
        AtomicReference<Integer> result = new AtomicReference<>();
        
        Maybe.<Integer>empty()
            .defaultIfEmpty(100)
            .subscribe(
                value -> result.set(value),
                error -> fail("No debería emitir error")
            );
        
        assertEquals(100, result.get());
    }
    
    @Test
    public void testDefaultIfEmptyButHasValue() {
        AtomicReference<Integer> result = new AtomicReference<>();
        
        Maybe.just(42)
            .defaultIfEmpty(100)
            .subscribe(
                value -> result.set(value),
                error -> fail("No debería emitir error")
            );
        
        assertEquals(42, result.get());
    }
    
    @Test
    public void testDefaultIfEmptyWithSupplier() {
        AtomicReference<Integer> result = new AtomicReference<>();
        
        Maybe.<Integer>empty()
            .defaultIfEmpty(() -> 200)
            .subscribe(
                value -> result.set(value),
                error -> fail("No debería emitir error")
            );
        
        assertEquals(200, result.get());
    }
    
    @Test
    public void testOnErrorReturn() {
        AtomicReference<Integer> result = new AtomicReference<>();
        
        Maybe.<Integer>error(new RuntimeException("Error"))
            .onErrorReturn(e -> -1)
            .subscribe(
                value -> result.set(value),
                error -> fail("No debería emitir error"),
                () -> fail("No debería completar vacío")
            );
        
        assertEquals(-1, result.get());
    }
    
    @Test
    public void testOnErrorResumeNext() {
        AtomicReference<Integer> result = new AtomicReference<>();
        
        Maybe.<Integer>error(new RuntimeException("Error"))
            .onErrorResumeNext(e -> Maybe.just(100))
            .subscribe(
                value -> result.set(value),
                error -> fail("No debería emitir error"),
                () -> fail("No debería completar vacío")
            );
        
        assertEquals(100, result.get());
    }
    
    @Test
    public void testOnErrorComplete() {
        AtomicBoolean completed = new AtomicBoolean(false);
        
        Maybe.<Integer>error(new RuntimeException("Error"))
            .onErrorComplete()
            .subscribe(
                value -> fail("No debería emitir valor"),
                error -> fail("No debería emitir error"),
                () -> completed.set(true)
            );
        
        assertTrue(completed.get());
    }
    
    @Test
    public void testDoOnSuccess() {
        AtomicReference<Integer> sideEffect = new AtomicReference<>();
        AtomicReference<Integer> result = new AtomicReference<>();
        
        Maybe.just(42)
            .doOnSuccess(value -> sideEffect.set(value * 2))
            .subscribe(
                value -> result.set(value),
                error -> fail("No debería emitir error"),
                () -> fail("No debería completar vacío")
            );
        
        assertEquals(42, result.get());
        assertEquals(84, sideEffect.get());
    }
    
    @Test
    public void testDoOnComplete() {
        AtomicBoolean sideEffect = new AtomicBoolean(false);
        AtomicBoolean completed = new AtomicBoolean(false);
        
        Maybe.empty()
            .doOnComplete(() -> sideEffect.set(true))
            .subscribe(
                value -> fail("No debería emitir valor"),
                error -> fail("No debería emitir error"),
                () -> completed.set(true)
            );
        
        assertTrue(completed.get());
        assertTrue(sideEffect.get());
    }
    
    @Test
    public void testToObservable() {
        AtomicReference<Integer> value = new AtomicReference<>();
        AtomicBoolean completed = new AtomicBoolean(false);
        
        Maybe.just(42)
            .toObservable()
            .subscribe(new Observer<Integer>() {
                @Override
                public void onNext(Integer v) {
                    value.set(v);
                }
                
                @Override
                public void onError(Throwable error) {
                    fail("No debería emitir error");
                }
                
                @Override
                public void onComplete() {
                    completed.set(true);
                }
            });
        
        assertEquals(42, value.get());
        assertTrue(completed.get());
    }
    
    @Test
    public void testToObservableEmpty() {
        AtomicBoolean hasValue = new AtomicBoolean(false);
        AtomicBoolean completed = new AtomicBoolean(false);
        
        Maybe.<Integer>empty()
            .toObservable()
            .subscribe(new Observer<Integer>() {
                @Override
                public void onNext(Integer v) {
                    hasValue.set(true);
                }
                
                @Override
                public void onError(Throwable error) {
                    fail("No debería emitir error");
                }
                
                @Override
                public void onComplete() {
                    completed.set(true);
                }
            });
        
        assertFalse(hasValue.get());
        assertTrue(completed.get());
    }
    
    @Test
    public void testToSingle() {
        AtomicReference<Integer> result = new AtomicReference<>();
        
        Maybe.just(42)
            .toSingle()
            .subscribe(
                value -> result.set(value),
                error -> fail("No debería emitir error")
            );
        
        assertEquals(42, result.get());
    }
    
    @Test
    public void testToSingleEmpty() {
        AtomicReference<Throwable> error = new AtomicReference<>();
        
        Maybe.empty()
            .toSingle()
            .subscribe(
                value -> fail("No debería emitir valor"),
                e -> error.set(e)
            );
        
        assertNotNull(error.get());
        assertTrue(error.get() instanceof java.util.NoSuchElementException);
    }
    
    @Test
    public void testDelay() throws InterruptedException {
        AtomicBoolean completed = new AtomicBoolean(false);
        long startTime = System.currentTimeMillis();
        
        Maybe.just(42)
            .delay(100, TimeUnit.MILLISECONDS)
            .subscribe(
                value -> completed.set(true),
                error -> fail("No debería emitir error"),
                () -> fail("No debería completar vacío")
            );
        
        assertFalse(completed.get());
        Thread.sleep(200);
        assertTrue(completed.get());
        
        long elapsed = System.currentTimeMillis() - startTime;
        assertTrue(elapsed >= 100, "Debería haber retrasado al menos 100ms");
    }
    
    @Test
    public void testSubscribeOn() throws InterruptedException {
        AtomicReference<String> threadName = new AtomicReference<>();
        AtomicBoolean completed = new AtomicBoolean(false);
        
        Maybe.fromCallable(() -> {
            threadName.set(Thread.currentThread().getName());
            return "test";
        })
        .subscribeOn(Schedulers.io())
        .subscribe(
            value -> completed.set(true),
            error -> fail("No debería emitir error"),
            () -> fail("No debería completar vacío")
        );
        
        Thread.sleep(200);
        assertTrue(completed.get());
        assertTrue(threadName.get().contains("RxIoScheduler"));
    }
    
    @Test
    public void testObserveOn() throws InterruptedException {
        AtomicReference<String> threadName = new AtomicReference<>();
        AtomicBoolean completed = new AtomicBoolean(false);
        
        Maybe.just(42)
            .observeOn(Schedulers.computation())
            .subscribe(
                value -> {
                    threadName.set(Thread.currentThread().getName());
                    completed.set(true);
                },
                error -> fail("No debería emitir error"),
                () -> fail("No debería completar vacío")
            );
        
        Thread.sleep(200);
        assertTrue(completed.get());
        assertTrue(threadName.get().contains("RxComputationScheduler"));
    }
}
