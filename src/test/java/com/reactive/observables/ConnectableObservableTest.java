package com.reactive.observables;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.reactive.core.Disposable;
import com.reactive.core.Observable;
import com.reactive.schedulers.Schedulers;
import com.reactive.testing.TestObserver;

/**
 * Comprehensive test suite for ConnectableObservable and related operators.
 */
public class ConnectableObservableTest {
    
    // ============ Publish Tests ============
    
    @Test
    public void testPublishBasic() {
        ConnectableObservable<Integer> connectable = Observable.just(1, 2, 3).publish();
        
        TestObserver<Integer> observer1 = new TestObserver<>();
        TestObserver<Integer> observer2 = new TestObserver<>();
        
        connectable.subscribe(observer1);
        connectable.subscribe(observer2);
        
        // No emissions until connect() is called
        observer1.assertEmpty();
        observer2.assertEmpty();
        
        // Connect and verify both observers receive all values
        connectable.connect();
        
        observer1.assertValues(1, 2, 3);
        observer1.assertComplete();
        
        observer2.assertValues(1, 2, 3);
        observer2.assertComplete();
    }
    
    @Test
    public void testPublishLateSubscriber() throws InterruptedException {
        AtomicInteger emissionCount = new AtomicInteger(0);
        
        Observable<Integer> source = Observable.create(emitter -> {
            for (int i = 1; i <= 3; i++) {
                emissionCount.incrementAndGet();
                emitter.onNext(i);
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            emitter.onComplete();
        });
        
        ConnectableObservable<Integer> connectable = source.publish();
        
        TestObserver<Integer> early = new TestObserver<>();
        connectable.subscribe(early);
        
        // Connect immediately
        connectable.connect();
        
        // Wait a bit, then subscribe late
        Thread.sleep(25);
        
        TestObserver<Integer> late = new TestObserver<>();
        connectable.subscribe(late);
        
        Thread.sleep(50);
        
        // Early subscriber gets all values
        early.assertValues(1, 2, 3);
        early.assertComplete();
        
        // Late subscriber only gets remaining values (likely just 3)
        assertTrue(late.values().size() < 3, "Late subscriber should miss some emissions");
        
        // Source should only emit once (multicasting)
        assertEquals(3, emissionCount.get(), "Source should emit exactly 3 times");
    }
    
    @Test
    public void testPublishMultipleConnects() {
        ConnectableObservable<Integer> connectable = Observable.just(1, 2, 3).publish();
        
        Disposable connection1 = connectable.connect();
        Disposable connection2 = connectable.connect();
        
        // Second connect should return the same or a no-op disposable
        assertNotNull(connection1);
        assertNotNull(connection2);
    }
    
    @Test
    public void testPublishWithError() {
        RuntimeException error = new RuntimeException("Test error");
        ConnectableObservable<Integer> connectable = Observable.<Integer>create(emitter -> {
            emitter.onNext(1);
            emitter.onNext(2);
            emitter.onError(error);
        }).publish();
        
        TestObserver<Integer> observer1 = new TestObserver<>();
        TestObserver<Integer> observer2 = new TestObserver<>();
        
        connectable.subscribe(observer1);
        connectable.subscribe(observer2);
        connectable.connect();
        
        observer1.assertValues(1, 2);
        observer1.assertError(RuntimeException.class);
        
        observer2.assertValues(1, 2);
        observer2.assertError(RuntimeException.class);
    }
    
    @Test
    public void testPublishDisconnect() {
        AtomicInteger subscriptionCount = new AtomicInteger(0);
        
        Observable<Integer> source = Observable.create(emitter -> {
            subscriptionCount.incrementAndGet();
            emitter.onNext(1);
            emitter.onNext(2);
            emitter.onComplete();
        });
        
        ConnectableObservable<Integer> connectable = source.publish();
        
        TestObserver<Integer> observer = new TestObserver<>();
        connectable.subscribe(observer);
        
        Disposable connection = connectable.connect();
        observer.assertValues(1, 2);
        
        // Disconnect
        connection.dispose();
        assertTrue(connection.isDisposed());
        
        // Reconnect should create a new subscription
        connectable.connect();
        
        // Should have subscribed twice
        assertEquals(2, subscriptionCount.get());
    }
    
    // ============ Replay Tests ============
    
    @Test
    public void testReplayUnbounded() {
        ConnectableObservable<Integer> connectable = Observable.just(1, 2, 3).replay();
        
        // Connect first
        connectable.connect();
        
        // Late subscriber should still receive all values
        TestObserver<Integer> lateObserver = new TestObserver<>();
        connectable.subscribe(lateObserver);
        
        lateObserver.assertValues(1, 2, 3);
        lateObserver.assertComplete();
    }
    
    @Test
    public void testReplaySizeBounded() {
        ConnectableObservable<Integer> connectable = Observable.just(1, 2, 3, 4, 5).replay(2);
        
        connectable.connect();
        
        // Late subscriber should only receive last 2 values
        TestObserver<Integer> lateObserver = new TestObserver<>();
        connectable.subscribe(lateObserver);
        
        lateObserver.assertValues(4, 5);
        lateObserver.assertComplete();
    }
    
    // ============ RefCount Tests ============
    
    @Test
    public void testRefCountBasic() {
        AtomicInteger subscriptionCount = new AtomicInteger(0);
        
        Observable<Integer> source = Observable.create(emitter -> {
            System.out.println("[testRefCountBasic] Source subscribed, count: " + subscriptionCount.incrementAndGet());
            emitter.onNext(1);
            emitter.onNext(2);
            emitter.onNext(3);
            emitter.onComplete();
            System.out.println("[testRefCountBasic] Source completed");
        });
        
        Observable<Integer> refCounted = source.publish().refCount();
        
        // First subscriber triggers connection
        TestObserver<Integer> observer1 = new TestObserver<>();
        System.out.println("[testRefCountBasic] Subscribing observer1");
        refCounted.subscribe(observer1);
        System.out.println("[testRefCountBasic] Observer1 values: " + observer1.values());
        
        observer1.assertValues(1, 2, 3);
        observer1.assertComplete();
        
        assertEquals(1, subscriptionCount.get(), "Should have connected once");
        
        // Second subscriber triggers new connection (previous completed)
        TestObserver<Integer> observer2 = new TestObserver<>();
        System.out.println("[testRefCountBasic] Subscribing observer2");
        refCounted.subscribe(observer2);
        System.out.println("[testRefCountBasic] Observer2 values: " + observer2.values());
        
        observer2.assertValues(1, 2, 3);
        observer2.assertComplete();
        
        assertEquals(2, subscriptionCount.get(), "Should have connected twice");
    }
    
    @Test
    public void testRefCountMultipleObservers() throws InterruptedException {
        AtomicInteger subscriptionCount = new AtomicInteger(0);
        
        Observable<Long> source = Observable.<Long>create(emitter -> {
            subscriptionCount.incrementAndGet();
            
            for (long i = 1; i <= 10; i++) {
                emitter.onNext(i);
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            emitter.onComplete();
        }).subscribeOn(Schedulers.io()); // Make it truly async
        
        Observable<Long> refCounted = source.publish().refCount();
        
        // First subscriber
        TestObserver<Long> observer1 = new TestObserver<>();
        refCounted.subscribe(observer1);
        
        Thread.sleep(30);
        
        // Second subscriber joins
        TestObserver<Long> observer2 = new TestObserver<>();
        refCounted.subscribe(observer2);
        
        Thread.sleep(150);
        
        // Both should complete
        observer1.assertComplete();
        observer2.assertComplete();
        
        // Should have connected only once
        assertEquals(1, subscriptionCount.get(), "Should connect only once");
        
        // Both observers should receive values (observer2 gets later ones)
        assertTrue(observer1.values().size() >= 10, "First observer should get all values");
        assertTrue(observer2.values().size() < 10, "Second observer should miss early values");
    }
    
    @Test
    public void testRefCountDispose() throws InterruptedException {
        AtomicInteger disposeCount = new AtomicInteger(0);
        
        Observable<Long> source = Observable.<Long>create(emitter -> {
            Disposable timer = new Disposable() {
                private volatile boolean disposed = false;
                
                @Override
                public void dispose() {
                    disposed = true;
                    disposeCount.incrementAndGet();
                }
                
                @Override
                public boolean isDisposed() {
                    return disposed;
                }
            };
            
            emitter.setDisposable(timer);
            
            for (long i = 1; i <= 100; i++) {
                if (timer.isDisposed()) break;
                emitter.onNext(i);
                try {
                    Thread.sleep(20); // Increased to 20ms for clearer timing
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            emitter.onComplete();
        }).subscribeOn(Schedulers.io()); // Make it truly async
        
        Observable<Long> refCounted = source.publish().refCount();
        
        TestObserver<Long> observer1 = new TestObserver<>();
        refCounted.subscribe(observer1);
        
        Thread.sleep(50);
        
        TestObserver<Long> observer2 = new TestObserver<>();
        refCounted.subscribe(observer2);
        
        Thread.sleep(50);
        
        // Dispose first observer
        observer1.dispose();
        
        Thread.sleep(50);
        
        // Dispose second observer (should trigger upstream disposal)
        // At this point we're at ~150ms, source should still be emitting (needs 2000ms total)
        observer2.dispose();
        
        Thread.sleep(50);
        
        // Upstream should have been disposed
        assertTrue(disposeCount.get() > 0, "Upstream should be disposed");
    }
    
    // ============ Share Tests ============
    
    @Test
    public void testShareBasic() throws InterruptedException {
        AtomicInteger subscriptionCount = new AtomicInteger(0);
        
        Observable<Integer> source = Observable.<Integer>create(emitter -> {
            subscriptionCount.incrementAndGet();
            emitter.onNext(1);
            emitter.onNext(2);
            emitter.onNext(3);
            emitter.onComplete();
        }).subscribeOn(Schedulers.io()); // Make it async
        
        Observable<Integer> shared = source.share();
        
        TestObserver<Integer> observer1 = new TestObserver<>();
        TestObserver<Integer> observer2 = new TestObserver<>();
        
        shared.subscribe(observer1);
        shared.subscribe(observer2);
        
        // Wait for completion
        Thread.sleep(100);
        
        // Both should receive all values
        observer1.assertValues(1, 2, 3);
        observer2.assertValues(1, 2, 3);
        
        // Source should only be subscribed once
        assertEquals(1, subscriptionCount.get());
    }
    
    @Test
    public void testShareEquivalentToPublishRefCount() {
        Observable<Integer> source = Observable.just(1, 2, 3);
        
        Observable<Integer> shared = source.share();
        Observable<Integer> publishRefCount = source.publish().refCount();
        
        TestObserver<Integer> sharedObserver = new TestObserver<>();
        TestObserver<Integer> publishObserver = new TestObserver<>();
        
        shared.subscribe(sharedObserver);
        publishRefCount.subscribe(publishObserver);
        
        // Both should behave identically
        sharedObserver.assertValues(1, 2, 3);
        publishObserver.assertValues(1, 2, 3);
    }
    
    // ============ AutoConnect Tests ============
    
    @Test
    public void testAutoConnectWithThreshold() throws InterruptedException {
        AtomicInteger subscriptionCount = new AtomicInteger(0);
        
        Observable<Integer> source = Observable.create(emitter -> {
            subscriptionCount.incrementAndGet();
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            emitter.onNext(1);
            emitter.onNext(2);
            emitter.onNext(3);
            emitter.onComplete();
        });
        
        Observable<Integer> autoConnect = source.publish().autoConnect(2);
        
        TestObserver<Integer> observer1 = new TestObserver<>();
        autoConnect.subscribe(observer1);
        
        // Should not connect yet
        assertEquals(0, subscriptionCount.get());
        
        Thread.sleep(20);
        
        TestObserver<Integer> observer2 = new TestObserver<>();
        autoConnect.subscribe(observer2);
        
        // Now should connect
        Thread.sleep(100);
        
        assertEquals(1, subscriptionCount.get(), "Should connect after 2 subscribers");
        
        observer1.assertValues(1, 2, 3);
        observer2.assertValues(1, 2, 3);
    }
    
    @Test
    public void testAutoConnectDefault() {
        AtomicInteger subscriptionCount = new AtomicInteger(0);
        
        Observable<Integer> source = Observable.create(emitter -> {
            subscriptionCount.incrementAndGet();
            emitter.onNext(1);
            emitter.onNext(2);
            emitter.onComplete();
        });
        
        Observable<Integer> autoConnect = source.publish().autoConnect(); // defaults to 1
        
        TestObserver<Integer> observer = new TestObserver<>();
        autoConnect.subscribe(observer);
        
        // Should connect immediately with first subscriber
        assertEquals(1, subscriptionCount.get());
        observer.assertValues(1, 2);
    }
    
    @Test
    public void testAutoConnectNeverDisconnects() throws InterruptedException {
        AtomicInteger disposeCount = new AtomicInteger(0);
        
        Observable<Long> source = Observable.create(emitter -> {
            Disposable timer = new Disposable() {
                private volatile boolean disposed = false;
                
                @Override
                public void dispose() {
                    disposed = true;
                    disposeCount.incrementAndGet();
                }
                
                @Override
                public boolean isDisposed() {
                    return disposed;
                }
            };
            
            emitter.setDisposable(timer);
            
            for (long i = 1; i <= 100; i++) {
                if (timer.isDisposed()) break;
                emitter.onNext(i);
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        
        Observable<Long> autoConnect = source.publish().autoConnect();
        
        TestObserver<Long> observer = new TestObserver<>();
        autoConnect.subscribe(observer);
        
        Thread.sleep(50);
        
        // Dispose observer
        observer.dispose();
        
        Thread.sleep(50);
        
        // Upstream should NOT be disposed (autoConnect never disconnects)
        assertEquals(0, disposeCount.get(), "AutoConnect should not dispose upstream");
    }
    
    // ============ Connection Callback Tests ============
    
    @Test
    public void testConnectWithCallback() {
        ConnectableObservable<Integer> connectable = Observable.just(1, 2, 3).publish();
        
        TestObserver<Integer> observer = new TestObserver<>();
        connectable.subscribe(observer);
        
        Disposable[] connectionHolder = new Disposable[1];
        
        connectable.connect(connection -> {
            connectionHolder[0] = connection;
        });
        
        assertNotNull(connectionHolder[0], "Callback should receive connection");
        observer.assertValues(1, 2, 3);
    }
}
