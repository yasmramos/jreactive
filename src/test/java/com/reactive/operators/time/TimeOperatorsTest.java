package com.reactive.operators.time;

import com.reactive.core.Observable;
import com.reactive.schedulers.Schedulers;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tests comprehensivos para operadores de tiempo: debounce, throttle, sample, delay
 * 
 * @author Yasmany Ramos García
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TimeOperatorsTest {
    
    private ScheduledExecutorService executor;
    
    @BeforeEach
    void setUp() {
        executor = Executors.newScheduledThreadPool(4);
    }
    
    @AfterEach
    void tearDown() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }
    
    // ============ DEBOUNCE TESTS ============
    
    @Test
    @Order(1)
    @DisplayName("Debounce: Emite solo después del período de silencio")
    void testDebounceBasic() throws InterruptedException {
        List<String> results = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch latch = new CountDownLatch(1);
        
        Observable<String> source = Observable.create(observer -> {
            executor.execute(() -> {
                try {
                    observer.onNext("A");
                    Thread.sleep(50);
                    observer.onNext("B");
                    Thread.sleep(50);
                    observer.onNext("C");
                    Thread.sleep(300); // Esperar más que el debounce timeout
                    observer.onNext("D");
                    Thread.sleep(50);
                    observer.onNext("E");
                    Thread.sleep(300); // Esperar más que el debounce timeout
                    observer.onComplete();
                } catch (Exception e) {
                    observer.onError(e);
                }
            });
        });
        
        source
            .debounce(200, TimeUnit.MILLISECONDS, Schedulers.computation())
            .subscribe(
                results::add,
                Throwable::printStackTrace,
                latch::countDown
            );
        
        assertTrue(latch.await(2, TimeUnit.SECONDS), "Should complete within 2 seconds");
        
        // Solo C y E deben ser emitidos (después de períodos de silencio)
        assertEquals(2, results.size(), "Should emit 2 debounced values");
        assertEquals("C", results.get(0));
        assertEquals("E", results.get(1));
    }
    
    @Test
    @Order(2)
    @DisplayName("Debounce: Emite último valor en onComplete")
    void testDebounceEmitsLastValueOnComplete() throws InterruptedException {
        List<Integer> results = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch latch = new CountDownLatch(1);
        
        Observable<Integer> source = Observable.create(observer -> {
            executor.execute(() -> {
                try {
                    observer.onNext(1);
                    Thread.sleep(50);
                    observer.onNext(2);
                    Thread.sleep(50);
                    observer.onNext(3);
                    // Complete antes que expire el debounce
                    observer.onComplete();
                } catch (Exception e) {
                    observer.onError(e);
                }
            });
        });
        
        source
            .debounce(200, TimeUnit.MILLISECONDS, Schedulers.computation())
            .subscribe(
                results::add,
                Throwable::printStackTrace,
                latch::countDown
            );
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        
        // El último valor (3) debe ser emitido en onComplete
        assertEquals(1, results.size());
        assertEquals(3, results.get(0));
    }
    
    @Test
    @Order(3)
    @DisplayName("Debounce: Maneja errores correctamente")
    void testDebounceHandlesErrors() throws InterruptedException {
        AtomicReference<Throwable> error = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        
        Observable<String> source = Observable.create(observer -> {
            executor.execute(() -> {
                try {
                    observer.onNext("A");
                    Thread.sleep(50);
                    observer.onError(new RuntimeException("Test error"));
                } catch (InterruptedException e) {
                    observer.onError(e);
                }
            });
        });
        
        source
            .debounce(100, TimeUnit.MILLISECONDS, Schedulers.computation())
            .subscribe(
                value -> {},
                e -> {
                    error.set(e);
                    latch.countDown();
                },
                () -> {}
            );
        
        assertTrue(latch.await(500, TimeUnit.MILLISECONDS));
        assertNotNull(error.get());
        assertEquals("Test error", error.get().getMessage());
    }
    
    // ============ THROTTLE FIRST TESTS ============
    
    @Test
    @Order(4)
    @DisplayName("ThrottleFirst: Emite primer elemento y luego ignora durante ventana")
    void testThrottleFirstBasic() throws InterruptedException {
        List<String> results = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch latch = new CountDownLatch(1);
        
        Observable<String> source = Observable.create(observer -> {
            executor.execute(() -> {
                try {
                    observer.onNext("A"); // t=0, emitido
                    Thread.sleep(50);
                    observer.onNext("B"); // t=50, ignorado (ventana activa)
                    Thread.sleep(100);
                    observer.onNext("C"); // t=150, ignorado (ventana activa)
                    Thread.sleep(100);
                    observer.onNext("D"); // t=250, emitido (ventana expiró)
                    Thread.sleep(50);
                    observer.onNext("E"); // t=300, ignorado (nueva ventana activa)
                    Thread.sleep(250);
                    observer.onNext("F"); // t=550, emitido (ventana expiró)
                    observer.onComplete();
                } catch (Exception e) {
                    observer.onError(e);
                }
            });
        });
        
        source
            .throttleFirst(200, TimeUnit.MILLISECONDS, Schedulers.computation())
            .subscribe(
                results::add,
                Throwable::printStackTrace,
                latch::countDown
            );
        
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        
        // A, D, F deben ser emitidos (primeros elementos después de cada ventana)
        assertEquals(3, results.size());
        assertEquals("A", results.get(0));
        assertEquals("D", results.get(1));
        assertEquals("F", results.get(2));
    }
    
    @Test
    @Order(5)
    @DisplayName("ThrottleFirst: No emite valores adicionales en onComplete")
    void testThrottleFirstDoesNotEmitOnComplete() throws InterruptedException {
        List<Integer> results = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch latch = new CountDownLatch(1);
        
        Observable<Integer> source = Observable.create(observer -> {
            executor.execute(() -> {
                try {
                    observer.onNext(1); // Emitido
                    Thread.sleep(50);
                    observer.onNext(2); // Ignorado
                    observer.onComplete();
                } catch (Exception e) {
                    observer.onError(e);
                }
            });
        });
        
        source
            .throttleFirst(200, TimeUnit.MILLISECONDS, Schedulers.computation())
            .subscribe(
                results::add,
                Throwable::printStackTrace,
                latch::countDown
            );
        
        assertTrue(latch.await(500, TimeUnit.MILLISECONDS));
        
        // Solo el primer valor debe ser emitido
        assertEquals(1, results.size());
        assertEquals(1, results.get(0));
    }
    
    // ============ SAMPLE / THROTTLE LAST TESTS ============
    
    @Test
    @Order(6)
    @DisplayName("Sample: Emite último valor en cada intervalo periódico")
    void testSampleBasic() throws InterruptedException {
        List<String> results = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch latch = new CountDownLatch(1);
        
        Observable<String> source = Observable.create(observer -> {
            executor.execute(() -> {
                try {
                    // Intervalo de sample: 200ms
                    observer.onNext("A"); // t=0
                    Thread.sleep(50);
                    observer.onNext("B"); // t=50
                    Thread.sleep(100);
                    observer.onNext("C"); // t=150  <- último antes de t=200, será emitido
                    Thread.sleep(100);
                    observer.onNext("D"); // t=250
                    Thread.sleep(100);
                    observer.onNext("E"); // t=350  <- último antes de t=400, será emitido
                    Thread.sleep(100);
                    observer.onNext("F"); // t=450
                    Thread.sleep(200);
                    observer.onComplete();
                } catch (Exception e) {
                    observer.onError(e);
                }
            });
        });
        
        source
            .sample(200, TimeUnit.MILLISECONDS, Schedulers.computation())
            .subscribe(
                results::add,
                Throwable::printStackTrace,
                latch::countDown
            );
        
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        
        // Debe emitir valores en intervalos de 200ms + último valor en complete
        assertTrue(results.size() >= 2, "Should emit at least 2 sampled values");
        assertTrue(results.contains("C") || results.contains("E"), 
            "Should contain sampled values");
    }
    
    @Test
    @Order(7)
    @DisplayName("Sample: Emite último valor en onComplete si no fue emitido")
    void testSampleEmitsLastValueOnComplete() throws InterruptedException {
        List<Integer> results = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch latch = new CountDownLatch(1);
        
        Observable<Integer> source = Observable.create(observer -> {
            executor.execute(() -> {
                try {
                    observer.onNext(1);
                    Thread.sleep(50);
                    observer.onNext(2);
                    Thread.sleep(50);
                    observer.onNext(3); // Último valor, debe emitirse en complete
                    Thread.sleep(50);
                    observer.onComplete();
                } catch (Exception e) {
                    observer.onError(e);
                }
            });
        });
        
        source
            .sample(500, TimeUnit.MILLISECONDS, Schedulers.computation())
            .subscribe(
                results::add,
                Throwable::printStackTrace,
                latch::countDown
            );
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        
        // El último valor debe ser emitido en onComplete
        assertEquals(1, results.size());
        assertEquals(3, results.get(0));
    }
    
    // ============ DELAY TESTS ============
    
    @Test
    @Order(8)
    @DisplayName("Delay: Retrasa cada elemento por el tiempo especificado")
    void testDelayBasic() throws InterruptedException {
        List<Long> timestamps = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch latch = new CountDownLatch(1);
        long startTime = System.currentTimeMillis();
        
        Observable.just("A", "B", "C")
            .delay(300, TimeUnit.MILLISECONDS, Schedulers.computation())
            .subscribe(
                value -> timestamps.add(System.currentTimeMillis() - startTime),
                Throwable::printStackTrace,
                latch::countDown
            );
        
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        
        assertEquals(3, timestamps.size());
        // Todos los timestamps deben ser >= 300ms
        for (Long timestamp : timestamps) {
            assertTrue(timestamp >= 250, 
                "Each element should be delayed by at least 250ms, got: " + timestamp);
        }
    }
    
    @Test
    @Order(9)
    @DisplayName("Delay: Preserva el orden de los elementos")
    void testDelayPreservesOrder() throws InterruptedException {
        List<Integer> results = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch latch = new CountDownLatch(1);
        
        Observable.just(1, 2, 3, 4, 5)
            .delay(100, TimeUnit.MILLISECONDS, Schedulers.computation())
            .subscribe(
                results::add,
                Throwable::printStackTrace,
                latch::countDown
            );
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        
        assertEquals(List.of(1, 2, 3, 4, 5), results, 
            "Elements should maintain their order after delay");
    }
    
    @Test
    @Order(10)
    @DisplayName("Delay: También retrasa onComplete")
    void testDelayDelaysCompletion() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean completed = new AtomicBoolean(false);
        long startTime = System.currentTimeMillis();
        AtomicReference<Long> completionTime = new AtomicReference<>();
        
        Observable.just("X")
            .delay(300, TimeUnit.MILLISECONDS, Schedulers.computation())
            .subscribe(
                value -> {},
                Throwable::printStackTrace,
                () -> {
                    completionTime.set(System.currentTimeMillis() - startTime);
                    completed.set(true);
                    latch.countDown();
                }
            );
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertTrue(completed.get());
        assertTrue(completionTime.get() >= 250, 
            "Completion should also be delayed, got: " + completionTime.get());
    }
    
    // ============ DELAY SUBSCRIPTION TESTS ============
    
    @Test
    @Order(11)
    @DisplayName("DelaySubscription: Retrasa la suscripción al Observable fuente")
    void testDelaySubscriptionBasic() throws InterruptedException {
        List<String> results = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger subscriptionCount = new AtomicInteger(0);
        long startTime = System.currentTimeMillis();
        AtomicReference<Long> firstEmissionTime = new AtomicReference<>();
        
        Observable<String> source = Observable.create(observer -> {
            subscriptionCount.incrementAndGet();
            observer.onNext("A");
            observer.onNext("B");
            observer.onComplete();
        });
        
        new ObservableDelaySubscription<>(source, 300, TimeUnit.MILLISECONDS, Schedulers.computation())
            .subscribe(
                value -> {
                    if (firstEmissionTime.get() == null) {
                        firstEmissionTime.set(System.currentTimeMillis() - startTime);
                    }
                    results.add(value);
                },
                Throwable::printStackTrace,
                latch::countDown
            );
        
        // La suscripción no debe ocurrir inmediatamente
        Thread.sleep(100);
        assertEquals(0, subscriptionCount.get(), "Should not subscribe immediately");
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        
        assertEquals(1, subscriptionCount.get(), "Should subscribe exactly once");
        assertEquals(List.of("A", "B"), results);
        assertTrue(firstEmissionTime.get() >= 250, 
            "First emission should occur after delay, got: " + firstEmissionTime.get());
    }
    
    // ============ INTEGRATION TESTS ============
    
    @Test
    @Order(12)
    @DisplayName("Integration: Debounce + Delay combinados")
    void testDebounceWithDelay() throws InterruptedException {
        List<String> results = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch latch = new CountDownLatch(1);
        
        Observable<String> source = Observable.create(observer -> {
            executor.execute(() -> {
                try {
                    observer.onNext("A");
                    Thread.sleep(50);
                    observer.onNext("B");
                    Thread.sleep(50);
                    observer.onNext("C");
                    Thread.sleep(300); // Trigger debounce
                    observer.onComplete();
                } catch (Exception e) {
                    observer.onError(e);
                }
            });
        });
        
        source
            .debounce(200, TimeUnit.MILLISECONDS, Schedulers.computation())
            .delay(100, TimeUnit.MILLISECONDS, Schedulers.computation())
            .subscribe(
                results::add,
                Throwable::printStackTrace,
                latch::countDown
            );
        
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        
        assertEquals(1, results.size());
        assertEquals("C", results.get(0));
    }
    
    @Test
    @Order(13)
    @DisplayName("Integration: ThrottleFirst + Sample combinados")
    void testThrottleFirstWithSample() throws InterruptedException {
        List<Integer> results = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch latch = new CountDownLatch(1);
        
        Observable<Integer> source = Observable.create(observer -> {
            executor.execute(() -> {
                try {
                    for (int i = 0; i < 20; i++) {
                        observer.onNext(i);
                        Thread.sleep(50);
                    }
                    observer.onComplete();
                } catch (Exception e) {
                    observer.onError(e);
                }
            });
        });
        
        source
            .throttleFirst(200, TimeUnit.MILLISECONDS, Schedulers.computation())
            .sample(300, TimeUnit.MILLISECONDS, Schedulers.computation())
            .subscribe(
                results::add,
                Throwable::printStackTrace,
                latch::countDown
            );
        
        assertTrue(latch.await(3, TimeUnit.SECONDS));
        
        // Debe haber reducido significativamente la cantidad de elementos
        assertTrue(results.size() < 10, 
            "Combined throttle and sample should significantly reduce elements, got: " + results.size());
    }
    
    @Test
    @Order(14)
    @DisplayName("Performance: Debounce con alta frecuencia de eventos")
    void testDebounceHighFrequency() throws InterruptedException {
        List<Integer> results = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch latch = new CountDownLatch(1);
        int eventCount = 1000;
        
        Observable<Integer> source = Observable.create(observer -> {
            executor.execute(() -> {
                try {
                    for (int i = 0; i < eventCount; i++) {
                        observer.onNext(i);
                        Thread.sleep(1); // Eventos muy rápidos
                    }
                    Thread.sleep(300); // Período de silencio para trigger debounce
                    observer.onComplete();
                } catch (Exception e) {
                    observer.onError(e);
                }
            });
        });
        
        source
            .debounce(100, TimeUnit.MILLISECONDS, Schedulers.computation())
            .subscribe(
                results::add,
                Throwable::printStackTrace,
                latch::countDown
            );
        
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        
        // Debounce debe reducir 1000 eventos a muy pocos
        assertTrue(results.size() < 10, 
            "Debounce should reduce 1000 rapid events to very few, got: " + results.size());
        assertEquals(eventCount - 1, results.get(results.size() - 1), 
            "Last event should be emitted");
    }
}
