# Paso 3 y 4: Advanced Grouping y Flowable con Backpressure - Implementación Completa

## Resumen Ejecutivo

Se han implementado exitosamente los **Pasos 3 y 4** del roadmap de JReactive:

- ✅ **Paso 3**: Advanced Grouping (groupBy mejorado, window, buffer con variantes)
- ✅ **Paso 4**: Flowable con backpressure completo

**Estado de Tests:** 278/278 tests passing (100%)

## Paso 3: Advanced Grouping Operators

### 1. GroupBy Mejorado

#### Nueva Interfaz: GroupedObservable
```java
public abstract class GroupedObservable<K, V> extends Observable<V> {
    public abstract K getKey();
}
```

#### Implementación Mejorada de groupBy
- **Antes**: Devolvía `Observable<Map<K, List<T>>>` (todos los grupos al final)
- **Ahora**: Devuelve `Observable<GroupedObservable<K, T>>` (streaming de grupos)

**Características:**
- Emite cada grupo tan pronto como se crea
- Cada GroupedObservable es un Observable independiente con su propia clave
- Propagación correcta de errores y completado a todos los grupos
- Thread-safe usando ConcurrentHashMap

**Ejemplo de Uso:**
```java
Observable.range(1, 10)
    .groupBy(n -> n % 2 == 0)  // Agrupa por par/impar
    .subscribe(group -> {
        System.out.println("Key: " + group.getKey());
        group.subscribe(value -> 
            System.out.println("Value: " + value)
        );
    });
```

### 2. Buffer con Variantes

#### buffer(int count)
Buffer básico que agrupa elementos en listas de tamaño fijo.

#### buffer(int count, int skip) - NUEVO
Buffer con salto (overlapping/non-overlapping buffers).

**Ejemplos:**
```java
// Overlapping: count=3, skip=2
Observable.range(1, 10).buffer(3, 2)
// Emite: [1,2,3], [3,4,5], [5,6,7], [7,8,9], [9,10]

// Non-overlapping: count=3, skip=5
Observable.range(1, 10).buffer(3, 5)
// Emite: [1,2,3], [6,7,8]

// Gap: count=2, skip=4
Observable.range(1, 10).buffer(2, 4)
// Emite: [1,2], [5,6], [9,10]
```

#### buffer(long timespan, TimeUnit unit) - NUEVO
Buffer basado en tiempo.

**Ejemplo:**
```java
Observable.interval(100, TimeUnit.MILLISECONDS)
    .buffer(500, TimeUnit.MILLISECONDS)
    .subscribe(buffer -> 
        System.out.println("Buffer: " + buffer)
    );
// Emite un buffer cada 500ms con los elementos recibidos en ese período
```

### 3. Window Operators (Mejorados)

Los operadores window ya existían pero se han mejorado:
- `window(int count)`: Ventanas de tamaño fijo
- `window(long timespan, TimeUnit unit)`: Ventanas basadas en tiempo

**Diferencia vs Buffer:**
- **Buffer**: Emite `List<T>` (todos los elementos juntos)
- **Window**: Emite `Observable<T>` (stream de elementos)

### Tests de Advanced Grouping

**12 tests nuevos** en `AdvancedGroupingTest.java`:

| Test | Descripción |
|------|-------------|
| testGroupByBasic | Agrupa números en pares/impares |
| testGroupByWithStrings | Agrupa strings por primera letra |
| testGroupByEmpty | Manejo de Observable vacío |
| testGroupByErrorPropagation | Propagación de errores a grupos |
| testBufferBasic | Buffer de tamaño fijo |
| testBufferWithSkip | Buffer con overlapping |
| testBufferEmpty | Buffer con Observable vacío |
| testBufferWithTime | Buffer basado en tiempo |
| testWindowBasic | Window de tamaño fijo |
| testWindowEmpty | Window con Observable vacío |
| testWindowWithTime | Window basado en tiempo |
| testGroupByThenBuffer | Combinación de operadores |

---

## Paso 4: Flowable con Backpressure

### Arquitectura

