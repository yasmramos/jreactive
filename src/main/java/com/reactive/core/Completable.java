package com.reactive.core;

import com.reactive.schedulers.Schedulers;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Represents a deferred computation that completes successfully or with an error, but doesn't emit any value.
 * 
 * <p>Completable is a specialized reactive type that only signals completion or error:
 * <ul>
 *   <li><b>Success</b>: Completes successfully via onComplete()</li>
 *   <li><b>Error</b>: Fails with an error via onError()</li>
 * </ul>
 * 
 * <p>Unlike other reactive types, Completable never emits values. It only indicates
 * whether an operation succeeded or failed. This makes it ideal for operations that
 * don't produce results but need to signal completion, such as:
 * <ul>
 *   <li>Database write operations (INSERT, UPDATE, DELETE)</li>
 *   <li>File write operations</li>
 *   <li>HTTP POST/PUT/DELETE requests</li>
 *   <li>Cache invalidation</li>
 *   <li>Initialization tasks</li>
 * </ul>
 * 
 * <h2>Example Usage</h2>
 * <pre>{@code
 * Completable.fromAction(() -> {
 *     database.saveUser(user);
 *     System.out.println("User saved!");
 * })
 * .subscribe(
 *     () -> System.out.println("Success!"),
 *     error -> System.err.println("Error: " + error)
 * );
 * }</pre>
 * 
 * <h2>Combination</h2>
 * <p>Multiple Completables can be combined:
 * <ul>
 *   <li>{@link #andThen(Completable)} - Execute sequentially</li>
 *   <li>{@link #merge(Completable...)} - Execute concurrently</li>
 *   <li>{@link #concat(Completable...)} - Execute one after another</li>
 * </ul>
 * 
 * <h2>Conversion</h2>
 * <p>Completable can be created from or converted to other reactive types:
 * <ul>
 *   <li>{@link #fromObservable(Observable)} - Ignores values, waits for completion</li>
 *   <li>{@link #toObservable()} - Converts to empty Observable</li>
 *   <li>{@link #andThen(Observable)} - Chains with Observable after completion</li>
 * </ul>
 * 
 * @see Observable
 * @see Single
 * @see Maybe
 * @see CompletableObserver
 */
public abstract class Completable {
    
    /**
     * Subscribes a {@link CompletableObserver} to this Completable.
     * 
     * <p>The observer will receive exactly one notification:
     * either onComplete() on success or onError() on failure.
     * 
     * @param observer the observer to subscribe
     * @see #subscribe(Runnable)
     * @see #subscribe(Runnable, Consumer)
     */
    public abstract void subscribe(CompletableObserver observer);
    
    /**
     * Subscribes with callbacks for completion and error.
     * 
     * <p>Example:
     * <pre>{@code
     * Completable.fromAction(() -> saveToDatabase())
     *     .subscribe(
     *         () -> System.out.println("Saved!"),
     *         error -> System.err.println("Error: " + error)
     *     );
     * }</pre>
     * 
     * @param onComplete callback for successful completion
     * @param onError callback for error
     * @return a Disposable to cancel the subscription
     */
    public final Disposable subscribe(Runnable onComplete, Consumer<Throwable> onError) {
        DisposableCompletableObserver observer = new DisposableCompletableObserver(onComplete, onError);
        subscribe(observer);
        return observer;
    }
    
    /**
     * Suscribe solo con lambda para completado.
     */
    public final Disposable subscribe(Runnable onComplete) {
        return subscribe(onComplete, e -> {});
    }
    
    // ==================== CREATION OPERATORS ====================
    
    /**
     * Creates a Completable that completes immediately.
     * 
     * <p>Example:
     * <pre>{@code
     * Completable.complete()
     *     .subscribe(() -> System.out.println("Complete!"));
     * // Output: Complete!
     * }</pre>
     * 
     * @return a Completable that completes immediately
     */
    public static Completable complete() {
        return new Completable() {
            @Override
            public void subscribe(CompletableObserver observer) {
                observer.onComplete();
            }
        };
    }
    
    /**
     * Creates a Completable that immediately emits an error.
     * 
     * <p>Example:
     * <pre>{@code
     * Completable.error(new RuntimeException("Failed"))
     *     .subscribe(
     *         () -> {},
     *         error -> System.err.println("Error: " + error.getMessage())
     *     );
     * // Output: Error: Failed
     * }</pre>
     * 
     * @param error the error to emit
     * @return a Completable that emits an error
     */
    public static Completable error(Throwable error) {
        return new Completable() {
            @Override
            public void subscribe(CompletableObserver observer) {
                observer.onError(error);
            }
        };
    }
    
    /**
     * Creates a Completable from an action to execute.
     * 
     * <p>The action is executed on subscription. If it throws an exception,
     * the Completable emits an error.
     * 
     * <p>Example:
     * <pre>{@code
     * Completable.fromAction(() -> {
     *     System.out.println("Performing action...");
     *     saveToDatabase();
     * })
     * .subscribe(
     *     () -> System.out.println("Success!"),
     *     error -> System.err.println("Error: " + error)
     * );
     * }</pre>
     * 
     * @param action the action to execute
     * @return a Completable that executes the action
     */
    public static Completable fromAction(Runnable action) {
        return new Completable() {
            @Override
            public void subscribe(CompletableObserver observer) {
                try {
                    action.run();
                    observer.onComplete();
                } catch (Exception e) {
                    observer.onError(e);
                }
            }
        };
    }
    
    /**
     * Crea un Completable desde un Callable.
     */
    public static Completable fromCallable(java.util.concurrent.Callable<Void> callable) {
        return new Completable() {
            @Override
            public void subscribe(CompletableObserver observer) {
                try {
                    callable.call();
                    observer.onComplete();
                } catch (Exception e) {
                    observer.onError(e);
                }
            }
        };
    }
    
    /**
     * Creates a Completable from an Observable, ignoring all emitted values.
     * 
     * <p>Only waits for the Observable to complete or error.
     * 
     * <p>Example:
     * <pre>{@code
     * Observable<Integer> obs = Observable.just(1, 2, 3);
     * Completable.fromObservable(obs)
     *     .subscribe(() -> System.out.println("Observable completed"));
     * // Output: Observable completed
     * }</pre>
     * 
     * @param observable the Observable to convert
     * @return a Completable that waits for the Observable to complete
     */
    public static Completable fromObservable(Observable<?> observable) {
        return new Completable() {
            @Override
            public void subscribe(CompletableObserver observer) {
                observable.subscribe(new Observer<Object>() {
                    @Override
                    public void onNext(Object value) {
                        // Ignorar valores
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        observer.onError(error);
                    }
                    
                    @Override
                    public void onComplete() {
                        observer.onComplete();
                    }
                });
            }
        };
    }
    
    /**
     * Crea un Completable desde un Single (ignora el valor, solo espera que complete).
     */
    public static <T> Completable fromSingle(Single<T> single) {
        return new Completable() {
            @Override
            public void subscribe(CompletableObserver observer) {
                single.subscribe(new SingleObserver<T>() {
                    @Override
                    public void onSuccess(T value) {
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
     * Crea un Completable desde un Maybe (ignora el valor, solo espera que complete).
     */
    public static <T> Completable fromMaybe(Maybe<T> maybe) {
        return new Completable() {
            @Override
            public void subscribe(CompletableObserver observer) {
                maybe.subscribe(new MaybeObserver<T>() {
                    @Override
                    public void onSuccess(T value) {
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
    
    // ==================== COMBINATION OPERATORS ====================
    
    /**
     * Executes this Completable, then the next Completable sequentially.
     * 
     * <p>Example:
     * <pre>{@code
     * Completable.fromAction(() -> System.out.println("First"))
     *     .andThen(Completable.fromAction(() -> System.out.println("Second")))
     *     .subscribe(() -> System.out.println("Done!"));
     * // Output:
     * // First
     * // Second
     * // Done!
     * }</pre>
     * 
     * @param next the Completable to execute after this one
     * @return a Completable that executes both sequentially
     */
    public final Completable andThen(Completable next) {
        return new Completable() {
            @Override
            public void subscribe(CompletableObserver observer) {
                Completable.this.subscribe(new CompletableObserver() {
                    @Override
                    public void onComplete() {
                        next.subscribe(observer);
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
     * Ejecuta este Completable, y luego el Observable.
     */
    public final <T> Observable<T> andThen(Observable<T> next) {
        return new Observable<T>() {
            @Override
            public void subscribe(Observer<? super T> observer) {
                Completable.this.subscribe(new CompletableObserver() {
                    @Override
                    public void onComplete() {
                        next.subscribe(observer);
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
     * Ejecuta este Completable, y luego el Single.
     */
    public final <T> Single<T> andThen(Single<T> next) {
        return new Single<T>() {
            @Override
            public void subscribe(SingleObserver<T> observer) {
                Completable.this.subscribe(new CompletableObserver() {
                    @Override
                    public void onComplete() {
                        next.subscribe(observer);
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
     * Ejecuta este Completable, y luego el Maybe.
     */
    public final <T> Maybe<T> andThen(Maybe<T> next) {
        return new Maybe<T>() {
            @Override
            public void subscribe(MaybeObserver<T> observer) {
                Completable.this.subscribe(new CompletableObserver() {
                    @Override
                    public void onComplete() {
                        next.subscribe(observer);
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
     * Merges multiple Completables, executing them concurrently.
     * 
     * <p>All Completables are subscribed to immediately. The result completes
     * when all sources complete, or emits an error if any source errors.
     * 
     * <p>Example:
     * <pre>{@code
     * Completable task1 = Completable.fromAction(() -> saveUser());
     * Completable task2 = Completable.fromAction(() -> saveOrder());
     * Completable task3 = Completable.fromAction(() -> sendEmail());
     * 
     * Completable.merge(task1, task2, task3)
     *     .subscribe(() -> System.out.println("All tasks complete!"));
     * }</pre>
     * 
     * @param sources the Completables to merge
     * @return a Completable that completes when all sources complete
     */
    public static Completable merge(Completable... sources) {
        return new Completable() {
            @Override
            public void subscribe(CompletableObserver observer) {
                if (sources.length == 0) {
                    observer.onComplete();
                    return;
                }
                
                int[] remaining = {sources.length};
                boolean[] errored = {false};
                
                for (Completable source : sources) {
                    source.subscribe(new CompletableObserver() {
                        @Override
                        public void onComplete() {
                            synchronized (remaining) {
                                if (errored[0]) return;
                                remaining[0]--;
                                if (remaining[0] == 0) {
                                    observer.onComplete();
                                }
                            }
                        }
                        
                        @Override
                        public void onError(Throwable error) {
                            synchronized (remaining) {
                                if (!errored[0]) {
                                    errored[0] = true;
                                    observer.onError(error);
                                }
                            }
                        }
                    });
                }
            }
        };
    }
    
    /**
     * Concatenates multiple Completables, executing them one after another.
     * 
     * <p>Each Completable is subscribed to only after the previous one completes.
     * 
     * <p>Example:
     * <pre>{@code
     * Completable step1 = Completable.fromAction(() -> initDatabase());
     * Completable step2 = Completable.fromAction(() -> loadConfig());
     * Completable step3 = Completable.fromAction(() -> startServer());
     * 
     * Completable.concat(step1, step2, step3)
     *     .subscribe(() -> System.out.println("All steps complete!"));
     * }</pre>
     * 
     * @param sources the Completables to execute sequentially
     * @return a Completable that executes all sources in order
     */
    public static Completable concat(Completable... sources) {
        return new Completable() {
            @Override
            public void subscribe(CompletableObserver observer) {
                concatInternal(observer, sources, 0);
            }
            
            private void concatInternal(CompletableObserver observer, Completable[] sources, int index) {
                if (index >= sources.length) {
                    observer.onComplete();
                    return;
                }
                
                sources[index].subscribe(new CompletableObserver() {
                    @Override
                    public void onComplete() {
                        concatInternal(observer, sources, index + 1);
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
     * If an error occurs, resumes with another Completable.
     * 
     * <p>Example:
     * <pre>{@code
     * Completable.error(new RuntimeException("Failed"))
     *     .onErrorResumeNext(error -> 
     *         Completable.fromAction(() -> System.out.println("Fallback"))
     *     )
     *     .subscribe(() -> System.out.println("Complete!"));
     * // Output:
     * // Fallback
     * // Complete!
     * }</pre>
     * 
     * @param resumeFunction function that returns a fallback Completable
     * @return a Completable that resumes with an alternative on error
     */
    public final Completable onErrorResumeNext(Function<Throwable, Completable> resumeFunction) {
        return new Completable() {
            @Override
            public void subscribe(CompletableObserver observer) {
                Completable.this.subscribe(new CompletableObserver() {
                    @Override
                    public void onComplete() {
                        observer.onComplete();
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        try {
                            Completable fallbackCompletable = resumeFunction.apply(error);
                            fallbackCompletable.subscribe(observer);
                        } catch (Exception e) {
                            observer.onError(e);
                        }
                    }
                });
            }
        };
    }
    
    /**
     * If an error occurs, completes successfully instead.
     * 
     * <p>Example:
     * <pre>{@code
     * Completable.error(new RuntimeException("Failed"))
     *     .onErrorComplete()
     *     .subscribe(() -> System.out.println("Complete!"));
     * // Output: Complete!
     * }</pre>
     * 
     * @return a Completable that completes on error
     */
    public final Completable onErrorComplete() {
        return new Completable() {
            @Override
            public void subscribe(CompletableObserver observer) {
                Completable.this.subscribe(new CompletableObserver() {
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
    
    /**
     * Reintenta en caso de error.
     */
    public final Completable retry(int times) {
        return new Completable() {
            @Override
            public void subscribe(CompletableObserver observer) {
                retryInternal(observer, times);
            }
            
            private void retryInternal(CompletableObserver observer, int attemptsLeft) {
                Completable.this.subscribe(new CompletableObserver() {
                    @Override
                    public void onComplete() {
                        observer.onComplete();
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
     * Ejecuta una acción cuando completa (efecto lateral).
     */
    public final Completable doOnComplete(Runnable onComplete) {
        return new Completable() {
            @Override
            public void subscribe(CompletableObserver observer) {
                Completable.this.subscribe(new CompletableObserver() {
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
     * Ejecuta una acción cuando ocurre un error (efecto lateral).
     */
    public final Completable doOnError(Consumer<Throwable> onError) {
        return new Completable() {
            @Override
            public void subscribe(CompletableObserver observer) {
                Completable.this.subscribe(new CompletableObserver() {
                    @Override
                    public void onComplete() {
                        observer.onComplete();
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
     * Delay: retrasa el completado.
     */
    public final Completable delay(long delay, TimeUnit unit) {
        return new Completable() {
            @Override
            public void subscribe(CompletableObserver observer) {
                Completable.this.subscribe(new CompletableObserver() {
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
    public final Completable subscribeOn(Scheduler scheduler) {
        return new Completable() {
            @Override
            public void subscribe(CompletableObserver observer) {
                scheduler.scheduleDirect(() -> Completable.this.subscribe(observer));
            }
        };
    }
    
    /**
     * Especifica el scheduler en el que se observan los resultados.
     */
    public final Completable observeOn(Scheduler scheduler) {
        return new Completable() {
            @Override
            public void subscribe(CompletableObserver observer) {
                Completable.this.subscribe(new CompletableObserver() {
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
     * Converts this Completable to an Observable that completes without emitting values.
     * 
     * <p>Example:
     * <pre>{@code
     * Completable.complete()
     *     .toObservable()
     *     .subscribe(
     *         value -> {}, // Never called
     *         error -> {},
     *         () -> System.out.println("Complete!")
     *     );
     * // Output: Complete!
     * }</pre>
     * 
     * @param <T> the type parameter
     * @return an Observable that completes without emitting
     */
    public final <T> Observable<T> toObservable() {
        return new Observable<T>() {
            @Override
            public void subscribe(Observer<? super T> observer) {
                Completable.this.subscribe(new CompletableObserver() {
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
    
    // ==================== HELPER CLASSES ====================
    
    static class DisposableCompletableObserver implements CompletableObserver, Disposable {
        private final Runnable onComplete;
        private final Consumer<Throwable> onError;
        private volatile boolean disposed;
        
        DisposableCompletableObserver(Runnable onComplete, Consumer<Throwable> onError) {
            this.onComplete = onComplete;
            this.onError = onError;
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
