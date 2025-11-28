# 📋 Inventario de Benchmarks JMH

## 📊 Estadísticas Generales

- **Total de archivos:** 8 benchmarks
- **Total de líneas:** 1,875 líneas de código
- **Framework:** JMH 1.37
- **Comparación:** Observable vs RxJava 3.1.8

---

## 📁 Archivos de Benchmark

### **1. BasicOperatorsBenchmark.java**
**Líneas:** ~146 líneas  
**Operadores probados:**
- `map()` - Transformación de elementos
- `filter()` - Filtrado de elementos
- `map() + filter()` - Combinación de operadores
- `flatMap()` - Transformación con expansión
- `map() + filter() + flatMap()` - Pipeline completo
- `take()` - Limitar elementos
- `skip()` - Saltar elementos
- `take() + skip()` - Combinación

**Parámetros:** size = 10, 100, 1,000, 10,000

---

### **2. CreationBenchmark.java**
**Líneas:** ~92 líneas  
**Métodos de creación probados:**
- `just()` - Crear con valores fijos
- `range()` - Crear rango de números
- `fromIterable()` - Crear desde colección
- `create()` - Crear con emitter personalizado

**Parámetros:** size = 10, 100, 1,000

---

### **3. ErrorHandlingBenchmark.java**
**Líneas:** ~203 líneas  
**Operadores de error probados:**
- `onErrorReturn()` - Retornar valor por defecto
- `onErrorResumeNext()` - Continuar con otro Observable
- `retry()` - Reintentar en caso de error
- `doOnError()` - Ejecutar acción en error

**Escenarios:**
- Sin error (happy path)
- Con error (error handling)

**Parámetros:** size = 10, 100, 1,000

---

### **4. SpecializedTypesBenchmark.java**
**Líneas:** ~397 líneas  
**Tipos especializados probados:**

**Single:**
- `just()`, `fromCallable()`
- `map()`, `flatMap()`, `zip()`
- `onErrorReturn()`

**Maybe:**
- `just()`, `empty()`, `fromCallable()`
- `map()`, `flatMap()`, `filter()`
- `defaultIfEmpty()`, `switchIfEmpty()`

**Completable:**
- `complete()`, `fromAction()`
- `andThen()`, `merge()`, `concat()`
- `onErrorComplete()`

**Parámetros:** count = 10, 100, 1,000

---

### **5. ThroughputBenchmark.java**
**Líneas:** ~178 líneas  
**Escenarios de alto volumen:**
- **Simple Pipeline** - map + filter
- **Complex Pipeline** - Múltiples operadores
- **FlatMap Intensive** - FlatMap con expansión
- **Merge Intensive** - Merge de múltiples streams
- **Zip Intensive** - Combinación de streams
- **Reduce Intensive** - Agregación de datos
- **Distinct Intensive** - Eliminación de duplicados
- **Grouped Operations** - GroupBy + reduce

**Parámetros:** size = 100,000, 1,000,000

---

### **6. MemoryBenchmark.java**
**Líneas:** ~210 líneas  
**Escenarios de memoria:**
- **Range Creation** - Creación de rangos
- **FromIterable Creation** - Creación desde colecciones
- **Long Chain** - Cadenas largas de operadores
- **FlatMap Memory** - Consumo de flatMap
- **Buffer Memory** - Buffering de elementos
- **Window Memory** - Windowing de streams
- **Distinct Memory** - Presión de GC con distinct
- **GroupBy Memory** - Presión de GC con groupBy
- **Scan Memory** - Acumulación de estados
- **Collect Memory** - Recolección en listas

**Configuración JVM:** `-Xms2G -Xmx2G`  
**Parámetros:** size = 10,000, 100,000

---

