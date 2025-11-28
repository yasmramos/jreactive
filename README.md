# JReactive

Una biblioteca de programación reactiva moderna para Java, diseñada para ser más simple y fácil de usar que RxJava, pero con todas las características esenciales.

## 🚀 Características

- **4 Tipos Reactivos**: Observable, Single, Maybe y Completable
- **API Simple e Intuitiva**: Más fácil de aprender que RxJava
- **60+ Operadores**: map, filter, flatMap, merge, zip, concat, retry y más
- **Manejo de Errores**: onErrorReturn, onErrorResumeNext, retry
- **Schedulers**: Soporte para ejecución asíncrona (io, computation, newThread)
- **Conversiones Fluidas**: Interoperabilidad completa entre tipos reactivos
- **Type-Safe**: Aprovecha el sistema de tipos de Java
- **Sin Dependencias**: Biblioteca standalone usando solo Java estándar

## 📋 Requisitos

- Java 11 o superior
- Maven 3.6+ o Gradle 7.0+ (opcional para build)

## 🔧 Instalación

### Usando Maven

```xml
<dependency>
    <groupId>com.reactive</groupId>
    <artifactId>jreactive</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Usando Gradle

```gradle
implementation 'com.reactive:jreactive:1.0.0'
```

### Compilar desde el código fuente

```bash
# Con Maven
mvn clean install

# Con Gradle
gradle build
```

## 📚 Conceptos Básicos

### Observable

Un `Observable` es un stream que puede emitir 0 o más elementos, seguido de una señal de completado o error.

```java
Observable<String> observable = Observable.just("Hola", "Mundo");
```

### Observer

Un `Observer` consume los eventos emitidos por un Observable:

```java
observable.subscribe(
    item -> System.out.println("Recibido: " + item),  // onNext
    error -> System.err.println("Error: " + error),    // onError
    () -> System.out.println("Completado!")            // onComplete
);
```

### Disposable

Representa una suscripción que puede ser cancelada:

```java
Disposable subscription = observable.subscribe(item -> System.out.println(item));
subscription.dispose(); // Cancelar la suscripción
```

## 🎭 Tipos Reactivos

La biblioteca ofrece 4 tipos reactivos para diferentes casos de uso:

### Observable<T> - Stream de 0 a N elementos
Usa `Observable` cuando tienes múltiples elementos o un stream de datos:
```java
Observable.just(1, 2, 3, 4, 5)
    .filter(x -> x % 2 == 0)
    .subscribe(System.out::println);
```

### Single<T> - Exactamente 1 elemento
Usa `Single` cuando siempre hay un resultado único:
```java
Single.fromCallable(() -> fetchUser(123))
    .map(user -> user.name)
    .subscribe(
        name -> System.out.println("Usuario: " + name),
        error -> System.err.println("Error: " + error)
    );
```

### Maybe<T> - 0 o 1 elemento
Usa `Maybe` para búsquedas que pueden no tener resultado:
```java
Maybe.fromCallable(() -> cache.get("key"))
    .defaultIfEmpty("valor-default")
    .subscribe(System.out::println);
```

### Completable - Solo completación/error
Usa `Completable` para operaciones sin resultado:
```java
Completable.fromRunnable(() -> saveToDatabase(data))
    .retry(3)
    .subscribe(
        () -> System.out.println("✓ Guardado"),
        error -> System.err.println("✗ Error")
    );
```

**📖 Guía completa**: Ver [SINGLE_MAYBE_COMPLETABLE.md](docs/SINGLE_MAYBE_COMPLETABLE.md)

## 🎯 Ejemplos de Uso

### Ejemplo 1: Observable Simple

```java
Observable.just("A", "B", "C")
    .subscribe(System.out::println);
// Salida: A B C
```

### Ejemplo 2: Transformación con map

```java
Observable.range(1, 5)
    .map(n -> n * 2)
    .subscribe(System.out::println);
// Salida: 2 4 6 8 10
```

### Ejemplo 3: Filtrado

```java
Observable.range(1, 10)
    .filter(n -> n % 2 == 0)
    .subscribe(System.out::println);
