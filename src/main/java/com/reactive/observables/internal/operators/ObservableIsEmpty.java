package com.reactive.observables.internal.operators;

import com.reactive.core.*;

/**
 * Determines whether the source Observable emits no items.
 * 
 * <p>This operator subscribes to the source and waits for either an item or completion.
 * If any item is emitted, immediately emits false and disposes the upstream.
 * If the source completes without emitting any items, emits true.</p>
 * 
 * <p><b>Marble Diagrams:</b></p>
 * <pre>
 * Empty source:
 * source:  --|
 * result:  --|true
 * 
 * Non-empty source:
 * source:  --1--2--|
 * result:  --false|
 * 
 * Immediate value:
 * source:  1--|
 * result:  false|
 * </pre>
 * 
 * @param <T> the type of items that would be emitted by the source
 */
public final class ObservableIsEmpty<T> extends Observable<Boolean> {
    
    private final Observable<T> source;
    
    /**
     * Creates an Observable that checks if the source is empty.
     * 
     * @param source the source Observable
     */
    public ObservableIsEmpty(Observable<T> source) {
        this.source = source;
    }
    
    @Override
    public void subscribe(Observer<? super Boolean> observer) {
        source.subscribe(new IsEmptyObserver<>(observer));
    }
    
    /**
     * Observer that detects if the source emits any items.
     */
    private static final class IsEmptyObserver<T> implements Observer<T>, Disposable {
        
        private final Observer<? super Boolean> downstream;
        private Disposable upstream;
        private volatile boolean disposed = false;
        private boolean done = false;
        
        IsEmptyObserver(Observer<? super Boolean> downstream) {
            this.downstream = downstream;
        }
        
        @Override
        public void onSubscribe(Disposable d) {
            this.upstream = d;
            downstream.onSubscribe(this);
        }
        
        @Override
        public void onNext(T value) {
            if (done || disposed) {
                return;
            }
            
            // Source emitted an item, so it's not empty
            done = true;
            disposed = true;
            if (upstream != null) {
                upstream.dispose();
            }
            downstream.onNext(false);
            downstream.onComplete();
        }
        
        @Override
        public void onError(Throwable error) {
            if (!done && !disposed) {
                done = true;
                disposed = true;
                downstream.onError(error);
            }
        }
        
        @Override
        public void onComplete() {
            if (!done && !disposed) {
                done = true;
                disposed = true;
                // Source completed without emitting any items
                downstream.onNext(true);
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
     * Creates an Observable that checks if the source is empty.
     * 
     * @param source the source Observable
     * @param <T> the type of items that would be emitted by the source
     * @return an Observable that emits true if the source is empty, false otherwise
     */
    public static <T> Observable<Boolean> create(Observable<T> source) {
        return new ObservableIsEmpty<>(source);
    }
}
