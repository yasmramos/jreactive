package com.reactive.benchmarks;

import com.reactive.core.Observable;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * Benchmark comparativo de creación de observables
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 2)
@Fork(1)
public class CreationBenchmark {

    @Param({"10", "100", "1000"})
    private int size;

    private Integer[] array;

    @Setup
    public void setup() {
        array = new Integer[size];
        for (int i = 0; i < size; i++) {
            array[i] = i;
        }
    }

    // ========== Observable Creation - Nuestra Implementación ==========

    @Benchmark
    public void ourJust(Blackhole bh) {
        Observable.just(1, 2, 3, 4, 5)
            .subscribe(bh::consume, Throwable::printStackTrace, () -> {});
    }

    @Benchmark
    public void ourRange(Blackhole bh) {
        Observable.range(0, size)
            .subscribe(bh::consume, Throwable::printStackTrace, () -> {});
    }

    @Benchmark
    public void ourFromIterable(Blackhole bh) {
        Observable.fromIterable(java.util.Arrays.asList(array))
            .subscribe(bh::consume, Throwable::printStackTrace, () -> {});
    }

    @Benchmark
    public void ourCreate(Blackhole bh) {
        Observable.create(emitter -> {
            for (int i = 0; i < size; i++) {
                emitter.onNext(i);
            }
            emitter.onComplete();
        }).subscribe(bh::consume, Throwable::printStackTrace, () -> {});
    }

    // ========== Observable Creation - RxJava 3 ==========

    @Benchmark
    public void rxJavaJust(Blackhole bh) {
        io.reactivex.rxjava3.core.Observable.just(1, 2, 3, 4, 5)
            .subscribe(bh::consume, Throwable::printStackTrace, () -> {});
    }

    @Benchmark
    public void rxJavaRange(Blackhole bh) {
        io.reactivex.rxjava3.core.Observable.range(0, size)
            .subscribe(bh::consume, Throwable::printStackTrace, () -> {});
    }

    @Benchmark
    public void rxJavaFromIterable(Blackhole bh) {
        io.reactivex.rxjava3.core.Observable.fromIterable(java.util.Arrays.asList(array))
            .subscribe(bh::consume, Throwable::printStackTrace, () -> {});
    }

    @Benchmark
    public void rxJavaCreate(Blackhole bh) {
        io.reactivex.rxjava3.core.Observable.create(emitter -> {
            for (int i = 0; i < size; i++) {
                emitter.onNext(i);
            }
            emitter.onComplete();
        }).subscribe(bh::consume, Throwable::printStackTrace, () -> {});
    }
}
