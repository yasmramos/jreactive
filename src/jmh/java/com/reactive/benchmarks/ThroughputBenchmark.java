package com.reactive.benchmarks;

import com.reactive.core.Observable;
import io.reactivex.rxjava3.core.Flowable;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * High-throughput benchmark comparing Observable and RxJava performance
 * with large data volumes
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 3)
@Fork(1)
public class ThroughputBenchmark {

    @Param({"100000", "1000000"})
    private int size;

    // ================== SIMPLE PIPELINE ==================
    
    @Benchmark
    public void observable_simplePipeline(Blackhole blackhole) {
        Observable.range(0, size)
                .map(x -> x * 2)
                .filter(x -> x % 4 == 0)
                .subscribe(blackhole::consume);
    }
    
    @Benchmark
    public void rxjava_simplePipeline(Blackhole blackhole) {
        Flowable.range(0, size)
                .map(x -> x * 2)
                .filter(x -> x % 4 == 0)
                .subscribe(blackhole::consume);
    }

    // ================== COMPLEX PIPELINE ==================
    
    @Benchmark
    public void observable_complexPipeline(Blackhole blackhole) {
        Observable.range(0, size)
                .map(x -> x * 2)
                .filter(x -> x % 2 == 0)
                .map(x -> x + 1)
                .filter(x -> x > 10)
                .map(x -> x * 3)
                .filter(x -> x % 6 == 0)
                .subscribe(blackhole::consume);
    }
    
    @Benchmark
    public void rxjava_complexPipeline(Blackhole blackhole) {
        Flowable.range(0, size)
                .map(x -> x * 2)
                .filter(x -> x % 2 == 0)
                .map(x -> x + 1)
                .filter(x -> x > 10)
                .map(x -> x * 3)
                .filter(x -> x % 6 == 0)
                .subscribe(blackhole::consume);
    }

    // ================== FLATMAP INTENSIVE ==================
    
    @Benchmark
    public void observable_flatMapIntensive(Blackhole blackhole) {
        Observable.range(0, size / 100)
                .flatMap(x -> Observable.range(x * 10, 10))
                .map(x -> x * 2)
                .subscribe(blackhole::consume);
    }
    
    @Benchmark
    public void rxjava_flatMapIntensive(Blackhole blackhole) {
        Flowable.range(0, size / 100)
                .flatMap(x -> Flowable.range(x * 10, 10))
                .map(x -> x * 2)
                .subscribe(blackhole::consume);
    }

    // ================== MERGE INTENSIVE ==================
    
    @Benchmark
    public void observable_mergeIntensive(Blackhole blackhole) {
        Observable.merge(
                Observable.range(0, size / 3),
                Observable.range(size / 3, size / 3),
                Observable.range(2 * size / 3, size / 3)
        ).subscribe(blackhole::consume);
    }
    
    @Benchmark
    public void rxjava_mergeIntensive(Blackhole blackhole) {
        Flowable.merge(
                Flowable.range(0, size / 3),
                Flowable.range(size / 3, size / 3),
                Flowable.range(2 * size / 3, size / 3)
        ).subscribe(blackhole::consume);
    }

    // ================== ZIP INTENSIVE ==================
    
    @Benchmark
    public void observable_zipIntensive(Blackhole blackhole) {
        Observable.zip(
                Observable.range(0, size),
                Observable.range(0, size),
                (a, b) -> a + b
        ).subscribe(blackhole::consume);
    }
    
    @Benchmark
    public void rxjava_zipIntensive(Blackhole blackhole) {
        Flowable.zip(
                Flowable.range(0, size),
                Flowable.range(0, size),
                (a, b) -> a + b
        ).subscribe(blackhole::consume);
    }

    // ================== REDUCE INTENSIVE ==================
    
    @Benchmark
    public void observable_reduceIntensive(Blackhole blackhole) {
        Observable.range(0, size)
                .reduce(0, Integer::sum)
                .subscribe(blackhole::consume);
    }
    
    @Benchmark
    public void rxjava_reduceIntensive(Blackhole blackhole) {
        Flowable.range(0, size)
                .reduce(0, Integer::sum)
                .subscribe(blackhole::consume);
    }

    // ================== DISTINCT INTENSIVE ==================
    
    @Benchmark
    public void observable_distinctIntensive(Blackhole blackhole) {
        Observable.range(0, size)
                .map(x -> x % 1000)
                .distinct()
                .subscribe(blackhole::consume);
    }
    
    @Benchmark
    public void rxjava_distinctIntensive(Blackhole blackhole) {
        Flowable.range(0, size)
                .map(x -> x % 1000)
                .distinct()
                .subscribe(blackhole::consume);
    }

    // ================== GROUPED OPERATIONS ==================
    
    @Benchmark
    public void observable_groupedOperations(Blackhole blackhole) {
        Observable.range(0, size)
                .groupBy(x -> x % 10)
                .flatMap(group -> group.reduce(0, Integer::sum))
                .subscribe(blackhole::consume);
    }
    
    @Benchmark
    public void rxjava_groupedOperations(Blackhole blackhole) {
        Flowable.range(0, size)
                .groupBy(x -> x % 10)
                .flatMap(group -> group.reduce(0, Integer::sum).toFlowable())
                .subscribe(blackhole::consume);
    }
}
