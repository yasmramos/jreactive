package com.reactive.hooks;

import com.reactive.core.Observable;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Global hooks for intercepting reactive stream events.
 * Useful for debugging, logging, and monitoring.
 */
public final class RxJavaHooks {
    private static volatile Consumer<Throwable> errorHandler;
    private static volatile Function<Runnable, Runnable> scheduleHandler;
    private static volatile Function<Observable, Observable> onObservableAssembly;
    private static volatile BiFunction<Observable, Object, Observable> onObservableSubscribe;
    
    private RxJavaHooks() {
        // Utility class
    }
    
    /**
     * Sets a global error handler for unhandled errors.
     * This is called when an error is not caught by any observer.
     */
    public static void setErrorHandler(Consumer<Throwable> handler) {
        errorHandler = handler;
    }
    
    /**
     * Gets the current error handler.
     */
    public static Consumer<Throwable> getErrorHandler() {
        return errorHandler;
    }
    
    /**
     * Invokes the error handler if one is set.
     */
    public static void onError(Throwable error) {
        Consumer<Throwable> handler = errorHandler;
        if (handler != null) {
            try {
                handler.accept(error);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        } else {
            error.printStackTrace();
        }
    }
    
    /**
     * Sets a hook that wraps every scheduled action.
     * Useful for tracking or modifying scheduled tasks.
     */
    public static void setScheduleHandler(Function<Runnable, Runnable> handler) {
        scheduleHandler = handler;
    }
    
    /**
     * Gets the current schedule handler.
     */
    public static Function<Runnable, Runnable> getScheduleHandler() {
        return scheduleHandler;
    }
    
    /**
     * Wraps a scheduled action with the schedule handler if one is set.
     */
    public static Runnable onSchedule(Runnable run) {
        Function<Runnable, Runnable> handler = scheduleHandler;
        if (handler != null) {
            return handler.apply(run);
        }
        return run;
    }
    
    /**
     * Sets a hook that is called when an Observable is assembled (created).
     * Useful for tracking Observable creation and applying global transformations.
     */
    public static void setOnObservableAssembly(Function<Observable, Observable> handler) {
        onObservableAssembly = handler;
    }
    
    /**
     * Gets the current Observable assembly hook.
     */
    public static Function<Observable, Observable> getOnObservableAssembly() {
        return onObservableAssembly;
    }
    
    /**
     * Applies the Observable assembly hook if one is set.
     */
    @SuppressWarnings("unchecked")
    public static <T> Observable<T> onAssembly(Observable<T> observable) {
        Function<Observable, Observable> handler = onObservableAssembly;
        if (handler != null) {
            return (Observable<T>) handler.apply(observable);
        }
        return observable;
    }
    
    /**
     * Sets a hook that is called when an observer subscribes to an Observable.
     * Useful for monitoring subscriptions.
     */
    public static void setOnObservableSubscribe(BiFunction<Observable, Object, Observable> handler) {
        onObservableSubscribe = handler;
    }
    
    /**
     * Gets the current Observable subscribe hook.
     */
    public static BiFunction<Observable, Object, Observable> getOnObservableSubscribe() {
        return onObservableSubscribe;
    }
    
    /**
     * Applies the Observable subscribe hook if one is set.
     */
    @SuppressWarnings("unchecked")
    public static <T> Observable<T> onSubscribe(Observable<T> observable, Object observer) {
        BiFunction<Observable, Object, Observable> handler = onObservableSubscribe;
        if (handler != null) {
            return (Observable<T>) handler.apply(observable, observer);
        }
        return observable;
    }
    
    /**
     * Resets all hooks to their default values.
     */
    public static void reset() {
        errorHandler = null;
        scheduleHandler = null;
        onObservableAssembly = null;
        onObservableSubscribe = null;
    }
    
    /**
     * Enables default debugging output.
     * Logs all events to System.out.
     */
    public static void enableDebugMode() {
        setErrorHandler(error -> {
            System.err.println("[RxJava Error] " + error.getClass().getSimpleName() + ": " + error.getMessage());
            error.printStackTrace();
        });
        
        setOnObservableAssembly(observable -> {
            System.out.println("[RxJava Assembly] Observable created: " + observable.getClass().getSimpleName());
            return observable;
        });
        
        setOnObservableSubscribe((observable, observer) -> {
            System.out.println("[RxJava Subscribe] Observer subscribed: " + observer.getClass().getSimpleName());
            return observable;
        });
        
        setScheduleHandler(runnable -> () -> {
            System.out.println("[RxJava Schedule] Task scheduled on thread: " + Thread.currentThread().getName());
            runnable.run();
        });
    }
}
