package com.reactive.operators;

import com.reactive.core.*;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Toma solo los primeros n elementos
 */
public class ObservableTake<T> extends Observable<T> {
    
    private final ObservableSource<T> source;
    private final long count;
    
    public ObservableTake(ObservableSource<T> source, long count) {
        this.source = source;
        this.count = count;
    }
    
    @Override
    public void subscribe(Observer<? super T> observer) {
        source.subscribe(new TakeObserver<>(observer, count));
    }
    
    static class TakeObserver<T> implements Observer<T> {
        private final Observer<? super T> downstream;
        private final long limit;
        private final AtomicLong remaining;
        private Disposable upstream;
        
        TakeObserver(Observer<? super T> downstream, long limit) {
            this.downstream = downstream;
            this.limit = limit;
            this.remaining = new AtomicLong(limit);
        }
        
        @Override
        public void onSubscribe(Disposable disposable) {
            this.upstream = disposable;
            downstream.onSubscribe(disposable);
            if (limit == 0) {
                upstream.dispose();
                downstream.onComplete();
            }
        }
        
        @Override
        public void onNext(T item) {
            long r = remaining.getAndDecrement();
            if (r > 0) {
                downstream.onNext(item);
                if (r == 1) {
                    upstream.dispose();
                    downstream.onComplete();
                }
            }
        }
        
        @Override
        public void onError(Throwable error) {
            if (remaining.get() > 0) {
                downstream.onError(error);
            }
        }
        
        @Override
        public void onComplete() {
            if (remaining.get() > 0) {
                downstream.onComplete();
            }
        }
    }
}
