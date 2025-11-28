package com.reactive.operators.advanced;

import com.reactive.core.Observable;
import com.reactive.core.Observer;
import java.util.function.BiFunction;

/**
 * Operador scan: acumula valores aplicando una función y emite cada paso.
 * 
 * Similar a reduce pero emite valores intermedios.
 * 
 * Útil para:
 * - Running totals (sumas acumuladas)
 * - Estados acumulados
 * - Construcción incremental de objetos
 * 
 * Ejemplo:
 * [1, 2, 3, 4] con scan((acc, x) -> acc + x, 0)
 * Emite: 1, 3, 6, 10
 * 
 * @param <T> Tipo de elementos de entrada
 * @param <R> Tipo de elementos acumulados
 */
public class ObservableScan<T, R> extends Observable<R> {
    
    private final Observable<T> source;
    private final BiFunction<R, T, R> accumulator;
    private final R initialValue;
    
    public ObservableScan(Observable<T> source, BiFunction<R, T, R> accumulator, R initialValue) {
        this.source = source;
        this.accumulator = accumulator;
        this.initialValue = initialValue;
    }
    
    @Override
    public void subscribe(Observer<? super R> observer) {
        source.subscribe(new Observer<T>() {
            private R accumulated = initialValue;
            private boolean first = true;
            
            @Override
            public void onNext(T value) {
                try {
                    if (first && initialValue != null) {
                        first = false;
                        // Emitir valor inicial si existe
                        observer.onNext(accumulated);
                    }
                    
                    accumulated = accumulator.apply(accumulated, value);
                    observer.onNext(accumulated);
                } catch (Exception e) {
                    observer.onError(e);
                }
            }
            
            @Override
            public void onError(Throwable error) {
                observer.onError(error);
            }
            
            @Override
            public void onComplete() {
                observer.onComplete();
            }
        });
    }
}
