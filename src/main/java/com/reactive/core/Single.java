package com.reactive.core;

import com.reactive.schedulers.Schedulers;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Represents a reactive stream that emits exactly one item or an error.
 * 
 * <p>Single is a specialized reactive type that guarantees one of two outcomes:
 * <ul>
 *   <li><b>Success</b>: Exactly one value is emitted via onSuccess()</li>
 *   <li><b>Error</b>: An error is emitted via onError()</li>
 * </ul>
 * 
 * <p>Unlike {@link Observable} which can emit 0 to N items, Single always completes
 * with either success or error. This makes it ideal for operations that produce
 * a single result, such as:
 * <ul>
 *   <li>HTTP requests</li>
 *   <li>Database queries returning a single row</li>
 *   <li>File reads</li>
 *   <li>Computations that yield one result</li>
 * </ul>
 * 
 * <h2>Example Usage</h2>
 * <pre>{@code
 * Single.fromCallable(() -> fetchUserById(123))
 *     .map(user -> user.getName())
 *     .subscribe(
 *         name -> System.out.println("User name: " + name),
 *         error -> System.err.println("Error: " + error)
 *     );
 * }</pre>
 * 
 * <h2>Conversion</h2>
 * <p>Single can be converted to/from other reactive types:
 * <ul>
 *   <li>{@link #toObservable()} - Converts to Observable that emits one item</li>
 *   <li>{@link #fromObservable(Observable)} - Creates Single from Observable's first item</li>
 * </ul>
 * 
 * @param <T> the type of the single value emitted
 * @see Observable
 * @see Maybe
 * @see SingleObserver
 */
public abstract class Single<T> {
    
    /**
     * Subscribes a {@link SingleObserver} to this Single.
     * 
     * <p>The observer will receive exactly one notification:
     * either onSuccess() with a value or onError() with an error.
     * 
     * @param observer the observer to subscribe
     * @see #subscribe(Consumer)
     * @see #subscribe(Consumer, Consumer)
     */
    public abstract void subscribe(SingleObserver<T> observer);
    
    /**
     * Subscribes with callbacks for success and error.
     * 
     * <p>Example:
     * <pre>{@code
     * Single.just(42)
     *     .subscribe(
     *         value -> System.out.println("Got: " + value),
     *         error -> System.err.println("Error: " + error)
     *     );
     * // Output: Got: 42
     * }</pre>
     * 
     * @param onSuccess callback for successful completion
     * @param onError callback for error
     * @return a Disposable to cancel the subscription
     */
    public final Disposable subscribe(Consumer<T> onSuccess, Consumer<Throwable> onError) {
        DisposableSingleObserver<T> observer = new DisposableSingleObserver<>(onSuccess, onError);
        subscribe(observer);
        return observer;
    }
    
    /**
     * Subscribes with only a success callback (errors are ignored).
     * 
     * <p><b>Warning:</b> Errors will be silently ignored. Use {@link #subscribe(Consumer, Consumer)}
     * if you need error handling.
     * 
     * @param onSuccess callback for successful completion
     * @return a Disposable to cancel the subscription
     */
    public final Disposable subscribe(Consumer<T> onSuccess) {
        return subscribe(onSuccess, e -> {});
    }
    
    // ==================== CREATION OPERATORS ====================
    
    /**
     * Creates a Single that emits the specified value and then completes.
     * 
     * <p>Example:
     * <pre>{@code
     * Single.just("Hello")
     *     .subscribe(System.out::println);
     * // Output: Hello
     * }</pre>
     * 
     * @param <T> the type of the value
     * @param value the value to emit
     * @return a Single that emits the value
     */
    public static <T> Single<T> just(T value) {
        return new Single<T>() {
            @Override
            public void subscribe(SingleObserver<T> observer) {
                observer.onSuccess(value);
            }
        };
    }
    
    /**
     * Creates a Single that immediately emits an error.
     * 
     * <p>Example:
     * <pre>{@code
     * Single.<String>error(new RuntimeException("Failed"))
     *     .subscribe(
     *         value -> System.out.println(value),
     *         error -> System.err.println("Error: " + error.getMessage())
     *     );
     * // Output: Error: Failed
     * }</pre>
     * 
     * @param <T> the type parameter
     * @param error the error to emit
     * @return a Single that emits an error
     */
    public static <T> Single<T> error(Throwable error) {
        return new Single<T>() {
            @Override
            public void subscribe(SingleObserver<T> observer) {
                observer.onError(error);
            }
        };
    }
    
    /**
     * Creates a Single from a Callable that will be invoked on subscription.
     * 
     * <p>The Callable is executed lazily when subscribe() is called.
     * 
     * <p>Example:
     * <pre>{@code
     * Single.fromCallable(() -> {
     *     return performExpensiveComputation();
     * })
     * .subscribe(
     *     result -> System.out.println("Result: " + result),
     *     error -> System.err.println("Error: " + error)
     * );
     * }</pre>
     * 
     * @param <T> the type of the result
     * @param callable the Callable to invoke
     * @return a Single that emits the Callable's result
     */
    public static <T> Single<T> fromCallable(java.util.concurrent.Callable<T> callable) {
        return new Single<T>() {
            @Override
            public void subscribe(SingleObserver<T> observer) {
                try {
                    T value = callable.call();
                    observer.onSuccess(value);
                } catch (Exception e) {
                    observer.onError(e);
                }
            }
        };
    }
    
    /**
     * Creates a Single from an Observable by taking its first emitted item.
     * 
     * <p>If the Observable completes without emitting any items, the Single
     * emits a NoSuchElementException.
     * 
     * <p>Example:
     * <pre>{@code
     * Observable<Integer> obs = Observable.just(1, 2, 3);
     * Single.fromObservable(obs)
     *     .subscribe(value -> System.out.println("First: " + value));
     * // Output: First: 1
     * }</pre>
     * 
     * @param <T> the type of items
     * @param observable the Observable to convert
     * @return a Single that emits the first item from the Observable
     */
    public static <T> Single<T> fromObservable(Observable<T> observable) {
        return new Single<T>() {
            @Override
            public void subscribe(SingleObserver<T> observer) {
                observable.subscribe(new Observer<T>() {
                    private T value;
                    private boolean hasValue = false;
                    
                    @Override
                    public void onNext(T item) {
                        if (!hasValue) {
                            value = item;
                            hasValue = true;
                        }
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        observer.onError(error);
                    }
                    
                    @Override
                    public void onComplete() {
                        if (hasValue) {
                            observer.onSuccess(value);
                        } else {
                            observer.onError(new java.util.NoSuchElementException("Observable completed without emitting any value"));
                        }
                    }
                });
            }
        };
    }
    
    // ==================== TRANSFORMATION OPERATORS ====================
    
    /**
     * Transforms the emitted item by applying a function.
     * 
     * <p>Example:
     * <pre>{@code
     * Single.just(5)
     *     .map(x -> x * 10)
     *     .subscribe(value -> System.out.println("Result: " + value));
     * // Output: Result: 50
     * }</pre>
     * 
     * @param <R> the type of the transformed item
     * @param mapper function to transform the item
     * @return a Single that emits the transformed item
     */
    public final <R> Single<R> map(Function<T, R> mapper) {
        return new Single<R>() {
            @Override
            public void subscribe(SingleObserver<R> observer) {
                Single.this.subscribe(new SingleObserver<T>() {
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
                    public void onError(Throwable error) {
                        observer.onError(error);
                    }
                });
            }
        };
    }
    
    /**
     * Transforms the emitted item into another Single and flattens the result.
     * 
     * <p>Example:
     * <pre>{@code
     * Single.just(5)
     *     .flatMap(x -> Single.just(x * 10))
     *     .subscribe(value -> System.out.println("Result: " + value));
     * // Output: Result: 50
     * }</pre>
     * 
     * @param <R> the type of items emitted by the inner Single
     * @param mapper function that transforms the item into a Single
     * @return a Single that emits the result from the inner Single
     */
    public final <R> Single<R> flatMap(Function<T, Single<R>> mapper) {
        return new Single<R>() {
            @Override
            public void subscribe(SingleObserver<R> observer) {
                Single.this.subscribe(new SingleObserver<T>() {
                    @Override
                    public void onSuccess(T value) {
                        try {
                            Single<R> nextSingle = mapper.apply(value);
                            nextSingle.subscribe(observer);
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
    
    /**
     * Filters the emitted item using a predicate.
     * 
     * <p>If the item passes the predicate test, it's emitted.
     * If it fails, a NoSuchElementException is emitted.
     * 
     * <p>Example:
     * <pre>{@code
     * Single.just(5)
     *     .filter(x -> x > 3)
     *     .subscribe(
     *         value -> System.out.println("Value: " + value),
     *         error -> System.err.println("Filtered out")
     *     );
     * // Output: Value: 5
     * }</pre>
     * 
     * @param predicate function to test the item
     * @return a Single that emits the item if it passes the test
     */
    public final Single<T> filter(Predicate<T> predicate) {
        return new Single<T>() {
            @Override
            public void subscribe(SingleObserver<T> observer) {
                Single.this.subscribe(new SingleObserver<T>() {
                    @Override
                    public void onSuccess(T value) {
                        try {
                            if (predicate.test(value)) {
                                observer.onSuccess(value);
                            } else {
                                observer.onError(new java.util.NoSuchElementException("Element did not pass filter"));
                            }
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
    
    // ==================== COMBINATION OPERATORS ====================
    
    /**
     * Combina este Single con otro usando una función.
     */
    public final <U, R> Single<R> zipWith(Single<U> other, BiFunction<T, U, R> zipper) {
        return zip(this, other, zipper);
    }
    
    /**
     * Combina dos Singles usando una función.
     */
    public static <T1, T2, R> Single<R> zip(Single<T1> s1, Single<T2> s2, BiFunction<T1, T2, R> zipper) {
        return new Single<R>() {
            @Override
            public void subscribe(SingleObserver<R> observer) {
                Object[] values = new Object[2];
                boolean[] completed = new boolean[2];
                boolean[] errored = {false};
                
                s1.subscribe(new SingleObserver<T1>() {
                    @Override
                    public void onSuccess(T1 value) {
                        synchronized (values) {
                            if (errored[0]) return;
                            values[0] = value;
                            completed[0] = true;
                            if (completed[1]) {
                                try {
                                    @SuppressWarnings("unchecked")
                                    R result = zipper.apply((T1) values[0], (T2) values[1]);
                                    observer.onSuccess(result);
                                } catch (Exception e) {
                                    observer.onError(e);
                                }
                            }
                        }
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        synchronized (values) {
                            if (!errored[0]) {
                                errored[0] = true;
                                observer.onError(error);
                            }
                        }
                    }
                });
                
                s2.subscribe(new SingleObserver<T2>() {
                    @Override
                    public void onSuccess(T2 value) {
                        synchronized (values) {
                            if (errored[0]) return;
                            values[1] = value;
                            completed[1] = true;
                            if (completed[0]) {
                                try {
                                    @SuppressWarnings("unchecked")
                                    R result = zipper.apply((T1) values[0], (T2) values[1]);
                                    observer.onSuccess(result);
                                } catch (Exception e) {
                                    observer.onError(e);
                                }
                            }
                        }
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        synchronized (values) {
                            if (!errored[0]) {
                                errored[0] = true;
                                observer.onError(error);
                            }
                        }
                    }
                });
            }
        };
    }
    
    // ==================== ERROR HANDLING OPERATORS ====================
    
    /**
     * Returns a fallback value if an error occurs.
     * 
     * <p>Example:
     * <pre>{@code
     * Single.error(new RuntimeException("Failed"))
     *     .onErrorReturn(error -> -1)
     *     .subscribe(value -> System.out.println("Value: " + value));
     * // Output: Value: -1
     * }</pre>
     * 
     * @param valueSupplier function that provides a fallback value
     * @return a Single that emits the fallback value on error
     */
    public final Single<T> onErrorReturn(Function<Throwable, T> valueSupplier) {
        return new Single<T>() {
            @Override
            public void subscribe(SingleObserver<T> observer) {
                Single.this.subscribe(new SingleObserver<T>() {
                    @Override
                    public void onSuccess(T value) {
                        observer.onSuccess(value);
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
     * Si ocurre un error, cambia a otro Single.
     */
    public final Single<T> onErrorResumeNext(Function<Throwable, Single<T>> resumeFunction) {
        return new Single<T>() {
            @Override
            public void subscribe(SingleObserver<T> observer) {
                Single.this.subscribe(new SingleObserver<T>() {
                    @Override
                    public void onSuccess(T value) {
                        observer.onSuccess(value);
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        try {
                            Single<T> fallbackSingle = resumeFunction.apply(error);
                            fallbackSingle.subscribe(observer);
                        } catch (Exception e) {
                            observer.onError(e);
                        }
                    }
                });
            }
        };
    }
    
    /**
     * Retries the subscription when an error occurs.
     * 
     * <p>Example:
     * <pre>{@code
     * AtomicInteger attempts = new AtomicInteger(0);
     * Single.defer(() -> {
     *     if (attempts.incrementAndGet() < 3) {
     *         return Single.error(new RuntimeException("Attempt " + attempts.get()));
     *     }
     *     return Single.just("Success!");
     * })
     * .retry(3)
     * .subscribe(System.out::println);
     * // Output: Success!
     * }</pre>
     * 
     * @param times the number of retry attempts
     * @return a Single that retries on error
     */
    public final Single<T> retry(int times) {
        return new Single<T>() {
            @Override
            public void subscribe(SingleObserver<T> observer) {
                retryInternal(observer, times);
            }
            
            private void retryInternal(SingleObserver<T> observer, int attemptsLeft) {
                Single.this.subscribe(new SingleObserver<T>() {
                    @Override
                    public void onSuccess(T value) {
                        observer.onSuccess(value);
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        if (attemptsLeft > 0) {
                            retryInternal(observer, attemptsLeft - 1);
                        } else {
                            observer.onError(error);
                        }
                    }
                });
            }
        };
    }
    
    // ==================== UTILITY OPERATORS ====================
    
    /**
     * Ejecuta una acción cuando se emite el valor (efecto lateral).
     */
    public final Single<T> doOnSuccess(Consumer<T> onSuccess) {
        return new Single<T>() {
            @Override
            public void subscribe(SingleObserver<T> observer) {
                Single.this.subscribe(new SingleObserver<T>() {
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
                    public void onError(Throwable error) {
                        observer.onError(error);
                    }
                });
            }
        };
    }
    
    /**
     * Ejecuta una acción cuando ocurre un error (efecto lateral).
     */
    public final Single<T> doOnError(Consumer<Throwable> onError) {
        return new Single<T>() {
            @Override
            public void subscribe(SingleObserver<T> observer) {
                Single.this.subscribe(new SingleObserver<T>() {
                    @Override
                    public void onSuccess(T value) {
                        observer.onSuccess(value);
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        try {
                            onError.accept(error);
                        } catch (Exception e) {
                            // Ignore exceptions in error handler
                        }
                        observer.onError(error);
                    }
                });
            }
        };
    }
    
    /**
     * Delay: retrasa la emisión del valor.
     */
    public final Single<T> delay(long delay, TimeUnit unit) {
        return new Single<T>() {
            @Override
            public void subscribe(SingleObserver<T> observer) {
                Single.this.subscribe(new SingleObserver<T>() {
                    @Override
                    public void onSuccess(T value) {
                        Schedulers.computation().scheduleDirect(() -> observer.onSuccess(value), delay, unit);
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
    public final Single<T> subscribeOn(Scheduler scheduler) {
        return new Single<T>() {
            @Override
            public void subscribe(SingleObserver<T> observer) {
                scheduler.scheduleDirect(() -> Single.this.subscribe(observer));
            }
        };
    }
    
    /**
     * Especifica el scheduler en el que se observan los resultados.
     */
    public final Single<T> observeOn(Scheduler scheduler) {
        return new Single<T>() {
            @Override
            public void subscribe(SingleObserver<T> observer) {
                Single.this.subscribe(new SingleObserver<T>() {
                    @Override
                    public void onSuccess(T value) {
                        scheduler.scheduleDirect(() -> observer.onSuccess(value));
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
     * Converts this Single to an Observable that emits one item and completes.
     * 
     * <p>Example:
     * <pre>{@code
     * Single.just(42)
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
     * @return an Observable that emits the Single's item
     */
    public final Observable<T> toObservable() {
        return new Observable<T>() {
            @Override
            public void subscribe(Observer<? super T> observer) {
                Single.this.subscribe(new SingleObserver<T>() {
                    @Override
                    public void onSuccess(T value) {
                        observer.onNext(value);
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
    
    // ==================== HELPER CLASSES ====================
    
    static class DisposableSingleObserver<T> implements SingleObserver<T>, Disposable {
        private final Consumer<T> onSuccess;
        private final Consumer<Throwable> onError;
        private volatile boolean disposed;
        
        DisposableSingleObserver(Consumer<T> onSuccess, Consumer<Throwable> onError) {
            this.onSuccess = onSuccess;
            this.onError = onError;
        }
        
        @Override
        public void onSuccess(T value) {
            if (!disposed) {
                disposed = true;
                onSuccess.accept(value);
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
