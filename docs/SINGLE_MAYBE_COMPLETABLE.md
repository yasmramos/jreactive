# Single, Maybe y Completable

## Introducción

Además de `Observable`, la biblioteca incluye tres tipos reactivos especializados que simplifican casos de uso específicos:

- **Single**: Emite exactamente **1 elemento** o un error
- **Maybe**: Emite **0 o 1 elemento**, luego completa o emite error
- **Completable**: Solo indica **completación o error**, sin emitir elementos

Estos tipos hacen el código más expresivo y seguro al tipo, comunicando claramente la intención.

---

## 🔵 Single

### ¿Cuándo usar Single?

Usa `Single` cuando una operación **siempre** produce exactamente un resultado:

- Consulta a base de datos que devuelve un registro
- Petición HTTP que devuelve una respuesta
- Leer un archivo completo
- Cálculo que siempre produce un resultado

### Características Principales

**Eventos:**
- `onSuccess(T)` - Emite el valor único
- `onError(Throwable)` - Emite un error

**Factory Methods:**
```java
Single.just(42)                          // Emite valor inmediato
Single.error(new Exception("Error"))     // Emite error
Single.fromCallable(() -> compute())     // Ejecuta cálculo
```

**Operadores Comunes:**
```java
single
    .map(x -> x * 2)                    // Transformar valor
    .flatMap(x -> otherSingle(x))       // Encadenar Singles
    .filter(x -> x > 10)                // Devuelve Maybe
    .onErrorReturn(0)                   // Valor por defecto si error
    .retry(3)                           // Reintentar si falla
    .subscribeOn(Schedulers.io())       // Ejecutar en I/O thread
    .observeOn(Schedulers.computation()) // Observar en computation thread
```

### Ejemplo Completo

```java
// Obtener datos de usuario por ID
Single<User> user = getUserById(123)
    .map(u -> {
        u.name = u.name.toUpperCase();
        return u;
    })
    .flatMap(u -> getAddressForUser(u.id))
    .onErrorReturn("Dirección desconocida")
    .doOnSuccess(address -> 
        System.out.println("Dirección: " + address)
    );

user.subscribe(
    address -> System.out.println("✓ " + address),
    error -> System.err.println("✗ Error: " + error)
);
```

### Conversiones

```java
Single<String> single = Single.just("Hello");

Observable<String> observable = single.toObservable();
Maybe<String> maybe = single.toMaybe();
```

---

## 🟡 Maybe

### ¿Cuándo usar Maybe?

Usa `Maybe` cuando una operación puede **o no** producir un resultado:

- Búsqueda que puede no encontrar nada
- Valores opcionales
- Primer/último elemento de una secuencia (puede estar vacía)
- Caché que puede no tener el valor

### Características Principales

**Eventos:**
- `onSuccess(T)` - Emite un valor
- `onComplete()` - Completa sin valor
- `onError(Throwable)` - Emite un error

**Factory Methods:**
```java
Maybe.just("Hola")              // Emite valor
Maybe.empty()                   // Completa vacío
Maybe.error(new Exception())    // Emite error
Maybe.fromCallable(() -> {      // Null = completa vacío
    String result = find();
    return result;  // null completa vacío
})
```

**Operadores Comunes:**
```java
maybe
    .map(x -> x.toUpperCase())          // Transformar si existe
    .filter(x -> x.length() > 5)        // Filtrar (devuelve Maybe)
    .defaultIfEmpty("Default")          // Devuelve Single
    .flatMap(x -> otherMaybe(x))        // Encadenar Maybes
    .onErrorComplete()                  // Error → completa vacío
    .switchIfEmpty(otherMaybe)          // Alternativa si vacío
```

### Ejemplo Completo

```java
// Buscar usuario por email
Maybe<User> user = findUserByEmail("john@example.com")
    .filter(u -> u.email.contains("@example.com"))
    .map(u -> u.name.toUpperCase())
    .doOnSuccess(name -> 
        System.out.println("✓ Usuario: " + name)
    )
    .doOnComplete(() -> 
        System.out.println("✗ Usuario no encontrado")
    );

user.subscribe(
    name -> System.out.println("Nombre: " + name),
    error -> System.err.println("Error: " + error),
    () -> System.out.println("Búsqueda completada sin resultado")
);
```

### Conversiones

```java
Maybe<String> maybe = Maybe.just("Hello");

Observable<String> observable = maybe.toObservable();
Single<String> single = maybe.toSingle();  // Error si vacío
Single<String> safe = maybe.defaultIfEmpty("Default");
```

