package com.reactive.core;

/**
 * Represents a function that accepts three arguments and produces a result.
 * @param <T1> the type of the first argument
 * @param <T2> the type of the second argument
 * @param <T3> the type of the third argument
 * @param <R> the type of the result
 */
@FunctionalInterface
public interface Function3<T1, T2, T3, R> {
    /**
     * Applies this function to the given arguments.
     */
    R apply(T1 t1, T2 t2, T3 t3);
}
