package com.reactive.testing;

import com.reactive.core.Disposable;
import com.reactive.core.Observer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * A test observer that records all events (onNext, onError, onComplete)
 * and provides assertion methods for testing reactive streams.
 * <p>
 * TestObserver is a powerful testing utility that implements the Observer interface
 * and captures all events emitted by a reactive stream. It provides fluent assertion
 * methods to verify the behavior of observables in unit tests.
 * <p>
 * <strong>Key Features:</strong>
 * <ul>
 *   <li>Records all values emitted via onNext</li>
 *   <li>Captures errors and completion events</li>
 *   <li>Provides fluent assertion API for easy testing</li>
 *   <li>Supports blocking until terminal event (await methods)</li>
 *   <li>Thread-safe event recording</li>
 * </ul>
 * <p>
 * <strong>Example - Basic usage:</strong>
 * <pre>{@code
 * TestObserver<Integer> observer = new TestObserver<>();
 * 
 * Observable.just(1, 2, 3)
 *     .subscribe(observer);
 * 
 * observer.assertNoErrors()
 *        .assertComplete()
 *        .assertValues(1, 2, 3);
 * }</pre>
 * <p>
 * <strong>Example - Testing error handling:</strong>
 * <pre>{@code
 * TestObserver<String> observer = new TestObserver<>();
 * 
 * Observable.error(new IllegalStateException("Test error"))
 *     .subscribe(observer);
 * 
 * observer.assertError(IllegalStateException.class)
 *        .assertNotComplete();
 * }</pre>
 * <p>
 * <strong>Example - Asynchronous testing:</strong>
 * <pre>{@code
 * TestObserver<Long> observer = new TestObserver<>();
 * 
 * Observable.interval(100, TimeUnit.MILLISECONDS)
 *     .take(3)
 *     .subscribe(observer);
 * 
 * observer.await(1, TimeUnit.SECONDS)
 *        .assertComplete()
 *        .assertValueCount(3);
 * }</pre>
 *
 * @param <T> the type of values being observed
 * @see Observer
 * @see Disposable
 * @since 1.0
 */
public class TestObserver<T> implements Observer<T>, Disposable {
    private final List<T> values = new ArrayList<>();
    private final List<Throwable> errors = new ArrayList<>();
    private int completeCount = 0;
    private volatile boolean disposed = false;
    private Disposable upstream;
    private final CountDownLatch terminalEventLatch = new CountDownLatch(1);
    
    @Override
    public void onSubscribe(Disposable d) {
        this.upstream = d;
    }
    
    @Override
    public void onNext(T value) {
        if (!disposed) {
            synchronized (values) {
                values.add(value);
            }
        }
    }
    
    @Override
    public void onError(Throwable error) {
        if (!disposed) {
            synchronized (errors) {
                errors.add(error);
            }
            terminalEventLatch.countDown();
        }
    }
    
    @Override
    public void onComplete() {
        if (!disposed) {
            completeCount++;
            terminalEventLatch.countDown();
        }
    }
    
    @Override
    public void dispose() {
        disposed = true;
        if (upstream != null) {
            upstream.dispose();
        }
    }
    
    @Override
    public boolean isDisposed() {
        return disposed;
    }
    
    /**
     * Asserts that no errors were received during the stream execution.
     * <p>
     * <strong>Example:</strong>
     * <pre>{@code
     * observer.assertNoErrors(); // Passes if no errors occurred
     * }</pre>
     *
     * @return this TestObserver for method chaining
     * @throws AssertionError if one or more errors were received
     */
    public TestObserver<T> assertNoErrors() {
        if (!errors.isEmpty()) {
            throw new AssertionError("Expected no errors but got: " + errors);
        }
        return this;
    }
    
    /**
     * Asserts that an error of the specified type was received.
     * <p>
     * <strong>Example:</strong>
     * <pre>{@code
     * observer.assertError(IllegalArgumentException.class);
     * }</pre>
     *
     * @param errorClass the expected error class
     * @return this TestObserver for method chaining
     * @throws AssertionError if no error or a different error type was received
     */
    public TestObserver<T> assertError(Class<? extends Throwable> errorClass) {
        if (errors.isEmpty()) {
            throw new AssertionError("Expected error of type " + errorClass.getName() + " but got no errors");
        }
        boolean found = errors.stream().anyMatch(e -> errorClass.isInstance(e));
        if (!found) {
            throw new AssertionError("Expected error of type " + errorClass.getName() + " but got: " + errors);
        }
        return this;
    }
    
    /**
     * Asserts that an error with the specified message was received.
     * <p>
     * <strong>Example:</strong>
     * <pre>{@code
     * observer.assertErrorMessage("Invalid input");
     * }</pre>
     *
     * @param message the expected error message
     * @return this TestObserver for method chaining
     * @throws AssertionError if no error or a different error message was received
     */
    public TestObserver<T> assertErrorMessage(String message) {
        if (errors.isEmpty()) {
            throw new AssertionError("Expected error with message '" + message + "' but got no errors");
        }
        boolean found = errors.stream().anyMatch(e -> message.equals(e.getMessage()));
        if (!found) {
            throw new AssertionError("Expected error with message '" + message + "' but got: " + errors);
        }
        return this;
    }
    
