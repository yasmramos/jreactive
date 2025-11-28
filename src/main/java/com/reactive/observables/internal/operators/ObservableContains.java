package com.reactive.observables.internal.operators;

import com.reactive.core.*;
import java.util.Objects;

/**
 * Determines whether the source Observable emits a specific item.
 * 
 * <p>This operator subscribes to the source and compares each item to the target value
 * using Objects.equals(). If a match is found, emits true immediately and disposes the upstream.
 * If no match is found by the time the source completes, emits false.</p>
 * 
 * <p><b>Marble Diagrams:</b></p>
 * <pre>
 * Item found:
 * source:  --1--2--3--4--|      (contains 3)
 * result:  --------true|
 * 
 * Item not found:
 * source:  --1--2--4--|          (contains 3)
 * result:  -----------|false
 * 
 * Empty source:
 * source:  --|
 * result:  --|false
 * </pre>
 * 
 * @param <T> the type of items emitted by the source
 */
public final class ObservableContains<T> extends Observable<Boolean> {
    
    private final Observable<T> source;
    private final T value;
    
    /**
     * Creates an Observable that checks if the source contains a specific value.
     * 
     * @param source the source Observable
     * @param value the value to search for
     */
    public ObservableContains(Observable<T> source, T value) {
        this.source = source;
        this.value = value;
    }
    
    @Override
    public void subscribe(Observer<? super Boolean> observer) {
        source.subscribe(new ContainsObserver<>(observer, value));
    }
    
    /**
     * Observer that searches for a specific value.
     */
    private static final class ContainsObserver<T> implements Observer<T>, Disposable {
        
        private final Observer<? super Boolean> downstream;
        private final T targetValue;
        private Disposable upstream;
        private volatile boolean disposed = false;
        private boolean done = false;
        
        ContainsObserver(Observer<? super Boolean> downstream, T targetValue) {
            this.downstream = downstream;
            this.targetValue = targetValue;
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
            
            if (Objects.equals(value, targetValue)) {
                done = true;
                disposed = true;
                if (upstream != null) {
                    upstream.dispose();
                }
                downstream.onNext(true);
                downstream.onComplete();
            }
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
                // If we made it here, value was not found
                downstream.onNext(false);
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
     * Creates an Observable that checks if the source contains a specific value.
     * 
     * @param source the source Observable
     * @param value the value to search for
     * @param <T> the type of items emitted by the source
     * @return an Observable that emits true if the value is found, false otherwise
     */
    public static <T> Observable<Boolean> create(Observable<T> source, T value) {
        return new ObservableContains<>(source, value);
    }
}
