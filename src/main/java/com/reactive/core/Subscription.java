package com.reactive.core;

/**
 * Represents a subscription to a reactive stream with backpressure support.
 * Allows the subscriber to request items and cancel the subscription.
 */
public interface Subscription {
    
    /**
     * Request n items from the upstream.
     * The upstream will emit at most n items.
     *
     * @param n the number of items to request (must be > 0)
     */
    void request(long n);
    
    /**
     * Cancel this subscription. No more items will be emitted.
     */
    void cancel();
}
