package com.reactive.debug;

import com.reactive.core.Disposable;
import com.reactive.core.Observable;
import com.reactive.schedulers.Schedulers;
import com.reactive.testing.TestObserver;

import java.util.concurrent.atomic.AtomicInteger;

public class DisposeDebug {
    public static void main(String[] args) throws InterruptedException {
        AtomicInteger disposeCount = new AtomicInteger(0);
        
        Observable<Long> source = Observable.<Long>create(emitter -> {
            System.out.println("[Source] Starting emission");
            Disposable timer = new Disposable() {
                private volatile boolean disposed = false;
                
                @Override
                public void dispose() {
                    disposed = true;
                    disposeCount.incrementAndGet();
                    System.out.println("[Timer] Disposed! Count: " + disposeCount.get());
                }
                
                @Override
                public boolean isDisposed() {
                    return disposed;
                }
            };
            
            emitter.setDisposable(timer);
            
            for (long i = 1; i <= 100; i++) {
                if (timer.isDisposed()) {
                    System.out.println("[Source] Timer disposed at i=" + i + ", breaking");
                    break;
                }
                emitter.onNext(i);
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            System.out.println("[Source] Completing");
            emitter.onComplete();
        }).subscribeOn(Schedulers.io());
        
        Observable<Long> refCounted = source.publish().refCount();
        
        TestObserver<Long> observer1 = new TestObserver<>();
        System.out.println("[Main] Subscribing observer1");
        refCounted.subscribe(observer1);
        
        Thread.sleep(30);
        
        TestObserver<Long> observer2 = new TestObserver<>();
        System.out.println("[Main] Subscribing observer2");
        refCounted.subscribe(observer2);
        
        Thread.sleep(30);
        
        System.out.println("[Main] Disposing observer1");
        observer1.dispose();
        
        Thread.sleep(30);
        
        System.out.println("[Main] Disposing observer2");
        observer2.dispose();
        
        Thread.sleep(50);
        
        System.out.println("\n[Result] Dispose count: " + disposeCount.get());
        System.out.println("[Result] Expected: > 0");
    }
}