// Salida: 2 4 6 8 10
```

### Ejemplo 4: FlatMap

```java
Observable.just("Hello", "World")
    .flatMap(word -> Observable.fromIterable(Arrays.asList(word.split(""))))
    .subscribe(System.out::println);
// Salida: H e l l o W o r l d
```

### Ejemplo 5: Manejo de Errores

```java
Observable.create(emitter -> {
    emitter.onNext("Item 1");
    throw new RuntimeException("Error!");
})
.onErrorReturn(error -> "Valor por defecto")
.subscribe(System.out::println);
// Salida: Item 1, Valor por defecto
```

### Ejemplo 6: Schedulers (Asíncrono)

```java
Observable.just("Tarea")
    .subscribeOn(Schedulers.io())        // Ejecutar en thread I/O
    .observeOn(Schedulers.computation()) // Observar en thread de cómputo
    .subscribe(item -> System.out.println(
        item + " en " + Thread.currentThread().getName()
    ));
```

### Ejemplo 7: Combinar Observables

```java
Observable<String> obs1 = Observable.just("A", "B", "C");
Observable<Integer> obs2 = Observable.just(1, 2, 3);

Observable.zip(obs1, obs2, (letter, number) -> letter + number)
    .subscribe(System.out::println);
// Salida: A1 B2 C3
```

### Ejemplo 8: Retry Automático

```java
int[] attempt = {0};

Observable.create(emitter -> {
    if (++attempt[0] < 3) {
        throw new RuntimeException("Fallo");
    }
    emitter.onNext("Éxito!");
    emitter.onComplete();
})
.retry(5)
.subscribe(System.out::println);
// Salida: Éxito! (después de 3 intentos)
```

## 🛠️ API Principal

### Métodos de Creación

| Método | Descripción |
|--------|-------------|
| `just(T...)` | Crea un Observable que emite los elementos especificados |
| `fromIterable(Iterable<T>)` | Crea un Observable desde un Iterable |
| `range(int, int)` | Emite un rango de números |
| `create(OnSubscribe)` | Crea un Observable personalizado |
| `empty()` | Observable que completa inmediatamente |
| `error(Throwable)` | Observable que emite un error |
| `interval(long, TimeUnit)` | Emite números incrementales periódicamente |

### Operadores de Transformación

| Método | Descripción |
|--------|-------------|
| `map(Function)` | Transforma cada elemento |
| `flatMap(Function)` | Transforma cada elemento en Observable y aplana |
| `concatMap(Function)` | Como flatMap pero mantiene el orden |
| `switchMap(Function)` | Cancela el Observable anterior al cambiar |

### Operadores de Filtrado

| Método | Descripción |
|--------|-------------|
| `filter(Predicate)` | Filtra elementos según condición |
| `take(long)` | Toma solo los primeros n elementos |
| `skip(long)` | Omite los primeros n elementos |
| `distinctUntilChanged()` | Filtra elementos consecutivos duplicados |
| `first(T)` | Emite solo el primer elemento |
| `last(T)` | Emite solo el último elemento |

### Operadores de Combinación

| Método | Descripción |
|--------|-------------|
| `concat(Observable...)` | Concatena Observables secuencialmente |
| `merge(Observable...)` | Fusiona Observables concurrentemente |
| `zip(Observable, Observable, BiFunction)` | Combina pares de elementos |
| `defaultIfEmpty(T)` | Emite valor por defecto si está vacío |

### Operadores de Utilidad

| Método | Descripción |
|--------|-------------|
| `doOnNext(Consumer)` | Ejecuta acción por cada elemento |
| `doOnError(Consumer)` | Ejecuta acción al ocurrir error |
| `doOnComplete(Runnable)` | Ejecuta acción al completar |
| `doOnSubscribe(Consumer)` | Ejecuta acción al suscribirse |
| `doOnDispose(Runnable)` | Ejecuta acción al cancelar |

### Manejo de Errores

| Método | Descripción |
|--------|-------------|
| `onErrorReturn(Function)` | Emite valor por defecto en caso de error |
| `onErrorResumeNext(Function)` | Continúa con otro Observable en caso de error |
| `retry()` | Reintenta infinitamente |
| `retry(long)` | Reintenta n veces |

### Schedulers

| Método | Descripción |
|--------|-------------|
| `subscribeOn(Scheduler)` | Especifica dónde se ejecuta la suscripción |
| `observeOn(Scheduler)` | Especifica dónde se observan los eventos |

#### Schedulers disponibles:

- `Schedulers.io()` - Para operaciones I/O (pool de threads cacheado)
- `Schedulers.computation()` - Para cálculos (pool fijo basado en CPU cores)
- `Schedulers.newThread()` - Crea un nuevo thread por tarea
- `Schedulers.immediate()` - Ejecuta inmediatamente en el thread actual

## 🏃 Ejecutar Ejemplos

```bash
# Con Maven
mvn exec:java -Dexec.mainClass="com.reactive.examples.BasicExamples"
mvn exec:java -Dexec.mainClass="com.reactive.examples.AdvancedExamples"

