package com.reactive.demo;

import com.reactive.core.Single;
import com.reactive.core.Maybe;
import com.reactive.core.Completable;

/**
 * Demostración de Single, Maybe y Completable
 */
public class SpecializedTypesDemo {
    
    public static void main(String[] args) {
        System.out.println("==================== SINGLE DEMO ====================");
        singleDemo();
        
        System.out.println("\n==================== MAYBE DEMO ====================");
        maybeDemo();
        
        System.out.println("\n==================== COMPLETABLE DEMO ====================");
        completableDemo();
        
        System.out.println("\n==================== CONVERSION DEMO ====================");
        conversionDemo();
    }
    
    private static void singleDemo() {
        System.out.println("\n1. Single.just():");
        Single.just(42)
            .subscribe(
                value -> System.out.println("  Valor recibido: " + value),
                error -> System.err.println("  Error: " + error)
            );
        
        System.out.println("\n2. Single.map():");
        Single.just(10)
            .map(x -> x * 2)
            .subscribe(
                value -> System.out.println("  10 * 2 = " + value),
                error -> System.err.println("  Error: " + error)
            );
        
        System.out.println("\n3. Single.flatMap():");
        Single.just(5)
            .flatMap(x -> Single.just(x + 10))
            .subscribe(
                value -> System.out.println("  5 + 10 = " + value),
                error -> System.err.println("  Error: " + error)
            );
        
        System.out.println("\n4. Single.zip():");
        Single.zip(
            Single.just(10),
            Single.just(20),
            (a, b) -> a + b
        ).subscribe(
            value -> System.out.println("  10 + 20 = " + value),
            error -> System.err.println("  Error: " + error)
        );
        
        System.out.println("\n5. Single.onErrorReturn():");
        Single.<Integer>error(new RuntimeException("Error intencional"))
            .onErrorReturn(e -> -1)
            .subscribe(
                value -> System.out.println("  Valor de fallback: " + value),
                error -> System.err.println("  Error: " + error)
            );
        
        System.out.println("\n6. Single.fromCallable():");
        Single.fromCallable(() -> {
            System.out.println("  Ejecutando callable...");
            return "Resultado del callable";
        }).subscribe(
            value -> System.out.println("  Resultado: " + value),
            error -> System.err.println("  Error: " + error)
        );
    }
    
    private static void maybeDemo() {
        System.out.println("\n1. Maybe.just():");
        Maybe.just(42)
            .subscribe(
                value -> System.out.println("  Valor recibido: " + value),
                error -> System.err.println("  Error: " + error),
                () -> System.out.println("  Completado vacío")
            );
        
        System.out.println("\n2. Maybe.empty():");
        Maybe.empty()
            .subscribe(
                value -> System.out.println("  Valor recibido: " + value),
                error -> System.err.println("  Error: " + error),
                () -> System.out.println("  Completado vacío")
            );
        
        System.out.println("\n3. Maybe.map():");
        Maybe.just(10)
            .map(x -> x * 3)
            .subscribe(
                value -> System.out.println("  10 * 3 = " + value),
                error -> System.err.println("  Error: " + error),
                () -> System.out.println("  Completado vacío")
            );
        
        System.out.println("\n4. Maybe.filter() - pasa el filtro:");
        Maybe.just(10)
            .filter(x -> x > 5)
            .subscribe(
                value -> System.out.println("  Valor que pasó el filtro: " + value),
                error -> System.err.println("  Error: " + error),
                () -> System.out.println("  Completado vacío (no pasó el filtro)")
            );
        
        System.out.println("\n5. Maybe.filter() - no pasa el filtro:");
        Maybe.just(3)
            .filter(x -> x > 5)
            .subscribe(
                value -> System.out.println("  Valor que pasó el filtro: " + value),
                error -> System.err.println("  Error: " + error),
                () -> System.out.println("  Completado vacío (no pasó el filtro)")
            );
        
        System.out.println("\n6. Maybe.defaultIfEmpty():");
        Maybe.<Integer>empty()
            .defaultIfEmpty(100)
            .subscribe(
                value -> System.out.println("  Valor por defecto: " + value),
                error -> System.err.println("  Error: " + error)
            );
        
        System.out.println("\n7. Maybe.switchIfEmpty():");
        Maybe.<Integer>empty()
            .switchIfEmpty(Maybe.just(200))
            .subscribe(
                value -> System.out.println("  Valor alternativo: " + value),
                error -> System.err.println("  Error: " + error),
                () -> System.out.println("  Completado vacío")
            );
    }
    
