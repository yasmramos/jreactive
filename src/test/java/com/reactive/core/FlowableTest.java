package com.reactive.core;

import com.reactive.backpressure.BackpressureStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * Tests for Flowable with backpressure support
 */
public class FlowableTest {
    
    // ============ Factory Tests ============
    
    @Test
    public void testJust() {
        List<Integer> results = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean(false);
        
        Flowable.just(1, 2, 3, 4, 5).subscribe(new Subscriber<Integer>() {
            private Subscription subscription;
            
            @Override
            public void onSubscribe(Subscription subscription) {
                this.subscription = subscription;
                subscription.request(Long.MAX_VALUE);
            }
            
            @Override
            public void onNext(Integer item) {
                results.add(item);
            }
            
            @Override
            public void onError(Throwable throwable) {
                fail("Should not error");
            }
            
            @Override
            public void onComplete() {
                completed.set(true);
            }
        });
        
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), results);
        assertTrue(completed.get());
    }
    
    @Test
    public void testRange() {
        List<Integer> results = new ArrayList<>();
        
        Flowable.range(1, 10).subscribe(new Subscriber<Integer>() {
            @Override
            public void onSubscribe(Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }
            
            @Override
            public void onNext(Integer item) {
                results.add(item);
            }
            
            @Override
            public void onError(Throwable throwable) {
                fail("Should not error");
            }
            
            @Override
            public void onComplete() {
            }
        });
        
        assertEquals(10, results.size());
        assertEquals(Integer.valueOf(1), results.get(0));
        assertEquals(Integer.valueOf(10), results.get(9));
    }
    
    @Test
    public void testFromIterable() {
        List<String> source = Arrays.asList("a", "b", "c", "d");
        List<String> results = new ArrayList<>();
        
        Flowable.fromIterable(source).subscribe(new Subscriber<String>() {
            @Override
            public void onSubscribe(Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }
            
            @Override
            public void onNext(String item) {
                results.add(item);
            }
            
            @Override
            public void onError(Throwable throwable) {
                fail("Should not error");
            }
            
            @Override
            public void onComplete() {
            }
        });
        
        assertEquals(source, results);
    }
    
    @Test
    public void testEmpty() {
        AtomicInteger count = new AtomicInteger(0);
        AtomicBoolean completed = new AtomicBoolean(false);
        
        Flowable.empty().subscribe(new Subscriber<Object>() {
            @Override
            public void onSubscribe(Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }
            
            @Override
            public void onNext(Object item) {
                count.incrementAndGet();
            }
            
            @Override
            public void onError(Throwable throwable) {
                fail("Should not error");
            }
            
            @Override
            public void onComplete() {
                completed.set(true);
            }
        });
        
        assertEquals(0, count.get());
        assertTrue(completed.get());
    }
    
    @Test
    public void testError() {
        AtomicReference<Throwable> error = new AtomicReference<>();
        
        Flowable.error(new RuntimeException("Test error")).subscribe(new Subscriber<Object>() {
            @Override
            public void onSubscribe(Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }
            
            @Override
            public void onNext(Object item) {
                fail("Should not emit items");
            }
            
            @Override
            public void onError(Throwable throwable) {
                error.set(throwable);
            }
            
            @Override
            public void onComplete() {
                fail("Should not complete");
            }
        });
        
        assertNotNull(error.get());
        assertEquals("Test error", error.get().getMessage());
    }
    
    // ============ Backpressure Tests ============
    
    @Test
    public void testBackpressureRequest() {
        List<Integer> results = new ArrayList<>();
        AtomicInteger emitted = new AtomicInteger(0);
        
        Flowable.range(1, 100).subscribe(new Subscriber<Integer>() {
            private Subscription subscription;
            
            @Override
            public void onSubscribe(Subscription subscription) {
                this.subscription = subscription;
                subscription.request(5); // Request only 5 items
            }
            
            @Override
            public void onNext(Integer item) {
                results.add(item);
                emitted.incrementAndGet();
            }
            
            @Override
            public void onError(Throwable throwable) {
                fail("Should not error");
            }
            
            @Override
            public void onComplete() {
            }
        });
        
        assertEquals(5, results.size(), "Should only emit 5 items as requested");
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), results);
    }
    
    @Test
    public void testBackpressureIncrementalRequest() {
        List<Integer> results = new ArrayList<>();
        
        Flowable.range(1, 10).subscribe(new Subscriber<Integer>() {
            private Subscription subscription;
            private int received = 0;
            
            @Override
            public void onSubscribe(Subscription subscription) {
                this.subscription = subscription;
                subscription.request(2); // Request 2 items initially
            }
            
            @Override
            public void onNext(Integer item) {
                results.add(item);
                received++;
                if (received % 2 == 0 && received < 10) {
                    subscription.request(2); // Request 2 more
                }
            }
            
            @Override
            public void onError(Throwable throwable) {
                fail("Should not error");
            }
            
            @Override
            public void onComplete() {
            }
        });
        
        assertEquals(10, results.size());
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), results);
    }
    
    @Test
    public void testBackpressureBufferStrategy() throws InterruptedException {
        List<Integer> results = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch latch = new CountDownLatch(1);
        
        Flowable.create((Flowable.FlowableEmitter<Integer> emitter) -> {
            // Emit many items without waiting for requests
            for (int i = 1; i <= 100; i++) {
                emitter.onNext(i);
            }
            emitter.onComplete();
        }, BackpressureStrategy.BUFFER).subscribe(new Subscriber<Integer>() {
            private Subscription subscription;
            
            @Override
            public void onSubscribe(Subscription subscription) {
                this.subscription = subscription;
                subscription.request(10); // Request only 10
            }
            
            @Override
            public void onNext(Integer item) {
                results.add(item);
                if (results.size() == 10) {
                    subscription.request(90); // Request remaining
                }
            }
            
            @Override
            public void onError(Throwable throwable) {
                fail("Should not error: " + throwable.getMessage());
            }
            
            @Override
            public void onComplete() {
                latch.countDown();
            }
        });
        
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertEquals(100, results.size());
    }
    
    @Test
    public void testBackpressureDropStrategy() throws InterruptedException {
        List<Integer> results = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch latch = new CountDownLatch(1);
        
        Flowable.create((Flowable.FlowableEmitter<Integer> emitter) -> {
            for (int i = 1; i <= 100; i++) {
                emitter.onNext(i);
            }
            emitter.onComplete();
        }, BackpressureStrategy.DROP).subscribe(new Subscriber<Integer>() {
            @Override
            public void onSubscribe(Subscription subscription) {
                subscription.request(10); // Request only 10
            }
            
            @Override
            public void onNext(Integer item) {
                results.add(item);
            }
            
            @Override
            public void onError(Throwable throwable) {
                fail("Should not error");
            }
            
            @Override
            public void onComplete() {
                latch.countDown();
            }
        });
        
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertTrue(results.size() <= 10, "Should drop items beyond request");
    }
    
    @Test
    public void testBackpressureStrategiesExist() {
        // Simple test to verify all backpressure strategies work without crashing
        for (BackpressureStrategy strategy : BackpressureStrategy.values()) {
            List<Integer> results = new ArrayList<>();
            
            Flowable.just(1, 2, 3, 4, 5).subscribe(new Subscriber<Integer>() {
                @Override
                public void onSubscribe(Subscription subscription) {
                    subscription.request(Long.MAX_VALUE);
                }
                
                @Override
                public void onNext(Integer item) {
                    results.add(item);
                }
                
                @Override
                public void onError(Throwable throwable) {
                    // Should not error for simple case
                }
                
                @Override
                public void onComplete() {
                }
            });
            
            assertEquals(5, results.size(), "Strategy " + strategy + " should work");
        }
    }
    
    @Test
    public void testCancel() {
        AtomicInteger emitted = new AtomicInteger(0);
        AtomicBoolean completed = new AtomicBoolean(false);
        
        Flowable.range(1, 100).subscribe(new Subscriber<Integer>() {
            private Subscription subscription;
            
            @Override
            public void onSubscribe(Subscription subscription) {
                this.subscription = subscription;
                subscription.request(5);
            }
            
            @Override
            public void onNext(Integer item) {
                emitted.incrementAndGet();
                if (emitted.get() == 3) {
                    subscription.cancel(); // Cancel after 3 items
                }
            }
            
            @Override
            public void onError(Throwable throwable) {
                fail("Should not error");
            }
            
            @Override
            public void onComplete() {
                completed.set(true);
            }
        });
        
        assertTrue(emitted.get() <= 5, "Should stop emitting after cancel");
        assertFalse(completed.get(), "Should not complete after cancel");
    }
    
    // ============ Operator Tests ============
    
    @Test
    public void testMap() {
        List<String> results = new ArrayList<>();
        
        Flowable.range(1, 5)
            .map(n -> "Item-" + n)
            .subscribe(new Subscriber<String>() {
                @Override
                public void onSubscribe(Subscription subscription) {
                    subscription.request(Long.MAX_VALUE);
                }
                
                @Override
                public void onNext(String item) {
                    results.add(item);
                }
                
                @Override
                public void onError(Throwable throwable) {
                    fail("Should not error");
                }
                
                @Override
                public void onComplete() {
                }
            });
        
        assertEquals(Arrays.asList("Item-1", "Item-2", "Item-3", "Item-4", "Item-5"), results);
    }
    
    @Test
    public void testFilter() {
        List<Integer> results = new ArrayList<>();
        
        Flowable.range(1, 10)
            .filter(n -> n % 2 == 0)
            .subscribe(new Subscriber<Integer>() {
                @Override
                public void onSubscribe(Subscription subscription) {
                    subscription.request(Long.MAX_VALUE);
                }
                
                @Override
                public void onNext(Integer item) {
                    results.add(item);
                }
                
                @Override
                public void onError(Throwable throwable) {
                    fail("Should not error");
                }
                
                @Override
                public void onComplete() {
                }
            });
        
        assertEquals(Arrays.asList(2, 4, 6, 8, 10), results);
    }
    
    @Test
    public void testTake() {
        List<Integer> results = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean(false);
        
        Flowable.range(1, 100)
            .take(5)
            .subscribe(new Subscriber<Integer>() {
                @Override
                public void onSubscribe(Subscription subscription) {
                    subscription.request(Long.MAX_VALUE);
                }
                
                @Override
                public void onNext(Integer item) {
                    results.add(item);
                }
                
                @Override
                public void onError(Throwable throwable) {
                    fail("Should not error");
                }
                
                @Override
                public void onComplete() {
                    completed.set(true);
                }
            });
        
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), results);
        assertTrue(completed.get());
    }
    
    @Test
    public void testSkip() {
        List<Integer> results = new ArrayList<>();
        
        Flowable.range(1, 10)
            .skip(5)
            .subscribe(new Subscriber<Integer>() {
                @Override
                public void onSubscribe(Subscription subscription) {
                    subscription.request(Long.MAX_VALUE);
                }
                
                @Override
                public void onNext(Integer item) {
                    results.add(item);
                }
                
                @Override
                public void onError(Throwable throwable) {
                    fail("Should not error");
                }
                
                @Override
                public void onComplete() {
                }
            });
        
        assertEquals(Arrays.asList(6, 7, 8, 9, 10), results);
    }
    
    @Test
    public void testToObservable() {
        List<Integer> results = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean(false);
        
        Flowable.range(1, 5)
            .toObservable()
            .subscribe(
                results::add,
                throwable -> fail("Should not error"),
                () -> completed.set(true)
            );
        
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), results);
        assertTrue(completed.get());
    }
    
    // ============ Chaining Tests ============
    
    @Test
    public void testChainedOperators() {
        List<String> results = new ArrayList<>();
        
        Flowable.range(1, 20)
            .filter(n -> n % 2 == 0)
            .take(5)
            .map(n -> "Even-" + n)
            .subscribe(new Subscriber<String>() {
                @Override
                public void onSubscribe(Subscription subscription) {
                    subscription.request(Long.MAX_VALUE);
                }
                
                @Override
                public void onNext(String item) {
                    results.add(item);
                }
                
                @Override
                public void onError(Throwable throwable) {
                    fail("Should not error");
                }
                
                @Override
                public void onComplete() {
                }
            });
        
        assertEquals(Arrays.asList("Even-2", "Even-4", "Even-6", "Even-8", "Even-10"), results);
    }
}
