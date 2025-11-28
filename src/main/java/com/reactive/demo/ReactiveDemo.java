package com.reactive.demo;

import com.reactive.core.Observable;
import com.reactive.core.Observer;
import com.reactive.schedulers.Schedulers;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Programa de demostración de la biblioteca JReactive
 * 
 * Demuestra las principales capacidades implementadas:
 * - Creación de Observables
 * - Operadores básicos (map, filter, take, skip, etc.)
 * - Operadores de transformación (flatMap, concatMap, switchMap)
 * - Schedulers (IO, Computation)
 * - Manejo de errores
 * - Operadores de combinación (merge, zip)
 */
public class ReactiveDemo {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=".repeat(80));
        System.out.println("BIBLIOTECA REACTIVE JAVA - DEMOSTRACIÓN DE FUNCIONALIDADES");
        System.out.println("=".repeat(80));
        System.out.println();

        // 1. Creación de Observables
        demonstrateCreation();

        // 2. Operadores básicos
        demonstrateBasicOperators();

        // 3. Operadores de transformación
        demonstrateTransformationOperators();

        // 4. Schedulers
        demonstrateSchedulers();

        // 5. Manejo de errores
        demonstrateErrorHandling();

        // 6. Operadores de combinación
        demonstrateCombinationOperators();

        // 7. Operadores avanzados
        demonstrateAdvancedOperators();

        System.out.println("\n" + "=".repeat(80));
        System.out.println("DEMOSTRACIÓN COMPLETADA");
        System.out.println("=".repeat(80));
        