# Con Gradle
gradle run
gradle runAdvancedExamples

# Compilar y ejecutar JAR
mvn package
java -jar target/jreactive-1.0.0-jar-with-dependencies.jar
```

## 📂 Estructura del Proyecto

```
jreactive/
├── src/
│   ├── main/java/com/reactive/
│   │   ├── core/              # Clases fundamentales
│   │   │   ├── Observable.java
│   │   │   ├── Observer.java
│   │   │   ├── Disposable.java
│   │   │   ├── Emitter.java
│   │   │   └── ...
│   │   ├── operators/         # Implementaciones de operadores
│   │   │   ├── ObservableMap.java
│   │   │   ├── ObservableFilter.java
│   │   │   ├── ObservableFlatMap.java
│   │   │   └── ...
│   │   └── schedulers/        # Sistema de schedulers
│   │       ├── Scheduler.java
│   │       └── Schedulers.java
│   └── examples/java/com/reactive/examples/
│       ├── BasicExamples.java
│       └── AdvancedExamples.java
├── pom.xml                    # Configuración Maven
├── build.gradle              # Configuración Gradle
└── README.md                 # Este archivo
```

## 🆚 Comparación con RxJava

| Característica | JReactive | RxJava |
|----------------|---------------|--------|
| Curva de aprendizaje | ⭐⭐ Baja | ⭐⭐⭐⭐ Alta |
| API | Simplificada | Completa |
| Operadores | Esenciales | Todos |
| Backpressure | Básico | Avanzado |
| Tamaño | Ligero | Grande |
| Dependencias | Ninguna | Varias |
| Caso de uso | Proyectos medianos | Proyectos grandes |

## 🤝 Contribuciones

Las contribuciones son bienvenidas. Para cambios importantes:

1. Fork el proyecto
2. Crea una rama para tu feature (`git checkout -b feature/AmazingFeature`)
3. Commit tus cambios (`git commit -m 'Add some AmazingFeature'`)
4. Push a la rama (`git push origin feature/AmazingFeature`)
5. Abre un Pull Request

## 📄 Licencia

Este proyecto está bajo la Licencia MIT - ver el archivo LICENSE para más detalles.

## 👥 Autor

**MiniMax Agent**

## 🙏 Agradecimientos

- Inspirado por RxJava y Project Reactor
- Diseñado para ser más accesible para desarrolladores que están aprendiendo programación reactiva
- Enfocado en simplicidad sin sacrificar funcionalidad esencial

## 📖 Recursos Adicionales

- [ReactiveX](http://reactivex.io/) - Especificación ReactiveX
- [Reactive Streams](https://www.reactive-streams.org/) - Especificación de Reactive Streams
- Ejemplos incluidos en `src/examples/java/com/reactive/examples/`

---

**¿Preguntas o sugerencias?** Abre un issue en el repositorio.
