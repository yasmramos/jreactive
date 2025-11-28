package com.reactive.debug;

import com.reactive.core.Observable;
import com.reactive.testing.TestObserver;
import java.util.concurrent.atomic.AtomicInteger;

public class RefCountDebug {
    public static void main(String[] args) throws InterruptedException {
        AtomicInteger subscriptionCount = new AtomicInteger(0);
        
        Observable<Long> source = Observable.create(emitter -> {
            int count = subscriptionCount.incrementAndGet();
            System.out.println("[Source] Subscribed! Count: " + count);
            
            for (long i = 1; i <= 10; i++) {
                System.out.println("[Source] Emitting: " + i);
                emitter.onNext(i);
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            System.out.println("[Source] Completing");
            emitter.onComplete();
        });
        
        Observable<Long> refCounted = source.publish().refCount();
        
        // First subscriber
        TestObserver<Long> observer1 = new TestObserver<>();
        System.out.println("[Main] Subscribing observer1 at t=0");
        refCounted.subscribe(observer1);
        
        Thread.sleep(30);
        
        // Second subscriber joins
        TestObserver<Long> observer2 = new TestObserver<>();
        System.out.println("[Main] Subscribing observer2 at t=30");
        refCounted.subscribe(observer2);
        
        Thread.sleep(150);
        
        System.out.println("\n[Result] Observer1 values: " + observer1.values().size());
        System.out.println("[Result] Observer2 values: " + observer2.values().size());
        System.out.println("[Result] Subscription count: " + subscriptionCount.get());
        System.out.println("[Result] Expected subscription count: 1");
    }
}
