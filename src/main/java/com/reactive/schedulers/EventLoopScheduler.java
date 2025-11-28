package com.reactive.schedulers;

import com.reactive.core.Disposable;
import com.reactive.core.Scheduler;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Advanced Scheduler that uses an event loop pattern.
 * Distributes work across a fixed number of single-threaded event loops.
 * Ideal for I/O-bound operations and maintaining thread affinity.
 */
public class EventLoopScheduler implements Scheduler {
    private final EventLoop[] eventLoops;
    private final AtomicInteger index = new AtomicInteger();
    private final int poolSize;
    
    public EventLoopScheduler() {
        this(Runtime.getRuntime().availableProcessors());
    }
    
    public EventLoopScheduler(int poolSize) {
        this.poolSize = poolSize;
        this.eventLoops = new EventLoop[poolSize];
        
        for (int i = 0; i < poolSize; i++) {
            eventLoops[i] = new EventLoop("EventLoop-" + i);
        }
    }
    
    private EventLoop nextEventLoop() {
        return eventLoops[Math.abs(index.getAndIncrement() % poolSize)];
    }
    
    @Override
    public Worker createWorker() {
        return new EventLoopWorker(nextEventLoop());
    }
    
    @Override
    public Disposable scheduleDirect(Runnable task) {
        return nextEventLoop().schedule(task);
    }
    
    @Override
    public Disposable scheduleDirect(Runnable task, long delay, TimeUnit unit) {
        return nextEventLoop().schedule(task, delay, unit);
    }
    
    @Override
    public Disposable schedulePeriodic(Runnable task, long initialDelay, long period, TimeUnit unit) {
        return nextEventLoop().schedulePeriodic(task, initialDelay, period, unit);
    }
    
    @Override
    public void shutdown() {
        for (EventLoop eventLoop : eventLoops) {
            eventLoop.shutdown();
        }
    }
    
    static class EventLoop {
        private final ScheduledExecutorService executor;
        private final String name;
        
        EventLoop(String name) {
            this.name = name;
            this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, name);
                t.setDaemon(true);
                return t;
            });
        }
        
        Disposable schedule(Runnable task) {
            Future<?> future = executor.submit(task);
            return Disposable.fromRunnable(() -> future.cancel(false));
        }
        
        Disposable schedule(Runnable task, long delay, TimeUnit unit) {
            ScheduledFuture<?> future = executor.schedule(task, delay, unit);
            return Disposable.fromRunnable(() -> future.cancel(false));
        }
        
        Disposable schedulePeriodic(Runnable task, long initialDelay, long period, TimeUnit unit) {
            ScheduledFuture<?> future = executor.scheduleAtFixedRate(task, initialDelay, period, unit);
            return Disposable.fromRunnable(() -> future.cancel(false));
        }
        
        void shutdown() {
            executor.shutdown();
        }
    }
    
    static class EventLoopWorker implements Worker {
        private final EventLoop eventLoop;
        private volatile boolean disposed;
        
        EventLoopWorker(EventLoop eventLoop) {
            this.eventLoop = eventLoop;
        }
        
        @Override
        public Disposable schedule(Runnable task) {
            if (disposed) {
                return Disposable.empty();
            }
            return eventLoop.schedule(task);
        }
        
        @Override
        public Disposable schedule(Runnable task, long delay, TimeUnit unit) {
            if (disposed) {
                return Disposable.empty();
            }
            return eventLoop.schedule(task, delay, unit);
        }
        
        @Override
        public Disposable schedulePeriodic(Runnable task, long initialDelay, long period, TimeUnit unit) {
            if (disposed) {
                return Disposable.empty();
            }
            return eventLoop.schedulePeriodic(task, initialDelay, period, unit);
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
}
