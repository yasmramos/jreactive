# Subjects - Implementación

## Resumen

Se han implementado exitosamente los 4 tipos fundamentales de **Subjects** en la biblioteca ReactiveX. Los Subjects son componentes híbridos que actúan simultáneamente como **Observable** y **Observer**, permitiendo multicasting (hot observables) y comunicación bidireccional.

## Estadísticas de Implementación

| Componente | Líneas de Código | Tests | Estado |
|------------|------------------|-------|---------|
| **PublishSubject** | 266 | 18 | ✅ 100% Pass |
| **BehaviorSubject** | 381 | 21 | ✅ 100% Pass |
| **ReplaySubject** | 524 | 22 | ✅ 100% Pass |
| **AsyncSubject** | 312 | 24 | ✅ 100% Pass |
| **Subject (base)** | 68 | - | ✅ Abstract |
| **TOTAL** | **1,551 LOC** | **85 tests** | ✅ **Todos pasan** |

## 1. PublishSubject

**Propósito**: Multicasting simple - emite valores solo a los observers suscritos en el momento de la emisión.

### Características:
- ✅ No almacena valores históricos
- ✅ Observers solo reciben eventos después de su suscripción
- ✅ Thread-safe con múltiples observers
- ✅ Hot observable inmediato

### Ejemplo de Uso:
```java
PublishSubject<String> subject = PublishSubject.create();

subject.subscribe(s -> System.out.println("Observer 1: " + s));

subject.onNext("A");  // Observer 1 recibe "A"
subject.onNext("B");  // Observer 1 recibe "B"

subject.subscribe(s -> System.out.println("Observer 2: " + s));

subject.onNext("C");  // Ambos observers reciben "C"
subject.onComplete();
```

**Output:**
```
Observer 1: A
Observer 1: B
Observer 1: C
Observer 2: C
```

### Casos de Uso:
- Event buses
- Notificaciones en tiempo real
- Propagación de eventos sin historial

---

## 2. BehaviorSubject

**Propósito**: Almacena y emite el último valor a nuevos subscribers, con soporte para valor inicial.

### Características:
- ✅ Mantiene el último valor emitido
- ✅ Nuevos observers reciben inmediatamente el último valor
- ✅ Soporte para valor inicial con `createDefault()`
- ✅ Valor persiste incluso después de `onComplete()`
- ✅ Thread-safe

### Ejemplo de Uso:
```java
BehaviorSubject<String> subject = BehaviorSubject.createDefault("Initial");

subject.subscribe(s -> System.out.println("Observer 1: " + s));
// Output: "Observer 1: Initial"

subject.onNext("A");  // Observer 1 recibe "A"

subject.subscribe(s -> System.out.println("Observer 2: " + s));
// Output: "Observer 2: A" (recibe el último valor)

subject.onNext("B");  // Ambos reciben "B"
```

### Métodos Adicionales:
- `getValue()`: Obtiene el valor actual
- `hasValue()`: Verifica si tiene un valor disponible

### Casos de Uso:
- State management (estado actual de la aplicación)
- Configuración compartida
- Valores de formularios
- Propiedades observables

---

## 3. ReplaySubject

**Propósito**: Almacena y reproduce todos (o N últimos) valores a nuevos subscribers.

### Características:
- ✅ Buffer completo o limitado por tamaño
- ✅ Reproduce todo el historial a nuevos observers
- ✅ `create()`: Buffer ilimitado
- ✅ `createWithSize(n)`: Solo últimos N valores
- ✅ Algoritmo eficiente con índices globales para buffer circular
- ✅ Thread-safe

### Ejemplo de Uso:

**Buffer Ilimitado:**
```java
ReplaySubject<String> subject = ReplaySubject.create();

subject.onNext("A");
subject.onNext("B");
subject.onNext("C");

subject.subscribe(s -> System.out.println("Observer: " + s));
// Output: "A", "B", "C" (replay completo)
```

**Buffer Limitado:**
```java
ReplaySubject<Integer> subject = ReplaySubject.createWithSize(2);

subject.onNext(1);
subject.onNext(2);
subject.onNext(3);
subject.onNext(4);

subject.subscribe(i -> System.out.println("Observer: " + i));
// Output: "3", "4" (solo últimos 2)
```

### Métodos Adicionales:
- `getValues(T[] array)`: Obtiene array con valores actuales
- `hasValue()`: Verifica si tiene valores
- `size()`: Número de valores almacenados