---

## 🟢 Completable

### ¿Cuándo usar Completable?

Usa `Completable` cuando solo importa el **éxito o fallo**, no hay resultado:

- Guardar archivo
- Enviar email
- Cerrar conexión
- Actualizar base de datos
- Limpiar recursos
- Operaciones de inicialización

### Características Principales

**Eventos:**
- `onComplete()` - Operación completada
- `onError(Throwable)` - Operación falló

**Factory Methods:**
```java
Completable.complete()                  // Completa inmediatamente
Completable.error(new Exception())      // Error inmediato
Completable.fromRunnable(() -> save())  // Ejecuta acción
Completable.fromCallable(() -> task())  // Ejecuta y completa
Completable.fromObservable(observable)  // Ignora elementos
```

**Operadores Comunes:**
```java
completable
    .andThen(otherCompletable)          // Secuencia
    .andThen(Single.just("Done"))       // Convierte a Single
    .onErrorComplete()                  // Error → completa ok
    .retry(3)                           // Reintentar
    .doOnComplete(() -> log("Done"))    // Side effect
```

### Ejemplo Completo

```java
// Workflow de registro de usuario
Completable workflow = Completable.fromRunnable(() -> 
        System.out.println("1. Validando datos...")
    )
    .andThen(Completable.fromRunnable(() -> 
        System.out.println("2. Guardando usuario...")
    ))
    .andThen(Completable.fromRunnable(() -> 
        System.out.println("3. Enviando email...")
    ))
    .doOnComplete(() -> 
        System.out.println("✓ Registro completado")
    )
    .retry(2);

workflow.subscribe(
    () -> System.out.println("Éxito"),
    error -> System.err.println("Error: " + error)
);
```

### Operaciones Paralelas

```java
Completable task1 = Completable.fromRunnable(() -> saveToDatabase());
Completable task2 = Completable.fromRunnable(() -> sendEmail());
Completable task3 = Completable.fromRunnable(() -> updateCache());

// Ejecutar todas en paralelo
Completable.merge(task1, task2, task3)
    .subscribe(
        () -> System.out.println("✓ Todas completadas"),
        error -> System.err.println("✗ Error: " + error)
    );
```

### Conversiones

```java
Completable completable = Completable.complete();

Observable<Void> observable = completable.toObservable();
Maybe<Void> maybe = completable.toMaybe();
```

---

## 🔄 Interoperabilidad entre Tipos

### Observable → Single/Maybe/Completable

```java
Observable<Integer> observable = Observable.just(1, 2, 3);

Single<Integer> first = observable.first(0);
Single<Integer> last = observable.last(0);
Maybe<Integer> firstMaybe = observable.firstElement();
Completable completable = Completable.fromObservable(observable);
```

### Single → Observable/Maybe/Completable

```java
Single<String> single = Single.just("Hello");

Observable<String> observable = single.toObservable();
Maybe<String> maybe = single.toMaybe();
Completable completable = Completable.fromSingle(single);
```

### Maybe → Observable/Single/Completable

```java
Maybe<String> maybe = Maybe.just("Hello");

Observable<String> observable = maybe.toObservable();
Single<String> single = maybe.toSingle();  // Error si vacío
Single<String> safe = maybe.defaultIfEmpty("Default");
Completable completable = Completable.fromMaybe(maybe);
```

### Completable → Observable/Maybe/Single

```java
Completable completable = Completable.complete();

Observable<Void> observable = completable.toObservable();
Maybe<Void> maybe = completable.toMaybe();
Single<String> single = completable.andThen(Single.just("Done"));
```

---

## 📊 Tabla de Comparación

| Tipo | Elementos | Eventos | Uso Principal |
|------|-----------|---------|---------------|
| **Observable** | 0..N | onNext, onError, onComplete | Streams de datos |
| **Single** | 1 | onSuccess, onError | Operación con resultado único |
| **Maybe** | 0..1 | onSuccess, onComplete, onError | Búsquedas opcionales |
| **Completable** | 0 | onComplete, onError | Operaciones sin resultado |

---

## 🎯 Guía de Selección

**Usa Observable cuando:**
- Tienes múltiples elementos
- No sabes cuántos elementos habrá
- Necesitas operadores de stream (buffer, window, etc.)

**Usa Single cuando:**
- Siempre hay exactamente 1 resultado
- Es un error no tener resultado
- Ejemplo: petición HTTP, consulta por ID

