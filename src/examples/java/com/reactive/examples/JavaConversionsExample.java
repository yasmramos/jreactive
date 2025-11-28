package com.reactive.examples;

import com.reactive.core.Observable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Ejemplos de uso de las conversiones Java estándar (Fase 2).
 * Demuestra cómo convertir Observable a tipos estándar de Java.
 */
public class JavaConversionsExample {
    
    public static void main(String[] args) throws Exception {
        System.out.println("=== Ejemplos de Conversiones Java Estándar ===\n");
        
        // Ejemplo 1: toFuture()
        futureExample();
        
        // Ejemplo 2: toCompletableFuture()
        completableFutureExample();
        
        // Ejemplo 3: toStream()
        streamExample();
        
        // Ejemplo 4: blockingIterable()
        blockingIterableExample();
        
        // Ejemplo 5: blockingFirst() y blockingLast()
        blockingFirstLastExample();
        
        // Ejemplo 6: Caso de uso real - Procesamiento de datos
        realWorldExample();
    }
    
    static void futureExample() throws Exception {
        System.out.println("--- Ejemplo 1: toFuture() ---");
        
        // Convertir Observable a Future
        Future<Integer> future = Observable.just(1, 2, 3, 4, 5).toFuture();
        
        System.out.println("Future creado, esperando resultado...");
        Integer result = future.get();
        System.out.println("Resultado (primer elemento): " + result);
        System.out.println("Future completado: " + future.isDone());
        System.out.println();
    }
    
    static void completableFutureExample() throws Exception {
        System.out.println("--- Ejemplo 2: toCompletableFuture() ---");
        
        // Composición de CompletableFutures
        CompletableFuture<Integer> future1 = Observable.just(10).toCompletableFuture();
        CompletableFuture<Integer> future2 = Observable.just(20).toCompletableFuture();
        
        // Combinar resultados
        CompletableFuture<Integer> combined = future1.thenCombine(future2, (a, b) -> {
            System.out.println("Combinando: " + a + " + " + b);
            return a + b;
        });
        
        Integer sum = combined.get();
        System.out.println("Suma combinada: " + sum);
        
        // Transformación con thenApply
        CompletableFuture<String> transformed = Observable.just(42)
            .toCompletableFuture()
            .thenApply(x -> "El resultado es: " + x);
        
        System.out.println(transformed.get());
        System.out.println();
    }
    
    static void streamExample() {
        System.out.println("--- Ejemplo 3: toStream() ---");
        
        // Convertir Observable a Stream y procesar
        Stream<Integer> stream = Observable.range(1, 20).toStream();
        
        // Operaciones de Stream
        List<String> result = stream
            .filter(x -> x % 2 == 0)          // Números pares
            .map(x -> x * 2)                   // Duplicar
            .limit(5)                          // Primeros 5
            .map(x -> "Número: " + x)         // Formatear
            .collect(Collectors.toList());
        
        System.out.println("Resultados del Stream:");
        result.forEach(System.out::println);
        
        // Reducción
        int sum = Observable.just(1, 2, 3, 4, 5)
            .toStream()
            .reduce(0, Integer::sum);
        System.out.println("\nSuma total: " + sum);
        System.out.println();
    }
    
    static void blockingIterableExample() {
        System.out.println("--- Ejemplo 4: blockingIterable() ---");
        
        // Crear Iterable bloqueante
        Iterable<Integer> iterable = Observable.range(1, 5).blockingIterable();
        
        System.out.println("Iterando con for-each:");
        for (Integer value : iterable) {
            System.out.println("  Valor: " + value);
        }
        
        // Procesar con procesamiento condicional
        System.out.println("\nProcesamiento condicional:");
        Observable<String> names = Observable.just("Alice", "Bob", "Charlie", "David");
        
        for (String name : names.blockingIterable()) {
            if (name.startsWith("C")) {
                System.out.println("  Encontrado: " + name);
                break; // Podemos salir cuando queramos
            }
        }
        System.out.println();
    }
    
