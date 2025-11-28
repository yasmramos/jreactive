package com.reactive.schedulers;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * Interfaz para ejecutar tareas en diferentes threads
 */
public interface Scheduler {
    
    /**
     * Ejecuta una tarea en este Scheduler
     */
    void execute(Runnable task);
    
    /**
     * Ejecuta una tarea con delay
     */
    void schedule(Runnable task, long delay, TimeUnit unit);
    
    /**
     * Ejecuta una tarea periódicamente
     */
    void schedulePeriodically(Runnable task, long initialDelay, long period, TimeUnit unit);
}
