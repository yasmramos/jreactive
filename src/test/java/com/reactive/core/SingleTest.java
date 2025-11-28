package com.reactive.core;

import org.junit.jupiter.api.Test;
import com.reactive.schedulers.Schedulers;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests para Single<T>
 */
public class SingleTest {
    
    @Test
    public void testJust() {
        AtomicReference<Integer> result = new AtomicReference<>();
        
        Single.just(42).subscribe(
            value -> result.set(value),
            error -> fail("No debería emitir error")
        );
        
        assertEquals(42, result.get());
    }
    
    @Test
    public void testError() {
        AtomicReference<Throwable> error = new AtomicReference<>();
        
        Single.error(new RuntimeException("Test error")).subscribe(
            value -> fail("No debería emitir valor"),
            e -> error.set(e)
        );
        
        assertNotNull(error.get());
        assertEquals("Test error", error.get().getMessage());
    }
    
    @Test
    public void testFromCallable() {
        AtomicReference<String> result = new AtomicReference<>();
        
        Single.fromCallable(() -> "Hello").subscribe(
            value -> result.set(value),
            error -> fail("No debería emitir error")
        );
        
        assertEquals("Hello", result.get());
    }
    
    @Test
    public void testFromCallableWithError() {
        AtomicReference<Throwable> error = new AtomicReference<>();
        
        Single.fromCallable(() -> {
            throw new RuntimeException("Callable error");
        }).subscribe(
            value -> fail("No debería emitir valor"),
            e -> error.set(e)
        );
        
        assertNotNull(error.get());
        assertEquals("Callable error", error.get().getMessage());
    }
    
    @Test
    public void testFromObservable() {
        AtomicReference<Integer> result = new AtomicReference<>();
        
        Observable<Integer> observable = Observable.just(1, 2, 3);
        Single.fromObservable(observable).subscribe(
            value -> result.set(value),
            error -> fail("No debería emitir error")
        );
        
        assertEquals(1, result.get());
    }
    
    @Test
    public void testFromObservableEmpty() {
        AtomicReference<Throwable> error = new AtomicReference<>();
        
        Observable<Integer> observable = Observable.empty();
        Single.fromObservable(observable).subscribe(
            value -> fail("No debería emitir valor"),
            e -> error.set(e)
        );
        
        assertNotNull(error.get());
        assertTrue(error.get() instanceof java.util.NoSuchElementException);
    }
    
    @Test
    public void testMap() {
        AtomicReference<Integer> result = new AtomicReference<>();
        
        Single.just(5)
            .map(x -> x * 2)
            .subscribe(
                value -> result.set(value),
                error -> fail("No debería emitir error")
            );
        
        assertEquals(10, result.get());
    }
    
    @Test
    public void testMapWithError() {
        AtomicReference<Throwable> error = new AtomicReference<>();
        
        Single.just(5)
            .map(x -> {
                throw new RuntimeException("Map error");
            })
            .subscribe(
                value -> fail("No debería emitir valor"),
                e -> error.set(e)
            );
        
        assertNotNull(error.get());
        assertEquals("Map error", error.get().getMessage());
    }
    
    @Test
    public void testFlatMap() {
        AtomicReference<String> result = new AtomicReference<>();
        
        Single.just(5)
            .flatMap(x -> Single.just("Value: " + x))
            .subscribe(
                value -> result.set(value),
                error -> fail("No debería emitir error")
            );
        
        assertEquals("Value: 5", result.get());
    }
    
    @Test
    public void testFilter() {
        AtomicReference<Integer> result = new AtomicReference<>();
        
        Single.just(10)
            .filter(x -> x > 5)
            .subscribe(
                value -> result.set(value),
                error -> fail("No debería emitir error")
            );
        
        assertEquals(10, result.get());
    }
    
    @Test
    public void testFilterNotPass() {
        AtomicReference<Throwable> error = new AtomicReference<>();
        
        Single.just(3)
            .filter(x -> x > 5)
            .subscribe(
                value -> fail("No debería emitir valor"),
                e -> error.set(e)
            );
        
        assertNotNull(error.get());
        assertTrue(error.get() instanceof java.util.NoSuchElementException);
    }
    
    @Test
    public void testZipWith() {
        AtomicReference<String> result = new AtomicReference<>();
        
        Single<Integer> s1 = Single.just(5);
        Single<String> s2 = Single.just("Hello");
        
        s1.zipWith(s2, (a, b) -> b + " " + a)
            .subscribe(
                value -> result.set(value),
                error -> fail("No debería emitir error")
            );
        
        // Dar tiempo para que se ejecuten ambos singles
        try { Thread.sleep(100); } catch (InterruptedException e) {}
        
        assertEquals("Hello 5", result.get());
    }
    
    @Test
    public void testZip() {
        AtomicReference<Integer> result = new AtomicReference<>();
        
        Single<Integer> s1 = Single.just(5);
        Single<Integer> s2 = Single.just(10);
        
        Single.zip(s1, s2, (a, b) -> a + b)
            .subscribe(
                value -> result.set(value),
                error -> fail("No debería emitir error")
            );
        
        // Dar tiempo para que se ejecuten ambos singles
        try { Thread.sleep(100); } catch (InterruptedException e) {}
        
        assertEquals(15, result.get());
    }
    