**Usa Maybe cuando:**
- Puede haber 0 o 1 resultado
- Vacío es válido, no es error
- Ejemplo: búsqueda, caché, primer elemento

**Usa Completable cuando:**
- No importa el resultado, solo éxito/fallo
- Es una acción con efecto secundario
- Ejemplo: guardar, eliminar, cerrar

---

## 💡 Ejemplos Prácticos

### Caso 1: API REST

```java
// GET /user/:id → Single (siempre devuelve un usuario)
Single<User> getUser(int id) {
    return Single.fromCallable(() -> 
        httpClient.get("/user/" + id)
    );
}

// GET /search?q= → Observable (múltiples resultados)
Observable<User> searchUsers(String query) {
    return Observable.fromIterable(
        httpClient.get("/search?q=" + query)
    );
}

// GET /cache/:key → Maybe (puede no existir)
Maybe<String> getCached(String key) {
    return Maybe.fromCallable(() -> 
        cache.get(key)  // null si no existe
    );
}

// DELETE /user/:id → Completable (solo éxito/fallo)
Completable deleteUser(int id) {
    return Completable.fromRunnable(() -> 
        httpClient.delete("/user/" + id)
    );
}
```

### Caso 2: Base de Datos

```java
// Buscar por ID único
Single<Product> findById(int id) {
    return Single.fromCallable(() -> 
        db.queryOne("SELECT * FROM products WHERE id = ?", id)
    );
}

// Buscar primero que coincida
Maybe<Product> findFirst(String category) {
    return Maybe.fromCallable(() -> 
        db.queryOne("SELECT * FROM products WHERE category = ?", category)
    );
}

// Buscar todos
Observable<Product> findAll() {
    return Observable.fromIterable(
        db.query("SELECT * FROM products")
    );
}

// Eliminar
Completable delete(int id) {
    return Completable.fromRunnable(() -> 
        db.execute("DELETE FROM products WHERE id = ?", id)
    );
}
```

### Caso 3: Workflow Complejo

```java
// Registro de usuario completo
public Single<User> registerUser(String email, String password) {
    // 1. Verificar que email no exista
    return findUserByEmail(email)
        .toSingle()
        .onErrorResumeNext(error -> {
            // 2. Email disponible, crear usuario
            return createUser(email, password)
                .doOnSuccess(user -> 
                    System.out.println("Usuario creado: " + user.id)
                );
        })
        .flatMap(user -> {
            // 3. Enviar email de bienvenida (Completable)
            Completable sendEmail = sendWelcomeEmail(user.email)
                .doOnComplete(() -> 
                    System.out.println("Email enviado")
                )
                .onErrorComplete();  // No fallar si email falla
            
            // 4. Retornar usuario después de enviar email
            return sendEmail.andThen(Single.just(user));
        });
}
```

---

## 🚀 Ventajas de Usar Tipos Específicos

### 1. **Seguridad de Tipo**
```java
// Con Observable - no está claro cuántos elementos hay
Observable<User> getUser(int id);  // ¿1? ¿muchos? ¿ninguno?

// Con Single - claramente 1 elemento
Single<User> getUser(int id);      // Siempre 1

// Con Maybe - claramente 0 o 1
Maybe<User> findUser(String email); // Puede no existir
```

### 2. **API Más Clara**
```java
// ❌ Confuso
Observable<Void> saveUser(User user);

// ✅ Claro
Completable saveUser(User user);
```

### 3. **Menos Código Boilerplate**
```java
// Con Observable
observable.subscribe(
    item -> {},           // No nos importa
    error -> handle(),
    () -> System.out.println("Done")
);

// Con Completable
completable.subscribe(
    () -> System.out.println("Done"),
    error -> handle()
);
```

### 4. **Optimizaciones Internas**
- `Single` no necesita gestionar múltiples elementos
- `Maybe` puede cortocircuitar más rápido
- `Completable` no tiene overhead de elementos

---

## 📝 Resumen

| Necesitas... | Usa... |
|--------------|--------|
| Exactamente 1 resultado | `Single<T>` |
| 0 o 1 resultado (búsqueda) | `Maybe<T>` |
| Solo éxito/fallo | `Completable` |
| Stream de múltiples elementos | `Observable<T>` |

**Regla general:** Usa el tipo más específico posible para tu caso de uso. Esto hace el código más legible, seguro y eficiente.
