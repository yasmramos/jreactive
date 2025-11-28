package com.reactive.testing;

import com.reactive.core.Disposable;
import com.reactive.core.Scheduler;
import java.util.PriorityQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A scheduler for testing that allows manual control of time progression.
 * <p>
 * TestScheduler is a special scheduler implementation that operates on virtual time,
 * allowing you to test time-dependent reactive operations deterministically without
 * waiting for real time to pass. This is invaluable for testing delayed emissions,
 * intervals, timeouts, and other time-based operators.
 * <p>
 * <strong>Key Features:</strong>
 * <ul>
 *   <li><strong>Virtual Time:</strong> Controls time progression manually</li>
 *   <li><strong>Deterministic Testing:</strong> No race conditions or timing issues</li>
 *   <li><strong>Fast Execution:</strong> Tests run instantly without real delays</li>
 *   <li><strong>Task Scheduling:</strong> All scheduled tasks are tracked and executed in order</li>
 * </ul>
 * <p>
 * <strong>Common Testing Patterns:</strong>
 * <ul>
 *   <li>Testing delay() operators</li>
 *   <li>Testing interval() emissions</li>
 *   <li>Testing timeout() behavior</li>
 *   <li>Testing debounce() and throttle() operators</li>
 *   <li>Testing scheduled periodic work</li>
 * </ul>
 * <p>
 * <strong>Example - Testing delayed emission:</strong>
 * <pre>{@code
 * TestScheduler scheduler = new TestScheduler();
 * TestObserver<Long> observer = new TestObserver<>();
 * 
 * Observable.just(1L)
 *     .delay(5, TimeUnit.SECONDS, scheduler)
 *     .subscribe(observer);
 * 
 * observer.assertEmpty(); // Nothing emitted yet
 * 
 * scheduler.advanceTimeBy(5, TimeUnit.SECONDS);
 * observer.assertValue(1L); // Now emitted
 * }</pre>
 * <p>
 * <strong>Example - Testing interval:</strong>
 * <pre>{@code
 * TestScheduler scheduler = new TestScheduler();
 * TestObserver<Long> observer = new TestObserver<>();
 * 
 * Observable.interval(1, TimeUnit.SECONDS, scheduler)
 *     .take(3)
 *     .subscribe(observer);
 * 
 * scheduler.advanceTimeBy(3, TimeUnit.SECONDS);
 * observer.assertValues(0L, 1L, 2L);
 * }</pre>
 * <p>
 * <strong>Example - Testing timeout:</strong>
 * <pre>{@code
 * TestScheduler scheduler = new TestScheduler();
 * TestObserver<String> observer = new TestObserver<>();
 * 
 * Observable.<String>never()
 *     .timeout(10, TimeUnit.SECONDS, scheduler)
 *     .subscribe(observer);
 * 
 * scheduler.advanceTimeBy(10, TimeUnit.SECONDS);
 * observer.assertError(TimeoutException.class);
 * }</pre>
 *
 * @see Scheduler
 * @see TestObserver
 * @since 1.0
 */
public class TestScheduler implements Scheduler {
    private final AtomicLong virtualTime = new AtomicLong(0);
    private final PriorityQueue<ScheduledTask> taskQueue = new PriorityQueue<>();
    private volatile boolean shutdown = false;
    
    @Override
    public Worker createWorker() {
        return new TestWorker();
    }
    
    @Override
    public Disposable scheduleDirect(Runnable task) {
        return scheduleDirect(task, 0, TimeUnit.NANOSECONDS);
    }
    
    @Override
    public Disposable scheduleDirect(Runnable task, long delay, TimeUnit unit) {
        long delayNanos = unit.toNanos(delay);
        long executeAt = virtualTime.get() + delayNanos;
        
        ScheduledTask scheduledTask = new ScheduledTask(task, executeAt, false, 0);
        synchronized (taskQueue) {
            taskQueue.offer(scheduledTask);
        }
        
        return Disposable.fromRunnable(() -> {
            synchronized (taskQueue) {
                taskQueue.remove(scheduledTask);
            }
        });
    }
    
