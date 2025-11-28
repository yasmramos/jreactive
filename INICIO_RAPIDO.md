# Guía de Inicio Rápido - JReactive

## ⚡ Inicio en 5 minutos

### 1. Verificar Requisitos
```bash
java -version  # Necesitas Java 11 o superior
```

### 2. Compilar el Proyecto

#### Opción A: Con Maven
```bash
cd jreactive
mvn clean compile
```

#### Opción B: Con Gradle
```bash
cd jreactive
gradle build
```

#### Opción C: Manual con javac
```bash
cd jreactive
chmod +x compile.sh
./compile.sh
```

### 3. Tu Primer Observable

Crea un archivo `MiPrimerReactivo.java`:

```java
import com.reactive.core.Observable;

public class MiPrimerReactivo {
    public static void main(String[] args) {
        // Crear un Observable simple
        Observable.just("Hola", "Mundo", "Reactivo")
            .subscribe(
                palabra -> System.out.println("Recibido: " + palabra),
                error -> System.err.println("Error: " + error),
                () -> System.out.println("¡Completado!")
            );
    }
}
```

Compilar y ejecutar:
```bash
javac -cp build/classes MiPrimerReactivo.java
java -cp .:build/classes MiPrimerReactivo
```

Salida:
```
Recibido: Hola
Recibido: Mundo
Recibido: Reactivo
¡Completado!
```

## 📝 Ejemplos Paso a Paso

### Ejemplo 1: Números Pares

```java
Observable.range(1, 10)
    .filter(n -> n % 2 == 0)
    .subscribe(System.out::println);

// Salida: 2, 4, 6, 8, 10
```

### Ejemplo 2: Transformación

```java
Observable.just("apple", "banana", "orange")
    .map(String::toUpperCase)
    .map(s -> s + "!")
    .subscribe(System.out::println);

// Salida: APPLE!, BANANA!, ORANGE!
```

### Ejemplo 3: Manejo de Errores

```java
Observable.create(emitter -> {
    emitter.onNext("Item 1");
    emitter.onNext("Item 2");
    throw new RuntimeException("¡Algo salió mal!");
})
.onErrorReturn(error -> "Valor de respaldo")
.subscribe(System.out::println);

// Salida: Item 1, Item 2, Valor de respaldo
```

### Ejemplo 4: Combinación

```java
Observable<String> letras = Observable.just("A", "B", "C");
Observable<Integer> numeros = Observable.just(1, 2, 3);

Observable.zip(letras, numeros, (letra, numero) -> letra + numero)
    .subscribe(System.out::println);

// Salida: A1, B2, C3
```

### Ejemplo 5: Asíncrono

```java
import com.reactive.schedulers.Schedulers;

Observable.just("Tarea 1", "Tarea 2", "Tarea 3")
    .subscribeOn(Schedulers.io())
    .map(tarea -> {
        System.out.println(tarea + " en: " + Thread.currentThread().getName());
        return tarea.toUpperCase();
    })
    .subscribe(System.out::println);

Thread.sleep(1000); // Esperar a que termine
```

## 🎯 Patrones Comunes

### Patrón 1: Pipeline de Datos
```java
Observable.fromIterable(listaUsuarios)
    .filter(usuario -> usuario.isActive())
    .map(usuario -> usuario.getName())
    .map(String::toUpperCase)
    .take(10)
    .subscribe(System.out::println);
```

### Patrón 2: Retry con Backoff
```java
Observable.create(emitter -> {
    // Llamada HTTP que puede fallar
    String resultado = llamadaHTTP();
    emitter.onNext(resultado);
    emitter.onComplete();
})
.retry(3)
.subscribe(
    resultado -> System.out.println("Éxito: " + resultado),
    error -> System.err.println("Fallo después de 3 reintentos")
);
```

### Patrón 3: FlatMap para Operaciones Anidadas
```java
Observable.just(1, 2, 3)
    .flatMap(id -> obtenerUsuario(id))
    .flatMap(usuario -> obtenerPedidos(usuario))
    .subscribe(pedido -> System.out.println(pedido));
```

### Patrón 4: Concatenación Secuencial
```java
Observable<String> paso1 = Observable.just("Inicio");
Observable<String> paso2 = Observable.just("Procesando");
Observable<String> paso3 = Observable.just("Completado");

Observable.concat(paso1, paso2, paso3)
    .subscribe(System.out::println);

// Salida: Inicio, Procesando, Completado
```

## 🔧 Tips y Trucos

### 1. Debug con doOnNext
```java
Observable.range(1, 5)
    .doOnNext(n -> System.out.println("Antes: " + n))
    .map(n -> n * 2)
    .doOnNext(n -> System.out.println("Después: " + n))
    .subscribe();
```

### 2. Disposable para Cancelar
```java
Disposable subscription = Observable.interval(1, TimeUnit.SECONDS)
    .subscribe(n -> System.out.println("Tick: " + n));

// Después de 5 segundos, cancelar
Thread.sleep(5000);
subscription.dispose();
```

### 3. Operadores Encadenados
```java
Observable.just("reactive", "programming", "is", "awesome")
    .filter(word -> word.length() > 2)
    .map(String::toUpperCase)
    .distinctUntilChanged()
    .take(3)
    .subscribe(System.out::println);
```

## 🚨 Errores Comunes

### ❌ No esperar a operaciones asíncronas
```java
// MALO
Observable.just("dato")
    .subscribeOn(Schedulers.io())
    .subscribe(System.out::println);
// El programa puede terminar antes de que se imprima

// BUENO
Disposable d = Observable.just("dato")
    .subscribeOn(Schedulers.io())
    .subscribe(System.out::println);
Thread.sleep(1000); // Esperar o usar CountDownLatch
```

### ❌ No manejar errores
```java
// MALO
Observable.create(emitter -> {
    throw new RuntimeException("Error!");
}).subscribe(System.out::println);
// El error no se maneja

// BUENO
Observable.create(emitter -> {
    throw new RuntimeException("Error!");
})
.onErrorReturn(e -> "Valor por defecto")
.subscribe(System.out::println);
```

### ❌ Olvidar subscribe()
```java
// MALO - No se ejecuta nada
Observable.just("dato")
    .map(String::toUpperCase);

// BUENO
Observable.just("dato")
    .map(String::toUpperCase)
    .subscribe(System.out::println);
```

## 📚 Próximos Pasos

1. ✅ Completa los ejemplos básicos
2. ✅ Experimenta con diferentes operadores
3. ✅ Lee la documentación completa en README.md
4. ✅ Prueba los ejemplos avanzados
5. ✅ Construye tu propia aplicación reactiva

## 🎓 Recursos

- **README.md** - Documentación completa
- **BasicExamples.java** - 7 ejemplos básicos
- **AdvancedExamples.java** - 4 ejemplos avanzados
- **PROXIMOS_PASOS.md** - Mejoras futuras

## 💡 ¿Necesitas Ayuda?

Si encuentras problemas:
1. Verifica la versión de Java (mínimo 11)
2. Revisa los mensajes de error
3. Consulta los ejemplos
4. Lee el README.md

¡Feliz programación reactiva! 🚀
