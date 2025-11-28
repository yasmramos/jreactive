package com.reactive.observables;

import com.reactive.core.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * ObservablePublish converts a cold Observable into a hot ConnectableObservable
 * using a PublishSubject for multicasting.
 * 
 * <p>When connect() is called, it subscribes to the source Observable and begins
 * emitting items through the PublishSubject to all subscribed observers.</p>
 * 
 * <p>New observers that subscribe after emissions have started will only receive
 * subsequently emitted items (no buffering of past items).</p>
 * 
 * @param <T> the type of items emitted
 */
public final class ObservablePublish<T> extends ConnectableObservable<T> {
    
    private final Observable<T> source;
    private volatile PublishSubject<T> subject;
    private final AtomicReference<Disposable> connection;
    private final AtomicBoolean connected;
    private volatile boolean subjectNeedsReset = false;
    private final Object lock = new Object();
    
    /**
     * Creates an ObservablePublish that multicasts the source Observable.
     * 
     * @param source the source Observable to multicast
     */
    public ObservablePublish(Observable<T> source) {
        this.source = source;
        this.subject = PublishSubject.create();
        this.connection = new AtomicReference<>();
        this.connected = new AtomicBoolean(false);
    }
    
    @Override
    public void subscribe(Observer<? super T> observer) {
        PublishSubject<T> currentSubject;
        synchronized (lock) {
            // Prepare a fresh subject if needed
            ensureFreshSubject();
            currentSubject = subject;
        }
        currentSubject.subscribe(observer);
    }
    
    @Override
    public Disposable connect() {
        // Ensure only one connection is made
        if (connected.compareAndSet(false, true)) {
            final PublishSubject<T> currentSubject;
            synchronized (lock) {
                // Ensure we have a fresh subject if needed
                ensureFreshSubject();
                currentSubject = subject;
                // Mark that we no longer need to reset until the next disconnection
                subjectNeedsReset = false;
            }
            
            Disposable d = source.subscribe(
                currentSubject::onNext,
                error -> {
                    currentSubject.onError(error);
                    // Source terminated, mark for reset
                    synchronized (lock) {
                        connected.set(false);
                        subjectNeedsReset = true;
                    }
                },
                () -> {
                    currentSubject.onComplete();
                    // Source terminated, mark for reset
                    synchronized (lock) {
                        connected.set(false);
                        subjectNeedsReset = true;
                    }
                }
            );
            
            connection.set(d);
            
            // Return a Disposable that can disconnect
            return new Disposable() {
                private volatile boolean disposed = false;
                
                @Override
                public void dispose() {
                    if (!disposed) {
                        disposed = true;
                        connected.set(false);
                        synchronized (lock) {
                            // Mark that the subject needs to be reset for the next connection
                            subjectNeedsReset = true;
                        }
                        Disposable upstream = connection.getAndSet(null);
                        if (upstream != null) {
                            upstream.dispose();
                        }
                    }
                }
                
                @Override
                public boolean isDisposed() {
                    return disposed;
                }
            };
        }
        
        // Already connected, return the existing connection
        Disposable existing = connection.get();
        return existing != null ? existing : Disposable.empty();
    }
    
    /**
     * Ensures that the subject is fresh (not terminated).
     * Must be called while holding the lock.
     */
    private void ensureFreshSubject() {
        // Create a new subject if the current one has terminated OR if reset is needed
        // This handles synchronous sources where the subject completes before connected flag is updated
        if (subject.hasComplete() || subject.hasThrowable() || subjectNeedsReset) {
            if (!connected.get()) {
                // Only actually create new subject if not currently connected
                subject = PublishSubject.create();
            }
        }
    }
    
    /**
     * Creates a ConnectableObservable that multicasts the source Observable using PublishSubject.
     * 
     * @param source the source Observable
     * @param <T> the type of items emitted
     * @return a ConnectableObservable that multicasts the source
     */
    public static <T> ConnectableObservable<T> create(Observable<T> source) {
        return new ObservablePublish<>(source);
    }
}
