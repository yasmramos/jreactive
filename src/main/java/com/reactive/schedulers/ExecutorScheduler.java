package com.reactive.schedulers;

import com.reactive.core.Disposable;
import com.reactive.core.Scheduler;
import java.util.concurrent.*;

public class ExecutorScheduler implements Scheduler {
    private final Executor executor;
    
    public ExecutorScheduler(Executor executor) {
        this.executor = executor;
    }
    
    @Override
    public Worker createWorker() {
        return new ExecutorWorker(executor);
    }
    
    @Override
    public Disposable scheduleDirect(Runnable task) {
        executor.execute(task);
        return Disposable.empty();
    }
    
    @Override
    public Disposable scheduleDirect(Runnable task, long delay, TimeUnit unit) {
        throw new UnsupportedOperationException("Executor scheduler does not support delayed execution");
    }
    
    @Override
    public Disposable schedulePeriodic(Runnable task, long initialDelay, long period, TimeUnit unit) {
        throw new UnsupportedOperationException("Executor scheduler does not support periodic execution");
    }
    
    @Override
    public void shutdown() {
    }
    
    static class ExecutorWorker implements Worker {
        private final Executor executor;
        private volatile boolean disposed;
        
        ExecutorWorker(Executor executor) {
            this.executor = executor;
        }
        
        @Override
        public Disposable schedule(Runnable task) {
            if (disposed) {
                return Disposable.empty();
            }
            executor.execute(task);
            return Disposable.empty();
        }
        
        @Override
        public Disposable schedule(Runnable task, long delay, TimeUnit unit) {
            throw new UnsupportedOperationException("Executor scheduler does not support delayed execution");
        }
        
        @Override
        public Disposable schedulePeriodic(Runnable task, long initialDelay, long period, TimeUnit unit) {
            throw new UnsupportedOperationException("Executor scheduler does not support periodic execution");
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
