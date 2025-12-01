# JReactive

[![CI Build](https://github.com/yasmramos/jreactive/actions/workflows/ci.yml/badge.svg)](https://github.com/yasmramos/jreactive/actions/workflows/ci.yml)
[![Java Version](https://img.shields.io/badge/Java-17%2B-blue.svg)](https://www.oracle.com/java/technologies/javase-jdk17-downloads.html)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![GitHub release](https://img.shields.io/github/v/release/yasmramos/jreactive)](https://github.com/yasmramos/jreactive/releases)
[![Test Coverage](https://img.shields.io/badge/tests-396%20passing-brightgreen.svg)](https://github.com/yasmramos/jreactive/actions/workflows/ci.yml)
[![GitHub Packages](https://img.shields.io/badge/GitHub%20Packages-v1.0.0--alpha-blue.svg)](https://github.com/yasmramos/jreactive/packages)

A modern reactive programming library for Java, designed to be simpler and easier to use than RxJava, while maintaining all essential features.

## 🚀 Features

- **4 Reactive Types**: Observable, Single, Maybe, and Completable
- **Simple and Intuitive API**: Easier to learn than RxJava
- **60+ Operators**: map, filter, flatMap, merge, zip, concat, retry, and more
- **Error Handling**: onErrorReturn, onErrorResumeNext, retry
- **Schedulers**: Support for asynchronous execution (io, computation, newThread)
- **Seamless Conversions**: Complete interoperability between reactive types
- **Type-Safe**: Leverages Java's type system
- **Zero Dependencies**: Standalone library using only standard Java

## 📋 Requirements

- Java 11 or higher
- Maven 3.6+ or Gradle 7.0+ (optional for build)

## 🔧 Installation

### Option 1: GitHub Packages (Recommended)

#### Maven

Add the GitHub Packages repository to your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/yasmramos/jreactive</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.reactive</groupId>
    <artifactId>jreactive</artifactId>
    <version>1.0.0-alpha</version>
</dependency>
```

Configure authentication in your `~/.m2/settings.xml`:

```xml
<servers>
    <server>
        <id>github</id>
        <username>YOUR_GITHUB_USERNAME</username>
        <password>YOUR_GITHUB_TOKEN</password>
    </server>
</servers>
```

#### Gradle

Add to your `build.gradle`:

```gradle
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/yasmramos/jreactive")
        credentials {
            username = project.findProperty("gpr.user") ?: System.getenv("GITHUB_USERNAME")
            password = project.findProperty("gpr.key") ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation 'com.reactive:jreactive:1.0.0-alpha'
}
```

### Option 2: Direct JAR Download

Download the latest release directly from GitHub:

| File | Description |
|------|-------------|
| [jreactive-1.0.0-alpha.jar](https://github.com/yasmramos/jreactive/releases/download/v1.0.0-alpha/jreactive-1.0.0-alpha.jar) | Main library JAR |
| [jreactive-1.0.0-alpha-sources.jar](https://github.com/yasmramos/jreactive/releases/download/v1.0.0-alpha/jreactive-1.0.0-alpha-sources.jar) | Source code JAR |

Add to your classpath:
```bash
java -cp jreactive-1.0.0-alpha.jar:. YourApplication
```

### Option 3: Build from Source

```bash
git clone https://github.com/yasmramos/jreactive.git
cd jreactive
mvn clean install
```

## 📚 Basic Concepts

### Observable

An `Observable` is a stream that can emit 0 or more elements, followed by a completion or error signal.

```java
Observable<String> observable = Observable.just("Hello", "World");
```

### Observer

An `Observer` consumes events emitted by an Observable:

```java
observable.subscribe(
    item -> System.out.println("Received: " + item),  // onNext
    error -> System.err.println("Error: " + error),    // onError
    () -> System.out.println("Completed!")             // onComplete
);
```

### Disposable

Represents a subscription that can be cancelled:

```java
Disposable subscription = observable.subscribe(item -> System.out.println(item));
subscription.dispose(); // Cancel the subscription
```

## 🎭 Reactive Types

The library offers 4 reactive types for different use cases:

### Observable<T> - Stream of 0 to N elements
Use `Observable` when you have multiple elements or a data stream:
```java
Observable.just(1, 2, 3, 4, 5)
    .filter(x -> x % 2 == 0)
    .subscribe(System.out::println);
```

### Single<T> - Exactly 1 element
Use `Single` when there's always a single result:
```java
Single.fromCallable(() -> fetchUser(123))
    .map(user -> user.name)
    .subscribe(
        name -> System.out.println("User: " + name),
        error -> System.err.println("Error: " + error)
    );
```

### Maybe<T> - 0 or 1 element
Use `Maybe` for lookups that might not have a result:
```java
Maybe.fromCallable(() -> cache.get("key"))
    .defaultIfEmpty("default-value")
    .subscribe(System.out::println);
```

### Completable - Only completion/error
Use `Completable` for operations without a result:
```java
Completable.fromRunnable(() -> saveToDatabase(data))
    .retry(3)
    .subscribe(
        () -> System.out.println("✓ Saved"),
        error -> System.err.println("✗ Error")
    );
```

**📖 Complete guide**: See [SINGLE_MAYBE_COMPLETABLE.md](docs/SINGLE_MAYBE_COMPLETABLE.md)

## 🎯 Usage Examples

### Example 1: Simple Observable

```java
Observable.just("A", "B", "C")
    .subscribe(System.out::println);
// Output: A B C
```

### Example 2: Transformation with map

```java
Observable.range(1, 5)
    .map(n -> n * 2)
    .subscribe(System.out::println);
// Output: 2 4 6 8 10
```

### Example 3: Filtering

```java
Observable.range(1, 10)
    .filter(n -> n % 2 == 0)
    .subscribe(System.out::println);
// Output: 2 4 6 8 10
```

### Example 4: FlatMap

```java
Observable.just("Hello", "World")
    .flatMap(word -> Observable.fromIterable(Arrays.asList(word.split(""))))
    .subscribe(System.out::println);
// Output: H e l l o W o r l d
```

### Example 5: Error Handling

```java
Observable.create(emitter -> {
    emitter.onNext("Item 1");
    throw new RuntimeException("Error!");
})
.onErrorReturn(error -> "Default value")
.subscribe(System.out::println);
// Output: Item 1, Default value
```

### Example 6: Schedulers (Asynchronous)

```java
Observable.just("Task")
    .subscribeOn(Schedulers.io())        // Execute on I/O thread
    .observeOn(Schedulers.computation()) // Observe on computation thread
    .subscribe(item -> System.out.println(
        item + " on " + Thread.currentThread().getName()
    ));
```

### Example 7: Combining Observables

```java
Observable<String> obs1 = Observable.just("A", "B", "C");
Observable<Integer> obs2 = Observable.just(1, 2, 3);

Observable.zip(obs1, obs2, (letter, number) -> letter + number)
    .subscribe(System.out::println);
// Output: A1 B2 C3
```

### Example 8: Automatic Retry

```java
int[] attempt = {0};

Observable.create(emitter -> {
    if (++attempt[0] < 3) {
        throw new RuntimeException("Failed");
    }
    emitter.onNext("Success!");
    emitter.onComplete();
})
.retry(5)
.subscribe(System.out::println);
// Output: Success! (after 3 attempts)
```

## 🛠️ Main API

### Creation Methods

| Method | Description |
|--------|-------------|
| `just(T...)` | Creates an Observable that emits the specified elements |
| `fromIterable(Iterable<T>)` | Creates an Observable from an Iterable |
| `range(int, int)` | Emits a range of numbers |
| `create(OnSubscribe)` | Creates a custom Observable |
| `empty()` | Observable that completes immediately |
| `error(Throwable)` | Observable that emits an error |
| `interval(long, TimeUnit)` | Emits incremental numbers periodically |

### Transformation Operators

| Method | Description |
|--------|-------------|
| `map(Function)` | Transforms each element |
| `flatMap(Function)` | Transforms each element into an Observable and flattens |
| `concatMap(Function)` | Like flatMap but maintains order |
| `switchMap(Function)` | Cancels the previous Observable on switch |

### Filtering Operators

| Method | Description |
|--------|-------------|
| `filter(Predicate)` | Filters elements based on a condition |
| `take(long)` | Takes only the first n elements |
| `skip(long)` | Skips the first n elements |
| `distinctUntilChanged()` | Filters consecutive duplicate elements |
| `first(T)` | Emits only the first element |
| `last(T)` | Emits only the last element |

### Combination Operators

| Method | Description |
|--------|-------------|
| `concat(Observable...)` | Concatenates Observables sequentially |
| `merge(Observable...)` | Merges Observables concurrently |
| `zip(Observable, Observable, BiFunction)` | Combines pairs of elements |
| `defaultIfEmpty(T)` | Emits default value if empty |

### Utility Operators

| Method | Description |
|--------|-------------|
| `doOnNext(Consumer)` | Executes action for each element |
| `doOnError(Consumer)` | Executes action on error |
| `doOnComplete(Runnable)` | Executes action on completion |
| `doOnSubscribe(Consumer)` | Executes action on subscription |
| `doOnDispose(Runnable)` | Executes action on disposal |

### Error Handling

| Method | Description |
|--------|-------------|
| `onErrorReturn(Function)` | Emits default value on error |
| `onErrorResumeNext(Function)` | Continues with another Observable on error |
| `retry()` | Retries infinitely |
| `retry(long)` | Retries n times |

### Schedulers

| Method | Description |
|--------|-------------|
| `subscribeOn(Scheduler)` | Specifies where subscription executes |
| `observeOn(Scheduler)` | Specifies where events are observed |

#### Available Schedulers:

- `Schedulers.io()` - For I/O operations (cached thread pool)
- `Schedulers.computation()` - For computations (fixed pool based on CPU cores)
- `Schedulers.newThread()` - Creates a new thread per task
- `Schedulers.immediate()` - Executes immediately on current thread

## 🏃 Running Examples

```bash
# With Maven
mvn exec:java -Dexec.mainClass="com.reactive.examples.BasicExamples"
mvn exec:java -Dexec.mainClass="com.reactive.examples.AdvancedExamples"

# With Gradle
gradle run
gradle runAdvancedExamples

# Build and run JAR
mvn package
java -jar target/jreactive-1.0.0-jar-with-dependencies.jar
```

## 📂 Project Structure

```
jreactive/
├── src/
│   ├── main/java/com/reactive/
│   │   ├── core/              # Core classes
│   │   │   ├── Observable.java
│   │   │   ├── Observer.java
│   │   │   ├── Disposable.java
│   │   │   ├── Emitter.java
│   │   │   └── ...
│   │   ├── operators/         # Operator implementations
│   │   │   ├── ObservableMap.java
│   │   │   ├── ObservableFilter.java
│   │   │   ├── ObservableFlatMap.java
│   │   │   └── ...
│   │   └── schedulers/        # Scheduler system
│   │       ├── Scheduler.java
│   │       └── Schedulers.java
│   └── examples/java/com/reactive/examples/
│       ├── BasicExamples.java
│       └── AdvancedExamples.java
├── pom.xml                    # Maven configuration
├── build.gradle              # Gradle configuration
└── README.md                 # This file
```

## 🆚 Comparison with RxJava

| Feature | JReactive | RxJava |
|---------|-----------|--------|
| Learning curve | ⭐⭐ Low | ⭐⭐⭐⭐ High |
| API | Simplified | Complete |
| Operators | Essential | All |
| Backpressure | Basic | Advanced |
| Size | Lightweight | Large |
| Dependencies | None | Several |
| Use case | Medium projects | Large projects |

## 🤝 Contributing

Contributions are welcome! For major changes:

1. Fork the project
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 👥 Author

**Yasmany Ramos García**

## 🙏 Acknowledgments

- Inspired by RxJava and Project Reactor
- Designed to be more accessible for developers learning reactive programming
- Focused on simplicity without sacrificing essential functionality

## 📖 Documentation

### Core Guides
- [Single, Maybe & Completable](docs/SINGLE_MAYBE_COMPLETABLE.md) - Guide to specialized reactive types
- [Subjects](docs/SUBJECTS.md) - Hot Observables and multicasting
- [Specialized Types](docs/SPECIALIZED_TYPES.md) - Overview of all specialized types

### Performance
- [Benchmarks](docs/benchmarks/BENCHMARKS.md) - Methodology and setup
- [Benchmark Results](docs/benchmarks/RESULTS.md) - Performance vs RxJava
- [Complete Documentation](docs/) - Full documentation index

### External Resources
- [ReactiveX](http://reactivex.io/) - ReactiveX Specification
- [Reactive Streams](https://www.reactive-streams.org/) - Reactive Streams Specification
- Examples included in `src/examples/java/com/reactive/examples/`

---

**Questions or suggestions?** Open an issue in the repository.
