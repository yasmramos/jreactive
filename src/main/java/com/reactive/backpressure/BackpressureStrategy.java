package com.reactive.backpressure;

/**
 * Backpressure strategies for handling situations where a producer emits items
 * faster than a consumer can process them.
 * 
 * <p>Backpressure is a critical concept in reactive programming that prevents
 * overwhelming downstream consumers when the upstream producer generates data
 * faster than it can be consumed.
 * 
 * <p><strong>When to use backpressure:</strong>
 * <ul>
 *   <li>Fast producers with slow consumers (e.g., file reading vs. network upload)</li>
 *   <li>Preventing OutOfMemoryError from unbounded buffering</li>
 *   <li>Controlling resource consumption in reactive pipelines</li>
 *   <li>Managing event floods from UI interactions or sensors</li>
 * </ul>
 * 
 * <p>Example usage:
 * <pre>{@code
 * Observable.create(emitter -> {
 *     // Fast producer
 *     for (int i = 0; i < 1_000_000; i++) {
 *         emitter.onNext(i);
 *     }
 * }, BackpressureStrategy.DROP_LATEST)
 * .observeOn(Schedulers.io())
 * .subscribe(item -> slowProcess(item));
 * }</pre>
 * 
 * @see Observable#create(OnSubscribe, BackpressureStrategy)
 */
public enum BackpressureStrategy {
    /**
     * Buffers all items until the observer is ready to consume them.
     * 
     * <p>All emitted items are stored in an unbounded queue. The consumer processes
     * items from the queue at its own pace.
     * 
     * <p><strong>Advantages:</strong>
     * <ul>
     *   <li>No data loss - all items are preserved</li>
     *   <li>Guarantees delivery order</li>
     *   <li>Simple to understand and use</li>
     * </ul>
     * 
     * <p><strong>Disadvantages:</strong>
     * <ul>
     *   <li>May cause OutOfMemoryError if buffer grows unbounded</li>
     *   <li>Increased latency due to queuing</li>
     *   <li>High memory consumption with fast producers</li>
     * </ul>
     * 
     * <p><strong>Use when:</strong>
     * <ul>
     *   <li>All items are important and must be processed</li>
     *   <li>Producer rate is predictable and bounded</li>
     *   <li>Short-lived bursts that eventually stabilize</li>
     * </ul>
     * 
     * <p>Example:
     * <pre>{@code
     * Observable.create(emitter -> {
     *     for (int i = 0; i < 100; i++) {
     *         emitter.onNext(i);
     *     }
     * }, BackpressureStrategy.BUFFER)
     * .subscribe(item -> process(item));
     * }</pre>
     */
    BUFFER,
    
    /**
     * Drops the most recently emitted items when the buffer is full.
     * 
     * <p>When the downstream consumer falls behind, new items are discarded while
     * keeping the oldest items in the buffer.
     * 
     * <p><strong>Advantages:</strong>
     * <ul>
     *   <li>Bounded memory usage</li>
     *   <li>Preserves oldest/earliest data</li>
     *   <li>No backpressure to upstream</li>
     * </ul>
     * 
     * <p><strong>Disadvantages:</strong>
     * <ul>
     *   <li>Data loss - recent items are discarded</li>
     *   <li>May miss important recent events</li>
     *   <li>Silent dropping (no error notification)</li>
     * </ul>
     * 
     * <p><strong>Use when:</strong>
     * <ul>
     *   <li>Historical data is more important than recent data</li>
     *   <li>Processing order from start is critical</li>
     *   <li>Sampling is acceptable for recent events</li>
     * </ul>
     * 
     * <p>Example:
     * <pre>{@code
     * // Keep first 100 items, drop recent ones if consumer is slow
     * Observable.create(emitter -> {
     *     sensor.onData(data -> emitter.onNext(data));
     * }, BackpressureStrategy.DROP_LATEST)
     * .subscribe(data -> analyze(data));
     * }</pre>
     */
    DROP_LATEST,
    
