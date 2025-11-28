package com.reactive.benchmarks;

import com.reactive.core.Single;
import com.reactive.core.Maybe;
import com.reactive.core.Completable;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Benchmark comparativo de Single, Maybe y Completable
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 2)
@Fork(1)
public class SpecializedTypesBenchmark {

    @Param({"10", "100", "1000"})
    private int count;

    // ==================== SINGLE BENCHMARKS ====================
    
    // Single Creation - Ours
    @Benchmark
    public void ourSingleJust(Blackhole bh) {
        for (int i = 0; i < count; i++) {
            Single.just(i).subscribe(bh::consume, Throwable::printStackTrace);
        }
    }
    
    @Benchmark
    public void ourSingleFromCallable(Blackhole bh) {
        for (int i = 0; i < count; i++) {
            Single.fromCallable(() -> 42).subscribe(bh::consume, Throwable::printStackTrace);
        }
    }
    
    // Single Operators - Ours
    @Benchmark
    public void ourSingleMap(Blackhole bh) {
        for (int i = 0; i < count; i++) {
            Single.just(i)
                .map(x -> x * 2)
                .subscribe(bh::consume, Throwable::printStackTrace);
        }
    }
    
    @Benchmark
    public void ourSingleFlatMap(Blackhole bh) {
        for (int i = 0; i < count; i++) {
            Single.just(i)
                .flatMap(x -> Single.just(x * 2))
                .subscribe(bh::consume, Throwable::printStackTrace);
        }
    }
    
    @Benchmark
    public void ourSingleZip(Blackhole bh) {
        for (int i = 0; i < count; i++) {
            Single.zip(
                Single.just(i),
                Single.just(i * 2),
                (a, b) -> a + b
            ).subscribe(bh::consume, Throwable::printStackTrace);
        }
    }
    
    @Benchmark
    public void ourSingleOnErrorReturn(Blackhole bh) {
        for (int i = 0; i < count; i++) {
            Single.just(i)
                .onErrorReturn(e -> -1)
                .subscribe(bh::consume, Throwable::printStackTrace);
        }
    }
    
    // Single Creation - RxJava
    @Benchmark
    public void rxJavaSingleJust(Blackhole bh) {
        for (int i = 0; i < count; i++) {
            io.reactivex.rxjava3.core.Single.just(i)
                .subscribe(bh::consume, Throwable::printStackTrace);
        }
    }
    
    @Benchmark
    public void rxJavaSingleFromCallable(Blackhole bh) {
        for (int i = 0; i < count; i++) {
            io.reactivex.rxjava3.core.Single.fromCallable(() -> 42)
                .subscribe(bh::consume, Throwable::printStackTrace);
        }
    }
    
    // Single Operators - RxJava
    @Benchmark
    public void rxJavaSingleMap(Blackhole bh) {
        for (int i = 0; i < count; i++) {
            io.reactivex.rxjava3.core.Single.just(i)
                .map(x -> x * 2)
                .subscribe(bh::consume, Throwable::printStackTrace);
        }
    }
    
    @Benchmark
    public void rxJavaSingleFlatMap(Blackhole bh) {
        for (int i = 0; i < count; i++) {
            io.reactivex.rxjava3.core.Single.just(i)
                .flatMap(x -> io.reactivex.rxjava3.core.Single.just(x * 2))
                .subscribe(bh::consume, Throwable::printStackTrace);
        }
    }
    
    @Benchmark
    public void rxJavaSingleZip(Blackhole bh) {
        for (int i = 0; i < count; i++) {
            io.reactivex.rxjava3.core.Single.zip(
                io.reactivex.rxjava3.core.Single.just(i),
                io.reactivex.rxjava3.core.Single.just(i * 2),
                (a, b) -> a + b
            ).subscribe(bh::consume, Throwable::printStackTrace);
        }
    }
    
    @Benchmark
    public void rxJavaSingleOnErrorReturn(Blackhole bh) {
        for (int i = 0; i < count; i++) {
            io.reactivex.rxjava3.core.Single.just(i)
                .onErrorReturn(e -> -1)
                .subscribe(bh::consume, Throwable::printStackTrace);
        }
    }
    
    // ==================== MAYBE BENCHMARKS ====================
    
    // Maybe Creation - Ours
    @Benchmark
    public void ourMaybeJust(Blackhole bh) {
        for (int i = 0; i < count; i++) {
            Maybe.just(i).subscribe(bh::consume, Throwable::printStackTrace, () -> {});
        }
    }
    
