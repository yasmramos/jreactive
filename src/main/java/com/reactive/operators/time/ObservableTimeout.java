package com.reactive.operators.time;

import com.reactive.core.Observable;
import com.reactive.core.Observer;
import com.reactive.schedulers.Scheduler;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Operador timeout: emite error si no se recibe ningún elemento en el tiempo especificado.
 * 
 * Útil para:
 * - Detectar operaciones que tardan demasiado
 * - Establecer límites de tiempo en peticiones
 * - Fallback en caso de timeout
 * 
 * Ejemplo:
 * Si timeout = 1000ms y el source no emite nada en 1 segundo, se emite TimeoutException.
 * 
 * @param <T> Tipo de elementos
 */
public class ObservableTimeout<T> extends Observable<T> {
    
    private final Observable<T> source;
    private final long timeout;
    private final TimeUnit unit;
    private final Scheduler scheduler;
    private final Observable<T> fallback;
    
    public ObservableTimeout(Observable<T> source, long timeout, TimeUnit unit, Scheduler scheduler) {
        this(source, timeout, unit, scheduler, null);
    }
    
    public ObservableTimeout(Observable<T> source, long timeout, TimeUnit unit, Scheduler scheduler, Observable<T> fallback) {
        this.source = source;
        this.timeout = timeout;
        this.unit = unit;
        this.scheduler = scheduler;
        this.fallback = fallback;
    }
    
    @Override
    public void subscribe(Observer<? super T> observer) {
        source.subscribe(new Observer<T>() {
            private final AtomicBoolean terminated = new AtomicBoolean(false);
            private final AtomicReference<Runnable> timeoutTask = new AtomicReference<>();
            
            {
                // Programar timeout inicial
                scheduleTimeout();
            }
            
            private void scheduleTimeout() {
                Runnable task = () -> {
                    if (terminated.compareAndSet(false, true)) {
                        if (fallback != null) {
                            // Cambiar al fallback
                            fallback.subscribe(observer);
                        } else {
                            // Emitir error de timeout
                            observer.onError(new TimeoutException("No se recibió ningún elemento en " + timeout + " " + unit));
                        }
                    }
                };
                
                timeoutTask.set(task);
                scheduler.schedule(task, timeout, unit);
            }
            
            @Override
            public void onNext(T value) {
                if (!terminated.get()) {
                    observer.onNext(value);
                    // Reprogramar timeout
                    scheduleTimeout();
                }
            }
            
            @Override
            public void onError(Throwable error) {
                if (terminated.compareAndSet(false, true)) {
                    observer.onError(error);
                }
            }
            
            @Override
            public void onComplete() {
                if (terminated.compareAndSet(false, true)) {
                    observer.onComplete();
                }
            }
        });
    }
}
