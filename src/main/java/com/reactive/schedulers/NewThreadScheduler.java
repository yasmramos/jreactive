package com.reactive.schedulers;

import com.reactive.core.Disposable;
import com.reactive.core.Scheduler;
import java.util.concurrent.*;

public class NewThreadScheduler implements Scheduler {
    @Override
    public Worker createWorker() {
        return new NewThreadWorker();
    }
    
    @Override
    public Disposable scheduleDirect(Runnable task) {
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
        return Disposable.fromRunnable(() -> thread.interrupt());
    }
    
    @Override
    public Disposable scheduleDirect(Runnable task, long delay, TimeUnit unit) {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        ScheduledFuture<?> future = executor.schedule(task, delay, unit);
        return Disposable.fromRunnable(() -> {
            future.cancel(false);
            executor.shutdown();
        });
    }
    
    @Override
    public Disposable schedulePeriodic(Runnable task, long initialDelay, long period, TimeUnit unit) {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        ScheduledFuture<?> future = executor.scheduleAtFixedRate(task, initialDelay, period, unit);
        return Disposable.fromRunnable(() -> {
            future.cancel(false);
            executor.shutdown();
        });
    }
    
    @Override
    public void shutdown() {
    }
    
    static class NewThreadWorker implements Worker {
        private final ScheduledExecutorService executor;
        private volatile boolean disposed;
        
        NewThreadWorker() {
            this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r);
                t.setDaemon(true);
                return t;
            });
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
            if (!disposed) {
                disposed = true;
                executor.shutdown();
            }
        }
        
        @Override
        public boolean isDisposed() {
            return disposed;
        }
    }
}
