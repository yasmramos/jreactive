import com.reactive.core.*;
import com.reactive.schedulers.Schedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Completable;

public class QuickBenchmark {
    
    private static final int WARMUP_ITERATIONS = 100_000;
    private static final int BENCHMARK_ITERATIONS = 1_000_000;
    
    public static void main(String[] args) {
        System.out.println("=".repeat(80));
        System.out.println("BENCHMARK DE RENDIMIENTO - TIPOS ESPECIALIZADOS");
        System.out.println("Comparando Reactive Java vs RxJava 3");
        System.out.println("=".repeat(80));
        System.out.println();
        
        // Single Benchmarks
        System.out.println("SINGLE BENCHMARKS:");
        System.out.println("-".repeat(80));
        benchmarkSingleJust();
        benchmarkSingleMap();
        benchmarkSingleFlatMap();
        System.out.println();
        
        // Maybe Benchmarks
        System.out.println("MAYBE BENCHMARKS:");
        System.out.println("-".repeat(80));
        benchmarkMaybeJust();
        benchmarkMaybeEmpty();
        benchmarkMaybeMap();
        System.out.println();
        
        // Completable Benchmarks
        System.out.println("COMPLETABLE BENCHMARKS:");
        System.out.println("-".repeat(80));
        benchmarkCompletableComplete();
        benchmarkCompletableAndThen();
        System.out.println();
        
        System.out.println("=".repeat(80));
        System.out.println("BENCHMARK COMPLETADO");
        System.out.println("=".repeat(80));
    }
    
    // Single Benchmarks
    
    private static void benchmarkSingleJust() {
        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            com.reactive.core.Single.just(42).subscribe(v -> {}, e -> {});
            Single.just(42).subscribe(v -> {}, e -> {});
        }
        