    @Benchmark
    public void ourMaybeEmpty(Blackhole bh) {
        for (int i = 0; i < count; i++) {
            Maybe.empty().subscribe(bh::consume, Throwable::printStackTrace, () -> {});
        }
    }
    
    @Benchmark
    public void ourMaybeFromCallable(Blackhole bh) {
        for (int i = 0; i < count; i++) {
            Maybe.fromCallable(() -> 42).subscribe(bh::consume, Throwable::printStackTrace, () -> {});
        }
    }
    
    // Maybe Operators - Ours
    @Benchmark
    public void ourMaybeMap(Blackhole bh) {
        for (int i = 0; i < count; i++) {
            Maybe.just(i)
                .map(x -> x * 2)
                .subscribe(bh::consume, Throwable::printStackTrace, () -> {});
        }
    }
    
    @Benchmark
    public void ourMaybeFlatMap(Blackhole bh) {
        for (int i = 0; i < count; i++) {
            Maybe.just(i)
                .flatMap(x -> Maybe.just(x * 2))
                .subscribe(bh::consume, Throwable::printStackTrace, () -> {});
        }
    }
    
    @Benchmark
    public void ourMaybeFilter(Blackhole bh) {
        for (int i = 0; i < count; i++) {
            Maybe.just(i)
                .filter(x -> x % 2 == 0)
                .subscribe(bh::consume, Throwable::printStackTrace, () -> {});
        }
    }
    
    @Benchmark
    public void ourMaybeDefaultIfEmpty(Blackhole bh) {
        for (int i = 0; i < count; i++) {
            Maybe.<Integer>empty()
                .defaultIfEmpty(42)
                .subscribe(bh::consume, Throwable::printStackTrace);
        }
    }
    
    @Benchmark
    public void ourMaybeSwitchIfEmpty(Blackhole bh) {
        for (int i = 0; i < count; i++) {
            Maybe.<Integer>empty()
                .switchIfEmpty(Maybe.just(42))
                .subscribe(bh::consume, Throwable::printStackTrace, () -> {});
        }
    }
    
    // Maybe Creation - RxJava
    @Benchmark
    public void rxJavaMaybeJust(Blackhole bh) {
        for (int i = 0; i < count; i++) {
            io.reactivex.rxjava3.core.Maybe.just(i)
                .subscribe(bh::consume, Throwable::printStackTrace, () -> {});
        }
    }
    
    @Benchmark
    public void rxJavaMaybeEmpty(Blackhole bh) {
        for (int i = 0; i < count; i++) {
            io.reactivex.rxjava3.core.Maybe.empty()
                .subscribe(bh::consume, Throwable::printStackTrace, () -> {});
        }
    }
    
    @Benchmark
    public void rxJavaMaybeFromCallable(Blackhole bh) {
        for (int i = 0; i < count; i++) {
            io.reactivex.rxjava3.core.Maybe.fromCallable(() -> 42)
                .subscribe(bh::consume, Throwable::printStackTrace, () -> {});
        }
    }
    
    // Maybe Operators - RxJava
    @Benchmark
    public void rxJavaMaybeMap(Blackhole bh) {
        for (int i = 0; i < count; i++) {
            io.reactivex.rxjava3.core.Maybe.just(i)
                .map(x -> x * 2)
                .subscribe(bh::consume, Throwable::printStackTrace, () -> {});
        }
    }
    
    @Benchmark
    public void rxJavaMaybeFlatMap(Blackhole bh) {
        for (int i = 0; i < count; i++) {
            io.reactivex.rxjava3.core.Maybe.just(i)
                .flatMap(x -> io.reactivex.rxjava3.core.Maybe.just(x * 2))
                .subscribe(bh::consume, Throwable::printStackTrace, () -> {});
        }
    }
    
    @Benchmark
    public void rxJavaMaybeFilter(Blackhole bh) {
        for (int i = 0; i < count; i++) {
            io.reactivex.rxjava3.core.Maybe.just(i)
                .filter(x -> x % 2 == 0)
                .subscribe(bh::consume, Throwable::printStackTrace, () -> {});
        }
    }
    
    @Benchmark
    public void rxJavaMaybeDefaultIfEmpty(Blackhole bh) {
        for (int i = 0; i < count; i++) {
            io.reactivex.rxjava3.core.Maybe.<Integer>empty()
                .defaultIfEmpty(42)
                .subscribe(bh::consume, Throwable::printStackTrace);
        }
    }
    
    @Benchmark
    public void rxJavaMaybeSwitchIfEmpty(Blackhole bh) {
        for (int i = 0; i < count; i++) {
            io.reactivex.rxjava3.core.Maybe.<Integer>empty()
                .switchIfEmpty(io.reactivex.rxjava3.core.Maybe.just(42))
                .subscribe(bh::consume, Throwable::printStackTrace, () -> {});
        }
    }
    
