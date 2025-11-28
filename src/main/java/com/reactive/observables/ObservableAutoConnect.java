package com.reactive.observables;

import com.reactive.core.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * ObservableAutoConnect wraps a ConnectableObservable and automatically connects
 * when a specified number of subscribers have subscribed.
 * 
 * <p><b>Key Difference from RefCount:</b></p>
 * <ul>
 * <li>autoConnect() connects when the threshold is reached and NEVER disconnects</li>
 * <li>refCount() connects on first subscriber and disconnects when all unsubscribe</li>
 * </ul>
 * 
 * <p>This is useful when you want the source to start emitting after a certain number
 * of observers have subscribed, but want it to continue emitting even if some or all
 * observers unsubscribe.</p>
 * 
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * // Start emitting when 2 subscribers are connected
 * Observable<Long> shared = Observable.interval(1, TimeUnit.SECONDS)
 *     .publish()
 *     .autoConnect(2);
 * 
 * shared.subscribe(x -> System.out.println("Sub1: " + x));
 * Thread.sleep(500);
 * shared.subscribe(x -> System.out.println("Sub2: " + x)); // Triggers connection
 * }</pre>
 * 
 * @param <T> the type of items emitted
 */
public final class ObservableAutoConnect<T> extends Observable<T> {
    
    private final ConnectableObservable<T> source;
    private final int numberOfSubscribers;
    private final AtomicInteger subscriberCount;
    private final AtomicReference<Disposable> connection;
    
    /**
     * Creates an ObservableAutoConnect that connects after the specified number of subscriptions.
     * 
     * @param source the ConnectableObservable to auto-connect
     * @param numberOfSubscribers the number of subscribers required to trigger connection
     */
    public ObservableAutoConnect(ConnectableObservable<T> source, int numberOfSubscribers) {
        if (numberOfSubscribers <= 0) {
            throw new IllegalArgumentException("numberOfSubscribers must be positive");
        }
        this.source = source;
        this.numberOfSubscribers = numberOfSubscribers;
        this.subscriberCount = new AtomicInteger(0);
        this.connection = new AtomicReference<>();
    }
    
    @Override
    public void subscribe(Observer<? super T> observer) {
        // Subscribe to the source first
        source.subscribe(observer);
        
        // Increment subscriber count and check if we should connect
        int count = subscriberCount.incrementAndGet();
        
        // Connect when the threshold is reached (only once)
        if (count == numberOfSubscribers && connection.get() == null) {
            synchronized (this) {
                // Double-check to prevent race condition
                if (connection.get() == null) {
                    Disposable d = source.connect();
                    connection.set(d);
                }
            }
        }
    }
    
    /**
     * Gets the current subscriber count.
     * 
     * @return the number of subscribers
     */
    public int getSubscriberCount() {
        return subscriberCount.get();
    }
    
    /**
     * Checks if the source has been connected.
     * 
     * @return true if connected, false otherwise
     */
    public boolean isConnected() {
        return connection.get() != null;
    }
    
    /**
     * Creates an Observable that auto-connects after the specified number of subscriptions.
     * 
     * @param source the ConnectableObservable to auto-connect
     * @param numberOfSubscribers the number of subscribers required to trigger connection
     * @param <T> the type of items emitted
     * @return an Observable that auto-connects
     */
    public static <T> Observable<T> create(ConnectableObservable<T> source, int numberOfSubscribers) {
        return new ObservableAutoConnect<>(source, numberOfSubscribers);
    }
}
