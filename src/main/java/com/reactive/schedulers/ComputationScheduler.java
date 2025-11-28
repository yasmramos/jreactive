package com.reactive.schedulers;

import com.reactive.core.Disposable;
import com.reactive.core.Scheduler;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Advanced Scheduler optimized for computational work.
 * Uses a fixed thread pool sized to the number of available processors.
 * Ideal for CPU-intensive operations.
 */
public class ComputationScheduler implements Scheduler {
    private final ScheduledExecutorService[] executors;
    private final AtomicInteger index = new AtomicInteger();
    private final int poolSize;
    
    public ComputationScheduler() {
        this.poolSize = Runtime.getRuntime().availableProcessors();
        this.executors = new ScheduledExecutorService[poolSize];
        
        for (int i = 0; i < poolSize; i++) {
            final int idx = i;
            executors[i] = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "RxComputationScheduler-" + idx);
                t.setDaemon(true);
                return t;
            });
        }
    }
    
    private ScheduledExecutorService nextExecutor() {
        return executors[Math.abs(index.getAndIncrement() % poolSize)];
    }
    
    @Override
    public Worker createWorker() {
        return new ComputationWorker(nextExecutor());
    }
    
    @Override
    public Disposable scheduleDirect(Runnable task) {
        Future<?> future = nextExecutor().submit(task);
        return Disposable.fromRunnable(() -> future.cancel(false));
    }
    
    @Override
    public Disposable scheduleDirect(Runnable task, long delay, TimeUnit unit) {
        ScheduledFuture<?> future = nextExecutor().schedule(task, delay, unit);
        return Disposable.fromRunnable(() -> future.cancel(false));
    }
    
    @Override
    public Disposable schedulePeriodic(Runnable task, long initialDelay, long period, TimeUnit unit) {
        ScheduledFuture<?> future = nextExecutor().scheduleAtFixedRate(task, initialDelay, period, unit);
        return Disposable.fromRunnable(() -> future.cancel(false));
    }
    
    @Override
    public void shutdown() {
        for (ScheduledExecutorService executor : executors) {
            executor.shutdown();
        }
    }
    
    static class ComputationWorker implements Worker {
        private final ScheduledExecutorService executor;
        private volatile boolean disposed;
        
        ComputationWorker(ScheduledExecutorService executor) {
            this.executor = executor;
        }
        
        @Override
        public Disposable schedule(Runnable task) {
            if (disposed) {
                return Disposable.empty();
            }
            Future<?> future = executor.submit(task);
            return Disposable.fromRunnable(() -> future.cancel(false));
        }
        
        @Override
        public Disposable schedule(Runnable task, long delay, TimeUnit unit) {
            if (disposed) {
                return Disposable.empty();
            }
            ScheduledFuture<?> future = executor.schedule(task, delay, unit);
            return Disposable.fromRunnable(() -> future.cancel(false));
        }
        
        @Override
        public Disposable schedulePeriodic(Runnable task, long initialDelay, long period, TimeUnit unit) {
            if (disposed) {
                return Disposable.empty();
            }
            ScheduledFuture<?> future = executor.scheduleAtFixedRate(task, initialDelay, period, unit);
            return Disposable.fromRunnable(() -> future.cancel(false));
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
