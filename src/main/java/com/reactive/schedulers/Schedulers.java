package com.reactive.schedulers;

import com.reactive.core.Scheduler;

/**
 * Factory class providing access to standard {@link Scheduler} implementations.
 * 
 * <p>Schedulers control the execution context (threads) for reactive operations.
 * This class provides convenient access to commonly used schedulers:
 * 
 * <ul>
 *   <li><strong>{@link #io()}</strong> - Optimized for I/O-bound operations (network, file system, database)
 *       <br>Uses an unbounded thread pool that grows as needed and reuses idle threads.
 *       <br>Suitable for operations that spend time waiting rather than computing.</li>
 *   
 *   <li><strong>{@link #computation()}</strong> - Optimized for CPU-intensive computations
 *       <br>Uses a fixed thread pool sized to the number of available processors.
 *       <br>Suitable for algorithms, data processing, and computational work.</li>
 *   
 *   <li><strong>{@link #newThread()}</strong> - Creates a new thread for each unit of work
 *       <br>Each task gets its own dedicated thread.
 *       <br>Useful for long-running operations that shouldn't share threads.</li>
 *   
 *   <li><strong>{@link #trampoline()}</strong> - Executes tasks sequentially on the current thread
 *       <br>Tasks are queued and executed one after another on the calling thread.
 *       <br>Useful for testing or when you need to avoid thread switching.</li>
 *   
 *   <li><strong>{@link #eventLoop()}</strong> - Event loop scheduler for continuous event processing
 *       <br>Maintains a pool of event loop threads for efficient event-driven programming.</li>
 *   
 *   <li><strong>{@link #from(java.util.concurrent.Executor)}</strong> - Wraps a custom Executor
 *       <br>Allows integration with existing thread pools and execution frameworks.</li>
 * </ul>
 * 
 * <p><strong>Usage example:</strong>
 * <pre>{@code
 * Observable.fromCallable(() -> downloadFile())  // Potentially blocking I/O
 *     .subscribeOn(Schedulers.io())              // Execute download on I/O thread
 *     .map(data -> processData(data))            // CPU-intensive processing
 *     .observeOn(Schedulers.computation())       // Switch to computation thread
 *     .subscribe(result -> updateUI(result));    // Observe results
 * }</pre>
 * 
 * <p><strong>Best practices:</strong>
 * <ul>
 *   <li>Use {@code subscribeOn()} to specify where the source Observable executes</li>
 *   <li>Use {@code observeOn()} to specify where downstream operations execute</li>
 *   <li>Choose {@code io()} for I/O operations, {@code computation()} for CPU work</li>
 *   <li>Avoid blocking operations on {@code computation()} scheduler</li>
 * </ul>
 * 
 * @see Scheduler
 * @see Observable#subscribeOn(Scheduler)
 * @see Observable#observeOn(Scheduler)
 */
public class Schedulers {
    private static final Scheduler TRAMPOLINE = new TrampolineScheduler();
    private static final Scheduler NEW_THREAD = new NewThreadScheduler();
    private static final Scheduler IO = new IOScheduler();
    private static final Scheduler COMPUTATION = new ComputationScheduler();
    private static final Scheduler EVENT_LOOP = new EventLoopScheduler();
    
    /**
     * Returns a Scheduler that executes tasks sequentially on the current thread.
     * 
     * <p>The trampoline scheduler queues tasks and executes them one at a time on
     * the thread that invoked the schedule method. This avoids recursive stack growth
     * by using a queue instead of recursion.
     * 
     * <p><strong>Use cases:</strong>
     * <ul>
     *   <li>Testing - predictable execution without thread switching</li>
     *   <li>Avoiding thread context switches</li>
     *   <li>Sequential execution on the calling thread</li>
     * </ul>
     * 
     * <p>Example:
     * <pre>{@code
     * Observable.just(1, 2, 3)
     *     .subscribeOn(Schedulers.trampoline())
     *     .subscribe(v -> System.out.println(Thread.currentThread().getName()));
     * // All items emitted on the calling thread
     * }</pre>
     * 
     * @return the trampoline Scheduler instance
     */
    public static Scheduler trampoline() {
        return TRAMPOLINE;
    }
    
