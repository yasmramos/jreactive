package com.reactive.reactivestreams;

import com.reactive.core.Observable;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for Project Reactor (Flux/Mono) integration.
 * Tests conversions between Observable and Reactor types.
 * 
 * Requires reactor-core dependency.
 */
public class ReactorIntegrationTest {
    
    // ============ toFlux() Tests ============
    
    @Test
    public void testToFlux_WithMultipleValues() {
        Object fluxObj = Observable.just(1, 2, 3, 4, 5).toFlux();
        
        // Verify it's actually a Flux
        assertTrue(fluxObj instanceof Flux);
        
        @SuppressWarnings("unchecked")
        Flux<Integer> flux = (Flux<Integer>) fluxObj;
        
        List<Integer> result = flux.collectList().block();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }
    
    @Test
    public void testToFlux_WithEmpty() {
        Object fluxObj = Observable.<Integer>empty().toFlux();
        
        @SuppressWarnings("unchecked")
        Flux<Integer> flux = (Flux<Integer>) fluxObj;
        
        List<Integer> result = flux.collectList().block();
        assertTrue(result.isEmpty());
    }
    
    @Test
    public void testToFlux_WithError() {
        RuntimeException testError = new RuntimeException("Test error");
        Object fluxObj = Observable.<Integer>error(testError).toFlux();
        
        @SuppressWarnings("unchecked")
        Flux<Integer> flux = (Flux<Integer>) fluxObj;
        
        assertThrows(RuntimeException.class, () -> {
            flux.blockFirst();
        });
    }
    
    @Test
    public void testToFlux_WithTransformation() {
        Object fluxObj = Observable.range(1, 10).toFlux();
        
        @SuppressWarnings("unchecked")
        Flux<Integer> flux = (Flux<Integer>) fluxObj;
        
        List<Integer> result = flux
            .filter(x -> x % 2 == 0)
            .map(x -> x * 2)
            .collectList()
            .block();
        
        assertEquals(Arrays.asList(4, 8, 12, 16, 20), result);
    }
    
    // ============ toMono() Tests ============
    
    @Test
    public void testToMono_WithSingleValue() {
        Object monoObj = Observable.just(42).toMono();
        
        assertTrue(monoObj instanceof Mono);
        
        @SuppressWarnings("unchecked")
        Mono<Integer> mono = (Mono<Integer>) monoObj;
        
        Integer result = mono.block();
        assertEquals(Integer.valueOf(42), result);
    }
    
    @Test
    public void testToMono_WithMultipleValues_TakesFirst() {
        Object monoObj = Observable.just(1, 2, 3, 4, 5).toMono();
        
        @SuppressWarnings("unchecked")
        Mono<Integer> mono = (Mono<Integer>) monoObj;
        
        Integer result = mono.block();
        assertEquals(Integer.valueOf(1), result);
    }
    
    @Test
    public void testToMono_WithEmpty() {
        Object monoObj = Observable.<Integer>empty().toMono();
        
        @SuppressWarnings("unchecked")
        Mono<Integer> mono = (Mono<Integer>) monoObj;
        
        Integer result = mono.block();
        assertNull(result);
    }
    
    @Test
    public void testToMono_WithError() {
        RuntimeException testError = new RuntimeException("Test error");
        Object monoObj = Observable.<Integer>error(testError).toMono();
        
        @SuppressWarnings("unchecked")
        Mono<Integer> mono = (Mono<Integer>) monoObj;
        
        assertThrows(RuntimeException.class, () -> {
            mono.block();
        });
    }
    
    // ============ fromFlux() Tests ============
    
    @Test
    public void testFromFlux_WithMultipleValues() {
        Flux<Integer> flux = Flux.just(1, 2, 3, 4, 5);
        
        Observable<Integer> observable = Observable.fromFlux(flux);
        
        List<Integer> result = observable.toList().blockingFirst();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }
    
    @Test
    public void testFromFlux_WithEmpty() {
        Flux<Integer> flux = Flux.empty();
        
        Observable<Integer> observable = Observable.fromFlux(flux);
        
        List<Integer> result = observable.toList().blockingFirst();
        assertTrue(result.isEmpty());
    }
    
    @Test
    public void testFromFlux_WithError() {
        RuntimeException testError = new RuntimeException("Test error");
        Flux<Integer> flux = Flux.error(testError);
        
        Observable<Integer> observable = Observable.fromFlux(flux);
        
        assertThrows(RuntimeException.class, () -> {
            observable.blockingFirst();
        });
    }
    
