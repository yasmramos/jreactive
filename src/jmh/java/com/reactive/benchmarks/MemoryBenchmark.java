package com.reactive.benchmarks;

import com.reactive.core.Observable;
import io.reactivex.rxjava3.core.Flowable;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Memory footprint benchmark comparing Observable and RxJava
 * Measures memory allocation and GC pressure
 */
@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 3)
@Fork(value = 1, jvmArgs = {"-Xms2G", "-Xmx2G"})
public class MemoryBenchmark {

    @Param({"10000", "100000"})
    private int size;

    private List<Integer> dataList;

    @Setup
    public void setup() {
        dataList = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            dataList.add(i);
        }
    }

    // ================== CREATION MEMORY ==================
    
    @Benchmark
    public void observable_rangeCreation(Blackhole blackhole) {
        Observable.range(0, size)
                .subscribe(blackhole::consume);
    }
    
    @Benchmark
    public void rxjava_rangeCreation(Blackhole blackhole) {
        Flowable.range(0, size)
                .subscribe(blackhole::consume);
    }
    
    @Benchmark
    public void observable_fromIterableCreation(Blackhole blackhole) {
        Observable.fromIterable(dataList)
                .subscribe(blackhole::consume);
    }
    
    @Benchmark
    public void rxjava_fromIterableCreation(Blackhole blackhole) {
        Flowable.fromIterable(dataList)
                .subscribe(blackhole::consume);
    }

    // ================== OPERATOR CHAIN MEMORY ==================
    
    @Benchmark
    public void observable_longChain(Blackhole blackhole) {
        Observable.range(0, size)
                .map(x -> x * 2)
                .filter(x -> x % 4 == 0)
                .map(x -> x + 1)
                .filter(x -> x > 10)
                .map(x -> x * 3)
                .filter(x -> x % 6 == 0)
                .map(x -> x - 5)
                .filter(x -> x > 0)
                .subscribe(blackhole::consume);
    }
    
    @Benchmark
    public void rxjava_longChain(Blackhole blackhole) {
        Flowable.range(0, size)
                .map(x -> x * 2)
                .filter(x -> x % 4 == 0)
                .map(x -> x + 1)
                .filter(x -> x > 10)
                .map(x -> x * 3)
                .filter(x -> x % 6 == 0)
                .map(x -> x - 5)
                .filter(x -> x > 0)
                .subscribe(blackhole::consume);
    }

    // ================== FLATMAP MEMORY ==================
    
    @Benchmark
    public void observable_flatMapMemory(Blackhole blackhole) {
        Observable.range(0, size / 10)
                .flatMap(x -> Observable.range(x * 10, 10))
                .subscribe(blackhole::consume);
    }
    
    @Benchmark
    public void rxjava_flatMapMemory(Blackhole blackhole) {
        Flowable.range(0, size / 10)
                .flatMap(x -> Flowable.range(x * 10, 10))
                .subscribe(blackhole::consume);
    }

    // ================== BUFFER MEMORY ==================
    
    @Benchmark
    public void observable_bufferMemory(Blackhole blackhole) {
        Observable.range(0, size)
                .buffer(100)
                .subscribe(blackhole::consume);
    }
    
    @Benchmark
    public void rxjava_bufferMemory(Blackhole blackhole) {
        Flowable.range(0, size)
                .buffer(100)
                .subscribe(blackhole::consume);
    }

    // ================== WINDOW MEMORY ==================
    
    @Benchmark
    public void observable_windowMemory(Blackhole blackhole) {
        Observable.range(0, size)
                .window(100)
                .flatMap(window -> window.reduce(0, Integer::sum))
                .subscribe(blackhole::consume);
    }
    
    @Benchmark
    public void rxjava_windowMemory(Blackhole blackhole) {
        Flowable.range(0, size)
                .window(100)
                .flatMap(window -> window.reduce(0, Integer::sum).toFlowable())
                .subscribe(blackhole::consume);
    }

    // ================== DISTINCT MEMORY (HIGH GC PRESSURE) ==================
    
    @Benchmark
    public void observable_distinctMemory(Blackhole blackhole) {
        Observable.range(0, size)
                .map(x -> x % 1000)
                .distinct()
                .subscribe(blackhole::consume);
    }
    
    @Benchmark
    public void rxjava_distinctMemory(Blackhole blackhole) {
        Flowable.range(0, size)
                .map(x -> x % 1000)
                .distinct()
                .subscribe(blackhole::consume);
    }

    // ================== GROUPBY MEMORY ==================
    
    @Benchmark
    public void observable_groupByMemory(Blackhole blackhole) {
        Observable.range(0, size)
                .groupBy(x -> x % 10)
                .flatMap(group -> group.count())
                .subscribe(blackhole::consume);
    }
    
    @Benchmark
    public void rxjava_groupByMemory(Blackhole blackhole) {
        Flowable.range(0, size)
                .groupBy(x -> x % 10)
                .flatMap(group -> group.count().toFlowable())
                .subscribe(blackhole::consume);
    }

    // ================== SCAN MEMORY ==================
    
    @Benchmark
    public void observable_scanMemory(Blackhole blackhole) {
        Observable.range(0, size)
                .scan(0, Integer::sum)
                .subscribe(blackhole::consume);
    }
    
    @Benchmark
    public void rxjava_scanMemory(Blackhole blackhole) {
        Flowable.range(0, size)
                .scan(0, Integer::sum)
                .subscribe(blackhole::consume);
    }

    // ================== COLLECT MEMORY ==================
    
    @Benchmark
    public void observable_collectMemory(Blackhole blackhole) {
        Observable.range(0, size)
                .collect(ArrayList<Integer>::new, List::add)
                .subscribe(blackhole::consume);
    }
    
    @Benchmark
    public void rxjava_collectMemory(Blackhole blackhole) {
        Flowable.range(0, size)
                .collect(ArrayList<Integer>::new, List::add)
                .subscribe(blackhole::consume);
    }
}
