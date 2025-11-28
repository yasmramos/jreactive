package com.reactive.operators;

import com.reactive.core.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/**
 * Similar a flatMap pero mantiene el orden secuencial
 */
public class ObservableConcatMap<T, R> extends Observable<R> {
    
    private final ObservableSource<T> source;
    private final Function<? super T, ? extends ObservableSource<? extends R>> mapper;
    
    public ObservableConcatMap(ObservableSource<T> source, 
                               Function<? super T, ? extends ObservableSource<? extends R>> mapper) {
        this.source = source;
        this.mapper = mapper;
    }
    
    @Override
    public void subscribe(Observer<? super R> observer) {
        source.subscribe(new ConcatMapObserver<>(observer, mapper));
    }
    
    static class ConcatMapObserver<T, R> implements Observer<T> {
        private final Observer<? super R> downstream;
        private final Function<? super T, ? extends ObservableSource<? extends R>> mapper;
        private final ConcurrentLinkedQueue<ObservableSource<? extends R>> queue = new ConcurrentLinkedQueue<>();
        private final AtomicBoolean active = new AtomicBoolean(false);
        private volatile boolean done;
        
        ConcatMapObserver(Observer<? super R> downstream, 
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
            try {
                ObservableSource<? extends R> inner = mapper.apply(item);
                queue.offer(inner);
                drain();
            } catch (Exception e) {
                onError(e);
            }
        }
        
        @Override
        public void onError(Throwable error) {
            downstream.onError(error);
        }
        
        @Override
        public void onComplete() {
            done = true;
            drain();
        }
        
        private void drain() {
            if (!active.compareAndSet(false, true)) {
                return;
            }
            
            ObservableSource<? extends R> next = queue.poll();
            if (next != null) {
                next.subscribe(new Observer<R>() {
                    @Override
                    public void onNext(R value) {
                        downstream.onNext(value);
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        downstream.onError(error);
                    }
                    
                    @Override
                    public void onComplete() {
                        active.set(false);
                        drain();
                    }
                });
            } else {
                active.set(false);
                if (done && queue.isEmpty()) {
                    downstream.onComplete();
                }
            }
        }
    }
}
