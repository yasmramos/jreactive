package com.reactive.benchmarks;

import com.reactive.core.Observable;
import io.reactivex.rxjava3.core.Flowable;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Reactive Streams and Backpressure benchmark
 * Compares backpressure handling between Observable and RxJava
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 3)
@Fork(1)
public class BackpressureBenchmark {

    @Param({"1000", "10000", "100000"})
    private int size;

    // ================== PUBLISHER CONVERSION ==================
    
    @Benchmark
    public void observable_toPublisher(Blackhole blackhole) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicLong count = new AtomicLong(0);
        
        Publisher<Integer> publisher = Observable.range(0, size).toPublisher();
        
        publisher.subscribe(new Subscriber<Integer>() {
            private Subscription subscription;
            
            @Override
            public void onSubscribe(Subscription s) {
                subscription = s;
                subscription.request(Long.MAX_VALUE);
            }
            
            @Override
            public void onNext(Integer value) {
                count.incrementAndGet();
                blackhole.consume(value);
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
        
        latch.await();
    }
    
    @Benchmark
    public void rxjava_toPublisher(Blackhole blackhole) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicLong count = new AtomicLong(0);
        
        Publisher<Integer> publisher = Flowable.range(0, size);
        
        publisher.subscribe(new Subscriber<Integer>() {
            private Subscription subscription;
            
            @Override
            public void onSubscribe(Subscription s) {
                subscription = s;
                subscription.request(Long.MAX_VALUE);
            }
            
            @Override
            public void onNext(Integer value) {
                count.incrementAndGet();
                blackhole.consume(value);
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
        
        latch.await();
    }

    // ================== BOUNDED BACKPRESSURE ==================
    
    @Benchmark
    public void observable_boundedBackpressure(Blackhole blackhole) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        
        Publisher<Integer> publisher = Observable.range(0, size).toPublisher();
        
        publisher.subscribe(new Subscriber<Integer>() {
            private Subscription subscription;
            private int received = 0;
            private static final int BATCH_SIZE = 100;
            
            @Override
            public void onSubscribe(Subscription s) {
                subscription = s;
                subscription.request(BATCH_SIZE);
            }
            
            @Override
            public void onNext(Integer value) {
                blackhole.consume(value);
                received++;
                if (received % BATCH_SIZE == 0) {
                    subscription.request(BATCH_SIZE);
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
        
        latch.await();
    }
    
    @Benchmark
    public void rxjava_boundedBackpressure(Blackhole blackhole) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        
        Publisher<Integer> publisher = Flowable.range(0, size);
        
        publisher.subscribe(new Subscriber<Integer>() {
            private Subscription subscription;
            private int received = 0;
            private static final int BATCH_SIZE = 100;
            
            @Override
            public void onSubscribe(Subscription s) {
                subscription = s;
                subscription.request(BATCH_SIZE);
            }
            
            @Override
            public void onNext(Integer value) {
                blackhole.consume(value);
                received++;
                if (received % BATCH_SIZE == 0) {
                    subscription.request(BATCH_SIZE);
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
        
        latch.await();
    }

    // ================== FROM PUBLISHER ==================
    
    @Benchmark
    public void observable_fromPublisher(Blackhole blackhole) {
        Publisher<Integer> publisher = Flowable.range(0, size);
        Observable.fromPublisher(publisher)
                .subscribe(blackhole::consume);
    }
    
    @Benchmark
    public void rxjava_fromPublisher(Blackhole blackhole) {
        Publisher<Integer> publisher = Flowable.range(0, size);
        Flowable.fromPublisher(publisher)
                .subscribe(blackhole::consume);
    }

    // ================== PIPELINE WITH BACKPRESSURE ==================
    
    @Benchmark
    public void observable_pipelineWithBackpressure(Blackhole blackhole) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        
        Publisher<Integer> publisher = Observable.range(0, size)
                .map(x -> x * 2)
                .filter(x -> x % 4 == 0)
                .toPublisher();
        
        publisher.subscribe(new Subscriber<Integer>() {
            private Subscription subscription;
            private int received = 0;
            private static final int BATCH_SIZE = 50;
            
            @Override
            public void onSubscribe(Subscription s) {
                subscription = s;
                subscription.request(BATCH_SIZE);
            }
            
            @Override
            public void onNext(Integer value) {
                blackhole.consume(value);
                received++;
                if (received % BATCH_SIZE == 0) {
                    subscription.request(BATCH_SIZE);
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
        
        latch.await();
    }
    
    @Benchmark
    public void rxjava_pipelineWithBackpressure(Blackhole blackhole) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        
        Publisher<Integer> publisher = Flowable.range(0, size)
                .map(x -> x * 2)
                .filter(x -> x % 4 == 0);
        
        publisher.subscribe(new Subscriber<Integer>() {
            private Subscription subscription;
            private int received = 0;
            private static final int BATCH_SIZE = 50;
            
            @Override
            public void onSubscribe(Subscription s) {
                subscription = s;
                subscription.request(BATCH_SIZE);
            }
            
            @Override
            public void onNext(Integer value) {
                blackhole.consume(value);
                received++;
                if (received % BATCH_SIZE == 0) {
                    subscription.request(BATCH_SIZE);
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
        
        latch.await();
    }

    // ================== ONE-BY-ONE REQUEST ==================
    
    @Benchmark
    public void observable_oneByOneRequest(Blackhole blackhole) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        
        Publisher<Integer> publisher = Observable.range(0, Math.min(size, 1000)).toPublisher();
        
        publisher.subscribe(new Subscriber<Integer>() {
            private Subscription subscription;
            
            @Override
            public void onSubscribe(Subscription s) {
                subscription = s;
                subscription.request(1);
            }
            
            @Override
            public void onNext(Integer value) {
                blackhole.consume(value);
                subscription.request(1);
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
        
        latch.await();
    }
    
    @Benchmark
    public void rxjava_oneByOneRequest(Blackhole blackhole) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        
        Publisher<Integer> publisher = Flowable.range(0, Math.min(size, 1000));
        
        publisher.subscribe(new Subscriber<Integer>() {
            private Subscription subscription;
            
            @Override
            public void onSubscribe(Subscription s) {
                subscription = s;
                subscription.request(1);
            }
            
            @Override
            public void onNext(Integer value) {
                blackhole.consume(value);
                subscription.request(1);
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
        
        latch.await();
    }
}