    @Test
    public void testOnErrorReturn() {
        AtomicReference<Integer> result = new AtomicReference<>();
        
        Single.<Integer>error(new RuntimeException("Error"))
            .onErrorReturn(e -> -1)
            .subscribe(
                value -> result.set(value),
                error -> fail("No debería emitir error")
            );
        
        assertEquals(-1, result.get());
    }
    
    @Test
    public void testOnErrorResumeNext() {
        AtomicReference<Integer> result = new AtomicReference<>();
        
        Single.<Integer>error(new RuntimeException("Error"))
            .onErrorResumeNext(e -> Single.just(100))
            .subscribe(
                value -> result.set(value),
                error -> fail("No debería emitir error")
            );
        
        assertEquals(100, result.get());
    }
    
    @Test
    public void testRetry() {
        AtomicInteger attempts = new AtomicInteger(0);
        AtomicReference<Integer> result = new AtomicReference<>();
        
        Single.fromCallable(() -> {
            int count = attempts.incrementAndGet();
            if (count < 3) {
                throw new RuntimeException("Retry " + count);
            }
            return count;
        })
        .retry(2)
        .subscribe(
            value -> result.set(value),
            error -> fail("No debería emitir error después de reintentos")
        );
        
        assertEquals(3, result.get());
        assertEquals(3, attempts.get());
    }
    
    @Test
    public void testDoOnSuccess() {
        AtomicInteger sideEffect = new AtomicInteger(0);
        AtomicReference<Integer> result = new AtomicReference<>();
        
        Single.just(42)
            .doOnSuccess(value -> sideEffect.set(value * 2))
            .subscribe(
                value -> result.set(value),
                error -> fail("No debería emitir error")
            );
        
        assertEquals(42, result.get());
        assertEquals(84, sideEffect.get());
    }
    
    @Test
    public void testDoOnError() {
        AtomicBoolean errorHandled = new AtomicBoolean(false);
        
        Single.error(new RuntimeException("Test"))
            .doOnError(e -> errorHandled.set(true))
            .subscribe(
                value -> fail("No debería emitir valor"),
                error -> {}
            );
        
        assertTrue(errorHandled.get());
    }
    
    @Test
    public void testToObservable() {
        AtomicInteger count = new AtomicInteger(0);
        AtomicReference<Integer> value = new AtomicReference<>();
        AtomicBoolean completed = new AtomicBoolean(false);
        
        Single.just(42)
            .toObservable()
            .subscribe(new Observer<Integer>() {
                @Override
                public void onNext(Integer v) {
                    count.incrementAndGet();
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
        
        assertEquals(1, count.get());
        assertEquals(42, value.get());
        assertTrue(completed.get());
    }
    
    @Test
    public void testSubscribeOn() throws InterruptedException {
        AtomicReference<String> threadName = new AtomicReference<>();
        AtomicBoolean completed = new AtomicBoolean(false);
        
        Single.fromCallable(() -> {
            threadName.set(Thread.currentThread().getName());
            return "test";
        })
        .subscribeOn(Schedulers.io())
        .subscribe(
            value -> completed.set(true),
            error -> fail("No debería emitir error")
        );
        
        Thread.sleep(200);
        assertTrue(completed.get());
        assertTrue(threadName.get().contains("RxIoScheduler"));
    }
    
    @Test
    public void testObserveOn() throws InterruptedException {
        AtomicReference<String> threadName = new AtomicReference<>();
        AtomicBoolean completed = new AtomicBoolean(false);
        
        Single.just(42)
            .observeOn(Schedulers.computation())
            .subscribe(
                value -> {
                    threadName.set(Thread.currentThread().getName());
                    completed.set(true);
                },
                error -> fail("No debería emitir error")
            );
        
        Thread.sleep(200);
        assertTrue(completed.get());
        assertTrue(threadName.get().contains("RxComputationScheduler"));
    }
    
    @Test
    public void testDelay() throws InterruptedException {
        AtomicBoolean completed = new AtomicBoolean(false);
        long startTime = System.currentTimeMillis();
        
        Single.just(42)
            .delay(100, TimeUnit.MILLISECONDS)
            .subscribe(
                value -> {
                    completed.set(true);
                },
                error -> fail("No debería emitir error")
            );
        
        assertFalse(completed.get());
        Thread.sleep(200);
        assertTrue(completed.get());
        
        long elapsed = System.currentTimeMillis() - startTime;
        assertTrue(elapsed >= 100, "Debería haber retrasado al menos 100ms");
    }
    
    @Test
    public void testDispose() {
        AtomicBoolean emitted = new AtomicBoolean(false);
        
        Disposable d = Single.just(42).subscribe(
            value -> emitted.set(true),
            error -> fail("No debería emitir error")
        );
        
        assertTrue(d.isDisposed());
        assertTrue(emitted.get());
    }
}
