package com.reactive.examples;

import com.reactive.core.*;
import com.reactive.schedulers.Schedulers;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Ejemplos avanzados mostrando casos de uso realistas
 */
public class AdvancedExamples {
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== JReactive - Ejemplos Avanzados ===\n");
        
        example1_SimulateApiCalls();
        example2_DataPipeline();
        example3_BackpressureHandling();
        example4_ChainingOperators();
        
        // Esperar para ver los resultados asíncronos
        Thread.sleep(5000);
        System.out.println("\n=== Ejemplos avanzados completados ===");
    }
    
    /**
     * Ejemplo 1: Simulación de llamadas a API
     */
    static void example1_SimulateApiCalls() {
        System.out.println("--- Ejemplo 1: Llamadas a API Simuladas ---");
        
        Observable<Integer> userIds = Observable.just(1, 2, 3, 4, 5);
        
        userIds
            .flatMap(userId -> fetchUserData(userId))
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.computation())
            .map(user -> user.toUpperCase())
            .subscribe(
                user -> System.out.println("Usuario procesado: " + user),
                error -> System.err.println("Error: " + error),
                () -> System.out.println("Todas las llamadas completadas!\n")
            );
    }
    
    // Simula una llamada HTTP asíncrona
    static Observable<String> fetchUserData(int userId) {
        return Observable.create(emitter -> {
            // Simular latencia de red
            Thread.sleep(100 * userId);
            emitter.onNext("User_" + userId);
            emitter.onComplete();
        });
    }
    
    /**
     * Ejemplo 2: Pipeline de procesamiento de datos
     */
    static void example2_DataPipeline() {
        System.out.println("--- Ejemplo 2: Pipeline de Datos ---");
        
        Observable.just(
            "apple,5,2.5",
            "banana,10,1.2",
            "orange,7,3.0",
            "invalid-data",  // Dato inválido
            "grape,15,2.8"
        )
        .map(line -> line.split(","))
        .filter(parts -> parts.length == 3)  // Filtrar datos inválidos
        .map(parts -> new Product(parts[0], Integer.parseInt(parts[1]), Double.parseDouble(parts[2])))
        .filter(product -> product.quantity > 5)  // Solo productos con stock
        .map(product -> {
            double total = product.quantity * product.price;
            return String.format("%s: %d unidades x $%.2f = $%.2f", 
                product.name, product.quantity, product.price, total);
        })
        .doOnNext(result -> System.out.println("Procesado: " + result))
        .subscribe(
            result -> {},  // Ya imprimimos en doOnNext
            error -> System.err.println("Error en pipeline: " + error),
            () -> System.out.println("Pipeline completado!\n")
        );
    }
    
    static class Product {
        String name;
        int quantity;
        double price;
        
        Product(String name, int quantity, double price) {
            this.name = name;
            this.quantity = quantity;
            this.price = price;
        }
    }
    
    /**
     * Ejemplo 3: Manejo de backpressure (simulado con take y skip)
     */
    static void example3_BackpressureHandling() {
        System.out.println("--- Ejemplo 3: Control de Flujo ---");
        
        // Generar muchos datos
        Observable.range(1, 100)
            .skip(10)        // Omitir los primeros 10
            .take(20)        // Tomar solo 20
            .filter(n -> n % 3 == 0)  // Solo múltiplos de 3
            .doOnNext(n -> System.out.println("Procesando: " + n))
            .subscribe(
                n -> {},  // Ya imprimimos en doOnNext
                error -> System.err.println("Error: " + error),
                () -> System.out.println("Control de flujo completado!\n")
            );
    }
    
    /**
     * Ejemplo 4: Encadenamiento complejo de operadores
     */
    static void example4_ChainingOperators() {
        System.out.println("--- Ejemplo 4: Encadenamiento Complejo ---");
        
        Observable.just("reactive", "programming", "is", "awesome")
            .map(String::toUpperCase)
            .filter(word -> word.length() > 2)
            .flatMap(word -> Observable.fromIterable(Arrays.asList(word.split(""))))
            .distinctUntilChanged()
            .take(15)
            .doOnNext(letter -> System.out.print(letter))
            .doOnComplete(() -> System.out.println())
            .onErrorReturn(error -> "ERROR")
            .subscribe(
                letter -> {},  // Ya imprimimos en doOnNext
                error -> System.err.println("Error: " + error),
                () -> System.out.println("Encadenamiento completado!\n")
            );
    }
}