    /**
     * Returns a Scheduler that creates a new thread for each unit of work.
     * 
     * <p>Each task scheduled on this scheduler runs on its own dedicated thread.
     * Threads are not pooled or reused.
     * 
     * <p><strong>Use cases:</strong>
     * <ul>
     *   <li>Long-running operations that shouldn't share threads</li>
     *   <li>Operations requiring thread isolation</li>
     *   <li>Tasks with specific thread requirements</li>
     * </ul>
     * 
     * <p><strong>Warning:</strong> Creating too many threads can exhaust system resources.
     * Consider using {@link #io()} for I/O operations instead.
     * 
     * <p>Example:
     * <pre>{@code
     * Observable.fromCallable(() -> longRunningTask())
     *     .subscribeOn(Schedulers.newThread())
     *     .subscribe(result -> System.out.println(result));
     * }</pre>
     * 
     * @return the new thread Scheduler instance
     */
    public static Scheduler newThread() {
        return NEW_THREAD;
    }
    
    /**
     * Returns a Scheduler optimized for I/O-bound operations.
     * 
     * <p>This scheduler uses an unbounded, cached thread pool that grows as needed
     * and shrinks when threads are idle. It's designed for operations that spend
     * most of their time waiting (network requests, file I/O, database queries).
     * 
     * <p><strong>Use cases:</strong>
     * <ul>
     *   <li>Network requests and HTTP calls</li>
     *   <li>File system operations (read/write)</li>
     *   <li>Database queries</li>
     *   <li>Any blocking I/O operations</li>
     * </ul>
     * 
     * <p>Example:
     * <pre>{@code
     * Observable.fromCallable(() -> {
     *     return httpClient.get("https://api.example.com/data");
     * })
     * .subscribeOn(Schedulers.io())
     * .subscribe(response -> processResponse(response));
     * }</pre>
     * 
     * @return the I/O Scheduler instance
     */
    public static Scheduler io() {
        return IO;
    }
    
    /**
     * Returns a Scheduler optimized for CPU-intensive computational work.
     * 
     * <p>This scheduler uses a fixed-size thread pool with one thread per available
     * processor core. It's designed for computational tasks that fully utilize CPU
     * resources without blocking.
     * 
     * <p><strong>Use cases:</strong>
     * <ul>
     *   <li>Data processing and transformations</li>
     *   <li>Algorithms and calculations</li>
     *   <li>Image/video processing</li>
     *   <li>Parsing and serialization</li>
     * </ul>
     * 
     * <p><strong>Warning:</strong> Do NOT use for blocking I/O operations as this
     * can starve the limited thread pool.
     * 
     * <p>Example:
     * <pre>{@code
     * Observable.range(1, 1000000)
     *     .map(n -> calculatePrime(n))  // CPU-intensive
     *     .observeOn(Schedulers.computation())
     *     .subscribe(prime -> System.out.println(prime));
     * }</pre>
     * 
     * @return the computation Scheduler instance
     */
    public static Scheduler computation() {
        return COMPUTATION;
    }
    
    /**
     * Returns an event loop Scheduler for continuous event processing.
     * 
     * <p>This scheduler maintains a pool of event loop threads optimized for
     * event-driven programming patterns. Each worker gets a dedicated event loop
     * thread for processing events sequentially.
     * 
     * <p><strong>Use cases:</strong>
     * <ul>
     *   <li>Event-driven applications</li>
     *   <li>Message processing systems</li>
     *   <li>Reactive event handlers</li>
     * </ul>
     * 
     * <p>Example:
     * <pre>{@code
     * Observable<Event> eventStream = getEventStream();
     * eventStream
     *     .observeOn(Schedulers.eventLoop())
     *     .subscribe(event -> handleEvent(event));
     * }</pre>
     * 
     * @return the event loop Scheduler instance
     */
    public static Scheduler eventLoop() {
        return EVENT_LOOP;
    }
    
    /**
     * Creates a Scheduler backed by the specified {@link java.util.concurrent.Executor}.
     * 
     * <p>This allows integration with existing thread pools and custom execution
     * frameworks. The returned Scheduler delegates all task execution to the
     * provided Executor.
     * 
     * <p><strong>Use cases:</strong>
     * <ul>
     *   <li>Integration with existing ExecutorService instances</li>
     *   <li>Custom thread pool configurations</li>
     *   <li>Framework-specific executors (e.g., ForkJoinPool)</li>
     *   <li>Application server managed thread pools</li>
     * </ul>
     * 
     * <p>Example:
     * <pre>{@code
     * ExecutorService customPool = Executors.newFixedThreadPool(4);
     * Scheduler customScheduler = Schedulers.from(customPool);
     * 
     * Observable.range(1, 100)
     *     .subscribeOn(customScheduler)
     *     .subscribe(v -> process(v));
     * 
     * // Don't forget to shutdown when done
     * customPool.shutdown();
     * }</pre>
     * 
     * @param executor the Executor to use for task execution
     * @return a Scheduler that delegates to the provided Executor
     */
    public static Scheduler from(java.util.concurrent.Executor executor) {
        return new ExecutorScheduler(executor);
    }
}