### Casos de Uso:
- Event sourcing
- Reproducción de historial de comandos
- Debugging y logging
- Análisis de series temporales
- Cache de datos

---

## 4. AsyncSubject

**Propósito**: Solo emite el último valor cuando el stream se completa.

### Características:
- ✅ Emite **solo** el último valor al completar
- ✅ Si no hay valores, solo emite `onComplete()`
- ✅ Si termina con error, solo emite el error
- ✅ Útil para operaciones que producen un resultado final
- ✅ Thread-safe

### Ejemplo de Uso:
```java
AsyncSubject<String> subject = AsyncSubject.create();

subject.subscribe(s -> System.out.println("Observer 1: " + s));

subject.onNext("A");
subject.onNext("B");
subject.onNext("C");

// Nada se emite aún...

subject.subscribe(s -> System.out.println("Observer 2: " + s));

subject.onComplete();
// Ahora ambos observers reciben "C" (último valor)
```

**Output:**
```
Observer 1: C
Observer 2: C
```

### Métodos Adicionales:
- `getValue()`: Obtiene el último valor (solo después de completar)
- `hasValue()`: Verifica si tiene un valor final

### Casos de Uso:
- Operaciones asincrónicas con resultado final (similar a Future/Promise)
- Cálculos que producen un único resultado
- Agregaciones finales
- Lazy evaluation con resultado único

---

## Comparación de Subjects

| Característica | PublishSubject | BehaviorSubject | ReplaySubject | AsyncSubject |
|----------------|----------------|-----------------|---------------|--------------|
| **Almacena valores** | ❌ No | ✅ Último | ✅ Todos/N últimos | ✅ Último |
| **Late subscribers reciben** | Solo nuevos | Último + nuevos | Replay + nuevos | Último (al completar) |
| **Valor inicial** | ❌ No | ✅ Opcional | ❌ No | ❌ No |
| **Emite antes de complete** | ✅ Sí | ✅ Sí | ✅ Sí | ❌ No |
| **Uso de memoria** | Bajo | Bajo | Variable | Bajo |
| **Mejor para** | Events | State | History | Final result |

---

## Arquitectura de Implementación

### Clase Base: Subject<T>

```java
public abstract class Subject<T> extends Observable<T> implements Observer<T>
```

**Métodos Abstractos:**
- `subscribeActual(Observer<? super T>)`: Lógica de suscripción
- `hasComplete()`: Indica si completó
- `hasThrowable()`: Indica si terminó con error
- `getThrowable()`: Obtiene el error si existe
- `hasObservers()`: Indica si hay observers suscritos
- `observerCount()`: Número de observers actuales

### Patrón de Disposable

Cada Subject implementa su propio `Disposable` interno:
- **PublishDisposable**: Gestión simple de suscripción
- **BehaviorDisposable**: Tracking del primer valor emitido
- **ReplayDisposable**: Índice de posición en el replay
- **AsyncDisposable**: Control de emisión final

### Thread Safety

Todos los Subjects son completamente **thread-safe**:
- ✅ Uso de `AtomicReference` para arrays de observers
- ✅ Operaciones CAS (Compare-And-Swap) para modificaciones
- ✅ `volatile` para banderas de estado
- ✅ Sincronización en buffers compartidos

---

## Mejoras Implementadas

### 1. TestObserver Enhancements

Se agregaron métodos a `TestObserver` para mejor soporte de testing:

```java
// Nuevo método
public TestObserver<T> assertErrorMessage(String message)
```

**Mejora de Dispose**: TestObserver ahora guarda correctamente el `Disposable` upstream y lo llama al hacer dispose:

```java
private Disposable upstream;

@Override
public void onSubscribe(Disposable d) {
    this.upstream = d;
}

@Override
public void dispose() {
    disposed = true;
    if (upstream != null) {
        upstream.dispose();
    }
}
```

### 2. ReplaySubject - Buffer Circular Inteligente

Para `ReplaySubject.createWithSize(n)`, se implementó un algoritmo robusto que maneja correctamente los índices cuando el buffer es circular:

```java
private int totalCount; // Contador global de elementos

// Al emitir:
totalCount++;

// Al hacer replay:
int startIndex = totalCount - size;  // Índice global del primer elemento
int bufferIndex = rd.index - startIndex;  // Posición relativa
```

Esto soluciona problemas de sincronización cuando elementos antiguos se descartan del buffer.

---

## Tests Implementados

