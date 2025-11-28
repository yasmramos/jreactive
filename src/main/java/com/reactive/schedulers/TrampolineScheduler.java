package com.reactive.schedulers;

import com.reactive.core.Disposable;
import com.reactive.core.Scheduler;
import java.util.ArrayDeque;
import java.util.concurrent.*;

public class TrampolineScheduler implements Scheduler {
    private static final ThreadLocal<ArrayDeque<Runnable>> QUEUE = ThreadLocal.withInitial(ArrayDeque::new);
    
    @Override
    public Worker createWorker() {
        return new TrampolineWorker();
    }
    
    @Override
    public Disposable scheduleDirect(Runnable task) {
        ArrayDeque<Runnable> queue = QUEUE.get();
        queue.offer(task);
        
        if (queue.size() == 1) {
            while (!queue.isEmpty()) {
                Runnable r = queue.poll();
                if (r != null) {
                    r.run();
                }
            }
        }
        return Disposable.empty();
    }
    
    @Override
    public Disposable scheduleDirect(Runnable task, long delay, TimeUnit unit) {
        throw new UnsupportedOperationException("Trampoline scheduler does not support delayed execution");
    }
    
    @Override
    public Disposable schedulePeriodic(Runnable task, long initialDelay, long period, TimeUnit unit) {
        throw new UnsupportedOperationException("Trampoline scheduler does not support periodic execution");
    }
    
    @Override
    public void shutdown() {
    }
    
    static class TrampolineWorker implements Worker {
        private volatile boolean disposed;
        
        @Override
        public Disposable schedule(Runnable task) {
            if (disposed) {
                return Disposable.empty();
            }
            
            ArrayDeque<Runnable> queue = QUEUE.get();
            queue.offer(task);
            
            if (queue.size() == 1) {
                while (!queue.isEmpty() && !disposed) {
                    Runnable r = queue.poll();
                    if (r != null) {
                        r.run();
                    }
                }
            }
            return Disposable.empty();
        }
        
        @Override
        public Disposable schedule(Runnable task, long delay, TimeUnit unit) {
            throw new UnsupportedOperationException("Trampoline scheduler does not support delayed execution");
        }
        
        @Override
        public Disposable schedulePeriodic(Runnable task, long initialDelay, long period, TimeUnit unit) {
            throw new UnsupportedOperationException("Trampoline scheduler does not support periodic execution");
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