        // Reactive Java
        long start = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            com.reactive.core.Single.just(42).subscribe(v -> {}, e -> {});
        }
        long reactiveTime = System.nanoTime() - start;
        
        // RxJava
        start = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            Single.just(42).subscribe(v -> {}, e -> {});
        }
        long rxTime = System.nanoTime() - start;
        
        printResults("Single.just", reactiveTime, rxTime);
    }
    
    private static void benchmarkSingleMap() {
        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            com.reactive.core.Single.just(42).map(v -> v * 2).subscribe(v -> {}, e -> {});
            Single.just(42).map(v -> v * 2).subscribe(v -> {}, e -> {});
        }
        
        // Reactive Java
        long start = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            com.reactive.core.Single.just(42).map(v -> v * 2).subscribe(v -> {}, e -> {});
        }
        long reactiveTime = System.nanoTime() - start;
        
        // RxJava
        start = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            Single.just(42).map(v -> v * 2).subscribe(v -> {}, e -> {});
        }
        long rxTime = System.nanoTime() - start;
        
        printResults("Single.map", reactiveTime, rxTime);
    }
    
    private static void benchmarkSingleFlatMap() {
        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            com.reactive.core.Single.just(42).flatMap(v -> com.reactive.core.Single.just(v * 2))
                .subscribe(v -> {}, e -> {});
            Single.just(42).flatMap(v -> Single.just(v * 2)).subscribe(v -> {}, e -> {});
        }
        
        // Reactive Java
        long start = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            com.reactive.core.Single.just(42).flatMap(v -> com.reactive.core.Single.just(v * 2))
                .subscribe(v -> {}, e -> {});
        }
        long reactiveTime = System.nanoTime() - start;
        
        // RxJava
        start = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            Single.just(42).flatMap(v -> Single.just(v * 2)).subscribe(v -> {}, e -> {});
        }
        long rxTime = System.nanoTime() - start;
        
        printResults("Single.flatMap", reactiveTime, rxTime);
    }
    
    // Maybe Benchmarks
    
    private static void benchmarkMaybeJust() {
        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            com.reactive.core.Maybe.just(42).subscribe(v -> {}, e -> {}, () -> {});
            Maybe.just(42).subscribe(v -> {}, e -> {}, () -> {});
        }
        
        // Reactive Java
        long start = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            com.reactive.core.Maybe.just(42).subscribe(v -> {}, e -> {}, () -> {});
        }
        long reactiveTime = System.nanoTime() - start;
        
        // RxJava
        start = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            Maybe.just(42).subscribe(v -> {}, e -> {}, () -> {});
        }
        long rxTime = System.nanoTime() - start;
        
        printResults("Maybe.just", reactiveTime, rxTime);
    }
    
    private static void benchmarkMaybeEmpty() {
        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            com.reactive.core.Maybe.empty().subscribe(v -> {}, e -> {}, () -> {});
            Maybe.empty().subscribe(v -> {}, e -> {}, () -> {});
        }
        
        // Reactive Java
        long start = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            com.reactive.core.Maybe.empty().subscribe(v -> {}, e -> {}, () -> {});
        }
        long reactiveTime = System.nanoTime() - start;
        
        // RxJava
        start = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            Maybe.empty().subscribe(v -> {}, e -> {}, () -> {});
        }
        long rxTime = System.nanoTime() - start;
        
        printResults("Maybe.empty", reactiveTime, rxTime);
    }
    
    private static void benchmarkMaybeMap() {
        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            com.reactive.core.Maybe.just(42).map(v -> v * 2).subscribe(v -> {}, e -> {}, () -> {});
            Maybe.just(42).map(v -> v * 2).subscribe(v -> {}, e -> {}, () -> {});
        }
        
        // Reactive Java
        long start = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            com.reactive.core.Maybe.just(42).map(v -> v * 2).subscribe(v -> {}, e -> {}, () -> {});
        }
        long reactiveTime = System.nanoTime() - start;
        
        // RxJava
        start = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            Maybe.just(42).map(v -> v * 2).subscribe(v -> {}, e -> {}, () -> {});
        }
        long rxTime = System.nanoTime() - start;
        
        printResults("Maybe.map", reactiveTime, rxTime);
    }
    
    // Completable Benchmarks
    
    private static void benchmarkCompletableComplete() {
        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            com.reactive.core.Completable.complete().subscribe(() -> {}, e -> {});
            Completable.complete().subscribe(() -> {}, e -> {});
        }
        
        // Reactive Java
        long start = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            com.reactive.core.Completable.complete().subscribe(() -> {}, e -> {});
        }
        long reactiveTime = System.nanoTime() - start;
        
        // RxJava
        start = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            Completable.complete().subscribe(() -> {}, e -> {});
        }
        long rxTime = System.nanoTime() - start;
        
        printResults("Completable.complete", reactiveTime, rxTime);
    }
    
    private static void benchmarkCompletableAndThen() {
        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            com.reactive.core.Completable.complete()
                .andThen(com.reactive.core.Completable.complete())
                .subscribe(() -> {}, e -> {});
            Completable.complete()
                .andThen(Completable.complete())
                .subscribe(() -> {}, e -> {});
        }
        
        // Reactive Java
        long start = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            com.reactive.core.Completable.complete()
                .andThen(com.reactive.core.Completable.complete())
                .subscribe(() -> {}, e -> {});
        }
        long reactiveTime = System.nanoTime() - start;
        
        // RxJava
        start = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            Completable.complete()
                .andThen(Completable.complete())
                .subscribe(() -> {}, e -> {});
        }
        long rxTime = System.nanoTime() - start;
        
        printResults("Completable.andThen", reactiveTime, rxTime);
    }
    
    private static void printResults(String benchmarkName, long reactiveTime, long rxTime) {
        double reactiveOpsPerSec = (BENCHMARK_ITERATIONS * 1_000_000_000.0) / reactiveTime;
        double rxOpsPerSec = (BENCHMARK_ITERATIONS * 1_000_000_000.0) / rxTime;
        double speedup = reactiveOpsPerSec / rxOpsPerSec;
        
        String comparison;
        if (speedup > 1.1) {
            comparison = String.format("%.2fx MÁS RÁPIDO", speedup);
        } else if (speedup < 0.9) {
            comparison = String.format("%.2fx más lento", 1.0 / speedup);
        } else {
            comparison = "SIMILAR";
        }
        
        System.out.printf("%-25s | Reactive: %10.2f M ops/s | RxJava: %10.2f M ops/s | %s%n",
            benchmarkName,
            reactiveOpsPerSec / 1_000_000,
            rxOpsPerSec / 1_000_000,
            comparison);
    }
}
