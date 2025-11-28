package com.reactive.schedulers;

import com.reactive.core.Disposable;
import com.reactive.core.Scheduler;
import java.util.concurrent.*;

public class IOScheduler implements Scheduler {
    private final ScheduledExecutorService executor;
    
    public IOScheduler() {
        this.executor = Executors.newScheduledThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors()),
            r -> {
                Thread t = new Thread(r);
                t.setName("RxIoScheduler-" + t.getId());
                t.setDaemon(true);
                return t;
            }
        );
    }
    
    @Override
    public Worker createWorker() {
        return new IOWorker(executor);
    }
    
    @Override
    public Disposable scheduleDirect(Runnable task) {
        Future<?> future = executor.submit(task);
        return Disposable.fromRunnable(() -> future.cancel(false));
    }
    
    @Override
    public Disposable scheduleDirect(Runnable task, long delay, TimeUnit unit) {
        ScheduledFuture<?> future = executor.schedule(task, delay, unit);
        return Disposable.fromRunnable(() -> future.cancel(false));
    }
    
    @Override
    public Disposable schedulePeriodic(Runnable task, long initialDelay, long period, TimeUnit unit) {
        ScheduledFuture<?> future = executor.scheduleAtFixedRate(task, initialDelay, period, unit);
        return Disposable.fromRunnable(() -> future.cancel(false));
    }
    
    @Override
    public void shutdown() {
        executor.shutdown();
    }
    
    static class IOWorker implements Worker {
        private final ScheduledExecutorService executor;
        private volatile boolean disposed;
        
        IOWorker(ScheduledExecutorService executor) {
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
