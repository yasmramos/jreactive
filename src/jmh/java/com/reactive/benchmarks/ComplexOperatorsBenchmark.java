package com.reactive.benchmarks;

import com.reactive.core.Observable;
import io.reactivex.rxjava3.core.Flowable;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * Complex operator chains benchmark comparing Observable and RxJava
 * Tests real-world scenarios with multiple operator combinations
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 3)
@Fork(1)
public class ComplexOperatorsBenchmark {

    @Param({"1000", "10000"})
    private int size;

    // ================== DATA PROCESSING PIPELINE ==================
    
    @Benchmark
    public void observable_dataProcessingPipeline(Blackhole blackhole) {
        Observable.range(0, size)
                .filter(x -> x % 2 == 0)
                .map(x -> x * 2)
                .flatMap(x -> Observable.just(x, x + 1, x + 2))
                .filter(x -> x % 3 == 0)
                .distinct()
                .take(size / 2)
                .map(x -> x.toString())
                .subscribe(blackhole::consume);
    }
    
    @Benchmark
    public void rxjava_dataProcessingPipeline(Blackhole blackhole) {
        Flowable.range(0, size)
                .filter(x -> x % 2 == 0)
                .map(x -> x * 2)
                .flatMap(x -> Flowable.just(x, x + 1, x + 2))
                .filter(x -> x % 3 == 0)
                .distinct()
                .take(size / 2)
                .map(x -> x.toString())
                .subscribe(blackhole::consume);
    }

    // ================== AGGREGATION PIPELINE ==================
    
    @Benchmark
    public void observable_aggregationPipeline(Blackhole blackhole) {
        Observable.range(0, size)
                .groupBy(x -> x % 10)
                .flatMap(group -> 
                    group.map(x -> x * 2)
                         .filter(x -> x > 10)
                         .reduce(0, Integer::sum)
                )
                .scan(0, Integer::sum)
                .subscribe(blackhole::consume);
    }
    
    @Benchmark
    public void rxjava_aggregationPipeline(Blackhole blackhole) {
        Flowable.range(0, size)
                .groupBy(x -> x % 10)
                .flatMap(group -> 
                    group.map(x -> x * 2)
                         .filter(x -> x > 10)
                         .reduce(0, Integer::sum)
                         .toFlowable()
                )
                .scan(0, Integer::sum)
                .subscribe(blackhole::consume);
    }

    // ================== COMBINATION PIPELINE ==================
    
    @Benchmark
    public void observable_combinationPipeline(Blackhole blackhole) {
        Observable<Integer> source1 = Observable.range(0, size / 2);
        Observable<Integer> source2 = Observable.range(size / 2, size / 2);
        Observable<Integer> merged = Observable.merge(source1, source2).map(x -> x * 2);
        
        Observable.zip(merged, Observable.range(0, size), (a, b) -> a + b)
                .filter(x -> x % 5 == 0)
                .distinctUntilChanged()
                .subscribe(blackhole::consume);
    }
    
    @Benchmark
    public void rxjava_combinationPipeline(Blackhole blackhole) {
        Flowable<Integer> source1 = Flowable.range(0, size / 2);
        Flowable<Integer> source2 = Flowable.range(size / 2, size / 2);
        
        Flowable.merge(source1, source2)
                .map(x -> x * 2)
                .zipWith(Flowable.range(0, size), (a, b) -> a + b)
                .filter(x -> x % 5 == 0)
                .distinctUntilChanged()
                .subscribe(blackhole::consume);
    }

    // ================== WINDOWING PIPELINE ==================
    
    @Benchmark
    public void observable_windowingPipeline(Blackhole blackhole) {
        Observable.range(0, size)
                .window(100)
                .flatMap(window -> 
                    window.filter(x -> x % 2 == 0)
                          .map(x -> x * 3)
                          .reduce(0, Integer::sum)
                )
                .filter(x -> x > 1000)
                .subscribe(blackhole::consume);
    }
    
    @Benchmark
    public void rxjava_windowingPipeline(Blackhole blackhole) {
        Flowable.range(0, size)
                .window(100)
                .flatMap(window -> 
                    window.filter(x -> x % 2 == 0)
                          .map(x -> x * 3)
                          .reduce(0, Integer::sum)
                          .toFlowable()
                )
                .filter(x -> x > 1000)
                .subscribe(blackhole::consume);
    }

    // ================== BUFFERING PIPELINE ==================
    
