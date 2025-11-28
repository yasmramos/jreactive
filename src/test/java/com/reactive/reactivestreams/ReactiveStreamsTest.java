package com.reactive.reactivestreams;

import com.reactive.core.Observable;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for Reactive Streams integration (Phase 3).
 * Tests Publisher conversions and interoperability.
 */
public class ReactiveStreamsTest {
    
    // ============ toPublisher() Tests ============
    
    @Test
    public void testToPublisher_WithMultipleValues() throws InterruptedException {
        Publisher<Integer> publisher = Observable.just(1, 2, 3, 4, 5).toPublisher();
        
        List<Integer> result = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);
        
        publisher.subscribe(new Subscriber<Integer>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(Long.MAX_VALUE);
            }
            
            @Override
            public void onNext(Integer value) {
                result.add(value);
            }
            
            @Override
            public void onError(Throwable t) {
                latch.countDown();
            }
            
            @Override
            public void onComplete() {
                latch.countDown();
            }
        });
        
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }
    
    @Test
    public void testToPublisher_WithBackpressure() throws InterruptedException {
        Publisher<Integer> publisher = Observable.range(1, 100).toPublisher();
        
        List<Integer> result = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);
        
        publisher.subscribe(new Subscriber<Integer>() {
            private Subscription subscription;
            
            @Override
            public void onSubscribe(Subscription s) {
                this.subscription = s;
                s.request(5); // Request only 5 items initially
            }
            
            @Override
            public void onNext(Integer value) {
                result.add(value);
                
                // Request more when we've consumed the batch
                if (result.size() % 5 == 0) {
                    subscription.request(5);
                }
            }
            
            @Override
            public void onError(Throwable t) {
                latch.countDown();
            }
            
            @Override
            public void onComplete() {
                latch.countDown();
            }
        });
        
        assertTrue(latch.await(3, TimeUnit.SECONDS));
        // Should have received all 100 items
        assertEquals(100, result.size());
    }
    
    @Test
    public void testToPublisher_WithEmpty() throws InterruptedException {
        Publisher<Integer> publisher = Observable.<Integer>empty().toPublisher();
        
        List<Integer> result = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean completed = new AtomicBoolean(false);
        
        publisher.subscribe(new Subscriber<Integer>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(Long.MAX_VALUE);
            }
            
            @Override
            public void onNext(Integer value) {
                result.add(value);
            }
            
            @Override
            public void onError(Throwable t) {
                latch.countDown();
            }
            
            @Override
            public void onComplete() {
                completed.set(true);
                latch.countDown();
            }
        });
        
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertTrue(result.isEmpty());
        assertTrue(completed.get());
    }
    
    @Test
    public void testToPublisher_WithError() throws InterruptedException {
        RuntimeException testError = new RuntimeException("Test error");
        Publisher<Integer> publisher = Observable.<Integer>error(testError).toPublisher();
        
        AtomicReference<Throwable> receivedError = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        
        publisher.subscribe(new Subscriber<Integer>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(Long.MAX_VALUE);
            }
            
            @Override
            public void onNext(Integer value) {
                // Should not receive any value
            }
            
            @Override
            public void onError(Throwable t) {
                receivedError.set(t);
                latch.countDown();
            }
            
            @Override
            public void onComplete() {
                latch.countDown();
            }
        });
        
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertNotNull(receivedError.get());
        assertEquals(testError, receivedError.get());
    }
    
    @Test
    public void testToPublisher_Cancellation() throws InterruptedException {
        Publisher<Integer> publisher = Observable.range(1, 1000).toPublisher();
        
        List<Integer> result = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);
        
        publisher.subscribe(new Subscriber<Integer>() {
            private Subscription subscription;
            
            @Override
            public void onSubscribe(Subscription s) {
                this.subscription = s;
                s.request(Long.MAX_VALUE);
            }
            
            @Override
            public void onNext(Integer value) {
                result.add(value);
                
                // Cancel after receiving 10 items
                if (result.size() == 10) {
                    subscription.cancel();
                    latch.countDown();
                }
            }
            
            @Override
            public void onError(Throwable t) {
                latch.countDown();
            }
            
            @Override
            public void onComplete() {
                latch.countDown();
            }
        });
        
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        // Should have received only 10 items before cancellation
        assertTrue(result.size() >= 10);
        assertTrue(result.size() <= 20); // Allow some buffering
    }
    
    // ============ fromPublisher() Tests ============
    
    @Test
    public void testFromPublisher_WithMultipleValues() {
        Publisher<Integer> publisher = subscriber -> {
            subscriber.onSubscribe(new Subscription() {
                private boolean cancelled = false;
                
                @Override
                public void request(long n) {
                    if (!cancelled) {
                        for (int i = 1; i <= 5; i++) {
                            subscriber.onNext(i);
                        }
                        subscriber.onComplete();
                    }
                }
                
                @Override
                public void cancel() {
                    cancelled = true;
                }
            });
        };
        
        List<Integer> result = new ArrayList<>();
        Observable.fromPublisher(publisher)
            .subscribe(result::add);
        
        // Wait a bit for async processing
        try { Thread.sleep(100); } catch (InterruptedException e) {}
        
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }
    
    @Test
    public void testFromPublisher_WithEmpty() throws InterruptedException {
        Publisher<Integer> publisher = subscriber -> {
            subscriber.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                    subscriber.onComplete();
                }
                
                @Override
                public void cancel() {
                }
            });
        };
        
        List<Integer> result = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);
        
        Observable.fromPublisher(publisher)
            .subscribe(
                result::add,
                e -> latch.countDown(),
                latch::countDown
            );
        
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertTrue(result.isEmpty());
    }
    
    @Test
    public void testFromPublisher_WithError() throws InterruptedException {
        RuntimeException testError = new RuntimeException("Test error");
        Publisher<Integer> publisher = subscriber -> {
            subscriber.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                    subscriber.onError(testError);
                }
                
                @Override
                public void cancel() {
                }
            });
        };
        
        AtomicReference<Throwable> receivedError = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        
        Observable.fromPublisher(publisher)
            .subscribe(
                value -> {},
                error -> {
                    receivedError.set(error);
                    latch.countDown();
                }
            );
        
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertEquals(testError, receivedError.get());
    }
    
    // ============ Round-trip Tests ============
    
    @Test
    public void testRoundTrip_ObservableToPublisherToObservable() throws InterruptedException {
        Observable<Integer> original = Observable.just(1, 2, 3, 4, 5);
        
        // Convert to Publisher and back
        Observable<Integer> roundTrip = Observable.fromPublisher(original.toPublisher());
        
        List<Integer> result = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);
        
        roundTrip.subscribe(
            result::add,
            e -> latch.countDown(),
            latch::countDown
        );
        
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }
    
    @Test
    public void testRoundTrip_WithTransformation() throws InterruptedException {
        Observable<Integer> original = Observable.range(1, 10);
        
        // Convert to Publisher, then back to Observable, then transform
        Observable<String> transformed = Observable.fromPublisher(original.toPublisher())
            .filter(x -> x % 2 == 0)
            .map(x -> "Number: " + x);
        
        List<String> result = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);
        
        transformed.subscribe(
            result::add,
            e -> latch.countDown(),
            latch::countDown
        );
        
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertEquals(Arrays.asList("Number: 2", "Number: 4", "Number: 6", "Number: 8", "Number: 10"), result);
    }
    
    // ============ Integration Tests ============
    
    @Test
    public void testPublisher_MultipleSubscribers() throws InterruptedException {
        Publisher<Integer> publisher = Observable.just(1, 2, 3).toPublisher();
        
        // First subscriber
        List<Integer> result1 = new ArrayList<>();
        CountDownLatch latch1 = new CountDownLatch(1);
        
        publisher.subscribe(new Subscriber<Integer>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(Long.MAX_VALUE);
            }
            
            @Override
            public void onNext(Integer value) {
                result1.add(value);
            }
            
            @Override
            public void onError(Throwable t) {
                latch1.countDown();
            }
            
            @Override
            public void onComplete() {
                latch1.countDown();
            }
        });
        
        // Second subscriber
        List<Integer> result2 = new ArrayList<>();
        CountDownLatch latch2 = new CountDownLatch(1);
        
        publisher.subscribe(new Subscriber<Integer>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(Long.MAX_VALUE);
            }
            
            @Override
            public void onNext(Integer value) {
                result2.add(value);
            }
            
            @Override
            public void onError(Throwable t) {
                latch2.countDown();
            }
            
            @Override
            public void onComplete() {
                latch2.countDown();
            }
        });
        
        assertTrue(latch1.await(2, TimeUnit.SECONDS));
        assertTrue(latch2.await(2, TimeUnit.SECONDS));
        
        assertEquals(Arrays.asList(1, 2, 3), result1);
        assertEquals(Arrays.asList(1, 2, 3), result2);
    }
    
    @Test
    public void testPublisher_WithObservableOperators() throws InterruptedException {
        Observable<Integer> source = Observable.range(1, 20)
            .filter(x -> x % 2 == 0)
            .map(x -> x * 2);
        
        Publisher<Integer> publisher = source.toPublisher();
        
        List<Integer> result = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);
        
        publisher.subscribe(new Subscriber<Integer>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(Long.MAX_VALUE);
            }
            
            @Override
            public void onNext(Integer value) {
                result.add(value);
            }
            
            @Override
            public void onError(Throwable t) {
                latch.countDown();
            }
            
            @Override
            public void onComplete() {
                latch.countDown();
            }
        });
        
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertEquals(Arrays.asList(4, 8, 12, 16, 20, 24, 28, 32, 36, 40), result);
    }
}
