package com.reactive.testing;

import com.reactive.core.Observable;
import org.junit.jupiter.api.Test;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

public class TestingUtilitiesTest {
    
    @Test
    public void testTestObserver_basicAssertions() {
        TestObserver<Integer> testObserver = new TestObserver<>();
        
        Observable.just(1, 2, 3)
            .subscribe(testObserver);
        
        testObserver
            .assertNoErrors()
            .assertComplete()
            .assertValueCount(3)
            .assertValues(1, 2, 3);
    }
    
    @Test
    public void testTestObserver_errorHandling() {
        TestObserver<Integer> testObserver = new TestObserver<>();
        
        Observable.<Integer>error(new RuntimeException("Test error"))
            .subscribe(testObserver);
        
        testObserver
            .assertError(RuntimeException.class)
            .assertNotComplete()
            .assertEmpty();
    }
    
    @Test
    public void testTestObserver_awaitTerminalEvent() throws InterruptedException {
        TestObserver<Long> testObserver = new TestObserver<>();
        
        Observable.timer(100, TimeUnit.MILLISECONDS)
            .subscribe(testObserver);
        
        testObserver.await(200, TimeUnit.MILLISECONDS);
        
        testObserver
            .assertNoErrors()
            .assertComplete()
            .assertValueCount(1);
    }
    
    @Test
    public void testTestScheduler_advanceTime() {
        TestScheduler scheduler = new TestScheduler();
        TestObserver<Long> testObserver = new TestObserver<>();
        
        Observable.interval(100, TimeUnit.MILLISECONDS, scheduler)
            .take(3)
            .subscribe(testObserver);
        
        // Initially no values
        assertEquals(0, testObserver.values().size());
        
        // Advance time by 150ms - should emit 1 value
        scheduler.advanceTimeBy(150, TimeUnit.MILLISECONDS);
        assertEquals(1, testObserver.values().size());
        
        // Advance by another 200ms - should emit 2 more values
        scheduler.advanceTimeBy(200, TimeUnit.MILLISECONDS);
        assertEquals(3, testObserver.values().size());
        
        testObserver.assertValues(0L, 1L, 2L);
    }
    
    @Test
    public void testTestScheduler_triggerActions() {
        TestScheduler scheduler = new TestScheduler();
        TestObserver<Integer> testObserver = new TestObserver<>();
        
        scheduler.scheduleDirect(() -> testObserver.onNext(1), 100, TimeUnit.MILLISECONDS);
        scheduler.scheduleDirect(() -> testObserver.onNext(2), 200, TimeUnit.MILLISECONDS);
        
        assertEquals(0, testObserver.values().size());
        
        scheduler.triggerActions();
        assertEquals(1, testObserver.values().size());
        assertEquals(1, testObserver.values().get(0));
        
        scheduler.triggerActions();
        assertEquals(2, testObserver.values().size());
        assertEquals(2, testObserver.values().get(1));
    }
    
    @Test
    public void testTestScheduler_periodicTask() {
        TestScheduler scheduler = new TestScheduler();
        TestObserver<Integer> testObserver = new TestObserver<>();
        
        java.util.concurrent.atomic.AtomicInteger counter = new java.util.concurrent.atomic.AtomicInteger(0);
        
        scheduler.schedulePeriodic(() -> {
            testObserver.onNext(counter.incrementAndGet());
        }, 0, 100, TimeUnit.MILLISECONDS);
        
        // Advance time to trigger multiple executions
        scheduler.advanceTimeBy(350, TimeUnit.MILLISECONDS);
        
        assertTrue(testObserver.values().size() >= 3, "Should execute periodic task multiple times");
    }
    
    @Test
    public void testTestScheduler_now() {
        TestScheduler scheduler = new TestScheduler();
        
        assertEquals(0, scheduler.nowMillis());
        
        scheduler.advanceTimeBy(500, TimeUnit.MILLISECONDS);
        assertEquals(500, scheduler.nowMillis());
        
        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        assertEquals(1500, scheduler.nowMillis());
    }
}
