package com.reactive.observables.internal.operators;

import com.reactive.core.*;

/**
 * Counts the number of items emitted by the source Observable and emits this count as a Single value.
 * 
 * <p>This operator subscribes to the source, counts each onNext signal, and when the source
 * completes, emits the total count.</p>
 * 
 * <p><b>Marble Diagram:</b></p>
 * <pre>
 * source:  --1--2--3--4--|
 * count:   --------------|4
 * </pre>
 * 
 * @param <T> the type of items emitted by the source
 */
public final class ObservableCount<T> extends Observable<Long> {
    
    private final Observable<T> source;
    
    /**
     * Creates an Observable that counts the items emitted by the source.
     * 
     * @param source the source Observable
     */
    public ObservableCount(Observable<T> source) {
        this.source = source;
    }
    
    @Override
    public void subscribe(Observer<? super Long> observer) {
        source.subscribe(new CountObserver<>(observer));
    }
    
    /**
     * Observer that counts items and emits the final count.
     */
    private static final class CountObserver<T> implements Observer<T>, Disposable {
        
        private final Observer<? super Long> downstream;
        private long count = 0;
        private Disposable upstream;
        private volatile boolean disposed = false;
        
        CountObserver(Observer<? super Long> downstream) {
            this.downstream = downstream;
        }
        
        @Override
        public void onSubscribe(Disposable d) {
            this.upstream = d;
            downstream.onSubscribe(this);
        }
        
        @Override
        public void onNext(T value) {
            if (!disposed) {
                count++;
            }
        }
        
        @Override
        public void onError(Throwable error) {
            if (!disposed) {
                disposed = true;
                downstream.onError(error);
            }
        }
        
        @Override
        public void onComplete() {
            if (!disposed) {
                disposed = true;
                downstream.onNext(count);
                downstream.onComplete();
            }
        }
        
        @Override
        public void dispose() {
            if (!disposed) {
                disposed = true;
                if (upstream != null) {
                    upstream.dispose();
                }
            }
        }
        
        @Override
        public boolean isDisposed() {
            return disposed;
        }
    }
    
    /**
     * Creates an Observable that counts the items emitted by the source.
     * 
     * @param source the source Observable
     * @param <T> the type of items emitted by the source
     * @return an Observable that emits the count
     */
    public static <T> Observable<Long> create(Observable<T> source) {
        return new ObservableCount<>(source);
    }
}
