package com.reactive.operators;

import com.reactive.core.*;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/**
 * Transforma cada elemento en un Observable y fusiona todos los resultados
 * Los Observables internos se ejecutan concurrentemente
 */
public class ObservableFlatMap<T, R> extends Observable<R> {
    
    private final ObservableSource<T> source;
    private final Function<? super T, ? extends ObservableSource<? extends R>> mapper;
    
    public ObservableFlatMap(ObservableSource<T> source, 
                             Function<? super T, ? extends ObservableSource<? extends R>> mapper) {
        this.source = source;
        this.mapper = mapper;
    }
    
    @Override
    public void subscribe(Observer<? super R> observer) {
        source.subscribe(new FlatMapObserver<>(observer, mapper));
    }
    
    static class FlatMapObserver<T, R> implements Observer<T> {
        private final Observer<? super R> downstream;
        private final Function<? super T, ? extends ObservableSource<? extends R>> mapper;
        private final AtomicInteger activeCount = new AtomicInteger(1); // Empieza en 1 para el Observable principal
        private final AtomicBoolean disposed = new AtomicBoolean(false);
        private volatile boolean done;
        
        FlatMapObserver(Observer<? super R> downstream, 
                       Function<? super T, ? extends ObservableSource<? extends R>> mapper) {
            this.downstream = downstream;
            this.mapper = mapper;
        }
        
        @Override
        public void onSubscribe(Disposable disposable) {
            downstream.onSubscribe(disposable);
        }
        
        @Override
        public void onNext(T item) {
            if (disposed.get()) return;
            
            try {
                ObservableSource<? extends R> inner = mapper.apply(item);
                activeCount.incrementAndGet();
                
                inner.subscribe(new Observer<R>() {
                    @Override
                    public void onNext(R value) {
                        if (!disposed.get()) {
                            downstream.onNext(value);
                        }
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        if (disposed.compareAndSet(false, true)) {
                            downstream.onError(error);
                        }
                    }
                    
                    @Override
                    public void onComplete() {
                        if (activeCount.decrementAndGet() == 0 && done) {
                            downstream.onComplete();
                        }
                    }
                });
            } catch (Exception e) {
                onError(e);
            }
        }
        
        @Override
        public void onError(Throwable error) {
            if (disposed.compareAndSet(false, true)) {
                downstream.onError(error);
            }
        }
        
        @Override
        public void onComplete() {
            done = true;
            if (activeCount.decrementAndGet() == 0) {
                downstream.onComplete();
            }
        }
    }
}
