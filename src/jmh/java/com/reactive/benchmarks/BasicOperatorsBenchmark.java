package com.reactive.benchmarks;

import com.reactive.core.Observable;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * Benchmark comparativo de operadores básicos entre nuestra implementación y RxJava
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 2)
@Fork(1)
public class BasicOperatorsBenchmark {

    @Param({"10", "100", "1000", "10000"})
    private int size;

    // ========== Nuestra Implementación ==========

    @Benchmark
    public void ourMap(Blackhole bh) {
        Observable.range(0, size)
            .map(x -> x * 2)
            .subscribe(bh::consume, Throwable::printStackTrace, () -> {});
    }

    @Benchmark
    public void ourFilter(Blackhole bh) {
        Observable.range(0, size)
            .filter(x -> x % 2 == 0)
            .subscribe(bh::consume, Throwable::printStackTrace, () -> {});
    }

    @Benchmark
    public void ourMapFilter(Blackhole bh) {
        Observable.range(0, size)
            .map(x -> x * 2)
            .filter(x -> x % 4 == 0)
            .subscribe(bh::consume, Throwable::printStackTrace, () -> {});
    }

    @Benchmark
    public void ourFlatMap(Blackhole bh) {
        Observable.range(0, size)
            .flatMap(x -> Observable.just(x, x + 1))
            .subscribe(bh::consume, Throwable::printStackTrace, () -> {});
    }

    @Benchmark
    public void ourMapFilterFlatMap(Blackhole bh) {
        Observable.range(0, size / 10)
            .map(x -> x * 2)
            .filter(x -> x % 4 == 0)
            .flatMap(x -> Observable.just(x, x + 1, x + 2))
            .subscribe(bh::consume, Throwable::printStackTrace, () -> {});
    }

    @Benchmark
    public void ourTake(Blackhole bh) {
        Observable.range(0, size)
            .take(size / 2)
            .subscribe(bh::consume, Throwable::printStackTrace, () -> {});
    }

    @Benchmark
    public void ourSkip(Blackhole bh) {
        Observable.range(0, size)
            .skip(size / 2)
            .subscribe(bh::consume, Throwable::printStackTrace, () -> {});
    }

    @Benchmark
    public void ourTakeSkip(Blackhole bh) {
        Observable.range(0, size)
            .skip(size / 4)
            .take(size / 2)
            .subscribe(bh::consume, Throwable::printStackTrace, () -> {});
    }

    // ========== RxJava 3 ==========

    @Benchmark
    public void rxJavaMap(Blackhole bh) {
        io.reactivex.rxjava3.core.Observable.range(0, size)
            .map(x -> x * 2)
            .subscribe(bh::consume, Throwable::printStackTrace, () -> {});
    }

    @Benchmark
    public void rxJavaFilter(Blackhole bh) {
        io.reactivex.rxjava3.core.Observable.range(0, size)
            .filter(x -> x % 2 == 0)
            .subscribe(bh::consume, Throwable::printStackTrace, () -> {});
    }

    @Benchmark
    public void rxJavaMapFilter(Blackhole bh) {
        io.reactivex.rxjava3.core.Observable.range(0, size)
            .map(x -> x * 2)
            .filter(x -> x % 4 == 0)
            .subscribe(bh::consume, Throwable::printStackTrace, () -> {});
    }

    @Benchmark
    public void rxJavaFlatMap(Blackhole bh) {
        io.reactivex.rxjava3.core.Observable.range(0, size)
            .flatMap(x -> io.reactivex.rxjava3.core.Observable.just(x, x + 1))
            .subscribe(bh::consume, Throwable::printStackTrace, () -> {});
    }

    @Benchmark
    public void rxJavaMapFilterFlatMap(Blackhole bh) {
        io.reactivex.rxjava3.core.Observable.range(0, size / 10)
            .map(x -> x * 2)
            .filter(x -> x % 4 == 0)
            .flatMap(x -> io.reactivex.rxjava3.core.Observable.just(x, x + 1, x + 2))
            .subscribe(bh::consume, Throwable::printStackTrace, () -> {});
    }

    @Benchmark
    public void rxJavaTake(Blackhole bh) {
        io.reactivex.rxjava3.core.Observable.range(0, size)
            .take(size / 2)
            .subscribe(bh::consume, Throwable::printStackTrace, () -> {});
    }

    @Benchmark
    public void rxJavaSkip(Blackhole bh) {
        io.reactivex.rxjava3.core.Observable.range(0, size)
            .skip(size / 2)
            .subscribe(bh::consume, Throwable::printStackTrace, () -> {});
    }

    @Benchmark
    public void rxJavaTakeSkip(Blackhole bh) {
        io.reactivex.rxjava3.core.Observable.range(0, size)
            .skip(size / 4)
            .take(size / 2)
            .subscribe(bh::consume, Throwable::printStackTrace, () -> {});
    }
}
