# Benchmarks Comparativos: JReactive vs RxJava 3

Este directorio contiene una suite completa de benchmarks usando JMH (Java Microbenchmark Harness) para comparar el rendimiento de nuestra implementación de programación reactiva contra RxJava 3.

## Estructura de Benchmarks

Los benchmarks están organizados en las siguientes categorías:

### 1. BasicOperatorsBenchmark
Compara el rendimiento de operadores básicos:
- `map` - Transformación de valores
- `filter` - Filtrado de elementos
- `flatMap` - Transformación con aplanamiento
- `take` / `skip` - Limitación y salto de elementos
- Combinaciones de operadores

**Parámetros:** size = {10, 100, 1000, 10000}

### 2. CreationBenchmark
Compara la creación de diferentes tipos reactivos:
- **Observable:** `just`, `range`, `fromIterable`, `create`
- **Single:** `just`, `fromCallable`
- **Maybe:** `just`, `empty`
- **Completable:** `complete`, `fromRunnable`

**Parámetros:** size = {10, 100, 1000}

### 3. SubjectsBenchmark
Compara el rendimiento de Subjects (Hot Observables):
- `PublishSubject` - Multicasting sin replay
- `BehaviorSubject` - Mantiene último valor
- `ReplaySubject` - Replay completo o limitado
- `AsyncSubject` - Solo último valor al completar

**Parámetros:** 
- size = {10, 100, 1000}
- observerCount = {1, 5, 10}

### 4. AdvancedOperatorsBenchmark
Compara operadores avanzados:
- `scan` - Acumulación de valores
- `buffer` - Agrupación en listas
- `window` - Agrupación en Observables
- `groupBy` - Agrupación por clave
- `distinctUntilChanged` - Eliminación de duplicados consecutivos

**Parámetros:** size = {10, 100, 1000}

### 5. CombinationOperatorsBenchmark
Compara operadores de combinación:
- `merge` - Fusión de múltiples fuentes
- `concat` - Concatenación secuencial
- `zip` - Combinación sincronizada
- `concatMap` - FlatMap con orden garantizado
- `switchMap` - FlatMap con cancelación

**Parámetros:** size = {10, 100, 1000}

### 6. ErrorHandlingBenchmark
Compara el manejo de errores:
- `onErrorReturn` - Valor por defecto en error
- `onErrorResumeNext` - Observable alternativo en error
- `retry` - Reintentos automáticos
- `doOnError` - Side-effects en errores

**Parámetros:** size = {10, 100, 1000}

## Cómo Ejecutar los Benchmarks

### Prerequisitos
- Java 17 o superior
- Maven 3.8+

### Compilar el JAR de Benchmarks

```bash
cd /workspace/jreactive
mvn clean package -Pbenchmarks
```

Esto generará un JAR ejecutable: `target/benchmarks.jar`

### Ejecutar Todos los Benchmarks

```bash
java -jar target/benchmarks.jar
```

⚠️ **Advertencia:** Ejecutar todos los benchmarks puede tomar **varias horas** debido a las múltiples iteraciones de warmup y measurement.

### Ejecutar un Benchmark Específico

```bash
# Solo operadores básicos
java -jar target/benchmarks.jar BasicOperatorsBenchmark

# Solo creación de observables
java -jar target/benchmarks.jar CreationBenchmark

# Solo Subjects
java -jar target/benchmarks.jar SubjectsBenchmark
```

### Ejecutar un Test Específico

```bash
# Solo test de map
java -jar target/benchmarks.jar ".*BasicOperatorsBenchmark.ourMap"

# Comparar map entre ambas implementaciones
java -jar target/benchmarks.jar ".*BasicOperatorsBenchmark.*.Map"
```

### Opciones de Configuración

Puedes personalizar la ejecución de benchmarks con parámetros JMH:

```bash
# Ejecutar con menos iteraciones (más rápido, menos preciso)
java -jar target/benchmarks.jar -wi 2 -i 3

# Especificar parámetros específicos
java -jar target/benchmarks.jar -p size=1000

# Formato de salida JSON
java -jar target/benchmarks.jar -rf json -rff results.json

# Ver opciones disponibles
java -jar target/benchmarks.jar -h
```

### Parámetros JMH Comunes

- `-wi N` - Warmup iterations (por defecto: 3)
- `-i N` - Measurement iterations (por defecto: 5)
- `-f N` - Forks (por defecto: 1)
- `-t N` - Threads (por defecto: 1)
- `-p param=value` - Parámetro específico
- `-rf format` - Formato de resultado (text, csv, json, latex)
- `-rff file` - Archivo de resultados

## Interpretación de Resultados

Los benchmarks usan el modo `Throughput` que mide **operaciones por milisegundo**.

### Ejemplo de Salida

```
Benchmark                                    (size)   Mode  Cnt      Score      Error  Units
BasicOperatorsBenchmark.ourMap                  10  thrpt    5   5432.123 ±   45.234  ops/ms
BasicOperatorsBenchmark.rxJavaMap               10  thrpt    5   5123.456 ±   52.345  ops/ms
```

- **Score más alto = mejor rendimiento**
- **Error más bajo = resultados más consistentes**

### Métricas Clave para Analizar

1. **Throughput absoluto:** ¿Cuántas operaciones por segundo?
2. **Comparación relativa:** ¿Cuánto más rápido/lento vs RxJava?
3. **Escalabilidad:** ¿Cómo cambia el rendimiento con diferentes tamaños?
4. **Consistencia:** ¿El error relativo es aceptable?

## Áreas de Mejora Identificadas

Después de ejecutar los benchmarks, analiza:

1. **Operadores con mayor diferencia negativa** → Prioridad de optimización
2. **Patrones de escalabilidad** → ¿Dónde hay problemas con grandes volúmenes?
3. **Overhead de creación** → ¿Los factory methods son eficientes?
4. **Manejo de errores** → ¿El overhead de error handling es razonable?

## Recomendaciones

- **Ejecuta primero un benchmark específico** para familiarizarte con la herramienta
- **Cierra otras aplicaciones** que puedan interferir con las mediciones
- **Ejecuta múltiples veces** benchmarks críticos para validar resultados
- **Compara versiones** después de optimizaciones para medir el impacto

## Contribuciones

Si identificas áreas de mejora basadas en los resultados de benchmarks:

1. Documenta el benchmark específico y los resultados
2. Propone la optimización con análisis de impacto
3. Ejecuta benchmarks antes y después para validar mejoras
4. Asegúrate de que los tests unitarios siguen pasando

## Limitaciones Conocidas

### Operadores No Implementados
- `Observable.defer()` - No existe en nuestra implementación
- `window(count, skip)` - Solo soportamos `window(count)`
- `zip(obs1, obs2, obs3, ...)` - Solo soportamos 2 observables

### Diferencias de API
- `onErrorReturn(value)` en nuestra impl. requiere `onErrorReturn(error -> value)`
- `scan(seed, accumulator)` tiene el orden de parámetros invertido respecto a RxJava

## Referencias

- [JMH Documentation](https://github.com/openjdk/jmh)
- [RxJava 3 Documentation](https://github.com/ReactiveX/RxJava)
- [Microbenchmarking Best Practices](https://www.oracle.com/technical-resources/articles/java/architect-benchmarking.html)
