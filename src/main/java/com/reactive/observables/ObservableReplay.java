package com.reactive.observables;

import com.reactive.core.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * ObservableReplay converts a cold Observable into a hot ConnectableObservable
 * using a ReplaySubject for multicasting with buffering.
 * 
 * <p>Unlike publish(), replay() buffers emitted items so that new observers
 * that subscribe after emissions have started can receive previously emitted items.</p>
 * 
 * <p>Supports two buffering strategies:</p>
 * <ul>
 * <li><b>Unbounded:</b> Buffers all emitted items</li>
 * <li><b>Size-bound:</b> Buffers up to a maximum number of items</li>
 * </ul>
 * 
 * @param <T> the type of items emitted
 */
public final class ObservableReplay<T> extends ConnectableObservable<T> {
    
    private final Observable<T> source;
    private final int bufferSize; // -1 for unbounded
    private volatile ReplaySubject<T> subject;
    private final AtomicReference<Disposable> connection;
    private final AtomicBoolean connected;
    private volatile boolean subjectNeedsReset = false;
    private final Object lock = new Object();
    
    /**
     * Creates an ObservableReplay with an unbounded ReplaySubject.
     * 
     * @param source the source Observable to multicast
     */
    public ObservableReplay(Observable<T> source) {
        this.source = source;
        this.bufferSize = -1;
        this.subject = ReplaySubject.create();
        this.connection = new AtomicReference<>();
        this.connected = new AtomicBoolean(false);
    }
    
    /**
     * Creates an ObservableReplay with a size-bounded ReplaySubject.
     * 
     * @param source the source Observable to multicast
     * @param bufferSize the maximum number of items to buffer
     */
    public ObservableReplay(Observable<T> source, int bufferSize) {
        this.source = source;
        this.bufferSize = bufferSize;
        this.subject = ReplaySubject.createWithSize(bufferSize);
        this.connection = new AtomicReference<>();
        this.connected = new AtomicBoolean(false);
    }
    
    @Override
    public void subscribe(Observer<? super T> observer) {
        ReplaySubject<T> currentSubject;
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
            final ReplaySubject<T> currentSubject;
            synchronized (lock) {
                // Ensure we have a fresh subject if needed
                ensureFreshSubject();
                currentSubject = subject;
                // Mark that we no longer need to reset until the next disconnection
                subjectNeedsReset = false;
            }
            
            Disposable d = source.subscribe(
                currentSubject::onNext,
                currentSubject::onError,
                currentSubject::onComplete
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
        // Only create a new subject if:
        // 1. We're not currently connected AND
        // 2. Either the subject needs reset OR the current subject has terminated
        if (!connected.get() && (subjectNeedsReset || subject.hasComplete() || subject.hasThrowable())) {
            subject = bufferSize < 0 ? 
                ReplaySubject.create() : 
                ReplaySubject.createWithSize(bufferSize);
            // Don't reset the flag here - let connect() do it after it connects
        }
    }
    
    /**
     * Creates a ConnectableObservable that multicasts with unbounded replay.
     * 
     * @param source the source Observable
     * @param <T> the type of items emitted
     * @return a ConnectableObservable with unbounded replay
     */
    public static <T> ConnectableObservable<T> create(Observable<T> source) {
        return new ObservableReplay<>(source);
    }
    
    /**
     * Creates a ConnectableObservable that multicasts with size-bounded replay.
     * 
     * @param source the source Observable
     * @param bufferSize the maximum number of items to buffer
     * @param <T> the type of items emitted
     * @return a ConnectableObservable with size-bounded replay
     */
    public static <T> ConnectableObservable<T> createWithSize(Observable<T> source, int bufferSize) {
        return new ObservableReplay<>(source, bufferSize);
    }
}
