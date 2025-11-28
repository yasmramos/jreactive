package com.reactive.observables;

import com.reactive.core.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

/**
 * ObservableRefCount wraps a ConnectableObservable and automatically manages the connection
 * based on the number of active subscribers.
 * 
 * <p><b>Behavior:</b></p>
 * <ul>
 * <li>First subscriber triggers connect()</li>
 * <li>Additional subscribers share the same connection</li>
 * <li>When the last subscriber unsubscribes, the connection is disposed</li>
 * <li>New subscribers after complete disposal will trigger a new connection</li>
 * </ul>
 * 
 * <p>This operator is useful for converting a ConnectableObservable into an auto-managed
 * hot Observable that activates and deactivates based on demand.</p>
 * 
 * @param <T> the type of items emitted
 */
public final class ObservableRefCount<T> extends Observable<T> {
    
    private final ConnectableObservable<T> source;
    private final AtomicInteger subscriberCount;
    private final AtomicReference<Disposable> connection;
    private final ReentrantLock lock;
    
    /**
     * Creates an ObservableRefCount that manages the connection to the source.
     * 
     * @param source the ConnectableObservable to manage
     */
    public ObservableRefCount(ConnectableObservable<T> source) {
        this.source = source;
        this.subscriberCount = new AtomicInteger(0);
        this.connection = new AtomicReference<>();
        this.lock = new ReentrantLock();
    }
    
    @Override
    public void subscribe(Observer<? super T> observer) {
        lock.lock();
        try {
            // Increment subscriber count
            int count = subscriberCount.incrementAndGet();
            
            // Subscribe to the source first to ensure observer is ready
            source.subscribe(new RefCountObserver<>(observer, this));
            
            // If this is the first subscriber, connect after subscribing
            if (count == 1) {
                Disposable connectionDisposable = source.connect();
                connection.set(connectionDisposable);
            }
            
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Called when an observer unsubscribes.
     * Decrements the subscriber count and disconnects if it reaches zero.
     */
    private void unsubscribe() {
        lock.lock();
        try {
            int count = subscriberCount.decrementAndGet();
            
            // If no more subscribers, disconnect
            if (count == 0) {
                Disposable conn = connection.getAndSet(null);
                if (conn != null) {
                    conn.dispose();
                }
            }
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Observer wrapper that handles reference counting.
     */
    private static final class RefCountObserver<T> implements Observer<T>, Disposable {
        
        private final Observer<? super T> downstream;
        private final ObservableRefCount<T> parent;
        private volatile boolean disposed = false;
        private Disposable upstream;
        
        RefCountObserver(Observer<? super T> downstream, ObservableRefCount<T> parent) {
            this.downstream = downstream;
            this.parent = parent;
        }
        
        @Override
        public void onSubscribe(Disposable d) {
            this.upstream = d;
            downstream.onSubscribe(this);
        }
        
        @Override
        public void onNext(T value) {
            if (!disposed) {
                downstream.onNext(value);
            }
        }
        
        @Override
        public void onError(Throwable error) {
            if (!disposed) {
                disposed = true;
                try {
                    downstream.onError(error);
                } finally {
                    parent.unsubscribe();
                }
            }
        }
        
        @Override
        public void onComplete() {
            if (!disposed) {
                disposed = true;
                try {
                    downstream.onComplete();
                } finally {
                    parent.unsubscribe();
                }
            }
        }
        
        @Override
        public void dispose() {
            if (!disposed) {
                disposed = true;
                if (upstream != null) {
                    upstream.dispose();
                }
                parent.unsubscribe();
            }
        }
        
        @Override
        public boolean isDisposed() {
            return disposed;
        }
    }
    
    /**
     * Creates an Observable that auto-manages the connection to a ConnectableObservable.
     * 
     * @param source the ConnectableObservable to manage
     * @param <T> the type of items emitted
     * @return an Observable with automatic connection management
     */
    public static <T> Observable<T> create(ConnectableObservable<T> source) {
        return new ObservableRefCount<>(source);
    }
}
