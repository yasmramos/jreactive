package com.reactive.operators.time;

import com.reactive.core.Observable;
import com.reactive.core.Observer;
import com.reactive.schedulers.Scheduler;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Operador delay: retrasa la emisión de elementos por un tiempo específico.
 * 
 * Cada elemento se emite después del tiempo especificado.
 * 
 * @param <T> Tipo de elementos
 */
public class ObservableDelay<T> extends Observable<T> {
    
    private final Observable<T> source;
    private final long delay;
    private final TimeUnit unit;
    private final Scheduler scheduler;
    
    public ObservableDelay(Observable<T> source, long delay, TimeUnit unit, Scheduler scheduler) {
        this.source = source;
        this.delay = delay;
        this.unit = unit;
        this.scheduler = scheduler;
    }
    
    @Override
    public void subscribe(Observer<? super T> observer) {
        source.subscribe(new Observer<T>() {
            @Override
            public void onNext(T value) {
                scheduler.schedule(() -> {
                    observer.onNext(value);
                }, delay, unit);
            }
            
            @Override
            public void onError(Throwable error) {
                scheduler.schedule(() -> {
                    observer.onError(error);
                }, delay, unit);
            }
            
            @Override
            public void onComplete() {
                scheduler.schedule(() -> {
                    observer.onComplete();
                }, delay, unit);
            }
        });
    }
}
