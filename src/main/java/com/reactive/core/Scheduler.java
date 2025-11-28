package com.reactive.core;

import java.util.concurrent.*;

/**
 * Interface for managing execution contexts and scheduling tasks in reactive streams.
 * 
 * <p>A Scheduler is responsible for controlling where and when reactive operations execute.
 * It provides mechanisms for:
 * <ul>
 *   <li>Executing tasks on specific thread pools or execution contexts</li>
 *   <li>Scheduling delayed and periodic tasks</li>
 *   <li>Managing worker threads for sequential task execution</li>
 *   <li>Resource cleanup and lifecycle management</li>
 * </ul>
 * 
 * <p><strong>Common scheduler types:</strong>
 * <ul>
 *   <li><strong>io()</strong> - For I/O-bound operations (network, disk)</li>
 *   <li><strong>computation()</strong> - For CPU-intensive computations</li>
 *   <li><strong>newThread()</strong> - Creates a new thread for each unit of work</li>
 *   <li><strong>trampoline()</strong> - Executes tasks on the current thread sequentially</li>
 *   <li><strong>from(Executor)</strong> - Custom executor-based scheduler</li>
 * </ul>
 * 
 * <p>Example usage with Observable:
 * <pre>{@code
 * Observable.just(1, 2, 3)
 *     .subscribeOn(Schedulers.io())        // Subscription happens on I/O thread
 *     .observeOn(Schedulers.computation()) // Observation happens on computation thread
 *     .subscribe(System.out::println);
 * }</pre>
 * 
 * @see Schedulers
 * @see Observable#subscribeOn(Scheduler)
 * @see Observable#observeOn(Scheduler)
 */
public interface Scheduler {
    /**
     * Creates a new Worker for sequential task execution.
     * 
     * <p>Workers provide sequential execution semantics - tasks scheduled on the same
     * Worker are guaranteed to execute in order without overlapping. Multiple Workers
     * from the same Scheduler may execute concurrently.
     * 
     * <p>Example:
     * <pre>{@code
     * Scheduler.Worker worker = scheduler.createWorker();
     * worker.schedule(() -> System.out.println("Task 1"));
     * worker.schedule(() -> System.out.println("Task 2"));
     * // Task 2 will execute after Task 1 completes
     * worker.dispose(); // Cleanup when done
     * }</pre>
     * 
     * @return a new Worker instance
     * @see Worker
     */
    Worker createWorker();
    
    /**
     * Schedules a task for immediate execution on this Scheduler.
     * 
     * <p>The task will be executed as soon as possible on a thread managed by this Scheduler.
     * 
     * <p>Example:
     * <pre>{@code
     * Disposable d = scheduler.scheduleDirect(() -> {
     *     System.out.println("Running on: " + Thread.currentThread().getName());
     * });
     * }</pre>
     * 
     * @param task the task to execute
     * @return a Disposable to cancel the scheduled task
     */
    Disposable scheduleDirect(Runnable task);
    /**
     * Schedules a task for delayed execution on this Scheduler.
     * 
     * <p>The task will be executed after the specified delay on a thread managed by this Scheduler.
     * 
     * <p>Example:
     * <pre>{@code
     * Disposable d = scheduler.scheduleDirect(
     *     () -> System.out.println("Delayed task"),
     *     5, TimeUnit.SECONDS
     * );
     * // Task will execute after 5 seconds
     * }</pre>
     * 
     * @param task the task to execute
     * @param delay the time to delay execution
     * @param unit the time unit of the delay parameter
     * @return a Disposable to cancel the scheduled task
     */
    Disposable scheduleDirect(Runnable task, long delay, TimeUnit unit);
    /**
     * Schedules a task for periodic execution on this Scheduler.
     * 
     * <p>The task will be executed repeatedly with a fixed period between executions.
     * The first execution occurs after the initial delay.
     * 
     * <p>Example:
     * <pre>{@code
     * Disposable d = scheduler.schedulePeriodic(
     *     () -> System.out.println("Heartbeat"),
     *     0, 1, TimeUnit.SECONDS
     * );
     * // Prints "Heartbeat" every second
     * d.dispose(); // Stop the periodic execution
     * }</pre>
     * 
     * @param task the task to execute periodically
     * @param initialDelay the time to delay the first execution
     * @param period the period between successive executions
     * @param unit the time unit of the delay and period parameters
     * @return a Disposable to cancel the periodic execution
     */
    Disposable schedulePeriodic(Runnable task, long initialDelay, long period, TimeUnit unit);
    
    /**
     * Shuts down this Scheduler and releases all associated resources.
     * 
     * <p>After shutdown, attempting to schedule new tasks may throw exceptions.
     * Already scheduled tasks may or may not complete.
     * 
     * <p>Example:
     * <pre>{@code
     * Scheduler scheduler = Schedulers.io();
     * // Use scheduler...
     * scheduler.shutdown(); // Cleanup resources
     * }</pre>
     */
    void shutdown();
    
    /**
     * Worker interface for sequential task execution on a Scheduler.
     * 
     * <p>A Worker provides sequential execution guarantees - tasks scheduled on the
     * same Worker instance will never execute concurrently with each other, and will
     * execute in the order they were scheduled.
     * 
     * <p>Workers should be disposed when no longer needed to free resources.
     * 
     * <p>Example:
     * <pre>{@code
     * Scheduler.Worker worker = Schedulers.computation().createWorker();
     * try {
     *     worker.schedule(() -> processData());
     *     worker.schedule(() -> sendResults());
     *     // sendResults() runs AFTER processData() completes
     * } finally {
     *     worker.dispose();
     * }
     * }</pre>
     * 
     * @see Scheduler#createWorker()
     */
    interface Worker extends Disposable {
        /**
         * Schedules a task for immediate execution on this Worker.
         * 
         * <p>The task will execute sequentially with respect to other tasks
         * scheduled on this Worker.
         * 
         * @param task the task to execute
         * @return a Disposable to cancel the scheduled task
         */
        Disposable schedule(Runnable task);
        /**
         * Schedules a task for delayed execution on this Worker.
         * 
         * <p>The task will execute after the specified delay, and will maintain
         * sequential ordering with other tasks on this Worker.
         * 
         * @param task the task to execute
         * @param delay the time to delay execution
         * @param unit the time unit of the delay parameter
         * @return a Disposable to cancel the scheduled task
         */
        Disposable schedule(Runnable task, long delay, TimeUnit unit);
        /**
         * Schedules a task for periodic execution on this Worker.
         * 
         * <p>The task will execute repeatedly with sequential guarantees - each
         * execution completes before the next begins.
         * 
         * @param task the task to execute periodically
         * @param initialDelay the time to delay the first execution
         * @param period the period between successive executions
         * @param unit the time unit of the delay and period parameters
         * @return a Disposable to cancel the periodic execution
         */
        Disposable schedulePeriodic(Runnable task, long initialDelay, long period, TimeUnit unit);
    }
}