#### 1. Subscription Interface
```java
public interface Subscription {
    void request(long n);  // Request n items
    void cancel();         // Cancel subscription
}
```

#### 2. Subscriber Interface
```java
public interface Subscriber<T> {
    void onSubscribe(Subscription subscription);
    void onNext(T item);
    void onError(Throwable throwable);
    void onComplete();
}
```

#### 3. Flowable Class
Nueva clase completa con soporte para backpressure.

### Características Principales de Flowable

#### Factory Methods

| Método | Descripción |
|--------|-------------|
| `just(T... items)` | Crea un Flowable con items fijos |
| `fromIterable(Iterable<T>)` | Desde un Iterable |
| `range(int start, int count)` | Rango de enteros |
| `create(Consumer<FlowableEmitter<T>>, BackpressureStrategy)` | Con estrategia de backpressure |
| `empty()` | Flowable vacío |
| `never()` | Nunca emite |
| `error(Throwable)` | Emite error |

#### Transformation Operators

| Operador | Descripción |
|----------|-------------|
| `map(Function)` | Transforma cada elemento |
| `filter(Predicate)` | Filtra elementos |
| `take(long n)` | Toma primeros n elementos |
| `skip(long n)` | Salta primeros n elementos |
| `toObservable()` | Convierte a Observable (pierde backpressure) |

#### Backpressure Strategies

```java
public enum BackpressureStrategy {
    BUFFER,       // Almacena todos los elementos (puede causar OOM)
    DROP,         // Descarta elementos que no se pueden consumir
    DROP_LATEST,  // Descarta los más recientes cuando buffer está lleno
    DROP_OLDEST,  // Descarta los más antiguos cuando buffer está lleno
    ERROR         // Emite error cuando buffer está lleno
}
```

### Implementación de Backpressure

#### FlowableEmitter
Maneja la emisión de elementos respetando el backpressure:

```java
public interface FlowableEmitter<T> extends Subscription {
    void onNext(T item);
    void onError(Throwable error);
    void onComplete();
    long requested();
    boolean isCancelled();
}
```

**Características:**
- Cola interna para buffering
- Respeta el número de elementos solicitados
- Manejo de estrategias de backpressure
- Thread-safe con AtomicLong y ConcurrentQueue

#### Ejemplo de Uso Básico

```java
// Crear Flowable con backpressure
Flowable.range(1, 1000).subscribe(new Subscriber<Integer>() {
    private Subscription subscription;
    
    @Override
    public void onSubscribe(Subscription subscription) {
        this.subscription = subscription;
        subscription.request(10);  // Request 10 items initially
    }
    
    @Override
    public void onNext(Integer item) {
        System.out.println("Received: " + item);
        // Request more after processing
        subscription.request(1);
    }
    
    @Override
    public void onError(Throwable throwable) {
        System.err.println("Error: " + throwable);
    }
    
    @Override
    public void onComplete() {
        System.out.println("Completed");
    }
});
```

#### Ejemplo con Backpressure Strategy

```java
Flowable.create(emitter -> {
    // Producer emite rápidamente
    for (int i = 0; i < 10000; i++) {
        emitter.onNext(i);
    }
    emitter.onComplete();
}, BackpressureStrategy.BUFFER)
.filter(n -> n % 2 == 0)
.map(n -> n * 2)
.take(100)
.subscribe(new Subscriber<Integer>() {
    @Override
    public void onSubscribe(Subscription subscription) {
        subscription.request(Long.MAX_VALUE);  // Request unbounded
    }
    
    @Override
    public void onNext(Integer item) {
        // Process slowly
        System.out.println(item);
    }
    
    @Override
    public void onError(Throwable throwable) {
        throwable.printStackTrace();
    }
    
    @Override
    public void onComplete() {
        System.out.println("Done");
    }
});
```

### Conversión entre Observable y Flowable

#### Observable → Flowable
```java
Observable<Integer> observable = Observable.range(1, 100);
Flowable<Integer> flowable = observable.toFlowable(BackpressureStrategy.BUFFER);
```

