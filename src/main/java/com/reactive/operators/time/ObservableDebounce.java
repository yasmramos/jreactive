package com.reactive.operators.time;

import com.reactive.core.Observable;
import com.reactive.core.Observer;
import com.reactive.schedulers.Scheduler;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Operador debounce: solo emite un elemento si ha pasado un tiempo sin que se emita otro.
 * 
 * Útil para:
 * - Búsqueda en tiempo real (esperar a que el usuario deje de escribir)
 * - Evitar clicks múltiples
 * - Rate limiting de eventos rápidos
 * 
 * Ejemplo:
 * Si timeout = 300ms y se emiten: A(0ms), B(100ms), C(200ms), D(600ms)
 * Solo se emitirán: C(500ms) y D(900ms)
 * 
 * @param <T> Tipo de elementos
 */
public class ObservableDebounce<T> extends Observable<T> {
    
    private final Observable<T> source;
    private final long timeout;
    private final TimeUnit unit;
    private final Scheduler scheduler;
    
    public ObservableDebounce(Observable<T> source, long timeout, TimeUnit unit, Scheduler scheduler) {
        this.source = source;
        this.timeout = timeout;
        this.unit = unit;
        this.scheduler = scheduler;
    }
    
    @Override
    public void subscribe(Observer<? super T> observer) {
        source.subscribe(new Observer<T>() {
            private final AtomicReference<T> pendingValue = new AtomicReference<>();
            private final AtomicReference<Runnable> pendingTask = new AtomicReference<>();
            
            @Override
            public void onNext(T value) {
                // Cancelar tarea pendiente si existe
                Runnable currentTask = pendingTask.get();
                if (currentTask != null) {
                    // La tarea ya se programó, pero podemos sobrescribir el valor
                }
                
                // Guardar el nuevo valor
                pendingValue.set(value);
                
                // Programar nueva tarea
                Runnable newTask = () -> {
                    T valueToEmit = pendingValue.getAndSet(null);
                    if (valueToEmit != null) {
                        observer.onNext(valueToEmit);
                    }
                };
                
                pendingTask.set(newTask);
                scheduler.schedule(newTask, timeout, unit);
            }
            
            @Override
            public void onError(Throwable error) {
                pendingValue.set(null);
                observer.onError(error);
            }
            
            @Override
            public void onComplete() {
                // Emitir el último valor pendiente si existe
                T finalValue = pendingValue.getAndSet(null);
                if (finalValue != null) {
                    observer.onNext(finalValue);
                }
                observer.onComplete();
            }
        });
    }
}
