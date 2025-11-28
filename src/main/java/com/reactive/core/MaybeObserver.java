package com.reactive.core;

/**
 * Observer interface for consuming 0 or 1 item emitted by a {@link Maybe}.
 * 
 * <p>A MaybeObserver receives notifications from a Maybe in the form of:
 * <ul>
 *   <li>{@link #onSubscribe(Disposable)} - Called when subscription begins</li>
 *   <li>{@link #onSuccess(Object)} - Called when a value is emitted (terminal)</li>
 *   <li>{@link #onComplete()} - Called when completing without a value (terminal)</li>
 *   <li>{@link #onError(Throwable)} - Called when an error occurs (terminal)</li>
 * </ul>
 * 
 * <p><strong>Protocol contract:</strong>
 * <ul>
 *   <li>onSubscribe is called exactly once before any other method</li>
 *   <li>Exactly one of onSuccess, onComplete, or onError is called (terminal events)</li>
 *   <li>After any terminal event, no further events are emitted</li>
 * </ul>
 * 
 * <p>Common use cases:
 * <ul>
 *   <li>Optional database lookups that may return no result</li>
 *   <li>Cache operations that may have a cache miss</li>
 *   <li>Searches that may find zero or one result</li>
 * </ul>
 * 
 * <p>Example implementation:
 * <pre>{@code
 * Maybe.just("Result")
 *     .subscribe(new MaybeObserver<String>() {
 *         @Override
 *         public void onSubscribe(Disposable d) {
 *             System.out.println("Subscribed!");
 *         }
 *         
 *         @Override
 *         public void onSuccess(String value) {
 *             System.out.println("Found: " + value);
 *         }
 *         
 *         @Override
 *         public void onComplete() {
 *             System.out.println("Completed without value");
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
 * @see Maybe
 * @see Observer
 * @see SingleObserver
 * @see Disposable
 */
public interface MaybeObserver<T> {
    
    /**
     * Called when the Maybe emits a value.
     * 
     * <p>This is a terminal event - the Maybe is considered complete after this
     * method is called. This method is called exactly once if the Maybe has a value.
     * 
     * <p>Example:
     * <pre>{@code
     * @Override
     * public void onSuccess(User value) {
     *     System.out.println("Found user: " + value.getName());
     *     // Process the result
     * }
     * }</pre>
     * 
     * @param value the emitted value
     */
    void onSuccess(T value);
    
    /**
     * Called when the Maybe completes without emitting a value.
     * 
     * <p>This is a terminal event - no further events will be emitted after
     * this method is called. This indicates the operation succeeded but produced
     * no result (similar to an empty Optional).
     * 
     * <p>Example:
     * <pre>{@code
     * @Override
     * public void onComplete() {
     *     System.out.println("No result found");
     *     // Handle empty case
     * }
     * }</pre>
     */
    void onComplete();
    
    /**
     * Called when the Maybe encounters an error.
     * 
     * <p>This is a terminal event - no further events will be emitted after
     * this method is called. This method is called exactly once if the Maybe fails.
     * 
     * <p>Example:
     * <pre>{@code
     * @Override
     * public void onError(Throwable error) {
     *     System.err.println("Lookup failed: " + error.getMessage());
     *     // Handle error
     * }
     * }</pre>
     * 
     * @param error the exception that caused the error
     */
    void onError(Throwable error);
    
    /**
     * Called when the MaybeObserver subscribes to the Maybe.
     * 
     * <p>This method is invoked exactly once before any {@link #onSuccess(Object)},
     * {@link #onComplete()}, or {@link #onError(Throwable)} events.
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
