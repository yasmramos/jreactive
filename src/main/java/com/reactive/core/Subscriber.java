package com.reactive.core;

/**
 * A subscriber that consumes items from a reactive stream with backpressure support.
 * Similar to Observer but with request/response flow control.
 *
 * @param <T> the type of items to observe
 */
public interface Subscriber<T> {
    
    /**
     * Called when the subscription is established.
     * The subscriber should call subscription.request(n) to start receiving items.
     *
     * @param subscription the subscription to use for requesting items
     */
    void onSubscribe(Subscription subscription);
    
    /**
     * Called when the upstream emits an item.
     *
     * @param item the emitted item
     */
    void onNext(T item);
    
    /**
     * Called when an error occurs.
     *
     * @param throwable the error
     */
    void onError(Throwable throwable);
    
    /**
     * Called when the stream completes successfully.
     */
    void onComplete();
}
