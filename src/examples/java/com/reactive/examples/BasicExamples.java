package com.reactive.examples;

import com.reactive.core.*;
import com.reactive.schedulers.Schedulers;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Ejemplos básicos de uso de la biblioteca JReactive
 */
public class BasicExamples {
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== JReactive - Ejemplos Básicos ===\n");
        
        example1_SimpleObservable();
        example2_OperatorsMapFilter();
        example3_ErrorHandling();
        example4_AsyncWithSchedulers();
        example5_CombiningObservables();
        example6_FlatMap();
        example7_RetryMechanism();
        
        // Esperar para ver los resultados asíncronos
        Thread.sleep(3000);
        System.out.println("\n=== Ejemplos completados ===");
    }
    
    /**
     * Ejemplo 1: Observable simple con just()
     */
    static void example1_SimpleObservable() {
        System.out.println("--- Ejemplo 1: Observable Simple ---");
        
        Observable.just("Hola", "Mundo", "Reactivo")
            .subscribe(
                item -> System.out.println("Recibido: " + item),
                error -> System.err.println("Error: " + error),
                () -> System.out.println("Completado!\n")
            );
    }
    
    /**
     * Ejemplo 2: Operadores map y filter
     */
    static void example2_OperatorsMapFilter() {
        System.out.println("--- Ejemplo 2: Map y Filter ---");
        
        Observable.range(1, 10)
            .filter(n -> n % 2 == 0)  // Solo números pares
            .map(n -> n * 2)           // Multiplicar por 2
            .subscribe(
                item -> System.out.println("Número procesado: " + item),
                Throwable::printStackTrace,
                () -> System.out.println("Stream completado!\n")
            );
    }
    
    /**
     * Ejemplo 3: Manejo de errores
     */
    static void example3_ErrorHandling() {
        System.out.println("--- Ejemplo 3: Manejo de Errores ---");
        
        Observable.create(emitter -> {
            emitter.onNext("Item 1");
            emitter.onNext("Item 2");
            throw new RuntimeException("Error simulado!");
        })
        .onErrorReturn(error -> "Valor por defecto: " + error.getMessage())
        .subscribe(
            item -> System.out.println("Recibido: " + item),
            error -> System.err.println("Error no manejado: " + error),
            () -> System.out.println("Completado con manejo de error!\n")
        );
    }
    
    /**
     * Ejemplo 4: Ejecución asíncrona con Schedulers
     */
    static void example4_AsyncWithSchedulers() {
        System.out.println("--- Ejemplo 4: Schedulers Asíncronos ---");
        
        Observable.just("Tarea 1", "Tarea 2", "Tarea 3")
            .subscribeOn(Schedulers.io())        // Suscripción en thread I/O
            .observeOn(Schedulers.computation()) // Observación en thread de cálculo
            .map(task -> {
                System.out.println(task + " procesada en: " + Thread.currentThread().getName());
                return task.toUpperCase();
            })
            .subscribe(
                item -> System.out.println("Resultado: " + item + " en " + Thread.currentThread().getName()),
                Throwable::printStackTrace,
                () -> System.out.println("Procesamiento asíncrono completado!\n")
            );
    }
    
    /**
     * Ejemplo 5: Combinación de Observables
     */
    static void example5_CombiningObservables() {
        System.out.println("--- Ejemplo 5: Combinar Observables ---");
        
        Observable<String> obs1 = Observable.just("A", "B", "C");
        Observable<String> obs2 = Observable.just("1", "2", "3");
        
        // Zip: combina elementos por pares
        Observable.zip(obs1, obs2, (letter, number) -> letter + number)
            .subscribe(
                item -> System.out.println("Combinado: " + item),
                Throwable::printStackTrace,
                () -> System.out.println("Combinación completada!\n")
            );
    }
    
    /**
     * Ejemplo 6: FlatMap para transformaciones complejas
     */
    static void example6_FlatMap() {
        System.out.println("--- Ejemplo 6: FlatMap ---");
        
        Observable.just("Reactive", "Java")
            .flatMap(word -> Observable.fromIterable(Arrays.asList(word.split(""))))
            .subscribe(
                letter -> System.out.print(letter + " "),
                Throwable::printStackTrace,
                () -> System.out.println("\nFlatMap completado!\n")
            );
    }
    
    /**
     * Ejemplo 7: Retry automático
     */
    static void example7_RetryMechanism() {
        System.out.println("--- Ejemplo 7: Retry Automático ---");
        
        final int[] attemptCount = {0};
        
        Observable.create(emitter -> {
            attemptCount[0]++;
            System.out.println("Intento #" + attemptCount[0]);
            
            if (attemptCount[0] < 3) {
                throw new RuntimeException("Fallo temporal");
            }
            
            emitter.onNext("¡Éxito después de " + attemptCount[0] + " intentos!");
            emitter.onComplete();
        })
        .retry(5)  // Reintentar hasta 5 veces
        .subscribe(
            item -> System.out.println("Resultado: " + item),
            error -> System.err.println("Error final: " + error),
            () -> System.out.println("Retry completado!\n")
        );
    }
}