    // ==================== COMPLETABLE BENCHMARKS ====================
    
    // Completable Creation - Ours
    @Benchmark
    public void ourCompletableComplete(Blackhole bh) {
        for (int i = 0; i < count; i++) {
            Completable.complete().subscribe(() -> bh.consume(1), Throwable::printStackTrace);
        }
    }
    
    @Benchmark
    public void ourCompletableFromAction(Blackhole bh) {
        AtomicInteger counter = new AtomicInteger(0);
        for (int i = 0; i < count; i++) {
            Completable.fromAction(counter::incrementAndGet)
                .subscribe(() -> bh.consume(1), Throwable::printStackTrace);
        }
    }
    
    // Completable Operators - Ours
    @Benchmark
    public void ourCompletableAndThen(Blackhole bh) {
        for (int i = 0; i < count; i++) {
            Completable.complete()
                .andThen(Completable.complete())
                .subscribe(() -> bh.consume(1), Throwable::printStackTrace);
        }
    }
    
    @Benchmark
    public void ourCompletableMerge(Blackhole bh) {
        for (int i = 0; i < count; i++) {
            Completable.merge(
                Completable.complete(),
                Completable.complete(),
                Completable.complete()
            ).subscribe(() -> bh.consume(1), Throwable::printStackTrace);
        }
    }
    
    @Benchmark
    public void ourCompletableConcat(Blackhole bh) {
        for (int i = 0; i < count; i++) {
            Completable.concat(
                Completable.complete(),
                Completable.complete(),
                Completable.complete()
            ).subscribe(() -> bh.consume(1), Throwable::printStackTrace);
        }
    }
    
    @Benchmark
    public void ourCompletableOnErrorComplete(Blackhole bh) {
        for (int i = 0; i < count; i++) {
            Completable.complete()
                .onErrorComplete()
                .subscribe(() -> bh.consume(1), Throwable::printStackTrace);
        }
    }
    
    // Completable Creation - RxJava
    @Benchmark
    public void rxJavaCompletableComplete(Blackhole bh) {
        for (int i = 0; i < count; i++) {
            io.reactivex.rxjava3.core.Completable.complete()
                .subscribe(() -> bh.consume(1), Throwable::printStackTrace);
        }
    }
    
    @Benchmark
    public void rxJavaCompletableFromAction(Blackhole bh) {
        AtomicInteger counter = new AtomicInteger(0);
        for (int i = 0; i < count; i++) {
            io.reactivex.rxjava3.core.Completable.fromAction(counter::incrementAndGet)
                .subscribe(() -> bh.consume(1), Throwable::printStackTrace);
        }
    }
    
    // Completable Operators - RxJava
    @Benchmark
    public void rxJavaCompletableAndThen(Blackhole bh) {
        for (int i = 0; i < count; i++) {
            io.reactivex.rxjava3.core.Completable.complete()
                .andThen(io.reactivex.rxjava3.core.Completable.complete())
                .subscribe(() -> bh.consume(1), Throwable::printStackTrace);
        }
    }
    
    @Benchmark
    public void rxJavaCompletableMerge(Blackhole bh) {
        for (int i = 0; i < count; i++) {
            io.reactivex.rxjava3.core.Completable.merge(java.util.Arrays.asList(
                io.reactivex.rxjava3.core.Completable.complete(),
                io.reactivex.rxjava3.core.Completable.complete(),
                io.reactivex.rxjava3.core.Completable.complete()
            )).subscribe(() -> bh.consume(1), Throwable::printStackTrace);
        }
    }
    
    @Benchmark
    public void rxJavaCompletableConcat(Blackhole bh) {
        for (int i = 0; i < count; i++) {
            io.reactivex.rxjava3.core.Completable.concat(java.util.Arrays.asList(
                io.reactivex.rxjava3.core.Completable.complete(),
                io.reactivex.rxjava3.core.Completable.complete(),
                io.reactivex.rxjava3.core.Completable.complete()
            )).subscribe(() -> bh.consume(1), Throwable::printStackTrace);
        }
    }
    
    @Benchmark
    public void rxJavaCompletableOnErrorComplete(Blackhole bh) {
        for (int i = 0; i < count; i++) {
            io.reactivex.rxjava3.core.Completable.complete()
                .onErrorComplete()
                .subscribe(() -> bh.consume(1), Throwable::printStackTrace);
        }
    }
}