    @Override
    public Disposable schedulePeriodic(Runnable task, long initialDelay, long period, TimeUnit unit) {
        long initialDelayNanos = unit.toNanos(initialDelay);
        long periodNanos = unit.toNanos(period);
        long executeAt = virtualTime.get() + initialDelayNanos;
        
        ScheduledTask scheduledTask = new ScheduledTask(task, executeAt, true, periodNanos);
        synchronized (taskQueue) {
            taskQueue.offer(scheduledTask);
        }
        
        return Disposable.fromRunnable(() -> {
            synchronized (taskQueue) {
                taskQueue.remove(scheduledTask);
            }
        });
    }
    
    @Override
    public void shutdown() {
        shutdown = true;
        synchronized (taskQueue) {
            taskQueue.clear();
        }
    }
    
    /**
     * Advances virtual time by the specified amount and executes all tasks scheduled up to that time.
     * <p>
     * This method moves the virtual clock forward and executes all scheduled tasks whose
     * execution time is less than or equal to the new time. Tasks are executed in chronological
     * order. Periodic tasks are rescheduled for their next execution time.
     * <p>
     * <strong>Example - Advance by specific duration:</strong>
     * <pre>{@code
     * scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);
     * // All tasks scheduled within first 100ms are executed
     * }</pre>
     * <p>
     * <strong>Example - Testing intervals:</strong>
     * <pre>{@code
     * Observable.interval(1, TimeUnit.SECONDS, scheduler)
     *     .subscribe(observer);
     * 
     * scheduler.advanceTimeBy(5, TimeUnit.SECONDS);
     * // Executes 5 interval emissions (0, 1, 2, 3, 4)
     * }</pre>
     *
     * @param delayTime the amount of time to advance
     * @param unit the time unit of delayTime
     */
    public void advanceTimeBy(long delayTime, TimeUnit unit) {
        if (shutdown) {
            return;
        }
        
        long delayNanos = unit.toNanos(delayTime);
        long targetTime = virtualTime.get() + delayNanos;
        
        while (true) {
            ScheduledTask task;
            synchronized (taskQueue) {
                task = taskQueue.peek();
                if (task == null || task.executeAt > targetTime) {
                    break;
                }
                taskQueue.poll();
            }
            
            virtualTime.set(task.executeAt);
            task.run();
            
            if (task.isPeriodic && !task.cancelled) {
                task.executeAt += task.period;
                synchronized (taskQueue) {
                    taskQueue.offer(task);
                }
            }
        }
        
        virtualTime.set(targetTime);
    }
    
    /**
     * Advances virtual time to the next scheduled task and executes it.
     * <p>
     * This method jumps to the time of the next pending task (regardless of how far in
     * the future it is) and executes only that task. This is useful for stepping through
     * scheduled tasks one at a time.
     * <p>
     * If there are no pending tasks, this method does nothing.
     * <p>
     * <strong>Example - Step through tasks:</strong>
     * <pre>{@code
     * Observable.timer(1, TimeUnit.HOURS, scheduler)
     *     .subscribe(observer1);
     * Observable.timer(2, TimeUnit.HOURS, scheduler)
     *     .subscribe(observer2);
     * 
     * scheduler.triggerActions(); // Executes first task (1 hour)
     * scheduler.triggerActions(); // Executes second task (2 hours)
     * }</pre>
     * <p>
     * <strong>Example - Debugging scheduled tasks:</strong>
     * <pre>{@code
     * while (scheduler.getPendingTaskCount() > 0) {
     *     System.out.println("Time: " + scheduler.nowMillis());
     *     scheduler.triggerActions();
     * }
     * }</pre>
     */
    public void triggerActions() {
        if (shutdown) {
            return;
        }
        
        ScheduledTask task;
        synchronized (taskQueue) {
            task = taskQueue.poll();
        }
        
        if (task != null) {
            virtualTime.set(task.executeAt);
            task.run();
            
            if (task.isPeriodic && !task.cancelled) {
                task.executeAt += task.period;
                synchronized (taskQueue) {
                    taskQueue.offer(task);
                }
            }
        }
    }
    
