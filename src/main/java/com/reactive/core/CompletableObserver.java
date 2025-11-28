package com.reactive.core;

/**
 * Observer interface for consuming completion or error signals from a {@link Completable}.
 * 
 * <p>A CompletableObserver receives notifications from a Completable in the form of:
 * <ul>
 *   <li>{@link #onSubscribe(Disposable)} - Called when subscription begins</li>
 *   <li>{@link #onComplete()} - Called when operation completes successfully (terminal)</li>
 *   <li>{@link #onError(Throwable)} - Called when an error occurs (terminal)</li>
 * </ul>
 * 
 * <p><strong>Protocol contract:</strong>
 * <ul>
 *   <li>onSubscribe is called exactly once before any other method</li>
 *   <li>Either onComplete or onError is called exactly once (terminal events)</li>
 *   <li>After any terminal event, no further events are emitted</li>
 *   <li>No values are emitted - only completion or error signals</li>
 * </ul>
 * 
 * <p>Common use cases:
 * <ul>
 *   <li>Write operations (save, update, delete) that don't return a result</li>
 *   <li>Cache invalidation or cleanup operations</li>
 *   <li>Background tasks that signal completion without producing a value</li>
 *   <li>Fire-and-forget asynchronous actions</li>
 * </ul>
 * 
 * <p>Example implementation:
 * <pre>{@code
 * Completable.fromRunnable(() -> System.out.println("Task completed"))
 *     .subscribe(new CompletableObserver() {
 *         @Override
 *         public void onSubscribe(Disposable d) {
 *             System.out.println("Subscribed to task");
 *         }
 *         
 *         @Override
 *         public void onComplete() {
 *             System.out.println("Task finished successfully");
 *         }
 *         
 *         @Override
 *         public void onError(Throwable error) {
 *             System.err.println("Task failed: " + error.getMessage());
 *         }
 *     });
 * }</pre>
 * 
 * @see Completable
 * @see Observer
 * @see Single
 * @see Maybe
 */
public interface CompletableObserver {
    
    /**
     * Called when the Completable operation completes successfully.
     * 
     * <p>This is a terminal event - no further events will be emitted after
     * this method is called. This signals that the operation has finished
     * successfully without producing a value.
     * 
     * <p>Example:
     * <pre>{@code
     * @Override
     * public void onComplete() {
     *     System.out.println("Database write completed");
     *     // Proceed with next operation
     * }
     * }</pre>
     */
    void onComplete();
    
    /**
     * Called when the Completable operation encounters an error.
     * 
     * <p>This is a terminal event - no further events will be emitted after
     * this method is called. This signals that the operation has failed.
     * 
     * <p>Example:
     * <pre>{@code
     * @Override
     * public void onError(Throwable error) {
     *     System.err.println("Operation failed: " + error.getMessage());
     *     // Handle error, retry, or cleanup
     * }
     * }</pre>
     * 
     * @param error the exception that caused the error
     */
    void onError(Throwable error);
    
    /**
     * Called when the CompletableObserver subscribes to the Completable.
     * 
     * <p>This method is invoked exactly once before either {@link #onComplete()}
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
     *     // Can call d.dispose() later to cancel the operation
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
