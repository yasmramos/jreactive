package com.reactive.core;

import com.reactive.schedulers.Schedulers;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Represents a reactive stream that emits either one item, no items, or an error.
 * 
 * <p>Maybe is a specialized reactive type with three possible outcomes:
 * <ul>
 *   <li><b>Success</b>: Exactly one value is emitted via onSuccess()</li>
 *   <li><b>Empty</b>: No value is emitted, completes via onComplete()</li>
 *   <li><b>Error</b>: An error is emitted via onError()</li>
 * </ul>
 * 
 * <p>Maybe is the reactive equivalent of {@link java.util.Optional} but with async support.
 * It's ideal for operations that may or may not return a value, such as:
 * <ul>
 *   <li>Database queries that might return no results</li>
 *   <li>Cache lookups that might miss</li>
 *   <li>Optional configuration values</li>
 *   <li>Search operations that might find nothing</li>
 * </ul>
 * 
 * <h2>Example Usage</h2>
 * <pre>{@code
 * Maybe.fromCallable(() -> findUserById(123))
     .map(user -> user.getName())
 *     .subscribe(
 *         name -> System.out.println("Found: " + name),
 *         error -> System.err.println("Error: " + error),
 *         () -> System.out.println("User not found")
 *     );
 * }</pre>
 * 
 * <h2>Conversion</h2>
 * <p>Maybe can be converted to/from other reactive types:
 * <ul>
 *   <li>{@link #toObservable()} - Converts to Observable (0 or 1 item)</li>
 *   <li>{@link #toSingle()} - Converts to Single (error if empty)</li>
 *   <li>{@link #defaultIfEmpty(Object)} - Converts to Single with default</li>
 * </ul>
 * 
 * @param <T> the type of the optional value
 * @see Single
 * @see Observable
 * @see MaybeObserver
 */
public abstract class Maybe<T> {
    
    /**
     * Subscribes a {@link MaybeObserver} to this Maybe.
     * 
     * <p>The observer will receive one of three notifications:
     * <ul>
     *   <li>onSuccess() - if a value is emitted</li>
 *   <li>onComplete() - if no value is emitted</li>
     *   <li>onError() - if an error occurs</li>
     * </ul>
     * 
     * @param observer the observer to subscribe
     * @see #subscribe(Consumer)
     * @see #subscribe(Consumer, Consumer, Runnable)
     */
    public abstract void subscribe(MaybeObserver<T> observer);
    
    /**
     * Subscribes with callbacks for success, error, and completion.
     * 
     * <p>Example:
     * <pre>{@code
     * Maybe.just(42)
     *     .subscribe(
     *         value -> System.out.println("Got: " + value),
     *         error -> System.err.println("Error: " + error),
     *         () -> System.out.println("Empty")
     *     );
     * // Output: Got: 42
     * }</pre>
     * 
     * @param onSuccess callback for successful emission
     * @param onError callback for error
     * @param onComplete callback for empty completion
     * @return a Disposable to cancel the subscription
     */
    public final Disposable subscribe(Consumer<T> onSuccess, Consumer<Throwable> onError, Runnable onComplete) {
        DisposableMaybeObserver<T> observer = new DisposableMaybeObserver<>(onSuccess, onError, onComplete);
        subscribe(observer);
        return observer;
    }
    
    /**
     * Suscribe solo con lambda para éxito.
     */
    public final Disposable subscribe(Consumer<T> onSuccess) {
        return subscribe(onSuccess, e -> {}, () -> {});
    }
    
    // ==================== CREATION OPERATORS ====================
    
    /**
     * Creates a Maybe that emits the specified value and then completes.
     * 
     * <p>Example:
     * <pre>{@code
     * Maybe.just("Hello")
     *     .subscribe(System.out::println);
     * // Output: Hello
     * }</pre>
     * 
     * @param <T> the type of the value
     * @param value the value to emit
     * @return a Maybe that emits the value
     */
    public static <T> Maybe<T> just(T value) {
        return new Maybe<T>() {
            @Override
            public void subscribe(MaybeObserver<T> observer) {
                observer.onSuccess(value);
            }
        };
    }
    
    /**
     * Creates a Maybe that completes without emitting any value.
     * 
     * <p>Example:
     * <pre>{@code
     * Maybe.<String>empty()
     *     .subscribe(
     *         value -> System.out.println("Got: " + value),
     *         error -> {},
     *         () -> System.out.println("Empty")
     *     );
     * // Output: Empty
     * }</pre>
     * 
     * @param <T> the type parameter
     * @return a Maybe that completes empty
     */
    public static <T> Maybe<T> empty() {
        return new Maybe<T>() {
            @Override
            public void subscribe(MaybeObserver<T> observer) {
                observer.onComplete();
            }
        };
    }
    
    /**
     * Creates a Maybe that immediately emits an error.
     * 
     * <p>Example:
     * <pre>{@code
     * Maybe.<String>error(new RuntimeException("Failed"))
     *     .subscribe(
     *         value -> {},
     *         error -> System.err.println("Error: " + error.getMessage()),
     *         () -> {}
     *     );
     * // Output: Error: Failed
     * }</pre>
     * 
     * @param <T> the type parameter
     * @param error the error to emit
     * @return a Maybe that emits an error
     */
    public static <T> Maybe<T> error(Throwable error) {
        return new Maybe<T>() {
            @Override
            public void subscribe(MaybeObserver<T> observer) {
                observer.onError(error);
            }
        };
    }
    
    /**
     * Crea un Maybe desde un Callable.
     */
    public static <T> Maybe<T> fromCallable(java.util.concurrent.Callable<T> callable) {
        return new Maybe<T>() {
            @Override
            public void subscribe(MaybeObserver<T> observer) {
                try {
                    T value = callable.call();
                    if (value != null) {
                        observer.onSuccess(value);
                    } else {
                        observer.onComplete();
                    }
                } catch (Exception e) {
                    observer.onError(e);
                }
            }
        };
    }
    
    /**
     * Crea un Maybe desde un Single.
     */
    public static <T> Maybe<T> fromSingle(Single<T> single) {
        return new Maybe<T>() {
            @Override
            public void subscribe(MaybeObserver<T> observer) {
                single.subscribe(new SingleObserver<T>() {
                    @Override
                    public void onSuccess(T value) {
                        observer.onSuccess(value);
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        observer.onError(error);
                    }
                });
            }
        };
    }
    
    /**
     * Crea un Maybe desde un Observable (toma el primer elemento o completa vacío).
     */
    public static <T> Maybe<T> fromObservable(Observable<T> observable) {
        return new Maybe<T>() {
            @Override
            public void subscribe(MaybeObserver<T> observer) {
                observable.subscribe(new Observer<T>() {
                    private boolean hasValue = false;
                    
                    @Override
                    public void onNext(T value) {
                        if (!hasValue) {
                            hasValue = true;
                            observer.onSuccess(value);
                        }
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        if (!hasValue) {
                            observer.onError(error);
                        }
                    }
                    
                    @Override
                    public void onComplete() {
                        if (!hasValue) {
                            observer.onComplete();
                        }
                    }
                });
            }
        };
    }
    
    // ==================== TRANSFORMATION OPERATORS ====================
    
    /**
     * Transforms the emitted item (if any) by applying a function.
     * 
     * <p>If the Maybe is empty, the result is also empty.
     * 
     * <p>Example:
     * <pre>{@code
     * Maybe.just(5)
     *     .map(x -> x * 10)
     *     .subscribe(value -> System.out.println("Result: " + value));
     * // Output: Result: 50
     * }</pre>
     * 
     * @param <R> the type of the transformed item
     * @param mapper function to transform the item
     * @return a Maybe that emits the transformed item
     */
    public final <R> Maybe<R> map(Function<T, R> mapper) {
        return new Maybe<R>() {
            @Override
            public void subscribe(MaybeObserver<R> observer) {
                Maybe.this.subscribe(new MaybeObserver<T>() {
                    @Override
                    public void onSuccess(T value) {
                        try {
                            R result = mapper.apply(value);
                            observer.onSuccess(result);
                        } catch (Exception e) {
                            observer.onError(e);
                        }
                    }
                    
                    @Override
                    public void onComplete() {
                        observer.onComplete();
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        observer.onError(error);
                    }
                });
            }
        };
    }
    
    /**
     * FlatMap: transforma el elemento en otro Maybe.
     */
    public final <R> Maybe<R> flatMap(Function<T, Maybe<R>> mapper) {
        return new Maybe<R>() {
            @Override
            public void subscribe(MaybeObserver<R> observer) {
                Maybe.this.subscribe(new MaybeObserver<T>() {
                    @Override
                    public void onSuccess(T value) {
                        try {
                            Maybe<R> nextMaybe = mapper.apply(value);
                            nextMaybe.subscribe(observer);
                        } catch (Exception e) {
                            observer.onError(e);
                        }
                    }
                    
                    @Override
                    public void onComplete() {
                        observer.onComplete();
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        observer.onError(error);
                    }
                });
            }
        };
    }
    
    /**
     * Filters the emitted item (if any) using a predicate.
     * 
     * <p>If the item fails the test, the Maybe completes empty.
     * 
     * <p>Example:
     * <pre>{@code
     * Maybe.just(5)
     *     .filter(x -> x > 10)
     *     .subscribe(
     *         value -> System.out.println("Value: " + value),
     *         error -> {},
     *         () -> System.out.println("Filtered out")
     *     );
     * // Output: Filtered out
     * }</pre>
     * 
     * @param predicate function to test the item
     * @return a Maybe that emits the item if it passes the test
     */
    public final Maybe<T> filter(Predicate<T> predicate) {
        return new Maybe<T>() {
            @Override
            public void subscribe(MaybeObserver<T> observer) {
                Maybe.this.subscribe(new MaybeObserver<T>() {
                    @Override
                    public void onSuccess(T value) {
                        try {
                            if (predicate.test(value)) {
                                observer.onSuccess(value);
                            } else {
                                observer.onComplete();
                            }
                        } catch (Exception e) {
                            observer.onError(e);
                        }
                    }
                    
                    @Override
                    public void onComplete() {
                        observer.onComplete();
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        observer.onError(error);
                    }
                });
            }
        };
    }
    
    // ==================== CONDITIONAL OPERATORS ====================
    
    /**
     * If this Maybe completes empty, switches to the specified Maybe.
     * 
     * <p>Example:
     * <pre>{@code
     * Maybe.<String>empty()
     *     .switchIfEmpty(Maybe.just("Default"))
     *     .subscribe(value -> System.out.println("Value: " + value));
     * // Output: Value: Default
     * }</pre>
     * 
     * @param other the Maybe to switch to if empty
     * @return a Maybe that switches to the alternative if empty
     */
    public final Maybe<T> switchIfEmpty(Maybe<T> other) {
        return new Maybe<T>() {
            @Override
            public void subscribe(MaybeObserver<T> observer) {
                Maybe.this.subscribe(new MaybeObserver<T>() {
                    @Override
                    public void onSuccess(T value) {
                        observer.onSuccess(value);
                    }
                    
                    @Override
                    public void onComplete() {
                        other.subscribe(observer);
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        observer.onError(error);
                    }
                });
            }
        };
    }
    
    /**
     * Converts this Maybe to a Single with a default value if empty.
     * 
     * <p>Example:
     * <pre>{@code
     * Maybe.<Integer>empty()
     *     .defaultIfEmpty(42)
     *     .subscribe(value -> System.out.println("Value: " + value));
     * // Output: Value: 42
     * }</pre>
     * 
     * @param defaultValue the value to emit if this Maybe is empty
     * @return a Single that emits the item or the default value
     */
    public final Single<T> defaultIfEmpty(T defaultValue) {
        return new Single<T>() {
            @Override
            public void subscribe(SingleObserver<T> observer) {
                Maybe.this.subscribe(new MaybeObserver<T>() {
                    @Override
                    public void onSuccess(T value) {
                        observer.onSuccess(value);
                    }
                    
                    @Override
                    public void onComplete() {
                        observer.onSuccess(defaultValue);
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        observer.onError(error);
                    }
                });
            }
        };
    }
    
    /**
     * Si el Maybe está vacío, usa el supplier para obtener un valor.
     */
    public final Single<T> defaultIfEmpty(Supplier<T> defaultSupplier) {
        return new Single<T>() {
            @Override
            public void subscribe(SingleObserver<T> observer) {
                Maybe.this.subscribe(new MaybeObserver<T>() {
                    @Override
                    public void onSuccess(T value) {
                        observer.onSuccess(value);
                    }
                    
                    @Override
                    public void onComplete() {
                        try {
                            observer.onSuccess(defaultSupplier.get());
                        } catch (Exception e) {
                            observer.onError(e);
                        }
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        observer.onError(error);
                    }
                });
            }
        };
    }
    
    // ==================== ERROR HANDLING OPERATORS ====================
    
    /**
     * Si ocurre un error, retorna el valor dado.
     */
    public final Maybe<T> onErrorReturn(Function<Throwable, T> valueSupplier) {
        return new Maybe<T>() {
            @Override
            public void subscribe(MaybeObserver<T> observer) {
                Maybe.this.subscribe(new MaybeObserver<T>() {
                    @Override
                    public void onSuccess(T value) {
                        observer.onSuccess(value);
                    }
                    
                    @Override
                    public void onComplete() {
                        observer.onComplete();
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        try {
                            T fallbackValue = valueSupplier.apply(error);
                            observer.onSuccess(fallbackValue);
                        } catch (Exception e) {
                            observer.onError(e);
                        }
                    }
                });
            }
        };
    }
    
    /**
     * Si ocurre un error, cambia a otro Maybe.
     */
    public final Maybe<T> onErrorResumeNext(Function<Throwable, Maybe<T>> resumeFunction) {
        return new Maybe<T>() {
            @Override
            public void subscribe(MaybeObserver<T> observer) {
                Maybe.this.subscribe(new MaybeObserver<T>() {
                    @Override
                    public void onSuccess(T value) {
                        observer.onSuccess(value);
                    }
                    
                    @Override
                    public void onComplete() {
                        observer.onComplete();
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        try {
                            Maybe<T> fallbackMaybe = resumeFunction.apply(error);
                            fallbackMaybe.subscribe(observer);
                        } catch (Exception e) {
                            observer.onError(e);
                        }
                    }
                });
            }
        };
    }
    
    /**
     * Si ocurre un error, completa vacío.
     */
    public final Maybe<T> onErrorComplete() {
        return new Maybe<T>() {
            @Override
            public void subscribe(MaybeObserver<T> observer) {
                Maybe.this.subscribe(new MaybeObserver<T>() {
                    @Override
                    public void onSuccess(T value) {
                        observer.onSuccess(value);
                    }
                    
                    @Override
                    public void onComplete() {
                        observer.onComplete();
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        observer.onComplete();
                    }
                });
            }
        };
    }
    
    // ==================== UTILITY OPERATORS ====================
    
    /**
     * Ejecuta una acción cuando se emite el valor (efecto lateral).
     */
    public final Maybe<T> doOnSuccess(Consumer<T> onSuccess) {
        return new Maybe<T>() {
            @Override
            public void subscribe(MaybeObserver<T> observer) {
                Maybe.this.subscribe(new MaybeObserver<T>() {
                    @Override
                    public void onSuccess(T value) {
                        try {
                            onSuccess.accept(value);
                        } catch (Exception e) {
                            observer.onError(e);
                            return;
                        }
                        observer.onSuccess(value);
                    }
                    
                    @Override
                    public void onComplete() {
                        observer.onComplete();
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        observer.onError(error);
                    }
                });
            }
        };
    }
    
    /**
     * Ejecuta una acción cuando completa vacío (efecto lateral).
     */
    public final Maybe<T> doOnComplete(Runnable onComplete) {
        return new Maybe<T>() {
            @Override
            public void subscribe(MaybeObserver<T> observer) {
                Maybe.this.subscribe(new MaybeObserver<T>() {
                    @Override
                    public void onSuccess(T value) {
                        observer.onSuccess(value);
                    }
                    
                    @Override
                    public void onComplete() {
                        try {
                            onComplete.run();
                        } catch (Exception e) {
                            // Ignore exceptions in complete handler
                        }
                        observer.onComplete();
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        observer.onError(error);
                    }
                });
            }
        };
    }
    
    /**
     * Delay: retrasa la emisión del valor o completado.
     */
    public final Maybe<T> delay(long delay, TimeUnit unit) {
        return new Maybe<T>() {
            @Override
            public void subscribe(MaybeObserver<T> observer) {
                Maybe.this.subscribe(new MaybeObserver<T>() {
                    @Override
                    public void onSuccess(T value) {
                        Schedulers.computation().scheduleDirect(() -> observer.onSuccess(value), delay, unit);
                    }
                    
                    @Override
                    public void onComplete() {
                        Schedulers.computation().scheduleDirect(() -> observer.onComplete(), delay, unit);
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        Schedulers.computation().scheduleDirect(() -> observer.onError(error), delay, unit);
                    }
                });
            }
        };
    }
    
    // ==================== SCHEDULER OPERATORS ====================
    
    /**
     * Especifica el scheduler en el que se suscribe.
     */
    public final Maybe<T> subscribeOn(Scheduler scheduler) {
        return new Maybe<T>() {
            @Override
            public void subscribe(MaybeObserver<T> observer) {
                scheduler.scheduleDirect(() -> Maybe.this.subscribe(observer));
            }
        };
    }
    
    /**
     * Especifica el scheduler en el que se observan los resultados.
     */
    public final Maybe<T> observeOn(Scheduler scheduler) {
        return new Maybe<T>() {
            @Override
            public void subscribe(MaybeObserver<T> observer) {
                Maybe.this.subscribe(new MaybeObserver<T>() {
                    @Override
                    public void onSuccess(T value) {
                        scheduler.scheduleDirect(() -> observer.onSuccess(value));
                    }
                    
                    @Override
                    public void onComplete() {
                        scheduler.scheduleDirect(() -> observer.onComplete());
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        scheduler.scheduleDirect(() -> observer.onError(error));
                    }
                });
            }
        };
    }
    
    // ==================== CONVERSION OPERATORS ====================
    
    /**
     * Converts this Maybe to an Observable.
     * 
     * <p>If the Maybe emits a value, the Observable emits that value and completes.
     * If the Maybe is empty, the Observable completes without emitting.
     * 
     * <p>Example:
     * <pre>{@code
     * Maybe.just(42)
     *     .toObservable()
     *     .subscribe(
     *         value -> System.out.println("Value: " + value),
     *         error -> {},
     *         () -> System.out.println("Complete")
     *     );
     * // Output:
     * // Value: 42
     * // Complete
     * }</pre>
     * 
     * @return an Observable representing this Maybe
     */
    public final Observable<T> toObservable() {
        return new Observable<T>() {
            @Override
            public void subscribe(Observer<? super T> observer) {
                Maybe.this.subscribe(new MaybeObserver<T>() {
                    @Override
                    public void onSuccess(T value) {
                        observer.onNext(value);
                        observer.onComplete();
                    }
                    
                    @Override
                    public void onComplete() {
                        observer.onComplete();
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        observer.onError(error);
                    }
                });
            }
        };
    }
    
    /**
     * Converts this Maybe to a Single.
     * 
     * <p>If the Maybe is empty, the Single emits a NoSuchElementException.
     * 
     * <p>Example:
     * <pre>{@code
     * Maybe.just(42)
     *     .toSingle()
     *     .subscribe(
     *         value -> System.out.println("Value: " + value),
     *         error -> System.err.println("Error: " + error)
     *     );
     * // Output: Value: 42
     * }</pre>
     * 
     * @return a Single that emits the Maybe's item or an error if empty
     */
    public final Single<T> toSingle() {
        return new Single<T>() {
            @Override
            public void subscribe(SingleObserver<T> observer) {
                Maybe.this.subscribe(new MaybeObserver<T>() {
                    @Override
                    public void onSuccess(T value) {
                        observer.onSuccess(value);
                    }
                    
                    @Override
                    public void onComplete() {
                        observer.onError(new java.util.NoSuchElementException("Maybe completed without emitting any value"));
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        observer.onError(error);
                    }
                });
            }
        };
    }
    
    // ==================== HELPER CLASSES ====================
    
    static class DisposableMaybeObserver<T> implements MaybeObserver<T>, Disposable {
        private final Consumer<T> onSuccess;
        private final Consumer<Throwable> onError;
        private final Runnable onComplete;
        private volatile boolean disposed;
        
        DisposableMaybeObserver(Consumer<T> onSuccess, Consumer<Throwable> onError, Runnable onComplete) {
            this.onSuccess = onSuccess;
            this.onError = onError;
            this.onComplete = onComplete;
        }
        
        @Override
        public void onSuccess(T value) {
            if (!disposed) {
                disposed = true;
                onSuccess.accept(value);
            }
        }
        
        @Override
        public void onComplete() {
            if (!disposed) {
                disposed = true;
                onComplete.run();
            }
        }
        
        @Override
        public void onError(Throwable error) {
            if (!disposed) {
                disposed = true;
                onError.accept(error);
            }
        }
        
        @Override
        public void dispose() {
            disposed = true;
        }
        
        @Override
        public boolean isDisposed() {
            return disposed;
        }
    }
}
