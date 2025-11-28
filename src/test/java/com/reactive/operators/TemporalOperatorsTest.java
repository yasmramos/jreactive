package com.reactive.operators;

import com.reactive.core.Observable;
import com.reactive.testing.TestObserver;
import com.reactive.testing.TestScheduler;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

public class TemporalOperatorsTest {
    
    @Test
    public void testWindow_count() throws InterruptedException {
        TestObserver<Observable<Integer>> testObserver = new TestObserver<>();
        List<List<Integer>> windows = new ArrayList<>();
        
        Observable.range(1, 10)
            .window(3)
            .subscribe(windowObservable -> {
                List<Integer> window = new ArrayList<>();
                windowObservable.subscribe(window::add);
                windows.add(window);
                testObserver.onNext(windowObservable);
            });
        
        Thread.sleep(100);
        
        assertEquals(4, windows.size(), "Should have 4 windows");
        assertEquals(List.of(1, 2, 3), windows.get(0));
        assertEquals(List.of(4, 5, 6), windows.get(1));
        assertEquals(List.of(7, 8, 9), windows.get(2));
        assertEquals(List.of(10), windows.get(3));
    }
    
    @Test
    public void testDebounce() {
        TestScheduler scheduler = new TestScheduler();
        TestObserver<Integer> testObserver = new TestObserver<>();
        
        Observable<Integer> source = Observable.create(emitter -> {
            emitter.onNext(1);
            scheduler.advanceTimeBy(50, TimeUnit.MILLISECONDS);
            emitter.onNext(2);
            scheduler.advanceTimeBy(50, TimeUnit.MILLISECONDS);
            emitter.onNext(3);
            scheduler.advanceTimeBy(200, TimeUnit.MILLISECONDS);
            emitter.onNext(4);
            scheduler.advanceTimeBy(200, TimeUnit.MILLISECONDS);
            emitter.onComplete();
        });
        
        source.debounce(100, TimeUnit.MILLISECONDS, scheduler)
            .subscribe(testObserver);
        
        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        
        // Should only emit items that had 100ms of silence after them
        testObserver.assertNoErrors();
        assertTrue(testObserver.values().size() <= 3, "Should debounce rapid emissions");
    }
    
    @Test
    public void testThrottleFirst() throws InterruptedException {
        TestObserver<Integer> testObserver = new TestObserver<>();
        
        Observable.range(1, 100)
            .throttleFirst(50, TimeUnit.MILLISECONDS)
            .subscribe(testObserver);
        
        Thread.sleep(200);
        
        testObserver.assertNoErrors();
        assertTrue(testObserver.values().size() < 100, "Should throttle emissions");
        assertTrue(testObserver.values().size() >= 1, "Should emit at least one value");
    }
    
    @Test
    public void testSample() throws InterruptedException {
        TestObserver<Integer> testObserver = new TestObserver<>();
        List<Integer> emitted = new ArrayList<>();
        
        Observable.interval(10, TimeUnit.MILLISECONDS)
            .take(50)
            .sample(30, TimeUnit.MILLISECONDS)
            .subscribe(value -> {
                emitted.add(value.intValue());
                testObserver.onNext(value.intValue());
            });
        
        Thread.sleep(600);
        
        testObserver.assertNoErrors();
        assertTrue(emitted.size() > 0 && emitted.size() < 50, 
            "Sample should emit subset of values. Emitted: " + emitted.size());
    }
    
    @Test
    public void testWindow_timespan() throws InterruptedException {
        TestScheduler scheduler = new TestScheduler();
        TestObserver<Observable<Long>> testObserver = new TestObserver<>();
        
        Observable.interval(10, TimeUnit.MILLISECONDS, scheduler)
            .take(15)
            .window(50, TimeUnit.MILLISECONDS, scheduler)
            .subscribe(testObserver);
        
        scheduler.advanceTimeBy(200, TimeUnit.MILLISECONDS);
        
        testObserver.assertNoErrors();
        assertTrue(testObserver.values().size() > 0, "Should create time-based windows");
    }
}
