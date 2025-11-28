package com.reactive.observables;

import com.reactive.core.*;
import com.reactive.testing.TestObserver;
import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

public class DebugTest2 {
    
    @Test
    public void testCreateWithRefCount() {
        System.out.println("\n=== Test Observable.create() with RefCount ===");
        AtomicInteger subscriptionCount = new AtomicInteger(0);
        
        Observable<Integer> source = Observable.create(emitter -> {
            System.out.println("Source subscribed, count: " + subscriptionCount.incrementAndGet());
            System.out.println("Emitting 1");
            emitter.onNext(1);
            System.out.println("Emitting 2");
            emitter.onNext(2);
            System.out.println("Emitting 3");
            emitter.onNext(3);
            System.out.println("Completing");
            emitter.onComplete();
            System.out.println("Completed");
        });
        
        System.out.println("Creating refCounted observable");
        Observable<Integer> refCounted = source.publish().refCount();
        
        System.out.println("Creating observer");
        TestObserver<Integer> observer1 = new TestObserver<>();
        
        System.out.println("Subscribing observer");
        refCounted.subscribe(observer1);
        
        System.out.println("After subscribe");
        System.out.println("Observer values: " + observer1.values());
        System.out.println("Observer complete: " + (observer1.getCompleteCount() > 0));
        
        observer1.assertValues(1, 2, 3);
    }
}
