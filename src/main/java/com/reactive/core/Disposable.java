package com.reactive.core;

/**
 * Represents a resource or operation that can be disposed/cancelled.
 * <p>
 * Disposable is a fundamental interface in reactive programming that represents
 * a subscription or resource that can be explicitly released. When disposed, the
 * resource is cleaned up and the associated reactive stream stops emitting events.
 * <p>
 * <strong>Key Characteristics:</strong>
 * <ul>
 *   <li><strong>Idempotent:</strong> Calling dispose() multiple times has no additional effect</li>
 *   <li><strong>Thread-safe:</strong> Can be disposed from any thread</li>
 *   <li><strong>One-way:</strong> Once disposed, a Disposable cannot be reactivated</li>
 * </ul>
 * <p>
 * <strong>Common Use Cases:</strong>
 * <ul>
 *   <li>Cancelling subscriptions to prevent memory leaks</li>
 *   <li>Stopping scheduled tasks</li>
 *   <li>Releasing resources (connections, file handles)</li>
 *   <li>Implementing cleanup logic</li>
 * </ul>
 * <p>
 * <strong>Example - Basic subscription management:</strong>
 * <pre>{@code
 * Disposable subscription = Observable.interval(1, TimeUnit.SECONDS)
 *     .subscribe(value -> System.out.println("Received: " + value));
 * 
 * // Later, when we want to stop receiving events
 * subscription.dispose();
 * }</pre>
 * <p>
 * <strong>Example - Resource cleanup:</strong>
 * <pre>{@code
 * Disposable resource = Disposable.fromRunnable(() -> {
 *     System.out.println("Cleaning up resources...");
 *     connection.close();
 *     fileHandle.release();
 * });
 * 
 * // When done, cleanup is executed
 * resource.dispose();
 * }</pre>
 * <p>
 * <strong>Example - Composite disposal:</strong>
 * <pre>{@code
 * CompositeDisposable disposables = new CompositeDisposable();
 * 
 * // Add multiple subscriptions
 * disposables.add(observable1.subscribe(observer1));
 * disposables.add(observable2.subscribe(observer2));
 * disposables.add(observable3.subscribe(observer3));
 * 
 * // Dispose all at once
 * disposables.dispose();
 * }</pre>
 *
 * @see Observer#onSubscribe(Disposable)
 * @see CompositeDisposable
 * @since 1.0
 */
public interface Disposable {
    /**
     * A constant representing a no-op Disposable that is already disposed.
     * Useful as a placeholder when a Disposable is required but no action is needed.
     */
    Disposable EMPTY = new EmptyDisposable();
    
    /**
     * Disposes the resource, cancels the operation, or unsubscribes from the stream.
     * <p>
     * This method is idempotent - calling it multiple times has the same effect as calling it once.
     * Implementations should ensure this method is thread-safe.
     * <p>
     * <strong>Example:</strong>
     * <pre>{@code
     * Disposable d = observable.subscribe(System.out::println);
     * d.dispose(); // Stops receiving events
     * d.dispose(); // Safe to call again, has no effect
     * }</pre>
     */
    void dispose();
    /**
     * Returns whether this Disposable has been disposed.
     * <p>
     * This method can be used to check if a subscription is still active before
     * performing operations or to avoid disposing an already disposed resource.
     * <p>
     * <strong>Example:</strong>
     * <pre>{@code
     * if (!subscription.isDisposed()) {
     *     // Still active, safe to continue
     *     doSomething();
     * }
     * }</pre>
     *
     * @return {@code true} if this Disposable has been disposed, {@code false} otherwise
     */
    boolean isDisposed();
    
    /**
     * Returns a Disposable that is already disposed and does nothing when disposed again.
     * <p>
     * This is useful when you need to return a Disposable but no actual cleanup is required,
     * such as when an operation completes synchronously or when implementing no-op scenarios.
     * <p>
     * <strong>Example:</strong>
     * <pre>{@code
     * public Disposable subscribe(Observer<T> observer) {
     *     if (items.isEmpty()) {
     *         observer.onComplete();
     *         return Disposable.empty(); // No cleanup needed
     *     }
     *     // ... normal subscription logic
     * }
     * }</pre>
     *
     * @return a Disposable that is already disposed
     */
    static Disposable empty() {
        return EmptyDisposable.INSTANCE;
    }
    
    /**
     * Creates a Disposable that executes the given action when disposed.
     * <p>
     * The action is executed only once, even if dispose() is called multiple times.
     * This is useful for implementing custom cleanup logic such as closing connections,
     * releasing locks, or performing any necessary teardown operations.
     * <p>
     * <strong>Example - Resource cleanup:</strong>
     * <pre>{@code
     * Connection connection = database.connect();
     * Disposable disposable = Disposable.fromRunnable(() -> {
     *     System.out.println("Closing database connection...");
     *     connection.close();
     * });
     * 
     * // Later...
     * disposable.dispose(); // Executes the cleanup action
     * }</pre>
     * <p>
     * <strong>Example - Multiple cleanup actions:</strong>
     * <pre>{@code
     * Disposable cleanup = Disposable.fromRunnable(() -> {
     *     fileHandle.close();
     *     cache.clear();
     *     logger.info("Resources cleaned up");
     * });
     * }</pre>
     *
     * @param action the action to execute when this Disposable is disposed
     * @return a Disposable that executes the action on disposal
     * @throws NullPointerException if action is null
     */
    static Disposable fromRunnable(Runnable action) {
        return new RunnableDisposable(action);
    }
}

class EmptyDisposable implements Disposable {
    static final EmptyDisposable INSTANCE = new EmptyDisposable();
    
    @Override
    public void dispose() {
    }
    
    @Override
    public boolean isDisposed() {
        return true;
    }
}

class RunnableDisposable implements Disposable {
    private volatile boolean disposed;
    private final Runnable action;
    
    RunnableDisposable(Runnable action) {
        this.action = action;
    }
    
    @Override
    public void dispose() {
        if (!disposed) {
            disposed = true;
            action.run();
        }
    }
    
    @Override
    public boolean isDisposed() {
        return disposed;
    }
}
