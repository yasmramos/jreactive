package com.reactive.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tests unitarios para Observable
 */
public class ObservableTest {

    @Test
    @DisplayName("Observable.just debe emitir un solo valor")
    public void testJust() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> result = new AtomicReference<>();
        
        Observable.just("Hello")
            .subscribe(
                value -> {
                    result.set(value);
                    latch.countDown();
                },
                error -> fail("No debería haber error"),
                () -> {}
            );
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals("Hello", result.get());
    }

    @Test
    @DisplayName("Observable.just debe emitir todos los elementos")
    public void testFromArray() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(3);
        List<Integer> results = new ArrayList<>();
        
        Observable.just(1, 2, 3)
            .subscribe(
                value -> {
                    results.add(value);
                    latch.countDown();
                },
                error -> fail("No debería haber error"),
                () -> {}
            );
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(Arrays.asList(1, 2, 3), results);
    }

    @Test
    @DisplayName("Observable.range debe emitir secuencia de números")
    public void testRange() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(5);
        List<Integer> results = new ArrayList<>();
        
        Observable.range(1, 5)
            .subscribe(
                value -> {
                    results.add(value);
                    latch.countDown();
                },
                error -> fail("No debería haber error"),
                () -> {}
            );
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), results);
    }

    @Test
    @DisplayName("Observable.empty debe completar sin emitir valores")
    public void testEmpty() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean completed = new AtomicBoolean(false);
        AtomicInteger count = new AtomicInteger(0);
        
        Observable.empty()
            .subscribe(
                value -> count.incrementAndGet(),
                error -> fail("No debería haber error"),
                () -> {
                    completed.set(true);
                    latch.countDown();
                }
            );
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(0, count.get());
        assertTrue(completed.get());
    }

    @Test
    @DisplayName("Observable.error debe emitir error")
    public void testError() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        
        Observable.error(new RuntimeException("Test error"))
            .subscribe(
                value -> fail("No debería emitir valores"),
                e -> {
                    error.set(e);
                    latch.countDown();
                },
                () -> fail("No debería completar")
            );
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertNotNull(error.get());
        assertEquals("Test error", error.get().getMessage());
    }

    @Test
    @DisplayName("map debe transformar valores")
    public void testMap() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(3);
        List<Integer> results = new ArrayList<>();
        
        Observable.just(1, 2, 3)
            .map(x -> x * 2)
            .subscribe(
                value -> {
                    results.add(value);
                    latch.countDown();
                },
                error -> fail("No debería haber error"),
                () -> {}
            );
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(Arrays.asList(2, 4, 6), results);
    }

    @Test
    @DisplayName("filter debe filtrar valores")
    public void testFilter() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(2);
        List<Integer> results = new ArrayList<>();
        
        Observable.just(1, 2, 3, 4, 5)
            .filter(x -> x % 2 == 0)
            .subscribe(
                value -> {
                    results.add(value);
                    latch.countDown();
                },
                error -> fail("No debería haber error"),
                () -> {}
            );
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(Arrays.asList(2, 4), results);
    }

    @Test
    @DisplayName("flatMap debe aplanar observables")
    public void testFlatMap() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(6);
        List<Integer> results = new ArrayList<>();
        
        Observable.just(1, 2, 3)
            .flatMap(x -> Observable.just(x, x * 10))
            .subscribe(
                value -> {
                    results.add(value);
                    latch.countDown();
                },
                error -> fail("No debería haber error"),
                () -> {}
            );
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(6, results.size());
    }

    @Test
    @DisplayName("take debe limitar cantidad de elementos")
    public void testTake() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(3);
        List<Integer> results = new ArrayList<>();
        
        Observable.range(1, 10)
            .take(3)
            .subscribe(
                value -> {
                    results.add(value);
                    latch.countDown();
                },
                error -> fail("No debería haber error"),
                () -> {}
            );
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(Arrays.asList(1, 2, 3), results);
    }

    @Test
    @DisplayName("skip debe saltar elementos")
    public void testSkip() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(3);
        List<Integer> results = new ArrayList<>();
        
        Observable.range(1, 5)
            .skip(2)
            .subscribe(
                value -> {
                    results.add(value);
                    latch.countDown();
                },
                error -> fail("No debería haber error"),
                () -> {}
            );
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(Arrays.asList(3, 4, 5), results);
    }

    @Test
    @DisplayName("merge debe combinar múltiples observables")
    public void testMerge() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(6);
        List<Integer> results = new ArrayList<>();
        
        Observable<Integer> obs1 = Observable.just(1, 2, 3);
        Observable<Integer> obs2 = Observable.just(4, 5, 6);
        
        Observable.merge(obs1, obs2)
            .subscribe(
                value -> {
                    results.add(value);
                    latch.countDown();
                },
                error -> fail("No debería haber error"),
                () -> {}
            );
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(6, results.size());
    }

    @Test
    @DisplayName("zip debe combinar elementos correspondientes")
    public void testZip() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(3);
        List<String> results = new ArrayList<>();
        
        Observable<Integer> obs1 = Observable.just(1, 2, 3);
        Observable<String> obs2 = Observable.just("A", "B", "C");
        
        Observable.zip(obs1, obs2, (a, b) -> a + b)
            .subscribe(
                value -> {
                    results.add(value);
                    latch.countDown();
                },
                error -> fail("No debería haber error"),
                () -> {}
            );
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(Arrays.asList("1A", "2B", "3C"), results);
    }

    @Test
    @DisplayName("doOnNext debe ejecutar acción sin afectar flujo")
    public void testDoOnNext() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(3);
        AtomicInteger sideEffectCount = new AtomicInteger(0);
        List<Integer> results = new ArrayList<>();
        
        Observable.just(1, 2, 3)
            .doOnNext(x -> sideEffectCount.incrementAndGet())
            .subscribe(
                value -> {
                    results.add(value);
                    latch.countDown();
                },
                error -> fail("No debería haber error"),
                () -> {}
            );
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(3, sideEffectCount.get());
        assertEquals(Arrays.asList(1, 2, 3), results);
    }

    @Test
    @DisplayName("onErrorReturn debe manejar errores")
    public void testOnErrorReturn() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger result = new AtomicInteger(0);
        
        Observable.create(emitter -> {
                emitter.onNext(1);
                emitter.onError(new RuntimeException("Error"));
            })
            .onErrorReturn((Throwable error) -> 999)
            .subscribe(
                value -> {
                    result.set((Integer)value);
                    latch.countDown();
                },
                error -> fail("No debería haber error"),
                () -> {}
            );
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(999, result.get());
    }

    @Test
    @DisplayName("Disposable debe cancelar suscripción")
    @Timeout(2)
    public void testDisposable() throws InterruptedException {
        AtomicInteger count = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);
        
        Disposable disposable = Observable.interval(100, TimeUnit.MILLISECONDS)
            .subscribe(
                value -> {
                    count.incrementAndGet();
                    if (count.get() == 3) {
                        latch.countDown();
                    }
                },
                error -> {},
                () -> {}
            );
        
        // Esperar a que emita 3 valores
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        
        // Cancelar suscripción
        disposable.dispose();
        int countBeforeDispose = count.get();
        
        // Esperar un poco más
        Thread.sleep(500);
        
        // El count no debería haber cambiado mucho después de dispose
        assertTrue(count.get() <= countBeforeDispose + 2); // Puede haber 1-2 más por timing
    }
}
