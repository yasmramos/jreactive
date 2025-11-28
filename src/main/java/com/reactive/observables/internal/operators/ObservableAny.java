package com.reactive.observables.internal.operators;

import com.reactive.core.*;
import java.util.function.Predicate;

/**
 * Determines whether any item emitted by the source Observable satisfies a given predicate.
 * 
 * <p>This operator subscribes to the source and tests each item against the predicate.
 * If any item passes the test, emits true immediately and disposes the upstream.
 * If no items pass or the source is empty, emits false when the source completes.</p>
 * 
 * <p><b>Marble Diagrams:</b></p>
 * <pre>
 * One item passes:
 * source:  --1--2--3--|          (any even)
 * any:     -----true|
 * 
 * No items pass:
 * source:  --1--3--5--|          (any even)
 * any:     -----------|false
 * 
 * Empty source:
 * source:  --|
 * any:     --|false
 * </pre>
 * 
 * @param <T> the type of items emitted by the source
 */
public final class ObservableAny<T> extends Observable<Boolean> {
    
    private final Observable<T> source;
    private final Predicate<? super T> predicate;
    
    /**
     * Creates an Observable that tests whether any item satisfies the predicate.
     * 
     * @param source the source Observable
     * @param predicate the predicate to test each item
     */
    public ObservableAny(Observable<T> source, Predicate<? super T> predicate) {
        this.source = source;
        this.predicate = predicate;
    }
    
    @Override
    public void subscribe(Observer<? super Boolean> observer) {
        source.subscribe(new AnyObserver<>(observer, predicate));
    }
    
    /**
     * Observer that tests items against a predicate until one passes.
     */
    private static final class AnyObserver<T> implements Observer<T>, Disposable {
        
        private final Observer<? super Boolean> downstream;
        private final Predicate<? super T> predicate;
        private Disposable upstream;
        private volatile boolean disposed = false;
        private boolean done = false;
        
        AnyObserver(Observer<? super Boolean> downstream, Predicate<? super T> predicate) {
            this.downstream = downstream;
            this.predicate = predicate;
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
            
            boolean result;
            try {
                result = predicate.test(value);
            } catch (Throwable ex) {
                onError(ex);
                return;
            }
            
            if (result) {
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
                // If we made it here, no items passed (or source was empty)
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
     * Creates an Observable that determines if any item satisfies the predicate.
     * 
     * @param source the source Observable
     * @param predicate the predicate to test each item
     * @param <T> the type of items emitted by the source
     * @return an Observable that emits true if any item passes, false otherwise
     */
    public static <T> Observable<Boolean> create(Observable<T> source, Predicate<? super T> predicate) {
        return new ObservableAny<>(source, predicate);
    }
}