    /**
     * Returns the current virtual time in the specified time unit.
     * <p>
     * The virtual time starts at 0 and only advances when advanceTimeBy() or
     * triggerActions() is called.
     * <p>
     * <strong>Example:</strong>
     * <pre>{@code
     * long seconds = scheduler.now(TimeUnit.SECONDS);
     * System.out.println("Virtual time: " + seconds + " seconds");
     * }</pre>
     *
     * @param unit the time unit to convert to
     * @return the current virtual time in the specified unit
     */
    public long now(TimeUnit unit) {
        return unit.convert(virtualTime.get(), TimeUnit.NANOSECONDS);
    }
    
    /**
     * Returns the current virtual time in milliseconds.
     * <p>
     * This is a convenience method equivalent to {@code now(TimeUnit.MILLISECONDS)}.
     * <p>
     * <strong>Example:</strong>
     * <pre>{@code
     * assertEquals(5000, scheduler.nowMillis()); // 5 seconds
     * }</pre>
     *
     * @return the current virtual time in milliseconds
     */
    public long nowMillis() {
        return now(TimeUnit.MILLISECONDS);
    }
    
    /**
     * Returns the number of pending tasks in the queue.
     * <p>
     * This includes all scheduled tasks (one-time and periodic) that have not
     * yet been executed. Useful for verifying that tasks were scheduled or for
     * debugging test scenarios.
     * <p>
     * <strong>Example - Verify task scheduling:</strong>
     * <pre>{@code
     * Observable.timer(1, TimeUnit.SECONDS, scheduler)
     *     .subscribe(observer);
     * 
     * assertEquals(1, scheduler.getPendingTaskCount());
     * 
     * scheduler.advanceTimeBy(1, TimeUnit.SECONDS);
     * assertEquals(0, scheduler.getPendingTaskCount()); // Task executed
     * }</pre>
     * <p>
     * <strong>Example - Test cleanup:</strong>
     * <pre>{@code
     * // Ensure all scheduled work completed
     * assertEquals(0, scheduler.getPendingTaskCount(),
     *     "Expected all tasks to be executed");
     * }</pre>
     *
     * @return the number of pending tasks
     */
    public int getPendingTaskCount() {
        synchronized (taskQueue) {
            return taskQueue.size();
        }
    }
    
    private class TestWorker implements Worker {
        private volatile boolean disposed = false;
        
        @Override
        public Disposable schedule(Runnable task) {
            return schedule(task, 0, TimeUnit.NANOSECONDS);
        }
        
        @Override
        public Disposable schedule(Runnable task, long delay, TimeUnit unit) {
            if (disposed) {
                return Disposable.empty();
            }
            return TestScheduler.this.scheduleDirect(task, delay, unit);
        }
        
        @Override
        public Disposable schedulePeriodic(Runnable task, long initialDelay, long period, TimeUnit unit) {
            if (disposed) {
                return Disposable.empty();
            }
            return TestScheduler.this.schedulePeriodic(task, initialDelay, period, unit);
        }
        
        @Override
        public void dispose() {
            disposed = true;
        }
        
        @Override
        public boolean isDisposed() {
            return disposed;
        }
    }
    
    private static class ScheduledTask implements Comparable<ScheduledTask>, Runnable {
        final Runnable task;
        long executeAt;
        final boolean isPeriodic;
        final long period;
        volatile boolean cancelled = false;
        
        ScheduledTask(Runnable task, long executeAt, boolean isPeriodic, long period) {
            this.task = task;
            this.executeAt = executeAt;
            this.isPeriodic = isPeriodic;
            this.period = period;
        }
        
        @Override
        public void run() {
            if (!cancelled) {
                task.run();
            }
        }
        
        @Override
        public int compareTo(ScheduledTask other) {
            return Long.compare(this.executeAt, other.executeAt);
        }
    }
}
