package com.reactive.operators;

import com.reactive.core.*;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Omite los primeros n elementos
 */
public class ObservableSkip<T> extends Observable<T> {
    
    private final ObservableSource<T> source;
    private final long count;
    
    public ObservableSkip(ObservableSource<T> source, long count) {
        this.source = source;
        this.count = count;
    }
    
    @Override
    public void subscribe(Observer<? super T> observer) {
        source.subscribe(new SkipObserver<>(observer, count));
    }
    
    static class SkipObserver<T> implements Observer<T> {
        private final Observer<? super T> downstream;
        private final AtomicLong remaining;
        
        SkipObserver(Observer<? super T> downstream, long count) {
            this.downstream = downstream;
            this.remaining = new AtomicLong(count);
        }
        
        @Override
        public void onSubscribe(Disposable disposable) {
            downstream.onSubscribe(disposable);
        }
        
        @Override
        public void onNext(T item) {
            if (remaining.get() > 0) {
                remaining.decrementAndGet();
            } else {
                downstream.onNext(item);
            }
        }
        
        @Override
        public void onError(Throwable error) {
            downstream.onError(error);
        }
        
        @Override
        public void onComplete() {
            downstream.onComplete();
        }
    }
}
