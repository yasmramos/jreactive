package com.reactive.core;

import org.junit.jupiter.api.Test;
import com.reactive.schedulers.Schedulers;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests para Completable
 */
public class CompletableTest {
    
    @Test
    public void testComplete() {
        AtomicBoolean completed = new AtomicBoolean(false);
        
        Completable.complete().subscribe(
            () -> completed.set(true),
            error -> fail("No debería emitir error")
        );
        
        assertTrue(completed.get());
    }
    
    @Test
    public void testError() {
        AtomicReference<Throwable> error = new AtomicReference<>();
        
        Completable.error(new RuntimeException("Test error")).subscribe(
            () -> fail("No debería completar"),
            e -> error.set(e)
        );
        
        assertNotNull(error.get());
        assertEquals("Test error", error.get().getMessage());
    }
    
    @Test
    public void testFromAction() {
        AtomicBoolean actionExecuted = new AtomicBoolean(false);
        AtomicBoolean completed = new AtomicBoolean(false);
        
        Completable.fromAction(() -> actionExecuted.set(true)).subscribe(
            () -> completed.set(true),
            error -> fail("No debería emitir error")
        );
        
        assertTrue(actionExecuted.get());
        assertTrue(completed.get());
    }
    
    @Test
    public void testFromActionWithError() {
        AtomicReference<Throwable> error = new AtomicReference<>();
        
        Completable.fromAction(() -> {
            throw new RuntimeException("Action error");
        }).subscribe(
            () -> fail("No debería completar"),
            e -> error.set(e)
        );
        
        assertNotNull(error.get());
        assertEquals("Action error", error.get().getMessage());
    }
    
    @Test
    public void testFromCallable() {
        AtomicBoolean completed = new AtomicBoolean(false);
        
        Completable.fromCallable(() -> {
            // Hacer algo
            return null;
        }).subscribe(
            () -> completed.set(true),
            error -> fail("No debería emitir error")
        );
        
        assertTrue(completed.get());
    }
    
    @Test
    public void testFromObservable() {
        AtomicBoolean completed = new AtomicBoolean(false);
        
        Observable<Integer> observable = Observable.just(1, 2, 3);
        Completable.fromObservable(observable).subscribe(
            () -> completed.set(true),
            error -> fail("No debería emitir error")
        );
        
        assertTrue(completed.get());
    }
    
    @Test
    public void testFromSingle() {
        AtomicBoolean completed = new AtomicBoolean(false);
        
        Single<Integer> single = Single.just(42);
        Completable.fromSingle(single).subscribe(
            () -> completed.set(true),
            error -> fail("No debería emitir error")
        );
        
        assertTrue(completed.get());
    }
    
    @Test
    public void testFromMaybe() {
        AtomicBoolean completed = new AtomicBoolean(false);
        
        Maybe<Integer> maybe = Maybe.just(42);
        Completable.fromMaybe(maybe).subscribe(
            () -> completed.set(true),
            error -> fail("No debería emitir error")
        );
        
        assertTrue(completed.get());
    }
    
    @Test
    public void testFromMaybeEmpty() {
        AtomicBoolean completed = new AtomicBoolean(false);
        
        Maybe<Integer> maybe = Maybe.empty();
        Completable.fromMaybe(maybe).subscribe(
            () -> completed.set(true),
            error -> fail("No debería emitir error")
        );
        
        assertTrue(completed.get());
    }
    
    @Test
    public void testAndThen() {
        AtomicInteger counter = new AtomicInteger(0);
        AtomicBoolean completed = new AtomicBoolean(false);
        
        Completable.fromAction(() -> counter.incrementAndGet())
            .andThen(Completable.fromAction(() -> counter.incrementAndGet()))
            .subscribe(
                () -> completed.set(true),
                error -> fail("No debería emitir error")
            );
        
        assertEquals(2, counter.get());
        assertTrue(completed.get());
    }
    
    @Test
    public void testAndThenObservable() {
        AtomicReference<Integer> value = new AtomicReference<>();
        AtomicBoolean completed = new AtomicBoolean(false);
        
        Completable.complete()
            .andThen(Observable.just(42))
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
    public void testAndThenSingle() {
        AtomicReference<Integer> value = new AtomicReference<>();
        
        Completable.complete()
            .andThen(Single.just(42))
            .subscribe(
                v -> value.set(v),
                error -> fail("No debería emitir error")
            );
        
        assertEquals(42, value.get());
    }
    
    @Test
    public void testAndThenMaybe() {
        AtomicReference<Integer> value = new AtomicReference<>();
        
        Completable.complete()
            .andThen(Maybe.just(42))
            .subscribe(
                v -> value.set(v),
                error -> fail("No debería emitir error"),
                () -> fail("No debería completar vacío")
            );
        
        assertEquals(42, value.get());
    }
    
    @Test
    public void testMerge() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        AtomicBoolean completed = new AtomicBoolean(false);
        
        Completable c1 = Completable.fromAction(() -> counter.incrementAndGet());
        Completable c2 = Completable.fromAction(() -> counter.incrementAndGet());
        Completable c3 = Completable.fromAction(() -> counter.incrementAndGet());
        
        Completable.merge(c1, c2, c3).subscribe(
            () -> completed.set(true),
            error -> fail("No debería emitir error")
        );
        
        Thread.sleep(100);
        assertEquals(3, counter.get());
        assertTrue(completed.get());
    }
    
