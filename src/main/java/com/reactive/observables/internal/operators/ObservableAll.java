package com.reactive.observables.internal.operators;

import com.reactive.core.*;
import java.util.function.Predicate;

/**
 * Determines whether all items emitted by the source Observable satisfy a given predicate.
 * 
 * <p>This operator subscribes to the source and tests each item against the predicate.
 * If any item fails the test, emits false immediately. If all items pass or the source
 * is empty, emits true when the source completes.</p>
 * 
 * <p><b>Marble Diagrams:</b></p>
 * <pre>
 * All items pass:
 * source:  --2--4--6--|          (all even)
 * all:     -----------|true
 * 
 * One item fails:
 * source:  --2--3--4--|          (not all even)
 * all:     -----false|
 * 
 * Empty source:
 * source:  --|
 * all:     --|true
 * </pre>
 * 
 * @param <T> the type of items emitted by the source
 */
public final class ObservableAll<T> extends Observable<Boolean> {
    
    private final Observable<T> source;
    private final Predicate<? super T> predicate;
    
    /**
     * Creates an Observable that tests whether all items satisfy the predicate.
     * 
     * @param source the source Observable
     * @param predicate the predicate to test each item
     */
    public ObservableAll(Observable<T> source, Predicate<? super T> predicate) {
        this.source = source;
        this.predicate = predicate;
    }
    
    @Override
    public void subscribe(Observer<? super Boolean> observer) {
        source.subscribe(new AllObserver<>(observer, predicate));
    }
    
    /**
     * Observer that tests all items against a predicate.
     */
    private static final class AllObserver<T> implements Observer<T>, Disposable {
        
        private final Observer<? super Boolean> downstream;
        private final Predicate<? super T> predicate;
        private Disposable upstream;
        private volatile boolean disposed = false;
        private boolean done = false;
        
        AllObserver(Observer<? super Boolean> downstream, Predicate<? super T> predicate) {
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
            
            if (!result) {
                done = true;
                disposed = true;
                if (upstream != null) {
                    upstream.dispose();
                }
                downstream.onNext(false);
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
                // If we made it here, all items passed (or source was empty)
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
     * Creates an Observable that determines if all items satisfy the predicate.
     * 
     * @param source the source Observable
     * @param predicate the predicate to test each item
     * @param <T> the type of items emitted by the source
     * @return an Observable that emits true if all items pass, false otherwise
     */
    public static <T> Observable<Boolean> create(Observable<T> source, Predicate<? super T> predicate) {
        return new ObservableAll<>(source, predicate);
    }
}
