package com.reactive.core;

/**
 * Observer interface for consuming items emitted by an {@link Observable}.
 * 
 * <p>An Observer receives notifications from an Observable in the form of:
 * <ul>
 *   <li>{@link #onSubscribe(Disposable)} - Called when subscription begins</li>
 *   <li>{@link #onNext(Object)} - Called for each emitted item</li>
 *   <li>{@link #onError(Throwable)} - Called when an error occurs (terminal)</li>
 *   <li>{@link #onComplete()} - Called when emission completes successfully (terminal)</li>
 * </ul>
 * 
 * <p><strong>Protocol contract:</strong>
 * <ul>
 *   <li>onSubscribe is called exactly once before any other method</li>
 *   <li>onNext may be called zero or more times</li>
 *   <li>Either onComplete or onError is called exactly once (terminal events)</li>
 *   <li>After onComplete or onError, no further events are emitted</li>
 * </ul>
 * 
 * <p>Example implementation:
 * <pre>{@code
 * Observable.just(1, 2, 3, 4, 5)
 *     .subscribe(new Observer<Integer>() {
 *         @Override
 *         public void onSubscribe(Disposable d) {
 *             System.out.println("Subscribed!");
 *         }
 *         
 *         @Override
 *         public void onNext(Integer value) {
 *             System.out.println("Received: " + value);
 *         }
 *         
 *         @Override
 *         public void onError(Throwable error) {
 *             System.err.println("Error: " + error.getMessage());
 *         }
 *         
 *         @Override
 *         public void onComplete() {
 *             System.out.println("Completed!");
 *         }
 *     });
 * }</pre>
 * 
 * @param <T> the type of items observed
 * @see Observable
 * @see Disposable
 */
public interface Observer<T> {
    /**
     * Called when the Observer subscribes to the Observable.
     * 
     * <p>This method is invoked exactly once before any {@link #onNext(Object)},
     * {@link #onError(Throwable)}, or {@link #onComplete()} events.
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
     * @param d the Disposable that manages the subscription
     * @see Disposable#dispose()
     */
    default void onSubscribe(Disposable d) {
        // Default implementation does nothing
    }
    /**
     * Called for each item emitted by the Observable.
     * 
     * <p>This method may be called zero or more times, once for each item
     * in the stream. It will not be called after {@link #onComplete()} or
     * {@link #onError(Throwable)} has been invoked.
     * 
     * <p>Example:
     * <pre>{@code
     * @Override
     * public void onNext(Integer value) {
     *     System.out.println("Received: " + value);
     *     // Process the value
     * }
     * }</pre>
     * 
     * @param value the item emitted by the Observable
     */
    void onNext(T value);
    /**
     * Called when the Observable encounters an error.
     * 
     * <p>This is a terminal event - no further events will be emitted after
     * this method is called. Neither {@link #onNext(Object)} nor {@link #onComplete()}
     * will be invoked after onError.
     * 
     * <p>Example:
     * <pre>{@code
     * @Override
     * public void onError(Throwable error) {
     *     System.err.println("Error occurred: " + error.getMessage());
     *     error.printStackTrace();
     * }
     * }</pre>
     * 
     * @param error the exception that caused the error
     */
    void onError(Throwable error);
    /**
     * Called when the Observable completes successfully.
     * 
     * <p>This is a terminal event - no further events will be emitted after
     * this method is called. Neither {@link #onNext(Object)} nor {@link #onError(Throwable)}
     * will be invoked after onComplete.
     * 
     * <p>Example:
     * <pre>{@code
     * @Override
     * public void onComplete() {
     *     System.out.println("Stream completed successfully!");
     *     // Cleanup resources
     * }
     * }</pre>
     */
    void onComplete();
}
