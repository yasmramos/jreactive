package com.reactive.operators.time;

import com.reactive.core.Observable;
import com.reactive.core.Observer;
import com.reactive.schedulers.Scheduler;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Operador throttleFirst: emite el primer elemento y luego ignora elementos por un período.
 * 
 * También conocido como "sample first" o "throttle leading".
 * 
 * Útil para:
 * - Prevenir clicks dobles
 * - Limitar tasa de eventos de UI
 * - Rate limiting de API calls
 * 
 * Ejemplo:
 * Si window = 500ms y se emiten: A(0ms), B(100ms), C(200ms), D(600ms), E(700ms)
 * Solo se emitirán: A(0ms) y D(600ms)
 * 
 * @param <T> Tipo de elementos
 */
public class ObservableThrottleFirst<T> extends Observable<T> {
    
    private final Observable<T> source;
    private final long windowDuration;
    private final TimeUnit unit;
    private final Scheduler scheduler;
    
    public ObservableThrottleFirst(Observable<T> source, long windowDuration, TimeUnit unit, Scheduler scheduler) {
        this.source = source;
        this.windowDuration = windowDuration;
        this.unit = unit;
        this.scheduler = scheduler;
    }
    
    @Override
    public void subscribe(Observer<? super T> observer) {
        source.subscribe(new Observer<T>() {
            private final AtomicLong lastEmitTime = new AtomicLong(0);
            private final AtomicBoolean gate = new AtomicBoolean(true);
            
            @Override
            public void onNext(T value) {
                long now = System.currentTimeMillis();
                long windowMs = unit.toMillis(windowDuration);
                
                // Si el gate está abierto, emitir y cerrar
                if (gate.get()) {
                    gate.set(false);
                    lastEmitTime.set(now);
                    observer.onNext(value);
                    
                    // Programar apertura del gate
                    scheduler.schedule(() -> {
                        gate.set(true);
                    }, windowDuration, unit);
                }
                // Si el gate está cerrado, descartar el valor
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