    /**
     * Drops the oldest items when the buffer is full.
     * 
     * <p>When the downstream consumer falls behind, the oldest buffered items are
     * discarded to make room for new items, keeping the buffer at a fixed size.
     * 
     * <p><strong>Advantages:</strong>
     * <ul>
     *   <li>Bounded memory usage</li>
     *   <li>Always processes most recent data</li>
     *   <li>Good for time-sensitive applications</li>
     * </ul>
     * 
     * <p><strong>Disadvantages:</strong>
     * <ul>
     *   <li>Data loss - oldest items are discarded</li>
     *   <li>May miss historical context</li>
     *   <li>Silent dropping (no error notification)</li>
     * </ul>
     * 
     * <p><strong>Use when:</strong>
     * <ul>
     *   <li>Recent data is more valuable than old data</li>
     *   <li>Real-time updates are critical (sensors, stock prices)</li>
     *   <li>Stale data can be safely ignored</li>
     * </ul>
     * 
     * <p>Example:
     * <pre>{@code
     * // Stock price updates - only care about latest prices
     * Observable.create(emitter -> {
     *     stockTicker.onPriceUpdate(price -> emitter.onNext(price));
     * }, BackpressureStrategy.DROP_OLDEST)
     * .subscribe(price -> updateDisplay(price));
     * }</pre>
     */
    DROP_OLDEST,
    
    /**
     * Signals an error (MissingBackpressureException) when the buffer is full.
     * 
     * <p>When the downstream consumer cannot keep up, an error is emitted to
     * indicate the backpressure issue rather than silently dropping data.
     * 
     * <p><strong>Advantages:</strong>
     * <ul>
     *   <li>Explicit failure notification</li>
     *   <li>No silent data loss</li>
     *   <li>Alerts to performance issues</li>
     * </ul>
     * 
     * <p><strong>Disadvantages:</strong>
     * <ul>
     *   <li>Stream terminates on buffer overflow</li>
     *   <li>Requires error handling</li>
     *   <li>May need retry logic</li>
     * </ul>
     * 
     * <p><strong>Use when:</strong>
     * <ul>
     *   <li>Data loss is unacceptable</li>
     *   <li>Backpressure should be an exceptional condition</li>
     *   <li>You want to detect and handle overload situations</li>
     * </ul>
     * 
     * <p>Example:
     * <pre>{@code
     * Observable.create(emitter -> {
     *     database.onChange(change -> emitter.onNext(change));
     * }, BackpressureStrategy.ERROR)
     * .retry(3)  // Retry on backpressure errors
     * .subscribe(
     *     change -> process(change),
     *     error -> log.error("Backpressure detected", error)
     * );
     * }</pre>
     */
    ERROR,
    
    /**
     * Drops items that cannot be consumed immediately without buffering.
     * 
     * <p>Items are only delivered if the downstream consumer is ready to receive them.
     * No buffering occurs - items are emitted on a best-effort basis.
     * 
     * <p><strong>Advantages:</strong>
     * <ul>
     *   <li>Minimal memory overhead (no buffering)</li>
     *   <li>Low latency - no queue delays</li>
     *   <li>Prevents memory buildup</li>
     * </ul>
     * 
     * <p><strong>Disadvantages:</strong>
     * <ul>
     *   <li>Maximum data loss potential</li>
     *   <li>No delivery guarantees</li>
     *   <li>Unpredictable sampling rate</li>
     * </ul>
     * 
     * <p><strong>Use when:</strong>
     * <ul>
     *   <li>Sampling is sufficient (not all items needed)</li>
     *   <li>Memory is extremely constrained</li>
     *   <li>Low latency is critical</li>
     *   <li>Fire-and-forget semantics are acceptable</li>
     * </ul>
     * 
     * <p>Example:
     * <pre>{@code
     * // Mouse move events - don't need every single movement
     * Observable.create(emitter -> {
     *     mouse.onMove(event -> emitter.onNext(event));
     * }, BackpressureStrategy.DROP)
     * .subscribe(event -> updateCursor(event));
     * }</pre>
     */
    DROP
}