        // Esperar un momento para que los schedulers terminen
        Thread.sleep(1000);
    }

    private static void demonstrateCreation() {
        System.out.println("\n" + "-".repeat(80));
        System.out.println("1. CREACIÓN DE OBSERVABLES");
        System.out.println("-".repeat(80));

        // just() - Emite elementos
        System.out.println("\n▶ Observable.just():");
        Observable.just("Hola", "Reactive", "Java")
                .subscribe(value -> System.out.println("  Recibido: " + value));

        // fromArray() - Crea desde un array
        System.out.println("\n▶ Observable.fromArray():");
        Observable.fromArray(new String[]{"Manzana", "Banana", "Cereza"})
                .subscribe(value -> System.out.println("  Fruta: " + value));

        // fromIterable() - Crea desde una colección
        System.out.println("\n▶ Observable.fromIterable():");
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);
        Observable.fromIterable(numbers)
                .subscribe(value -> System.out.println("  Número: " + value));

        // range() - Genera una secuencia de enteros
        System.out.println("\n▶ Observable.range():");
        Observable.range(10, 5)
                .subscribe(value -> System.out.println("  Valor: " + value));

        // create() - Creación personalizada
        System.out.println("\n▶ Observable.create():");
        Observable.create(emitter -> {
            emitter.onNext("Elemento 1");
            emitter.onNext("Elemento 2");
            emitter.onNext("Elemento 3");
            emitter.onComplete();
        }).subscribe(value -> System.out.println("  " + value));

        // empty() - Observable vacío
        System.out.println("\n▶ Observable.empty():");
        Observable.empty()
                .subscribe(
                        value -> System.out.println("  onNext: " + value),
                        error -> System.out.println("  onError: " + error),
                        () -> System.out.println("  onComplete: Secuencia vacía completada")
                );
    }

    private static void demonstrateBasicOperators() {
        System.out.println("\n" + "-".repeat(80));
        System.out.println("2. OPERADORES BÁSICOS");
        System.out.println("-".repeat(80));

        // map() - Transforma cada elemento
        System.out.println("\n▶ map() - Duplicar números:");
        Observable.range(1, 5)
                .map(x -> x * 2)
                .subscribe(value -> System.out.println("  " + value));

        // filter() - Filtra elementos
        System.out.println("\n▶ filter() - Solo números pares:");
        Observable.range(1, 10)
                .filter(x -> x % 2 == 0)
                .subscribe(value -> System.out.println("  " + value));

        // take() - Toma los primeros N elementos
        System.out.println("\n▶ take() - Primeros 3 elementos:");
        Observable.range(1, 10)
                .take(3)
                .subscribe(value -> System.out.println("  " + value));

        // skip() - Salta los primeros N elementos
        System.out.println("\n▶ skip() - Saltar primeros 5:");
        Observable.range(1, 8)
                .skip(5)
                .subscribe(value -> System.out.println("  " + value));

        // distinct() - Elimina duplicados consecutivos
        System.out.println("\n▶ distinctUntilChanged():");
        Observable.just(1, 1, 2, 2, 2, 3, 3, 4, 3)
                .distinctUntilChanged()
                .subscribe(value -> System.out.println("  " + value));
    }

    private static void demonstrateTransformationOperators() {
        System.out.println("\n" + "-".repeat(80));
        System.out.println("3. OPERADORES DE TRANSFORMACIÓN");
        System.out.println("-".repeat(80));

        // flatMap() - Aplana múltiples Observables
        System.out.println("\n▶ flatMap():");
        Observable.range(1, 3)
                .flatMap(x -> Observable.just(x, x * 10))
                .subscribe(value -> System.out.println("  " + value));

        // concatMap() - Aplana en orden secuencial
        System.out.println("\n▶ concatMap():");
        Observable.range(1, 3)
                .concatMap(x -> Observable.just(x, x * 10))
                .subscribe(value -> System.out.println("  " + value));

        // switchMap() - Cambia al Observable más reciente
        System.out.println("\n▶ switchMap():");
        Observable.just(1, 2, 3)
                .switchMap(x -> Observable.just(x, x * 100))
                .subscribe(value -> System.out.println("  " + value));
    }

    private static void demonstrateSchedulers() {
        System.out.println("\n" + "-".repeat(80));
        System.out.println("4. SCHEDULERS (Programación asíncrona)");
        System.out.println("-".repeat(80));

        // subscribeOn() - Ejecuta la suscripción en un Scheduler específico
        System.out.println("\n▶ subscribeOn() - Ejecución en IO Scheduler:");
        Observable.just("Tarea en IO")
                .subscribeOn(Schedulers.io())
                .subscribe(value -> System.out.println("  " + value + " [Thread: " + Thread.currentThread().getName() + "]"));

        // observeOn() - Observa en un Scheduler específico
        System.out.println("\n▶ observeOn() - Observación en Computation Scheduler:");
        Observable.just("Procesando...")
                .observeOn(Schedulers.computation())
                .map(s -> s.toUpperCase())
                .subscribe(value -> System.out.println("  " + value + " [Thread: " + Thread.currentThread().getName() + "]"));

        // Esperar un poco para que los schedulers terminen
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void demonstrateErrorHandling() {
        System.out.println("\n" + "-".repeat(80));
        System.out.println("5. MANEJO DE ERRORES");
        System.out.println("-".repeat(80));

        // onErrorReturn() - Retorna un valor por defecto en caso de error
        System.out.println("\n▶ onErrorReturn():");
        Observable.create(emitter -> {
            emitter.onNext(1);
            emitter.onNext(2);
            emitter.onError(new RuntimeException("Error simulado"));
        })
        .onErrorReturn(error -> -1)
        .subscribe(
                value -> System.out.println("  Valor: " + value),
                error -> System.out.println("  Error: " + error.getMessage())
        );

        // onErrorResumeNext() - Continúa con otro Observable en caso de error
        System.out.println("\n▶ onErrorResumeNext():");
        Observable.create(emitter -> {
            emitter.onNext("A");
            emitter.onNext("B");
            emitter.onError(new RuntimeException("Error!"));
        })
        .onErrorResumeNext(error -> Observable.just("X", "Y", "Z"))
        .subscribe(value -> System.out.println("  " + value));

        // retry() - Reintenta en caso de error
        System.out.println("\n▶ retry():");
        final int[] attempts = {0};
        Observable.create(emitter -> {
            attempts[0]++;
            System.out.println("  Intento #" + attempts[0]);
            if (attempts[0] < 3) {
                emitter.onError(new RuntimeException("Fallo"));
            } else {
                emitter.onNext("Éxito!");
                emitter.onComplete();
            }
        })
        .retry(3)
        .subscribe(
                value -> System.out.println("  Resultado: " + value),
                error -> System.out.println("  Error final: " + error.getMessage())
        );

        // doOnError() - Ejecuta una acción cuando ocurre un error
        System.out.println("\n▶ doOnError():");
        Observable.error(new RuntimeException("Error de prueba"))
                .doOnError(error -> System.out.println("  ¡Error detectado!: " + error.getMessage()))
                .subscribe(
                        value -> System.out.println("  Valor: " + value),
                        error -> {} // Manejador silencioso
                );
    }

    private static void demonstrateCombinationOperators() {
        System.out.println("\n" + "-".repeat(80));
        System.out.println("6. OPERADORES DE COMBINACIÓN");
        System.out.println("-".repeat(80));

        // merge() - Combina múltiples Observables
        System.out.println("\n▶ merge():");
        Observable<Integer> obs1 = Observable.just(1, 2, 3);
        Observable<Integer> obs2 = Observable.just(4, 5, 6);
        Observable.merge(obs1, obs2)
                .subscribe(value -> System.out.println("  " + value));

        // zip() - Combina elementos por posición
        System.out.println("\n▶ zip():");
        Observable<String> letters = Observable.just("A", "B", "C");
        Observable<Integer> nums = Observable.just(1, 2, 3);
        Observable.zip(letters, nums, (letter, num) -> letter + num)
                .subscribe(value -> System.out.println("  " + value));

        // concat() - Concatena Observables secuencialmente usando concatMap
        System.out.println("\n▶ concat (usando concatMap):");
        Observable<String> first = Observable.just("Primero", "Segundo");
        Observable<String> second = Observable.just("Tercero", "Cuarto");
        first.concatMap(s -> Observable.just(s))
                .subscribe(value -> System.out.println("  " + value));
    }

    private static void demonstrateAdvancedOperators() {
        System.out.println("\n" + "-".repeat(80));
        System.out.println("7. OPERADORES AVANZADOS");
        System.out.println("-".repeat(80));

        // scan() - Acumulador
        System.out.println("\n▶ scan() - Suma acumulativa:");
        Observable.range(1, 5)
                .scan(0, (acc, value) -> acc + value)
                .subscribe(value -> System.out.println("  Acumulado: " + value));

        // buffer() - Agrupa elementos
        System.out.println("\n▶ buffer() - Grupos de 3:");
        Observable.range(1, 10)
                .buffer(3)
                .subscribe(buffer -> System.out.println("  Buffer: " + buffer));

        // reduce() - Reduce a un único valor
        System.out.println("\n▶ reduce() - Suma total:");
        Observable.range(1, 5)
                .reduce((acc, value) -> acc + value)
                .subscribe(value -> System.out.println("  Total: " + value));
    }
}