### **7. ComplexOperatorsBenchmark.java**
**Líneas:** ~289 líneas  
**Pipelines complejos:**
- **Data Processing Pipeline** - filter + map + flatMap + distinct + take
- **Aggregation Pipeline** - groupBy + map + filter + reduce + scan
- **Combination Pipeline** - merge + map + zip + filter + distinctUntilChanged
- **Windowing Pipeline** - window + filter + map + reduce
- **Buffering Pipeline** - buffer + map + filter + flatMap + distinct
- **Error Handling Pipeline** - map (con errores) + onErrorReturn + filter
- **Nested FlatMap Pipeline** - flatMap anidados + filter + distinct
- **Statistical Pipeline** - map + scan + skip + take + distinctUntilChanged
- **ETL Pipeline** - extract + transform + groupBy + aggregate + scan

**Parámetros:** size = 1,000, 10,000

---

### **8. BackpressureBenchmark.java**
**Líneas:** ~360 líneas  
**Reactive Streams y backpressure:**
- **Publisher Conversion** - toPublisher() con demanda ilimitada
- **Bounded Backpressure** - Solicitud por lotes (batch size 100)
- **From Publisher** - fromPublisher() conversión
- **Pipeline with Backpressure** - map + filter con backpressure
- **One-by-One Request** - Solicitud elemento por elemento

**Escenarios:**
- Demanda ilimitada (Long.MAX_VALUE)
- Demanda por lotes (100 elementos)
- Demanda uno por uno (1 elemento)

**Parámetros:** size = 1,000, 10,000, 100,000

---

## 🎯 Configuración JMH Estándar

```java
@BenchmarkMode(Mode.Throughput)        // Medir operaciones/segundo
@OutputTimeUnit(TimeUnit.SECONDS)      // Unidad: ops/s o ops/ms
@State(Scope.Thread)                   // Estado por thread
@Warmup(iterations = 3, time = 2)      // Calentamiento: 3 × 2s
@Measurement(iterations = 5, time = 3) // Medición: 5 × 3s  
@Fork(1)                               // 1 fork de JVM
```

---

## 📊 Métricas Medidas

1. **Throughput** - Operaciones por segundo/millisegundo
2. **Average Time** - Tiempo promedio por operación
3. **Memory Footprint** - Consumo de memoria (en benchmarks específicos)
4. **GC Pressure** - Presión sobre el Garbage Collector

---

## 🚀 Comandos de Ejecución

### **Ejecutar todos los benchmarks**
```bash
java -jar target/benchmarks.jar
```

### **Ejecutar benchmark específico**
```bash
java -jar target/benchmarks.jar BasicOperatorsBenchmark
```

### **Ejecutar con parámetros personalizados**
```bash
java -jar target/benchmarks.jar BasicOperatorsBenchmark \
  -p size=1000 -wi 5 -i 10 -f 3
```

### **Generar reporte JSON**
```bash
java -jar target/benchmarks.jar -rf json -rff results.json
```

### **Listar benchmarks disponibles**
```bash
java -jar target/benchmarks.jar -l
```

---

## 📈 Resumen de Cobertura

| Categoría | Benchmarks | Observable | RxJava |
|-----------|------------|------------|--------|
| Creación | 4 | ✅ | ✅ |
| Transformación | 8 | ✅ | ✅ |
| Filtrado | 6 | ✅ | ✅ |
| Error Handling | 8 | ✅ | ✅ |
| Agregación | 6 | ✅ | ✅ |
| Tipos Especializados | 15 | ✅ | ✅ |
| Alto Throughput | 8 | ✅ | ✅ |
| Memoria | 10 | ✅ | ✅ |
| Backpressure | 5 | ✅ | ✅ |

**Total de escenarios:** ~70 benchmarks individuales

---

## 📝 Archivos de Resultados

- `benchmark_results_basic.txt` - Resultados de operadores básicos
- `benchmark_results_throughput.txt` - Resultados de alto throughput
- `target/benchmarks.jar` - JAR ejecutable (~10 MB con dependencias)

---

**Creado:** 2025-11-27  
**Versión:** 2.0.0-SNAPSHOT  
**Framework:** JMH 1.37  
**Java:** OpenJDK 17

