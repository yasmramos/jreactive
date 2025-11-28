package com.reactive.observables;

import com.reactive.core.*;
import java.util.function.Consumer;

/**
 * A ConnectableObservable is a special type of Observable that does not begin emitting items
 * when it is subscribed to, but only when its connect() method is called.
 * 
 * This allows multiple subscribers to subscribe before emissions begin, enabling
 * multicasting of a single subscription to the underlying source Observable.
 * 
 * <p>ConnectableObservables are useful for converting cold Observables into hot Observables.</p>
 * 
 * <h3>Cold vs Hot Observables:</h3>
 * <ul>
 * <li><b>Cold Observable:</b> Each subscriber receives its own independent stream of data.
 *     The source is activated per subscription.</li>
 * <li><b>Hot Observable:</b> All subscribers share the same stream of data.
 *     The source is activated once and multicast to all subscribers.</li>
 * </ul>
 * 
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * ConnectableObservable<Integer> connectable = Observable.range(1, 5).publish();
 * 
 * // First subscriber
 * connectable.subscribe(x -> System.out.println("Sub1: " + x));
 * 
 * // Second subscriber
 * connectable.subscribe(x -> System.out.println("Sub2: " + x));
 * 
 * // Now both subscribers will receive the same emissions
 * connectable.connect();
 * }</pre>
 * 
 * @param <T> the type of items emitted by the ConnectableObservable
 */
public abstract class ConnectableObservable<T> extends Observable<T> {
    
    /**
     * Instructs the ConnectableObservable to begin emitting items from its underlying
     * Observable to its subscribers.
     * 
     * <p>Calling connect() causes the ConnectableObservable to subscribe to the upstream
     * source and begin multicasting emissions to all currently subscribed observers.</p>
     * 
     * @return a Disposable that can be used to disconnect the connection
     */
    public abstract Disposable connect();
    
    /**
     * Instructs the ConnectableObservable to begin emitting items from its underlying
     * Observable to its subscribers, and calls the specified Consumer with a Disposable
     * that can be used to disconnect.
     * 
     * @param connection a Consumer that will receive the connection Disposable
     */
    public final void connect(Consumer<Disposable> connection) {
        try {
            connection.accept(connect());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Returns an Observable that automatically connects to this ConnectableObservable
     * when the first Observer subscribes, and disconnects when the last Observer unsubscribes.
     * 
     * <p>This is a convenience method that combines connect() with reference counting,
     * managing the connection lifecycle automatically based on subscriber count.</p>
     * 
     * <h3>Behavior:</h3>
     * <ul>
     * <li>First subscriber triggers connect()</li>
     * <li>Additional subscribers share the same connection</li>
     * <li>Last subscriber disconnect triggers upstream disposal</li>
     * <li>New subscribers after all unsubscribe will trigger a new connection</li>
     * </ul>
     * 
     * @return an Observable that auto-connects and auto-disconnects
     */
    public final Observable<T> refCount() {
        return new ObservableRefCount<>(this);
    }
    
    /**
     * Returns an Observable that automatically connects to this ConnectableObservable
     * when the specified number of Observers have subscribed to it.
     * 
     * @param numberOfSubscribers the number of subscribers required to trigger auto-connect
     * @return an Observable that auto-connects after the specified number of subscriptions
     */
    public final Observable<T> autoConnect(int numberOfSubscribers) {
        return new ObservableAutoConnect<>(this, numberOfSubscribers);
    }
    
    /**
     * Returns an Observable that automatically connects to this ConnectableObservable
     * when the first Observer subscribes.
     * 
     * <p>Unlike refCount(), autoConnect() never disconnects from the source,
     * even when all observers unsubscribe.</p>
     * 
     * @return an Observable that auto-connects on first subscription
     */
    public final Observable<T> autoConnect() {
        return autoConnect(1);
    }
}
