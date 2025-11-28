package com.reactive.hooks;

import com.reactive.core.Observable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

public class HooksTest {
    
    @AfterEach
    public void cleanup() {
        RxJavaHooks.reset();
    }
    
    @Test
    public void testErrorHandler() {
        List<Throwable> errors = new ArrayList<>();
        RxJavaHooks.setErrorHandler(errors::add);
        
        Throwable testError = new RuntimeException("Test error");
        RxJavaHooks.onError(testError);
        
        assertEquals(1, errors.size());
        assertEquals(testError, errors.get(0));
    }
    
    @Test
    public void testScheduleHandler() {
        AtomicInteger executionCount = new AtomicInteger(0);
        
        RxJavaHooks.setScheduleHandler(runnable -> () -> {
            executionCount.incrementAndGet();
            runnable.run();
        });
        
        Runnable task = () -> {};
        Runnable wrapped = RxJavaHooks.onSchedule(task);
        wrapped.run();
        
        assertEquals(1, executionCount.get());
    }
    
    @Test
    public void testOnObservableAssembly() {
        AtomicBoolean hookCalled = new AtomicBoolean(false);
        
        RxJavaHooks.setOnObservableAssembly(observable -> {
            hookCalled.set(true);
            return observable;
        });
        
        Observable<Integer> observable = Observable.just(1, 2, 3);
        RxJavaHooks.onAssembly(observable);
        
        assertTrue(hookCalled.get());
    }
    
    @Test
    public void testOnObservableSubscribe() {
        AtomicBoolean hookCalled = new AtomicBoolean(false);
        
        RxJavaHooks.setOnObservableSubscribe((observable, observer) -> {
            hookCalled.set(true);
            return observable;
        });
        
        Observable<Integer> observable = Observable.just(1, 2, 3);
        Object observer = new Object();
        RxJavaHooks.onSubscribe(observable, observer);
        
        assertTrue(hookCalled.get());
    }
    
    @Test
    public void testReset() {
        RxJavaHooks.setErrorHandler(e -> {});
        RxJavaHooks.setScheduleHandler(r -> r);
        
        assertNotNull(RxJavaHooks.getErrorHandler());
        assertNotNull(RxJavaHooks.getScheduleHandler());
        
        RxJavaHooks.reset();
        
        assertNull(RxJavaHooks.getErrorHandler());
        assertNull(RxJavaHooks.getScheduleHandler());
    }
    
    @Test
    public void testEnableDebugMode() {
        // This test just verifies that enableDebugMode doesn't throw
        RxJavaHooks.enableDebugMode();
        
        assertNotNull(RxJavaHooks.getErrorHandler());
        assertNotNull(RxJavaHooks.getScheduleHandler());
        assertNotNull(RxJavaHooks.getOnObservableAssembly());
        assertNotNull(RxJavaHooks.getOnObservableSubscribe());
    }
}
