package com.reactive.core;

/**
 * Functional interface for Observable subscription
 */
@FunctionalInterface
public interface OnSubscribe<T> {
    void subscribe(Emitter<T> emitter) throws Exception;
}
