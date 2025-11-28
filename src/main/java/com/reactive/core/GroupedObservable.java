package com.reactive.core;

/**
 * A specialized Observable that represents a group of items with a common key.
 * Emitted by the groupBy operator.
 *
 * @param <K> the type of the key
 * @param <V> the type of the values
 */
public abstract class GroupedObservable<K, V> extends Observable<V> {
    
    /**
     * Returns the key that identifies this group.
     *
     * @return the key of this group
     */
    public abstract K getKey();
}