    static void blockingFirstLastExample() {
        System.out.println("--- Ejemplo 5: blockingFirst() y blockingLast() ---");
        
        // blockingFirst()
        Integer first = Observable.range(10, 5).blockingFirst();
        System.out.println("Primer elemento: " + first);
        
        // blockingLast()
        Integer last = Observable.range(10, 5).blockingLast();
        System.out.println("Último elemento: " + last);
        
        // blockingFirst con valor por defecto
        Integer firstOrDefault = Observable.<Integer>empty().blockingFirst(999);
        System.out.println("Primero con default (vacío): " + firstOrDefault);
        
        // blockingLast con valor por defecto
        Integer lastOrDefault = Observable.<Integer>empty().blockingLast(-1);
        System.out.println("Último con default (vacío): " + lastOrDefault);
        System.out.println();
    }
    
    static void realWorldExample() throws Exception {
        System.out.println("--- Ejemplo 6: Caso de Uso Real ---");
        System.out.println("Procesamiento de transacciones bancarias\n");
        
        // Simular transacciones
        Observable<Transaction> transactions = Observable.just(
            new Transaction("TX001", 100.0),
            new Transaction("TX002", -50.0),   // Retiro
            new Transaction("TX003", 200.0),
            new Transaction("TX004", -30.0),
            new Transaction("TX005", 150.0)
        );
        
        // Análisis 1: Suma total usando Stream
        double total = transactions
            .toStream()
            .mapToDouble(Transaction::getAmount)
            .sum();
        System.out.println("Balance total: $" + total);
        
        // Análisis 2: Transacciones positivas (depósitos)
        List<Transaction> deposits = Observable.just(
                new Transaction("TX001", 100.0),
                new Transaction("TX002", -50.0),
                new Transaction("TX003", 200.0),
                new Transaction("TX004", -30.0),
                new Transaction("TX005", 150.0)
            )
            .toStream()
            .filter(tx -> tx.getAmount() > 0)
            .collect(Collectors.toList());
        
        System.out.println("\nDepósitos (" + deposits.size() + " transacciones):");
        deposits.forEach(tx -> System.out.println("  " + tx.getId() + ": $" + tx.getAmount()));
        
        // Análisis 3: Primera y última transacción
        Transaction firstTx = Observable.just(
                new Transaction("TX001", 100.0),
                new Transaction("TX002", -50.0),
                new Transaction("TX003", 200.0),
                new Transaction("TX004", -30.0),
                new Transaction("TX005", 150.0)
            ).blockingFirst();
        
        Transaction lastTx = Observable.just(
                new Transaction("TX001", 100.0),
                new Transaction("TX002", -50.0),
                new Transaction("TX003", 200.0),
                new Transaction("TX004", -30.0),
                new Transaction("TX005", 150.0)
            ).blockingLast();
        
        System.out.println("\nPrimera transacción: " + firstTx.getId() + " ($" + firstTx.getAmount() + ")");
        System.out.println("Última transacción: " + lastTx.getId() + " ($" + lastTx.getAmount() + ")");
        
        // Análisis 4: Procesamiento asíncrono con CompletableFuture
        Observable<Transaction> txObservable = Observable.just(
            new Transaction("TX001", 100.0),
            new Transaction("TX002", -50.0),
            new Transaction("TX003", 200.0),
            new Transaction("TX004", -30.0),
            new Transaction("TX005", 150.0)
        );
        
        List<Transaction> txList = txObservable.toStream().collect(Collectors.toList());
        double sum = txList.stream().mapToDouble(Transaction::getAmount).sum();
        long count = txList.size();
        double avg = sum / count;
        
        CompletableFuture<String> reportFuture = Observable
            .just("Reporte: " + count + " transacciones, promedio: $" + String.format("%.2f", avg))
            .toCompletableFuture();
        
        System.out.println("\n" + reportFuture.get());
        
        System.out.println("\n=== Fin de los Ejemplos ===");
    }
    
    // Clase auxiliar para el ejemplo
    static class Transaction {
        private final String id;
        private final double amount;
        
        public Transaction(String id, double amount) {
            this.id = id;
            this.amount = amount;
        }
        
        public String getId() {
            return id;
        }
        
        public double getAmount() {
            return amount;
        }
        
        @Override
        public String toString() {
            return "Transaction{id='" + id + "', amount=" + amount + "}";
        }
    }
}