    @Test
    public void testMergeWithError() throws InterruptedException {
        AtomicReference<Throwable> error = new AtomicReference<>();
        
        Completable c1 = Completable.complete();
        Completable c2 = Completable.error(new RuntimeException("Error"));
        Completable c3 = Completable.complete();
        
        Completable.merge(c1, c2, c3).subscribe(
            () -> fail("No debería completar"),
            e -> error.set(e)
        );
        
        Thread.sleep(100);
        assertNotNull(error.get());
    }
    
    @Test
    public void testConcat() {
        AtomicInteger counter = new AtomicInteger(0);
        AtomicBoolean completed = new AtomicBoolean(false);
        
        Completable c1 = Completable.fromAction(() -> counter.set(counter.get() + 1));
        Completable c2 = Completable.fromAction(() -> counter.set(counter.get() * 2));
        Completable c3 = Completable.fromAction(() -> counter.set(counter.get() + 10));
        
        Completable.concat(c1, c2, c3).subscribe(
            () -> completed.set(true),
            error -> fail("No debería emitir error")
        );
        
        assertEquals(12, counter.get()); // (0 + 1) * 2 + 10 = 12
        assertTrue(completed.get());
    }
    
    @Test
    public void testOnErrorResumeNext() {
        AtomicBoolean completed = new AtomicBoolean(false);
        
        Completable.error(new RuntimeException("Error"))
            .onErrorResumeNext(e -> Completable.complete())
            .subscribe(
                () -> completed.set(true),
                error -> fail("No debería emitir error")
            );
        
        assertTrue(completed.get());
    }
    
    @Test
    public void testOnErrorComplete() {
        AtomicBoolean completed = new AtomicBoolean(false);
        
        Completable.error(new RuntimeException("Error"))
            .onErrorComplete()
            .subscribe(
                () -> completed.set(true),
                error -> fail("No debería emitir error")
            );
        
        assertTrue(completed.get());
    }
    
    @Test
    public void testRetry() {
        AtomicInteger attempts = new AtomicInteger(0);
        AtomicBoolean completed = new AtomicBoolean(false);
        
        Completable.fromAction(() -> {
            int count = attempts.incrementAndGet();
            if (count < 3) {
                throw new RuntimeException("Retry " + count);
            }
        })
        .retry(2)
        .subscribe(
            () -> completed.set(true),
            error -> fail("No debería emitir error después de reintentos")
        );
        
        assertTrue(completed.get());
        assertEquals(3, attempts.get());
    }
    
    @Test
    public void testDoOnComplete() {
        AtomicBoolean sideEffect = new AtomicBoolean(false);
        AtomicBoolean completed = new AtomicBoolean(false);
        
        Completable.complete()
            .doOnComplete(() -> sideEffect.set(true))
            .subscribe(
                () -> completed.set(true),
                error -> fail("No debería emitir error")
            );
        
        assertTrue(completed.get());
        assertTrue(sideEffect.get());
    }
    
    @Test
    public void testDoOnError() {
        AtomicBoolean errorHandled = new AtomicBoolean(false);
        
        Completable.error(new RuntimeException("Test"))
            .doOnError(e -> errorHandled.set(true))
            .subscribe(
                () -> fail("No debería completar"),
                error -> {}
            );
        
        assertTrue(errorHandled.get());
    }
    
    @Test
    public void testDelay() throws InterruptedException {
        AtomicBoolean completed = new AtomicBoolean(false);
        long startTime = System.currentTimeMillis();
        
        Completable.complete()
            .delay(100, TimeUnit.MILLISECONDS)
            .subscribe(
                () -> completed.set(true),
                error -> fail("No debería emitir error")
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
        
        Completable.fromAction(() -> {
            threadName.set(Thread.currentThread().getName());
        })
        .subscribeOn(Schedulers.io())
        .subscribe(
            () -> completed.set(true),
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
        
        Completable.complete()
            .observeOn(Schedulers.computation())
            .subscribe(
                () -> {
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
    public void testToObservable() {
        AtomicBoolean hasValue = new AtomicBoolean(false);
        AtomicBoolean completed = new AtomicBoolean(false);
        
        Completable.complete()
            .<Integer>toObservable()
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
    public void testChaining() {
        AtomicInteger counter = new AtomicInteger(0);
        AtomicBoolean completed = new AtomicBoolean(false);
        
        Completable.fromAction(() -> counter.incrementAndGet())
            .andThen(Completable.fromAction(() -> counter.incrementAndGet()))
            .andThen(Completable.fromAction(() -> counter.incrementAndGet()))
            .doOnComplete(() -> counter.incrementAndGet())
            .subscribe(
                () -> completed.set(true),
                error -> fail("No debería emitir error")
            );
        
        assertEquals(4, counter.get());
        assertTrue(completed.get());
    }
}
