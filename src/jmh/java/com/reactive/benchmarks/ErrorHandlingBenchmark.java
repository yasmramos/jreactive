package com.reactive.benchmarks;

import com.reactive.core.Observable;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Benchmark comparativo de operadores de manejo de errores
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 2)
@Fork(1)
public class ErrorHandlingBenchmark {

    @Param({"10", "100", "1000"})
    private int size;

    // ========== OnErrorReturn - Nuestra Implementación ==========

    @Benchmark
    public void ourOnErrorReturnSuccess(Blackhole bh) {
        Observable.range(0, size)
            .onErrorReturn(error -> -1)
            .subscribe(bh::consume, Throwable::printStackTrace, () -> {});
    }

    @Benchmark
    public void ourOnErrorReturnWithError(Blackhole bh) {
        Observable.<Integer>create(emitter -> {
            for (int i = 0; i < size / 2; i++) {
                emitter.onNext(i);
            }
            emitter.onError(new RuntimeException("Error"));
        })
        .onErrorReturn(error -> -1)
        .subscribe(bh::consume, error -> {}, () -> {});
    }

    // ========== OnErrorResumeNext - Nuestra Implementación ==========

    @Benchmark
    public void ourOnErrorResumeNextSuccess(Blackhole bh) {
        Observable.range(0, size)
            .onErrorResumeNext((Throwable error) -> Observable.just(-1, -2, -3))
            .subscribe(bh::consume, Throwable::printStackTrace, () -> {});
    }

    @Benchmark
    public void ourOnErrorResumeNextWithError(Blackhole bh) {
        Observable.<Integer>create(emitter -> {
            for (int i = 0; i < size / 2; i++) {
                emitter.onNext(i);
            }
            emitter.onError(new RuntimeException("Error"));
        })
        .onErrorResumeNext((Throwable error) -> Observable.range(0, size / 2))
        .subscribe(bh::consume, error -> {}, () -> {});
    }

    // ========== Retry - Nuestra Implementación ==========

    @Benchmark
    public void ourRetryNoError(Blackhole bh) {
        Observable.range(0, size)
            .retry(3)
            .subscribe(bh::consume, Throwable::printStackTrace, () -> {});
    }

    @Benchmark
    public void ourRetryWithError(Blackhole bh) {
        AtomicInteger attempts = new AtomicInteger(0);
        Observable.<Integer>create(emitter -> {
            int attempt = attempts.incrementAndGet();
            for (int i = 0; i < size / 2; i++) {
                emitter.onNext(i);
            }
            if (attempt < 3) {
                emitter.onError(new RuntimeException("Error"));
            } else {
                emitter.onComplete();
            }
        })
        .retry(3)
        .subscribe(bh::consume, error -> {}, () -> {});
    }

    // ========== DoOnError - Nuestra Implementación ==========

    @Benchmark
    public void ourDoOnErrorNoError(Blackhole bh) {
        Observable.range(0, size)
            .doOnError(error -> bh.consume(error))
            .subscribe(bh::consume, error -> {}, () -> {});
    }

    @Benchmark
    public void ourDoOnErrorWithError(Blackhole bh) {
        Observable.<Integer>create(emitter -> {
            for (int i = 0; i < size / 2; i++) {
                emitter.onNext(i);
            }
            emitter.onError(new RuntimeException("Error"));
        })
        .doOnError(error -> bh.consume(error))
        .subscribe(value -> bh.consume(value), error -> {}, () -> {});
    }

    // ========== OnErrorReturn - RxJava 3 ==========

    @Benchmark
    public void rxJavaOnErrorReturnSuccess(Blackhole bh) {
        io.reactivex.rxjava3.core.Observable.range(0, size)
            .onErrorReturn(error -> -1)
            .subscribe(bh::consume, Throwable::printStackTrace, () -> {});
    }

    @Benchmark
    public void rxJavaOnErrorReturnWithError(Blackhole bh) {
        io.reactivex.rxjava3.core.Observable.<Integer>create(emitter -> {
            for (int i = 0; i < size / 2; i++) {
                emitter.onNext(i);
            }
            emitter.onError(new RuntimeException("Error"));
        })
        .onErrorReturn(error -> -1)
        .subscribe(bh::consume, error -> {}, () -> {});
    }

    // ========== OnErrorResumeNext - RxJava 3 ==========

    @Benchmark
    public void rxJavaOnErrorResumeNextSuccess(Blackhole bh) {
        io.reactivex.rxjava3.core.Observable.range(0, size)
            .onErrorResumeNext(error -> io.reactivex.rxjava3.core.Observable.just(-1, -2, -3))
            .subscribe(bh::consume, Throwable::printStackTrace, () -> {});
    }

    @Benchmark
    public void rxJavaOnErrorResumeNextWithError(Blackhole bh) {
        io.reactivex.rxjava3.core.Observable.<Integer>create(emitter -> {
            for (int i = 0; i < size / 2; i++) {
                emitter.onNext(i);
            }
            emitter.onError(new RuntimeException("Error"));
        })
        .onErrorResumeNext(error -> io.reactivex.rxjava3.core.Observable.range(0, size / 2))
        .subscribe(bh::consume, error -> {}, () -> {});
    }

    // ========== Retry - RxJava 3 ==========

    @Benchmark
    public void rxJavaRetryNoError(Blackhole bh) {
        io.reactivex.rxjava3.core.Observable.range(0, size)
            .retry(3)
            .subscribe(bh::consume, Throwable::printStackTrace, () -> {});
    }

    @Benchmark
    public void rxJavaRetryWithError(Blackhole bh) {
        AtomicInteger attempts = new AtomicInteger(0);
        io.reactivex.rxjava3.core.Observable.<Integer>create(emitter -> {
            int attempt = attempts.incrementAndGet();
            for (int i = 0; i < size / 2; i++) {
                emitter.onNext(i);
            }
            if (attempt < 3) {
                emitter.onError(new RuntimeException("Error"));
            } else {
                emitter.onComplete();
            }
        })
        .retry(3)
        .subscribe(bh::consume, error -> {}, () -> {});
    }

    // ========== DoOnError - RxJava 3 ==========

    @Benchmark
    public void rxJavaDoOnErrorNoError(Blackhole bh) {
        io.reactivex.rxjava3.core.Observable.range(0, size)
            .doOnError(error -> bh.consume(error))
            .subscribe(bh::consume, error -> {}, () -> {});
    }

    @Benchmark
    public void rxJavaDoOnErrorWithError(Blackhole bh) {
        io.reactivex.rxjava3.core.Observable.<Integer>create(emitter -> {
            for (int i = 0; i < size / 2; i++) {
                emitter.onNext(i);
            }
            emitter.onError(new RuntimeException("Error"));
        })
        .doOnError(error -> bh.consume(error))
        .subscribe(value -> bh.consume(value), error -> {}, () -> {});
    }
}