    /**
     * Asserts that the stream completed normally (onComplete was called).
     * <p>
     * <strong>Example:</strong>
     * <pre>{@code
     * observer.assertComplete(); // Passes if stream completed
     * }</pre>
     *
     * @return this TestObserver for method chaining
     * @throws AssertionError if the stream did not complete
     */
    public TestObserver<T> assertComplete() {
        if (completeCount == 0) {
            throw new AssertionError("Expected completion but stream did not complete");
        }
        return this;
    }
    
    /**
     * Asserts that the stream did not complete.
     * <p>
     * Useful for testing streams that should remain active or terminate with an error.
     * <p>
     * <strong>Example:</strong>
     * <pre>{@code
     * observer.assertNotComplete(); // Passes if stream is still active
     * }</pre>
     *
     * @return this TestObserver for method chaining
     * @throws AssertionError if the stream completed
     */
    public TestObserver<T> assertNotComplete() {
        if (completeCount > 0) {
            throw new AssertionError("Expected no completion but stream completed " + completeCount + " times");
        }
        return this;
    }
    
    /**
     * Asserts that exactly the specified number of values were received.
     * <p>
     * <strong>Example:</strong>
     * <pre>{@code
     * observer.assertValueCount(5); // Expects exactly 5 values
     * }</pre>
     *
     * @param count the expected number of values
     * @return this TestObserver for method chaining
     * @throws AssertionError if the actual count differs from expected
     */
    public TestObserver<T> assertValueCount(int count) {
        int actual = values.size();
        if (actual != count) {
            throw new AssertionError("Expected " + count + " values but got " + actual + ": " + values);
        }
        return this;
    }
    
    /**
     * Asserts that exactly one value was received and it equals the expected value.
     * <p>
     * <strong>Example:</strong>
     * <pre>{@code
     * observer.assertValue("expected");
     * }</pre>
     *
     * @param expected the expected value
     * @return this TestObserver for method chaining
     * @throws AssertionError if zero, multiple, or a different value was received
     */
    public TestObserver<T> assertValue(T expected) {
        if (values.size() != 1) {
            throw new AssertionError("Expected exactly one value but got " + values.size() + ": " + values);
        }
        T actual = values.get(0);
        if (!expected.equals(actual)) {
            throw new AssertionError("Expected value: " + expected + " but got: " + actual);
        }
        return this;
    }
    
    /**
     * Asserts that the received values match the expected values in order.
     * <p>
     * <strong>Example:</strong>
     * <pre>{@code
     * observer.assertValues(1, 2, 3, 4, 5);
     * }</pre>
     *
     * @param expected the expected values in order
     * @return this TestObserver for method chaining
     * @throws AssertionError if the values don't match or are in wrong order
     */
    @SafeVarargs
    public final TestObserver<T> assertValues(T... expected) {
        if (values.size() != expected.length) {
            throw new AssertionError("Expected " + expected.length + " values but got " + values.size() + ": " + values);
        }
        for (int i = 0; i < expected.length; i++) {
            T expectedValue = expected[i];
            T actualValue = values.get(i);
            if (!expectedValue.equals(actualValue)) {
                throw new AssertionError("Value at index " + i + " - expected: " + expectedValue + " but got: " + actualValue);
            }
        }
        return this;
    }
    
    /**
     * Asserts that no values were received (empty stream).
     * <p>
     * <strong>Example:</strong>
     * <pre>{@code
     * observer.assertEmpty(); // Expects zero emissions
     * }</pre>
     *
     * @return this TestObserver for method chaining
     * @throws AssertionError if one or more values were received
     */
    public TestObserver<T> assertEmpty() {
        if (!values.isEmpty()) {
            throw new AssertionError("Expected no values but got " + values.size() + ": " + values);
        }
        return this;
    }
    
    /**
     * Asserts that the stream has terminated (either completed or errored).
     * <p>
     * <strong>Example:</strong>
     * <pre>{@code
     * observer.assertTerminated(); // Passes if onComplete or onError was called
     * }</pre>
     *
     * @return this TestObserver for method chaining
     * @throws AssertionError if the stream is still active
     */
    public TestObserver<T> assertTerminated() {
        if (completeCount == 0 && errors.isEmpty()) {
            throw new AssertionError("Expected stream to be terminated but it's still active");
        }
        return this;
    }
    
    /**
     * Asserts that the stream has not terminated yet.
     * <p>
     * Useful for testing infinite streams or streams that should remain active.
     * <p>
     * <strong>Example:</strong>
     * <pre>{@code
     * observer.assertNotTerminated(); // Stream should still be active
     * }</pre>
     *
     * @return this TestObserver for method chaining
     * @throws AssertionError if the stream has terminated
     */
    public TestObserver<T> assertNotTerminated() {
        if (completeCount > 0 || !errors.isEmpty()) {
            throw new AssertionError("Expected stream not to be terminated but it was");
        }
        return this;
    }
    