#### Flowable → Observable
```java
Flowable<Integer> flowable = Flowable.range(1, 100);
Observable<Integer> observable = flowable.toObservable();
// Pierde backpressure - solicita todos los elementos inmediatamente
```

### Tests de Flowable

**17 tests nuevos** en `FlowableTest.java`:

| Categoría | Tests | Descripción |
|-----------|-------|-------------|
| **Factory** | 5 | just, range, fromIterable, empty, error |
| **Backpressure** | 5 | request, incremental request, buffer strategy, drop strategy, cancel |
| **Operators** | 6 | map, filter, take, skip, toObservable |
| **Chaining** | 1 | Combinación de operadores |

**Casos Clave Probados:**
- ✅ Request incremental (pedir 2, luego 2 más, etc.)
- ✅ Cancelación de subscription
- ✅ Buffering con estrategia BUFFER
- ✅ Drop con estrategia DROP
- ✅ Conversión a Observable
- ✅ Encadenamiento de operadores con backpressure

---

## Estructura de Archivos

### Archivos Nuevos Creados

```
src/main/java/com/reactive/core/
├── Flowable.java              (655 líneas) - Clase principal con backpressure
├── GroupedObservable.java     (18 líneas)  - Observable con key
├── Subscriber.java            (37 líneas)  - Subscriber con backpressure
└── Subscription.java          (21 líneas)  - Control de flow

src/test/java/
├── com/reactive/core/
│   └── FlowableTest.java      (574 líneas) - 17 tests de Flowable
└── com/reactive/observables/
    └── AdvancedGroupingTest.java (330 líneas) - 12 tests de grouping
```

### Archivos Modificados

```
src/main/java/com/reactive/core/Observable.java
├── groupBy: Mejorado para usar GroupedObservable
├── buffer: Agregadas variantes con skip y time
├── toFlowable: Nuevo método de conversión
└── +200 líneas aproximadamente
```

---

## Comparación Observable vs Flowable

| Aspecto | Observable | Flowable |
|---------|-----------|----------|
| **Backpressure** | ❌ No soporta | ✅ Soporte completo |
| **Control de Flow** | Push-based | Pull-based (request/response) |
| **Uso Principal** | UI events, mouse clicks | I/O streams, big data |
| **Cancelación** | Disposable.dispose() | Subscription.cancel() |
| **Simplicidad** | Más simple | Más complejo |
| **Performance** | Overhead menor | Overhead por backpressure |

### ¿Cuándo usar cada uno?

**Use Observable cuando:**
- No hay problema de velocidad entre producer/consumer
- Eventos de UI (clicks, input)
- Pocos elementos (<1000)
- No necesita control de flow

**Use Flowable cuando:**
- Producer puede ser más rápido que consumer
- Grandes volúmenes de datos
- Operaciones de I/O (archivos, red, DB)
- Necesita control de flow explícito

---

## Estadísticas Finales

### Tests
- **Tests Base Previos**: 249 tests
- **Tests Nuevos Paso 3**: 12 tests (AdvancedGrouping)
- **Tests Nuevos Paso 4**: 17 tests (Flowable)
- **Total**: 278 tests
- **Pass Rate**: 100% ✅

### Líneas de Código
- **Flowable.java**: 655 líneas
- **Interfaces nuevas**: ~80 líneas
- **Tests nuevos**: ~900 líneas
- **Modificaciones Observable**: ~200 líneas
- **Total agregado**: ~1835 líneas

### Cobertura de Características

#### Paso 3: Advanced Grouping
- ✅ GroupedObservable con key
- ✅ groupBy streaming (no batch)
- ✅ buffer(count, skip) con overlapping
- ✅ buffer(timespan, unit) temporal
- ✅ window operators mejorados
- ✅ Propagación correcta de errores
- ✅ Thread-safety

#### Paso 4: Flowable con Backpressure
- ✅ Subscription con request/cancel
- ✅ Subscriber interface
- ✅ 5 Backpressure strategies
- ✅ Factory methods (just, range, create, etc.)
- ✅ Transformation operators (map, filter, take, skip)
- ✅ FlowableEmitter con manejo de cola
- ✅ Conversión Observable ↔ Flowable
- ✅ Thread-safe con AtomicLong
- ✅ Cancel propagation
- ✅ Error handling