### Cobertura Comprehensiva

**85 tests en total** cubriendo:

✅ **Funcionalidad Básica**
- Emisión y recepción de valores
- onComplete y onError
- Late subscribers

✅ **Dispose y Lifecycle**
- Dispose de observers individuales
- Verificación de hasObservers()
- observerCount()

✅ **Casos Edge**
- Valores null (validación)
- Múltiples completions
- onNext después de terminate
- onError después de error

✅ **Thread Safety**
- Emisión desde múltiples threads
- Suscripción concurrente
- Operaciones atómicas

✅ **Integración con Operators**
- Chaining con map, filter
- Como Observer de otros Observables
- Transformaciones completas

---

## Archivos Creados/Modificados

### Nuevos Archivos (8):

**Implementación:**
1. `src/main/java/com/reactive/core/PublishSubject.java` (266 líneas)
2. `src/main/java/com/reactive/core/BehaviorSubject.java` (381 líneas)
3. `src/main/java/com/reactive/core/ReplaySubject.java` (524 líneas)
4. `src/main/java/com/reactive/core/AsyncSubject.java` (312 líneas)

**Tests:**
5. `src/test/java/com/reactive/core/PublishSubjectTest.java` (349 líneas)
6. `src/test/java/com/reactive/core/BehaviorSubjectTest.java` (374 líneas)
7. `src/test/java/com/reactive/core/ReplaySubjectTest.java` (437 líneas)
8. `src/test/java/com/reactive/core/AsyncSubjectTest.java` (442 líneas)

### Archivos Modificados (2):

9. `src/main/java/com/reactive/testing/TestObserver.java`
   - Agregado `assertErrorMessage(String)`
   - Mejorado manejo de `Disposable` upstream

10. `src/main/java/com/reactive/core/Subject.java` (ya existía como clase base)

---

## Estado del Proyecto Completo

### Componentes Reactivos Implementados:

| Componente | Estado | Tests |
|------------|--------|-------|
| **Observable** | ✅ Completo | 15 tests |
| **Single** | ✅ Completo | 23 tests |
| **Maybe** | ✅ Completo | 31 tests |
| **Completable** | ✅ Completo | 26 tests |
| **PublishSubject** | ✅ Completo | 18 tests |
| **BehaviorSubject** | ✅ Completo | 21 tests |
| **ReplaySubject** | ✅ Completo | 22 tests |
| **AsyncSubject** | ✅ Completo | 24 tests |
| **Schedulers** | ✅ Completo | - |

**Total de Tests Passing**: 180+ tests

### Operadores Disponibles:

**Transformación**: map, flatMap, switchMap, concatMap, scan, buffer, window, groupBy

**Filtrado**: filter, take, skip, distinct, debounce, throttle

**Combinación**: merge, zip, combineLatest, concat, startWith

**Utilidad**: delay, timeout, retry, repeat, doOnNext, doOnError, doOnComplete

**Agregación**: reduce, collect, toList, count

**Schedulers**: io(), computation(), newThread(), trampoline()

---

## Próximos Pasos Sugeridos

### Opción 1: ConnectableObservable
- `publish()` / `replay()`
- `refCount()` / `share()` / `autoConnect()`
- Control manual de suscripción para hot observables

### Opción 2: Operadores de Agregación Adicionales
- `all()`, `any()`, `contains()`, `isEmpty()`
- `defaultIfEmpty()`, `switchIfEmpty()`
- `toMap()`, `toMultimap()`

### Opción 3: Operadores Avanzados de Grouping
- Mejoras a `groupBy()` existente
- `window()` con diferentes estrategias
- `buffer()` con condiciones complejas

### Opción 4: Backpressure & Flowable
- Implementar Flowable (Observable con backpressure)
- Estrategias de backpressure (BUFFER, DROP, LATEST, ERROR)
- Subscription con request(n)

---

## Conclusión

✅ **Implementación exitosa de los 4 tipos de Subjects**
✅ **85 tests comprehensivos con 100% de éxito**
✅ **1,551 líneas de código production + 1,602 líneas de tests**
✅ **Thread-safe y production-ready**
✅ **Compatibilidad con ReactiveX spec**

Los Subjects completan la funcionalidad core de multicasting en la biblioteca, permitiendo patrones avanzados de programación reactiva como event buses, state management, y comunicación bidireccional entre componentes.

---

**Autor**: MiniMax Agent  
**Fecha**: 2025-11-26