    @Test
    public void testFromFlux_WithRange() {
        Flux<Integer> flux = Flux.range(1, 10);
        
        Observable<Integer> observable = Observable.fromFlux(flux);
        
        List<Integer> result = observable.toList().blockingFirst();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), result);
    }
    
    // ============ fromMono() Tests ============
    
    @Test
    public void testFromMono_WithSingleValue() {
        Mono<Integer> mono = Mono.just(42);
        
        Observable<Integer> observable = Observable.fromMono(mono);
        
        Integer result = observable.blockingFirst();
        assertEquals(Integer.valueOf(42), result);
    }
    
    @Test
    public void testFromMono_WithEmpty() {
        Mono<Integer> mono = Mono.empty();
        
        Observable<Integer> observable = Observable.fromMono(mono);
        
        Integer result = observable.blockingFirst(-1);
        assertEquals(Integer.valueOf(-1), result);
    }
    
    @Test
    public void testFromMono_WithError() {
        RuntimeException testError = new RuntimeException("Test error");
        Mono<Integer> mono = Mono.error(testError);
        
        Observable<Integer> observable = Observable.fromMono(mono);
        
        assertThrows(RuntimeException.class, () -> {
            observable.blockingFirst();
        });
    }
    
    // ============ Round-trip Tests ============
    
    @Test
    public void testRoundTrip_ObservableToFluxToObservable() {
        Observable<Integer> original = Observable.just(1, 2, 3, 4, 5);
        
        // Convert to Flux and back
        @SuppressWarnings("unchecked")
        Flux<Integer> flux = (Flux<Integer>) original.toFlux();
        Observable<Integer> roundTrip = Observable.fromFlux(flux);
        
        List<Integer> result = roundTrip.toList().blockingFirst();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }
    
    @Test
    public void testRoundTrip_ObservableToMonoToObservable() {
        Observable<Integer> original = Observable.just(42);
        
        // Convert to Mono and back
        @SuppressWarnings("unchecked")
        Mono<Integer> mono = (Mono<Integer>) original.toMono();
        Observable<Integer> roundTrip = Observable.fromMono(mono);
        
        Integer result = roundTrip.blockingFirst();
        assertEquals(Integer.valueOf(42), result);
    }
    
    @Test
    public void testRoundTrip_FluxToObservableToFlux() {
        Flux<Integer> original = Flux.range(1, 10);
        
        // Convert to Observable and back to Flux
        Observable<Integer> observable = Observable.fromFlux(original);
        @SuppressWarnings("unchecked")
        Flux<Integer> roundTrip = (Flux<Integer>) observable.toFlux();
        
        List<Integer> result = roundTrip.collectList().block();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), result);
    }
    
    // ============ Integration Tests ============
    
    @Test
    public void testIntegration_ObservableAndFluxOperators() {
        // Start with Observable, convert to Flux, apply operations, convert back
        Observable<Integer> observable = Observable.range(1, 20);
        
        @SuppressWarnings("unchecked")
        Flux<Integer> flux = (Flux<Integer>) observable
            .filter(x -> x % 2 == 0)  // Observable operator
            .toFlux();
        
        List<Integer> result = flux
            .map(x -> x * 2)          // Flux operator
            .take(5)                  // Flux operator
            .collectList()
            .block();
        
        assertEquals(Arrays.asList(4, 8, 12, 16, 20), result);
    }
    
    @Test
    public void testIntegration_CombineFluxAndObservable() {
        // Create two Fluxes
        Flux<Integer> flux1 = Flux.just(1, 2, 3);
        Flux<Integer> flux2 = Flux.just(4, 5, 6);
        
        // Convert to Observables and merge
        Observable<Integer> obs1 = Observable.fromFlux(flux1);
        Observable<Integer> obs2 = Observable.fromFlux(flux2);
        
        Observable<Integer> merged = Observable.merge(obs1, obs2);
        
        List<Integer> result = merged
            .toList()
            .blockingFirst();
        
        // Result should contain all elements (order may vary with merge)
        assertEquals(6, result.size());
        assertTrue(result.containsAll(Arrays.asList(1, 2, 3, 4, 5, 6)));
    }
    
    @Test
    public void testIntegration_FluxFlatMapWithObservable() {
        Flux<Integer> flux = Flux.just(1, 2, 3);
        
        // Use flatMap with Observable conversion
        Flux<Integer> result = flux.flatMap(x -> {
            Observable<Integer> obs = Observable.range(x * 10, 3);
            @SuppressWarnings("unchecked")
            Flux<Integer> innerFlux = (Flux<Integer>) obs.toFlux();
            return innerFlux;
        });
        
        List<Integer> collected = result.collectList().block();
        
        // Should contain ranges [10,11,12], [20,21,22], [30,31,32]
        assertEquals(9, collected.size());
    }
    
    @Test
    public void testIntegration_MonoZipWithObservable() {
        Mono<Integer> mono1 = Mono.just(10);
        Mono<Integer> mono2 = Mono.just(20);
        
        Observable<Integer> obs1 = Observable.fromMono(mono1);
        Observable<Integer> obs2 = Observable.fromMono(mono2);
        
        Observable<Integer> combined = Observable.zip(obs1, obs2, (a, b) -> a + b);
        
        Integer result = combined.blockingFirst();
        assertEquals(Integer.valueOf(30), result);
    }
}
