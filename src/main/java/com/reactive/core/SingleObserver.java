package com.reactive.core;

/**
 * Observer interface for consuming the single value emitted by a {@link Single}.
 * 
 * <p>A SingleObserver receives notifications from a Single in the form of:
 * <ul>
 *   <li>{@link #onSubscribe(Disposable)} - Called when subscription begins</li>
 *   <li>{@link #onSuccess(Object)} - Called when the single value is emitted (terminal)</li>
 *   <li>{@link #onError(Throwable)} - Called when an error occurs (terminal)</li>
 * </ul>
 * 
 * <p><strong>Protocol contract:</strong>
 * <ul>
 *   <li>onSubscribe is called exactly once before any other method</li>
 *   <li>Either onSuccess or onError is called exactly once (terminal events)</li>
 *   <li>After onSuccess or onError, no further events are emitted</li>
 * </ul>
 * 
 * <p>Common use cases:
 * <ul>
 *   <li>HTTP requests that return a single response</li>
 *   <li>Database queries that return a single row</li>
 *   <li>Asynchronous computations with a single result</li>
 * </ul>
 * 
 * <p>Example implementation:
 * <pre>{@code
 * Single.just("Hello, World!")
 *     .subscribe(new SingleObserver<String>() {
 *         @Override
 *         public void onSubscribe(Disposable d) {
 *             System.out.println("Subscribed!");
 *         }
 *         
 *         @Override
 *         public void onSuccess(String value) {
 *             System.out.println("Received: " + value);
 *         }
 *         
 *         @Override
 *         public void onError(Throwable error) {
 *             System.err.println("Error: " + error.getMessage());
 *         }
 *     });
 * }</pre>
 * 
 * @param <T> the type of the item observed
 * @see Single
 * @see Observer
 * @see Disposable
 */
public interface SingleObserver<T> {
    
    /**
     * Called when the Single emits its single value.
     * 
     * <p>This is a terminal event - the Single is considered complete after this
     * method is called. This method is called exactly once if the Single succeeds.
     * 
     * <p>Example:
     * <pre>{@code
     * @Override
     * public void onSuccess(String value) {
     *     System.out.println("Result: " + value);
     *     // Process the single result
     * }
     * }</pre>
     * 
     * @param value the emitted value
     */
    void onSuccess(T value);
    
    /**
     * Called when the Single encounters an error.
     * 
     * <p>This is a terminal event - no further events will be emitted after
     * this method is called. This method is called exactly once if the Single fails.
     * 
     * <p>Example:
     * <pre>{@code
     * @Override
     * public void onError(Throwable error) {
     *     System.err.println("Operation failed: " + error.getMessage());
     *     // Handle error
     * }
     * }</pre>
     * 
     * @param error the exception that caused the error
     */
    void onError(Throwable error);
    
    /**
     * Called when the SingleObserver subscribes to the Single.
     * 
     * <p>This method is invoked exactly once before either {@link #onSuccess(Object)}
     * or {@link #onError(Throwable)} events.
     * 
     * <p>The provided {@link Disposable} allows the observer to cancel the
     * subscription at any time.
     * 
     * <p>Example:
     * <pre>{@code
     * @Override
     * public void onSubscribe(Disposable d) {
     *     this.disposable = d;
     *     // Can call d.dispose() later to cancel
     * }
     * }</pre>
     * 
     * @param disposable the Disposable that manages the subscription
     * @see Disposable#dispose()
     */
    default void onSubscribe(Disposable disposable) {
        // Implementación por defecto vacía
    }
}