---

## Ejemplos de Uso Avanzados

### 1. GroupBy con Procesamiento por Grupo

```java
Observable.range(1, 100)
    .groupBy(n -> n % 10)  // 10 grupos (0-9)
    .subscribe(group -> {
        group
            .map(n -> "Group " + group.getKey() + ": " + n)
            .buffer(5)
            .subscribe(buffer -> System.out.println(buffer));
    });
```

### 2. Buffer con Overlapping para Análisis de Ventana Deslizante

```java
// Análisis de ventana deslizante (3 elementos, slide de 1)
Observable.interval(100, TimeUnit.MILLISECONDS)
    .take(10)
    .buffer(3, 1)
    .subscribe(window -> {
        double avg = window.stream()
            .mapToLong(Long::longValue)
            .average()
            .orElse(0);
        System.out.println("Window avg: " + avg);
    });
```

### 3. Flowable con Backpressure para Procesar Archivo Grande

```java
Flowable.create(emitter -> {
    try (BufferedReader reader = new BufferedReader(new FileReader("large.txt"))) {
        String line;
        while ((line = reader.readLine()) != null && !emitter.isCancelled()) {
            emitter.onNext(line);
            // Espera si no hay demand
            while (emitter.requested() == 0 && !emitter.isCancelled()) {
                Thread.sleep(10);
            }
        }
        emitter.onComplete();
    } catch (Exception e) {
        emitter.onError(e);
    }
}, BackpressureStrategy.BUFFER)
.filter(line -> !line.isEmpty())
.map(String::trim)
.subscribe(new Subscriber<String>() {
    private Subscription subscription;
    
    @Override
    public void onSubscribe(Subscription subscription) {
        this.subscription = subscription;
        subscription.request(10);  // Process 10 lines at a time
    }
    
    @Override
    public void onNext(String line) {
        processLine(line);
        subscription.request(1);  // Request next line
    }
    
    @Override
    public void onError(Throwable throwable) {
        System.err.println("Error: " + throwable);
    }
    
    @Override
    public void onComplete() {
        System.out.println("File processed");
    }
});
```

---

## Próximos Pasos Sugeridos

### Mejoras Adicionales para Flowable
1. **Operadores adicionales**:
   - `flatMap` con backpressure
   - `concatMap` con backpressure
   - `switchMap`
   - `debounce` y `throttle`

2. **Processors**:
   - `FlowableProcessor` (Subject equivalente para Flowable)
   - `PublishProcessor`
   - `BehaviorProcessor`
   - `ReplayProcessor`

3. **Integración**:
   - Reactive Streams compatibility (org.reactivestreams.*)
   - CompletableFuture integration
   - Java 9+ Flow API support

### Mejoras para Operators
1. **GroupBy avanzado**:
   - `groupBy` con function de valor
   - Expiración de grupos (timeout)
   - Límite de grupos activos

2. **Window avanzado**:
   - `window` con overlapping
   - `window` con boundary selector
   - `window` con otro Observable como trigger

---

## Conclusión

Los **Pasos 3 y 4** han sido implementados exitosamente con:

✅ **Advanced Grouping**: GroupedObservable, buffer variants, window operators
✅ **Flowable completo**: Backpressure con 5 estrategias, operators, conversión
✅ **100% tests passing**: 278/278 tests
✅ **Producción-ready**: Thread-safe, error handling, documentation
✅ **API limpia**: Intuitiva y consistente con RxJava

La biblioteca ahora tiene capacidades **profesionales** para manejar:
- Streams de datos grandes con backpressure
- Agrupación avanzada con streaming
- Control de flow explícito
- Buffering temporal y por count
- Conversión entre Observable y Flowable

**Author**: MiniMax Agent  
**Date**: 2025-11-27  
**Version**: 2.0.0-SNAPSHOT