    private static void completableDemo() {
        System.out.println("\n1. Completable.complete():");
        Completable.complete()
            .subscribe(
                () -> System.out.println("  ¡Completado exitosamente!"),
                error -> System.err.println("  Error: " + error)
            );
        
        System.out.println("\n2. Completable.fromAction():");
        Completable.fromAction(() -> System.out.println("  Ejecutando acción..."))
            .subscribe(
                () -> System.out.println("  ¡Acción completada!"),
                error -> System.err.println("  Error: " + error)
            );
        
        System.out.println("\n3. Completable.andThen():");
        Completable.fromAction(() -> System.out.println("  Primera acción"))
            .andThen(Completable.fromAction(() -> System.out.println("  Segunda acción")))
            .subscribe(
                () -> System.out.println("  ¡Ambas acciones completadas!"),
                error -> System.err.println("  Error: " + error)
            );
        
        System.out.println("\n4. Completable.merge():");
        Completable.merge(
            Completable.fromAction(() -> System.out.println("  Acción 1")),
            Completable.fromAction(() -> System.out.println("  Acción 2")),
            Completable.fromAction(() -> System.out.println("  Acción 3"))
        ).subscribe(
            () -> System.out.println("  ¡Todas las acciones completadas!"),
            error -> System.err.println("  Error: " + error)
        );
        
        // Dar tiempo para que las acciones concurrentes se ejecuten
        try { Thread.sleep(100); } catch (InterruptedException e) {}
        
        System.out.println("\n5. Completable.concat():");
        Completable.concat(
            Completable.fromAction(() -> System.out.println("  Paso 1")),
            Completable.fromAction(() -> System.out.println("  Paso 2")),
            Completable.fromAction(() -> System.out.println("  Paso 3"))
        ).subscribe(
            () -> System.out.println("  ¡Pipeline completado!"),
            error -> System.err.println("  Error: " + error)
        );
        
        System.out.println("\n6. Completable.onErrorComplete():");
        Completable.error(new RuntimeException("Error intencional"))
            .onErrorComplete()
            .subscribe(
                () -> System.out.println("  Completado (error convertido a éxito)"),
                error -> System.err.println("  Error: " + error)
            );
    }
    
    private static void conversionDemo() {
        System.out.println("\n1. Single -> Observable:");
        Single.just(42)
            .toObservable()
            .subscribe(
                value -> System.out.println("  Observable recibió: " + value),
                error -> System.err.println("  Error: " + error),
                () -> System.out.println("  Observable completado")
            );
        
        System.out.println("\n2. Maybe -> Observable:");
        Maybe.just(100)
            .toObservable()
            .subscribe(
                value -> System.out.println("  Observable recibió: " + value),
                error -> System.err.println("  Error: " + error),
                () -> System.out.println("  Observable completado")
            );
        
        System.out.println("\n3. Maybe (vacío) -> Observable:");
        Maybe.empty()
            .toObservable()
            .subscribe(
                value -> System.out.println("  Observable recibió: " + value),
                error -> System.err.println("  Error: " + error),
                () -> System.out.println("  Observable completado sin valores")
            );
        
        System.out.println("\n4. Maybe -> Single:");
        Maybe.just(200)
            .toSingle()
            .subscribe(
                value -> System.out.println("  Single recibió: " + value),
                error -> System.err.println("  Error: " + error)
            );
        
        System.out.println("\n5. Completable -> Observable:");
        Completable.complete()
            .<Integer>toObservable()
            .subscribe(
                value -> System.out.println("  Observable recibió: " + value),
                error -> System.err.println("  Error: " + error),
                () -> System.out.println("  Observable completado sin valores")
            );
        
        System.out.println("\n6. Completable.andThen(Single):");
        Completable.fromAction(() -> System.out.println("  Preparando datos..."))
            .andThen(Single.just("Datos listos"))
            .subscribe(
                value -> System.out.println("  Single recibió: " + value),
                error -> System.err.println("  Error: " + error)
            );
    }
}
