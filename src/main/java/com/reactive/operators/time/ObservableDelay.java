package com.reactive.operators.time;

import com.reactive.core.Disposable;
import com.reactive.core.Observable;
import com.reactive.core.Observer;
import com.reactive.core.Scheduler;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Operador delay: retrasa la emisión de cada elemento por un tiempo específico.
 * 
 * <p>Cada elemento (onNext), error (onError) y completado (onComplete) se retrasa por el mismo período.
 * Los elementos mantienen su orden relativo pero se emiten más tarde en el tiempo.
 * 
 * <p>Útil para:
 * <ul>
 *   <li>Simular latencia en testing</li>
 *   <li>Implementar timeouts y reintentos</li>
 *   <li>Coordinar timing entre múltiples streams</li>
 *   <li>Añadir pausa antes de procesar elementos</li>
 *   <li>Implementar animaciones con timing específico</li>
 * </ul>
 * 
 * <p>Ejemplo de comportamiento:
 * <pre>
 * Si delay = 1000ms y la fuente emite:
 *   A(0ms), B(100ms), C(200ms), complete(300ms)
 * Se emitirán:
 *   A(1000ms), B(1100ms), C(1200ms), complete(1300ms)
 * (cada evento se retrasa exactamente 1000ms)
 * </pre>
 * 
 * <p>Nota importante: Este operador preserva el orden de los elementos pero cambia
 * el scheduler en el que se emiten. Todos los elementos se emiten en el scheduler especificado.
 * 
 * <p>Diferencia con delaySubscription:
 * <ul>
 *   <li><strong>delay</strong>: Retrasa cada elemento individual</li>
 *   <li><strong>delaySubscription</strong>: Retrasa cuándo empezar a observar</li>
 * </ul>
 * 
 * @param <T> Tipo de elementos
 * @author Yasmany Ramos García
 * @see ObservableDelaySubscription
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
        AtomicBoolean disposed = new AtomicBoolean(false);
        AtomicReference<Disposable> upstreamDisposable = new AtomicReference<>();
        List<Disposable> scheduledTasks = new CopyOnWriteArrayList<>();
        
        Disposable mainDisposable = new Disposable() {
            @Override
            public void dispose() {
                disposed.set(true);
                Disposable d = upstreamDisposable.get();
                if (d != null) {
                    d.dispose();
                }
                // Cancelar todas las tareas programadas
                for (Disposable task : scheduledTasks) {
                    if (task != null) {
                        task.dispose();
                    }
                }
                scheduledTasks.clear();
            }
            
            @Override
            public boolean isDisposed() {
                return disposed.get();
            }
        };
        
        source.subscribe(new Observer<T>() {
            @Override
            public void onSubscribe(Disposable d) {
                upstreamDisposable.set(d);
                observer.onSubscribe(mainDisposable);
            }
            
            @Override
            public void onNext(T value) {
                if (!disposed.get()) {
                    Disposable task = scheduler.scheduleDirect(() -> {
                        if (!disposed.get()) {
                            observer.onNext(value);
                        }
                    }, delay, unit);
                    if (task != null) {
                        scheduledTasks.add(task);
                    }
                }
            }
            
            @Override
            public void onError(Throwable error) {
                if (!disposed.getAndSet(true)) {
                    Disposable task = scheduler.scheduleDirect(() -> {
                        observer.onError(error);
                    }, delay, unit);
                    if (task != null) {
                        scheduledTasks.add(task);
                    }
                }
            }
            
            @Override
            public void onComplete() {
                if (!disposed.getAndSet(true)) {
                    Disposable task = scheduler.scheduleDirect(() -> {
                        observer.onComplete();
                    }, delay, unit);
                    if (task != null) {
                        scheduledTasks.add(task);
                    }
                }
            }
        });
    }
}