    /**
     * Blocks until the stream terminates (completes or errors).
     * <p>
     * This method waits indefinitely for a terminal event. Use the timeout variant
     * for safer testing.
     * <p>
     * <strong>Example:</strong>
     * <pre>{@code
     * observer.await()
     *        .assertComplete();
     * }</pre>
     *
     * @return this TestObserver for method chaining
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public TestObserver<T> await() throws InterruptedException {
        terminalEventLatch.await();
        return this;
    }
    
    /**
     * Blocks until the stream terminates or the timeout expires.
     * <p>
     * <strong>Example:</strong>
     * <pre>{@code
     * observer.await(5, TimeUnit.SECONDS)
     *        .assertComplete();
     * }</pre>
     *
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout
     * @return this TestObserver for method chaining
     * @throws InterruptedException if the thread is interrupted while waiting
     * @throws AssertionError if the stream does not terminate within the timeout
     */
    public TestObserver<T> await(long timeout, TimeUnit unit) throws InterruptedException {
        if (!terminalEventLatch.await(timeout, unit)) {
            throw new AssertionError("Stream did not terminate within " + timeout + " " + unit);
        }
        return this;
    }
    
    /**
     * Blocks until a terminal event is received, swallowing InterruptedException.
     * <p>
     * This is a convenience method that wraps await() and converts InterruptedException
     * to RuntimeException. Use this when you don't want to handle interruption.
     * <p>
     * <strong>Example:</strong>
     * <pre>{@code
     * observer.awaitTerminalEvent()
     *        .assertNoErrors();
     * }</pre>
     *
     * @return this TestObserver for method chaining
     * @throws RuntimeException if the thread is interrupted (wraps InterruptedException)
     */
    public TestObserver<T> awaitTerminalEvent() {
        try {
            terminalEventLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
        return this;
    }
    
    /**
     * Blocks until a terminal event is received or timeout expires, swallowing InterruptedException.
     * <p>
     * <strong>Example:</strong>
     * <pre>{@code
     * observer.awaitTerminalEvent(3, TimeUnit.SECONDS)
     *        .assertComplete();
     * }</pre>
     *
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout
     * @return this TestObserver for method chaining
     * @throws RuntimeException if the thread is interrupted
     * @throws AssertionError if timeout expires before terminal event
     */
    public TestObserver<T> awaitTerminalEvent(long timeout, TimeUnit unit) {
        try {
            if (!terminalEventLatch.await(timeout, unit)) {
                throw new AssertionError("Did not receive terminal event within " + timeout + " " + unit);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
        return this;
    }
    
    /**
     * Returns a copy of all values received via onNext.
     * <p>
     * The returned list is a snapshot and will not reflect future emissions.
     * <p>
     * <strong>Example:</strong>
     * <pre>{@code
     * List<Integer> values = observer.values();
     * assertEquals(5, values.size());
     * }</pre>
     *
     * @return a list containing all received values
     */
    public List<T> values() {
        synchronized (values) {
            return new ArrayList<>(values);
        }
    }
    
    /**
     * Returns a copy of all errors received via onError.
     * <p>
     * Normally contains at most one error (reactive streams protocol),
     * but the list structure allows for implementation flexibility.
     * <p>
     * <strong>Example:</strong>
     * <pre>{@code
     * List<Throwable> errors = observer.errors();
     * assertFalse(errors.isEmpty());
     * }</pre>
     *
     * @return a list containing all received errors
     */
    public List<Throwable> errors() {
        synchronized (errors) {
            return new ArrayList<>(errors);
        }
    }
    
    /**
     * Returns the number of times onComplete was called.
     * <p>
     * Should be 0 or 1 in well-behaved reactive streams.
     * <p>
     * <strong>Example:</strong>
     * <pre>{@code
     * assertEquals(1, observer.getCompleteCount());
     * }</pre>
     *
     * @return the completion count
     */
    public int getCompleteCount() {
        return completeCount;
    }
    
    /**
     * Returns whether any errors were received.
     * <p>
     * <strong>Example:</strong>
     * <pre>{@code
     * if (observer.hasErrors()) {
     *     System.out.println("Stream failed: " + observer.errors());
     * }
     * }</pre>
     *
     * @return {@code true} if one or more errors were received, {@code false} otherwise
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
    
    /**
     * Returns whether the stream has completed.
     * <p>
     * <strong>Example:</strong>
     * <pre>{@code
     * assertTrue(observer.isCompleted());
     * }</pre>
     *
     * @return {@code true} if onComplete was called, {@code false} otherwise
     */
    public boolean isCompleted() {
        return completeCount > 0;
    }
    
    /**
     * Returns whether the stream has terminated (completed or errored).
     * <p>
     * <strong>Example:</strong>
     * <pre>{@code
     * if (observer.isTerminated()) {
     *     System.out.println("Stream finished");
     * }
     * }</pre>
     *
     * @return {@code true} if the stream has terminated, {@code false} if still active
     */
    public boolean isTerminated() {
        return completeCount > 0 || !errors.isEmpty();
    }
}