    @Benchmark
    public void observable_bufferingPipeline(Blackhole blackhole) {
        Observable.range(0, size)
                .buffer(50)
                .map(list -> list.stream().mapToInt(Integer::intValue).sum())
                .filter(x -> x % 100 == 0)
                .flatMap(x -> Observable.just(x, x * 2, x * 3))
                .distinct()
                .subscribe(blackhole::consume);
    }
    
    @Benchmark
    public void rxjava_bufferingPipeline(Blackhole blackhole) {
        Flowable.range(0, size)
                .buffer(50)
                .map(list -> list.stream().mapToInt(Integer::intValue).sum())
                .filter(x -> x % 100 == 0)
                .flatMap(x -> Flowable.just(x, x * 2, x * 3))
                .distinct()
                .subscribe(blackhole::consume);
    }

    // ================== ERROR HANDLING PIPELINE ==================
    
    @Benchmark
    public void observable_errorHandlingPipeline(Blackhole blackhole) {
        Observable.range(0, size)
                .map(x -> {
                    if (x % 1000 == 999) {
                        throw new RuntimeException("Test error");
                    }
                    return x * 2;
                })
                .onErrorReturn(e -> -1)
                .filter(x -> x >= 0)
                .distinctUntilChanged()
                .subscribe(blackhole::consume);
    }
    
    @Benchmark
    public void rxjava_errorHandlingPipeline(Blackhole blackhole) {
        Flowable.range(0, size)
                .map(x -> {
                    if (x % 1000 == 999) {
                        throw new RuntimeException("Test error");
                    }
                    return x * 2;
                })
                .onErrorReturn(e -> -1)
                .filter(x -> x >= 0)
                .distinctUntilChanged()
                .subscribe(blackhole::consume);
    }

    // ================== NESTED FLATMAP PIPELINE ==================
    
    @Benchmark
    public void observable_nestedFlatMapPipeline(Blackhole blackhole) {
        Observable.range(0, size / 10)
                .flatMap(x -> 
                    Observable.range(x * 10, 10)
                              .map(y -> y * 2)
                              .flatMap(y -> Observable.just(y, y + 1))
                )
                .filter(x -> x % 5 == 0)
                .distinct()
                .take(size / 2)
                .subscribe(blackhole::consume);
    }
    
    @Benchmark
    public void rxjava_nestedFlatMapPipeline(Blackhole blackhole) {
        Flowable.range(0, size / 10)
                .flatMap(x -> 
                    Flowable.range(x * 10, 10)
                            .map(y -> y * 2)
                            .flatMap(y -> Flowable.just(y, y + 1))
                )
                .filter(x -> x % 5 == 0)
                .distinct()
                .take(size / 2)
                .subscribe(blackhole::consume);
    }

    // ================== STATISTICAL PIPELINE ==================
    
    @Benchmark
    public void observable_statisticalPipeline(Blackhole blackhole) {
        Observable.range(0, size)
                .map(x -> x * 2)
                .scan(0, Integer::sum)
                .skip(10)
                .take(size - 20)
                .map(x -> x / 10)
                .distinctUntilChanged()
                .subscribe(blackhole::consume);
    }
    
    @Benchmark
    public void rxjava_statisticalPipeline(Blackhole blackhole) {
        Flowable.range(0, size)
                .map(x -> x * 2)
                .scan(0, Integer::sum)
                .skip(10)
                .take(size - 20)
                .map(x -> x / 10)
                .distinctUntilChanged()
                .subscribe(blackhole::consume);
    }

    // ================== REAL-WORLD ETL PIPELINE ==================
    
    @Benchmark
    public void observable_etlPipeline(Blackhole blackhole) {
        Observable.range(0, size)
                // Extract
                .map(x -> x * 100)
                // Transform
                .filter(x -> x % 200 == 0)
                .map(x -> x.toString())
                .map(String::length)
                // Group and aggregate
                .groupBy(x -> x % 5)
                .flatMap(group -> group.count())
                // Post-processing
                .map(x -> x * 10)
                .scan(0L, Long::sum)
                // Load
                .subscribe(blackhole::consume);
    }
    
    @Benchmark
    public void rxjava_etlPipeline(Blackhole blackhole) {
        Flowable.range(0, size)
                // Extract
                .map(x -> x * 100)
                // Transform
                .filter(x -> x % 200 == 0)
                .map(x -> x.toString())
                .map(String::length)
                // Group and aggregate
                .groupBy(x -> x % 5)
                .flatMap(group -> group.count().toFlowable())
                // Post-processing
                .map(x -> x * 10)
                .scan(0L, Long::sum)
                // Load
                .subscribe(blackhole::consume);
    }
}
