package com.reactive.core;

import com.reactive.schedulers.Schedulers;
import com.reactive.backpressure.BackpressureStrategy;
import com.reactive.observables.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

/**
 * Represents a stream of 0 to N items that can be observed.
 * 
 * <p>Observable is the core reactive type that emits a sequence of values over time.
 * It can emit zero or more items followed by either a completion signal or an error.
 * 
 * <h2>Lifecycle</h2>
 * <p>An Observable follows this lifecycle:
 * <ol>
 *   <li><b>Subscription</b>: Observer subscribes to the Observable</li>
 *   <li><b>Emission</b>: Observable emits 0 to N items via onNext()</li>
 *   <li><b>Termination</b>: Observable completes via onComplete() or onError()</li>
 * </ol>
 * 
 * <h2>Threading</h2>
 * <p>By default, Observables are synchronous and execute on the calling thread.
 * Use {@link #subscribeOn(Scheduler)} to specify subscription thread and
 * {@link #observeOn(Scheduler)} to specify observation thread.
 * 
 * <h2>Error Handling</h2>
 * <p>Errors propagate down the chain and terminate the sequence.
 * Use operators like {@link #onErrorReturn}, {@link #onErrorResumeNext},
 * or {@link #retry} to handle errors gracefully.
 * 
 * <h2>Example Usage</h2>
 * <pre>{@code
 * Observable.just(1, 2, 3, 4, 5)
 *     .filter(x -> x % 2 == 0)
 *     .map(x -> x * 10)
 *     .subscribe(
 *         value -> System.out.println("Value: " + value),
 *         error -> System.err.println("Error: " + error),
 *         () -> System.out.println("Complete!")
 *     );
 * // Output:
 * // Value: 20
 * // Value: 40
 * // Complete!
 * }</pre>
 * 
 * @param <T> the type of items emitted by this Observable
 * @see Single
 * @see Maybe
 * @see Completable
 * @see Observer
 */
public abstract class Observable<T> implements ObservableSource<T> {
    
    /**
     * Subscribes an Observer to this Observable.
     * 
     * <p>This is the primary subscription method. The Observer will receive
     * notifications through its onNext(), onError(), and onComplete() callbacks.
     * 
     * <p><b>Note:</b> This method doesn't return a Disposable. Use the lambda-based
     * subscribe methods if you need to dispose of the subscription.
     * 
     * @param observer the Observer to subscribe
     * @see #subscribe(Consumer)
     * @see #subscribe(Consumer, Consumer)
     * @see #subscribe(Consumer, Consumer, Runnable)
     */
    public abstract void subscribe(Observer<? super T> observer);
    
    /**
     * Subscribes with only an onNext callback.
     * 
     * <p>Errors will be printed to System.err. Use {@link #subscribe(Consumer, Consumer)}
     * if you need to handle errors.
     * 
     * <p>Example:
     * <pre>{@code
     * Disposable d = Observable.just(1, 2, 3)
     *     .subscribe(value -> System.out.println(value));
     * }</pre>
     * 
     * @param onNext callback for each emitted item
     * @return a Disposable to cancel the subscription
     */
    public final Disposable subscribe(Consumer<T> onNext) {
        DisposableObserver<T> observer = new DisposableObserver<>(onNext, Throwable::printStackTrace, () -> {});
        subscribe(observer);
        return observer;
    }
    
    /**
     * Subscribes with onNext and onError callbacks.
     * 
     * <p>Example:
     * <pre>{@code
     * Disposable d = Observable.just(1, 2, 3)
     *     .subscribe(
     *         value -> System.out.println(value),
     *         error -> System.err.println("Error: " + error)
     *     );
     * }</pre>
     * 
     * @param onNext callback for each emitted item
     * @param onError callback for errors
     * @return a Disposable to cancel the subscription
     */
    public final Disposable subscribe(Consumer<T> onNext, Consumer<Throwable> onError) {
        DisposableObserver<T> observer = new DisposableObserver<>(onNext, onError, () -> {});
        subscribe(observer);
        return observer;
    }
    
    /**
     * Subscribes with onNext, onError, and onComplete callbacks.
     * 
     * <p>This is the most complete subscription method using lambdas.
     * 
     * <p>Example:
     * <pre>{@code
     * Disposable d = Observable.just(1, 2, 3)
     *     .subscribe(
     *         value -> System.out.println("Value: " + value),
     *         error -> System.err.println("Error: " + error),
     *         () -> System.out.println("Complete!")
     *     );
     * }</pre>
     * 
     * @param onNext callback for each emitted item
     * @param onError callback for errors
     * @param onComplete callback when the sequence completes
     * @return a Disposable to cancel the subscription
     */
    public final Disposable subscribe(Consumer<T> onNext, Consumer<Throwable> onError, Runnable onComplete) {
        DisposableObserver<T> observer = new DisposableObserver<>(onNext, onError, onComplete);
        subscribe(observer);
        return observer;
    }
    
    // ============ Creation Operators ============
    
    /**
     * Creates an Observable that emits the specified items and then completes.
     * 
     * <p>The items are emitted synchronously on subscription.
     * 
     * <p>Example:
     * <pre>{@code
     * Observable.just(1, 2, 3)
     *     .subscribe(System.out::println);
     * // Emits: 1, 2, 3, then completes
     * }</pre>
     * 
     * @param <T> the type of items
     * @param items the items to emit
     * @return an Observable that emits the specified items
     * @see #fromArray(Object[])
     * @see #fromIterable(Iterable)
     */
    public static <T> Observable<T> just(T... items) {
        return new Observable<T>() {
            @Override
            public void subscribe(Observer<? super T> observer) {
                observer.onSubscribe(Disposable.EMPTY);
                for (T item : items) {
                    observer.onNext(item);
                }
                observer.onComplete();
            }
        };
    }
    
    /**
     * Creates an Observable that emits all items from the given array.
     * 
     * <p>Identical to {@link #just(Object[])} but more explicit about the source.
     * 
     * <p>Example:
     * <pre>{@code
     * String[] names = {"Alice", "Bob", "Charlie"};
     * Observable.fromArray(names)
     *     .subscribe(System.out::println);
     * }</pre>
     * 
     * @param <T> the type of items
     * @param array the array of items to emit
     * @return an Observable that emits all items from the array
     */
    public static <T> Observable<T> fromArray(T[] array) {
        return just(array);
    }
    
    /**
     * Creates an Observable that emits all items from the given Iterable.
     * 
     * <p>Works with any Iterable including List, Set, Queue, etc.
     * 
     * <p>Example:
     * <pre>{@code
     * List<String> list = Arrays.asList("A", "B", "C");
     * Observable.fromIterable(list)
     *     .subscribe(System.out::println);
     * }</pre>
     * 
     * @param <T> the type of items
     * @param iterable the Iterable to convert
     * @return an Observable that emits all items from the Iterable
     */
    public static <T> Observable<T> fromIterable(Iterable<T> iterable) {
        return new Observable<T>() {
            @Override
            public void subscribe(Observer<? super T> observer) {
                observer.onSubscribe(Disposable.EMPTY);
                for (T item : iterable) {
                    observer.onNext(item);
                }
                observer.onComplete();
            }
        };
    }
    
    /**
     * Creates an Observable that emits the result of calling a Callable.
     * 
     * <p>The Callable is invoked on subscription. Useful for lazy evaluation
     * or wrapping blocking operations.
     * 
     * <p>Example:
     * <pre>{@code
     * Observable.fromCallable(() -> {
     *     return expensiveComputation();
     * }).subscribe(result -> System.out.println(result));
     * }</pre>
     * 
     * @param <T> the type of the result
     * @param callable the Callable to invoke
     * @return an Observable that emits the Callable's result
     */
    public static <T> Observable<T> fromCallable(Callable<T> callable) {
        return new Observable<T>() {
            @Override
            public void subscribe(Observer<? super T> observer) {
                observer.onSubscribe(Disposable.EMPTY);
                try {
                    T result = callable.call();
                    observer.onNext(result);
                    observer.onComplete();
                } catch (Exception e) {
                    observer.onError(e);
                }
            }
        };
    }
    
    /**
     * Creates an Observable with full control over emissions.
     * 
     * <p>This is the most flexible creation method. Use the provided Emitter
     * to manually emit items, errors, or completion.
     * 
     * <p><b>Important:</b> Check {@link Emitter#isDisposed()} before emitting
     * to avoid unnecessary work if the subscription was cancelled.
     * 
     * <p>Example:
     * <pre>{@code
     * Observable.create(emitter -> {
     *     emitter.onNext("Hello");
     *     emitter.onNext("World");
     *     emitter.onComplete();
     * }).subscribe(System.out::println);
     * }</pre>
     * 
     * @param <T> the type of items to emit
     * @param source function that receives an Emitter and uses it to emit items
     * @return an Observable with custom emission logic
     * @see Emitter
     */
    public static <T> Observable<T> create(Consumer<Emitter<T>> source) {
        return new Observable<T>() {
            @Override
            public void subscribe(Observer<? super T> observer) {
                EmitterImpl<T> emitter = new EmitterImpl<>(observer);
                observer.onSubscribe(emitter);
                try {
                    source.accept(emitter);
                } catch (Exception e) {
                    emitter.onError(e);
                }
            }
        };
    }
    
    /**
     * Defers Observable creation until subscription time.
     * 
     * <p>Useful when you want to create a fresh Observable for each subscriber,
     * or when the Observable depends on state that might change.
     * 
     * <p>Example:
     * <pre>{@code
     * Observable<Long> deferred = Observable.defer(() -> 
     *     Observable.just(System.currentTimeMillis())
     * );
     * 
     * deferred.subscribe(System.out::println); // Time 1
     * Thread.sleep(1000);
     * deferred.subscribe(System.out::println); // Time 2 (different)
     * }</pre>
     * 
     * @param <T> the type of items
     * @param supplier function that returns an Observable
     * @return an Observable that creates a new source for each subscription
     */
    public static <T> Observable<T> defer(Supplier<Observable<T>> supplier) {
        return new Observable<T>() {
            @Override
            public void subscribe(Observer<? super T> observer) {
                try {
                    Observable<T> observable = supplier.get();
                    observable.subscribe(observer);
                } catch (Exception e) {
                    observer.onError(e);
                }
            }
        };
    }
    
    /**
     * Creates an Observable that completes immediately without emitting items.
     * 
     * <p>Useful for representing "no data" scenarios or as a placeholder.
     * 
     * <p>Example:
     * <pre>{@code
     * Observable.<String>empty()
     *     .subscribe(
     *         value -> System.out.println("Value: " + value),
     *         error -> System.err.println("Error"),
     *         () -> System.out.println("Complete!")
     *     );
     * // Output: Complete!
     * }</pre>
     * 
     * @param <T> the type parameter
     * @return an Observable that completes without emitting
     * @see #never()
     */
    public static <T> Observable<T> empty() {
        return new Observable<T>() {
            @Override
            public void subscribe(Observer<? super T> observer) {
                observer.onSubscribe(Disposable.EMPTY);
                observer.onComplete();
            }
        };
    }
    
    /**
     * Creates an Observable that never emits anything and never completes.
     * 
     * <p>Useful for testing or representing infinite wait scenarios.
     * 
     * <p>Example:
     * <pre>{@code
     * Observable.<String>never()
     *     .timeout(1, TimeUnit.SECONDS)
     *     .subscribe(
     *         value -> System.out.println(value),
     *         error -> System.err.println("Timeout!") // This will be called
     *     );
     * }</pre>
     * 
     * @param <T> the type parameter
     * @return an Observable that never emits or completes
     * @see #empty()
     */
    public static <T> Observable<T> never() {
        return new Observable<T>() {
            @Override
            public void subscribe(Observer<? super T> observer) {
                observer.onSubscribe(Disposable.EMPTY);
                // Never emits
            }
        };
    }
    
    /**
     * Creates an Observable that immediately emits an error.
     * 
     * <p>Useful for error scenarios or testing error handling.
     * 
     * <p>Example:
     * <pre>{@code
     * Observable.<String>error(new RuntimeException("Failed"))
     *     .subscribe(
     *         value -> System.out.println(value),
     *         error -> System.err.println("Error: " + error.getMessage())
     *     );
     * // Output: Error: Failed
     * }</pre>
     * 
     * @param <T> the type parameter
     * @param error the error to emit
     * @return an Observable that emits an error
     */
    public static <T> Observable<T> error(Throwable error) {
        return new Observable<T>() {
            @Override
            public void subscribe(Observer<? super T> observer) {
                observer.onSubscribe(Disposable.EMPTY);
                observer.onError(error);
            }
        };
    }
    
    /**
     * Creates an Observable that emits sequential numbers at specified intervals.
     * 
     * <p>Starts emitting after the specified period and continues indefinitely.
     * Equivalent to {@code interval(period, period, unit)}.
     * 
     * <p>Example:
     * <pre>{@code
     * Observable.interval(1, TimeUnit.SECONDS)
     *     .take(5)
     *     .subscribe(value -> System.out.println("Tick: " + value));
     * // Emits: 0, 1, 2, 3, 4 (one per second)
     * }</pre>
     * 
     * @param period the interval between emissions
     * @param unit the time unit of the period
     * @return an Observable that emits sequential numbers periodically
     * @see #timer(long, TimeUnit)
     */
    public static Observable<Long> interval(long period, TimeUnit unit) {
        return interval(period, period, unit);
    }
    
    public static Observable<Long> interval(long period, TimeUnit unit, Scheduler scheduler) {
        return interval(period, period, unit, scheduler);
    }
    
    public static Observable<Long> interval(long initialDelay, long period, TimeUnit unit) {
        return interval(initialDelay, period, unit, Schedulers.computation());
    }
    
    public static Observable<Long> interval(long initialDelay, long period, TimeUnit unit, Scheduler scheduler) {
        return new Observable<Long>() {
            @Override
            public void subscribe(Observer<? super Long> observer) {
                AtomicLong counter = new AtomicLong(0);
                scheduler.schedulePeriodic(() -> {
                    observer.onNext(counter.getAndIncrement());
                }, initialDelay, period, unit);
            }
        };
    }
    
    public static Observable<Long> timer(long delay, TimeUnit unit) {
        return timer(delay, unit, Schedulers.computation());
    }
    
    public static Observable<Long> timer(long delay, TimeUnit unit, Scheduler scheduler) {
        return new Observable<Long>() {
            @Override
            public void subscribe(Observer<? super Long> observer) {
                scheduler.scheduleDirect(() -> {
                    observer.onNext(0L);
                    observer.onComplete();
                }, delay, unit);
            }
        };
    }
    
    /**
     * Creates an Observable that emits a range of sequential integers.
     * 
     * <p>Emits {@code count} integers starting from {@code start}.
     * 
     * <p>Example:
     * <pre>{@code
     * Observable.range(5, 3)
     *     .subscribe(System.out::println);
     * // Emits: 5, 6, 7
     * }</pre>
     * 
     * @param start the starting value
     * @param count the number of sequential integers to emit
     * @return an Observable that emits a range of integers
     */
    public static Observable<Integer> range(int start, int count) {
        return new Observable<Integer>() {
            @Override
            public void subscribe(Observer<? super Integer> observer) {
                observer.onSubscribe(Disposable.EMPTY);
                for (int i = 0; i < count; i++) {
                    observer.onNext(start + i);
                }
                observer.onComplete();
            }
        };
    }
    
    // ============ Transformation Operators ============
    
    /**
     * Transforms each emitted item by applying a function.
     * 
     * <p>This is one of the most commonly used operators for transforming data.
     * 
     * <p>Example:
     * <pre>{@code
     * Observable.just(1, 2, 3, 4, 5)
     *     .map(x -> x * 10)
     *     .subscribe(System.out::println);
     * // Emits: 10, 20, 30, 40, 50
     * }</pre>
     * 
     * @param <R> the type of the transformed items
     * @param mapper function to transform each item
     * @return an Observable that emits transformed items
     * @see #flatMap(Function)
     * @see #scan(Object, BiFunction)
     */
    public final <R> Observable<R> map(Function<T, R> mapper) {
        return new Observable<R>() {
            @Override
            public void subscribe(Observer<? super R> observer) {
                Observable.this.subscribe(new Observer<T>() {
                    @Override
                    public void onNext(T value) {
                        try {
                            R result = mapper.apply(value);
                            observer.onNext(result);
                        } catch (Exception e) {
                            observer.onError(e);
                        }
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
     * Transforms each item into an Observable and flattens all emissions.
     * 
     * <p>Also known as "mergeMap". Items from all inner Observables are merged
     * concurrently, so the order is not guaranteed.
     * 
     * <p>Use {@link #concatMap} if you need to preserve order.
     * 
     * <p>Example:
     * <pre>{@code
     * Observable.just(1, 2, 3)
     *     .flatMap(x -> Observable.just(x, x * 10))
     *     .subscribe(System.out::println);
     * // Emits: 1, 10, 2, 20, 3, 30 (order may vary)
     * }</pre>
     * 
     * @param <R> the type of items emitted by the inner Observables
     * @param mapper function that transforms each item into an Observable
     * @return an Observable that emits the merged output of all inner Observables
     * @see #concatMap(Function)
     * @see #switchMap(Function)
     */
    public final <R> Observable<R> flatMap(Function<T, Observable<R>> mapper) {
        return new Observable<R>() {
            @Override
            public void subscribe(Observer<? super R> observer) {
                AtomicInteger activeCount = new AtomicInteger(1);
                AtomicBoolean errorOccurred = new AtomicBoolean(false);
                
                Observable.this.subscribe(new Observer<T>() {
                    @Override
                    public void onNext(T value) {
                        if (errorOccurred.get()) return;
                        
                        try {
                            activeCount.incrementAndGet();
                            Observable<R> inner = mapper.apply(value);
                            inner.subscribe(new Observer<R>() {
                                @Override
                                public void onNext(R innerValue) {
                                    if (!errorOccurred.get()) {
                                        observer.onNext(innerValue);
                                    }
                                }
                                
                                @Override
                                public void onError(Throwable error) {
                                    if (errorOccurred.compareAndSet(false, true)) {
                                        observer.onError(error);
                                    }
                                }
                                
                                @Override
                                public void onComplete() {
                                    if (activeCount.decrementAndGet() == 0 && !errorOccurred.get()) {
                                        observer.onComplete();
                                    }
                                }
                            });
                        } catch (Exception e) {
                            if (errorOccurred.compareAndSet(false, true)) {
                                observer.onError(e);
                            }
                        }
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        if (errorOccurred.compareAndSet(false, true)) {
                            observer.onError(error);
                        }
                    }
                    
                    @Override
                    public void onComplete() {
                        if (activeCount.decrementAndGet() == 0 && !errorOccurred.get()) {
                            observer.onComplete();
                        }
                    }
                });
            }
        };
    }
    
    /**
     * Transforms each item into an Observable and concatenates emissions sequentially.
     * 
     * <p>Unlike {@link #flatMap}, this operator preserves order by subscribing to
     * inner Observables one at a time and waiting for each to complete before
     * subscribing to the next.
     * 
     * <p>Example:
     * <pre>{@code
     * Observable.just(1, 2, 3)
     *     .concatMap(x -> Observable.just(x, x * 10))
     *     .subscribe(System.out::println);
     * // Emits: 1, 10, 2, 20, 3, 30 (guaranteed order)
     * }</pre>
     * 
     * @param <R> the type of items emitted by the inner Observables
     * @param mapper function that transforms each item into an Observable
     * @return an Observable that emits the concatenated output sequentially
     * @see #flatMap(Function)
     * @see #switchMap(Function)
     */
    public final <R> Observable<R> concatMap(Function<T, Observable<R>> mapper) {
        return new Observable<R>() {
            @Override
            public void subscribe(Observer<? super R> observer) {
                Queue<Observable<R>> queue = new ConcurrentLinkedQueue<>();
                AtomicBoolean processing = new AtomicBoolean(false);
                AtomicBoolean completed = new AtomicBoolean(false);
                
                Runnable processNext = new Runnable() {
                    @Override
                    public void run() {
                        if (processing.get()) return;
                        processing.set(true);
                        
                        Observable<R> next = queue.poll();
                        if (next != null) {
                            next.subscribe(new Observer<R>() {
                                @Override
                                public void onNext(R value) {
                                    observer.onNext(value);
                                }
                                
                                @Override
                                public void onError(Throwable error) {
                                    observer.onError(error);
                                }
                                
                                @Override
                                public void onComplete() {
                                    processing.set(false);
                                    run();
                                }
                            });
                        } else {
                            processing.set(false);
                            if (completed.get()) {
                                observer.onComplete();
                            }
                        }
                    }
                };
                
                Observable.this.subscribe(new Observer<T>() {
                    @Override
                    public void onNext(T value) {
                        try {
                            queue.offer(mapper.apply(value));
                            processNext.run();
                        } catch (Exception e) {
                            observer.onError(e);
                        }
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        observer.onError(error);
                    }
                    
                    @Override
                    public void onComplete() {
                        completed.set(true);
                        if (!processing.get() && queue.isEmpty()) {
                            observer.onComplete();
                        }
                    }
                });
            }
        };
    }
    
    /**
     * Transforms each item into an Observable, cancelling the previous inner Observable.
     * 
     * <p>Only emissions from the most recent inner Observable are propagated.
     * Earlier inner Observables are cancelled when a new item arrives.
     * 
     * <p>Useful for scenarios like search-as-you-type where you only care about
     * the latest request.
     * 
     * <p>Example:
     * <pre>{@code
     * Observable.just(1, 2, 3)
     *     .switchMap(x -> Observable.just(x, x * 10).delay(100, MILLISECONDS))
     *     .subscribe(System.out::println);
     * // Only emits values from the last inner Observable
     * }</pre>
     * 
     * @param <R> the type of items emitted by the inner Observables
     * @param mapper function that transforms each item into an Observable
     * @return an Observable that emits from only the latest inner Observable
     * @see #flatMap(Function)
     * @see #concatMap(Function)
     */
    public final <R> Observable<R> switchMap(Function<T, Observable<R>> mapper) {
        return new Observable<R>() {
            @Override
            public void subscribe(Observer<? super R> observer) {
                AtomicReference<Disposable> currentSubscription = new AtomicReference<>();
                AtomicBoolean completed = new AtomicBoolean(false);
                
                Observable.this.subscribe(new Observer<T>() {
                    @Override
                    public void onNext(T value) {
                        Disposable previous = currentSubscription.get();
                        if (previous != null) {
                            previous.dispose();
                        }
                        
                        try {
                            Observable<R> inner = mapper.apply(value);
                            Disposable subscription = inner.subscribe(
                                observer::onNext,
                                observer::onError,
                                () -> {
                                    if (completed.get()) {
                                        observer.onComplete();
                                    }
                                }
                            );
                            currentSubscription.set(subscription);
                        } catch (Exception e) {
                            observer.onError(e);
                        }
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        observer.onError(error);
                    }
                    
                    @Override
                    public void onComplete() {
                        completed.set(true);
                        if (currentSubscription.get() == null) {
                            observer.onComplete();
                        }
                    }
                });
            }
        };
    }
    
    /**
     * Applies an accumulator function over the sequence and emits each intermediate result.
     * 
     * <p>Similar to {@link #reduce} but emits all intermediate accumulations,
     * not just the final result.
     * 
     * <p>Example:
     * <pre>{@code
     * Observable.just(1, 2, 3, 4, 5)
     *     .scan(0, (acc, x) -> acc + x)
     *     .subscribe(System.out::println);
     * // Emits: 0, 1, 3, 6, 10, 15
     * }</pre>
     * 
     * @param <R> the type of the accumulated value
     * @param initialValue the initial accumulator value
     * @param accumulator function to combine accumulator with each item
     * @return an Observable that emits all intermediate accumulated values
     * @see #reduce(BiFunction)
     */
    public final <R> Observable<R> scan(R initialValue, BiFunction<R, T, R> accumulator) {
        return new Observable<R>() {
            @Override
            public void subscribe(Observer<? super R> observer) {
                AtomicReference<R> accumRef = new AtomicReference<>(initialValue);
                
                Observable.this.subscribe(new Observer<T>() {
                    @Override
                    public void onNext(T value) {
                        try {
                            R newValue = accumulator.apply(accumRef.get(), value);
                            accumRef.set(newValue);
                            observer.onNext(newValue);
                        } catch (Exception e) {
                            observer.onError(e);
                        }
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
    
    // ============ Filtering Operators ============
    
    /**
     * Filters items emitted by an Observable by only emitting those that satisfy a predicate.
     * 
     * <p>Example:
     * <pre>{@code
     * Observable.range(1, 10)
     *     .filter(x -> x % 2 == 0)
     *     .subscribe(System.out::println);
     * // Emits: 2, 4, 6, 8, 10
     * }</pre>
     * 
     * @param predicate a function to test each item
     * @return an Observable that emits only items that pass the predicate test
     * @see #take(long)
     * @see #skip(long)
     */
    public final Observable<T> filter(Predicate<T> predicate) {
        return new Observable<T>() {
            @Override
            public void subscribe(Observer<? super T> observer) {
                Observable.this.subscribe(new Observer<T>() {
                    @Override
                    public void onNext(T value) {
                        try {
                            if (predicate.test(value)) {
                                observer.onNext(value);
                            }
                        } catch (Exception e) {
                            observer.onError(e);
                        }
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
     * Emits only the first {@code count} items from the source.
     * 
     * <p>Completes after emitting the specified number of items or when
     * the source completes, whichever comes first.
     * 
     * <p>Example:
     * <pre>{@code
     * Observable.just(1, 2, 3, 4, 5)
     *     .take(3)
     *     .subscribe(System.out::println);
     * // Emits: 1, 2, 3
     * }</pre>
     * 
     * @param count the number of items to emit
     * @return an Observable that emits at most {@code count} items
     * @see #skip(long)
     * @see #takeLast(int)
     */
    public final Observable<T> take(long count) {
        return new Observable<T>() {
            @Override
            public void subscribe(Observer<? super T> observer) {
                AtomicLong taken = new AtomicLong(0);
                
                Observable.this.subscribe(new Observer<T>() {
                    @Override
                    public void onNext(T value) {
                        long current = taken.getAndIncrement();
                        if (current < count) {
                            observer.onNext(value);
                            if (current + 1 == count) {
                                observer.onComplete();
                            }
                        }
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
     * Skips the first {@code count} items and emits the rest.
     * 
     * <p>Example:
     * <pre>{@code
     * Observable.just(1, 2, 3, 4, 5)
     *     .skip(2)
     *     .subscribe(System.out::println);
     * // Emits: 3, 4, 5
     * }</pre>
     * 
     * @param count the number of items to skip
     * @return an Observable that skips the first {@code count} items
     * @see #take(long)
     * @see #skipLast(int)
     */
    public final Observable<T> skip(long count) {
        return new Observable<T>() {
            @Override
            public void subscribe(Observer<? super T> observer) {
                AtomicLong skipped = new AtomicLong(0);
                
                Observable.this.subscribe(new Observer<T>() {
                    @Override
                    public void onNext(T value) {
                        if (skipped.getAndIncrement() >= count) {
                            observer.onNext(value);
                        }
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
     * Emits only distinct items by using equality comparison.
     * 
     * <p>Items are compared using {@link Object#equals(Object)}.
     * All seen items are kept in memory, so use with caution on infinite streams.
     * 
     * <p>Example:
     * <pre>{@code
     * Observable.just(1, 2, 2, 3, 1, 4)
     *     .distinct()
     *     .subscribe(System.out::println);
     * // Emits: 1, 2, 3, 4
     * }</pre>
     * 
     * @return an Observable that emits only distinct items
     * @see #distinctUntilChanged()
     */
    public final Observable<T> distinct() {
        return new Observable<T>() {
            @Override
            public void subscribe(Observer<? super T> observer) {
                Set<T> seen = ConcurrentHashMap.newKeySet();
                
                Observable.this.subscribe(new Observer<T>() {
                    @Override
                    public void onNext(T value) {
                        if (seen.add(value)) {
                            observer.onNext(value);
                        }
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
     * Emits items only when they are different from the previous item.
     * 
     * <p>Unlike {@link #distinct()}, this only compares consecutive items
     * and doesn't keep all items in memory.
     * 
     * <p>Example:
     * <pre>{@code
     * Observable.just(1, 1, 2, 2, 2, 3, 1, 1)
     *     .distinctUntilChanged()
     *     .subscribe(System.out::println);
     * // Emits: 1, 2, 3, 1
     * }</pre>
     * 
     * @return an Observable that emits items only when different from the previous
     * @see #distinct()
     */
    public final Observable<T> distinctUntilChanged() {
        return new Observable<T>() {
            @Override
            public void subscribe(Observer<? super T> observer) {
                AtomicReference<T> lastValue = new AtomicReference<>();
                
                Observable.this.subscribe(new Observer<T>() {
                    @Override
                    public void onNext(T value) {
                        T last = lastValue.get();
                        if (last == null || !last.equals(value)) {
                            lastValue.set(value);
                            observer.onNext(value);
                        }
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
    
    // ============ NEW: Temporal Operators ============
    
    /**
     * Divides the observable into windows of specified size.
     * Each window is emitted as a separate Observable.
     */
    public final Observable<Observable<T>> window(int count) {
        return new Observable<Observable<T>>() {
            @Override
            public void subscribe(Observer<? super Observable<T>> observer) {
                AtomicInteger counter = new AtomicInteger(0);
                List<Observer<T>> currentWindow = Collections.synchronizedList(new ArrayList<>());
                
                Observable.this.subscribe(new Observer<T>() {
                    @Override
                    public void onNext(T value) {
                        if (counter.get() == 0) {
                            Observable<T> windowObservable = Observable.create(emitter -> {
                                currentWindow.add(new Observer<T>() {
                                    @Override
                                    public void onNext(T v) {
                                        emitter.onNext(v);
                                    }
                                    
                                    @Override
                                    public void onError(Throwable error) {
                                        emitter.onError(error);
                                    }
                                    
                                    @Override
                                    public void onComplete() {
                                        emitter.onComplete();
                                    }
                                });
                            });
                            observer.onNext(windowObservable);
                        }
                        
                        for (Observer<T> windowObserver : currentWindow) {
                            windowObserver.onNext(value);
                        }
                        
                        if (counter.incrementAndGet() >= count) {
                            for (Observer<T> windowObserver : currentWindow) {
                                windowObserver.onComplete();
                            }
                            currentWindow.clear();
                            counter.set(0);
                        }
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        for (Observer<T> windowObserver : currentWindow) {
                            windowObserver.onError(error);
                        }
                        observer.onError(error);
                    }
                    
                    @Override
                    public void onComplete() {
                        for (Observer<T> windowObserver : currentWindow) {
                            windowObserver.onComplete();
                        }
                        observer.onComplete();
                    }
                });
            }
        };
    }
    
    /**
     * Divides the observable into windows with skip parameter.
     * If skip < count: overlapping windows
     * If skip > count: gaps between windows
     * If skip == count: same as window(count)
     */
    public final Observable<Observable<T>> window(int count, int skip) {
        return new Observable<Observable<T>>() {
            @Override
            public void subscribe(Observer<? super Observable<T>> observer) {
                List<Subject<T>> windows = Collections.synchronizedList(new ArrayList<>());
                List<Integer> windowStarts = Collections.synchronizedList(new ArrayList<>());
                AtomicInteger index = new AtomicInteger(0);
                
                Observable.this.subscribe(new Observer<T>() {
                    @Override
                    public void onNext(T value) {
                        int currentIndex = index.getAndIncrement();
                        
                        // Start new window?
                        if (currentIndex % skip == 0) {
                            Subject<T> newWindow = PublishSubject.create();
                            windows.add(newWindow);
                            windowStarts.add(currentIndex);
                            observer.onNext(newWindow);
                        }
                        
                        // Add to active windows and close completed ones
                        List<Integer> indicesToRemove = new ArrayList<>();
                        for (int i = 0; i < windows.size(); i++) {
                            int windowStart = windowStarts.get(i);
                            int windowEnd = windowStart + count;
                            
                            // Add value to window if within range
                            if (currentIndex >= windowStart && currentIndex < windowEnd) {
                                windows.get(i).onNext(value);
                            }
                            
                            // Close window if this is the last element
                            if (currentIndex == windowEnd - 1) {
                                windows.get(i).onComplete();
                                indicesToRemove.add(i);
                            }
                        }
                        
                        // Remove completed windows (iterate backwards to avoid index issues)
                        for (int i = indicesToRemove.size() - 1; i >= 0; i--) {
                            int idx = indicesToRemove.get(i);
                            windows.remove(idx);
                            windowStarts.remove(idx);
                        }
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        for (Subject<T> window : windows) {
                            window.onError(error);
                        }
                        observer.onError(error);
                    }
                    
                    @Override
                    public void onComplete() {
                        for (Subject<T> window : windows) {
                            window.onComplete();
                        }
                        observer.onComplete();
                    }
                });
            }
        };
    }
    
    /**
     * Divides the observable into time-based windows.
     */
    public final Observable<Observable<T>> window(long timespan, TimeUnit unit) {
        return window(timespan, unit, Schedulers.computation());
    }
    
    public final Observable<Observable<T>> window(long timespan, TimeUnit unit, Scheduler scheduler) {
        return new Observable<Observable<T>>() {
            @Override
            public void subscribe(Observer<? super Observable<T>> observer) {
                List<Observer<T>> currentWindow = Collections.synchronizedList(new ArrayList<>());
                AtomicBoolean firstWindow = new AtomicBoolean(true);
                
                scheduler.schedulePeriodic(() -> {
                    if (!firstWindow.get()) {
                        for (Observer<T> windowObserver : currentWindow) {
                            windowObserver.onComplete();
                        }
                        currentWindow.clear();
                    }
                    
                    Observable<T> windowObservable = Observable.create(emitter -> {
                        currentWindow.add(new Observer<T>() {
                            @Override
                            public void onNext(T v) {
                                emitter.onNext(v);
                            }
                            
                            @Override
                            public void onError(Throwable error) {
                                emitter.onError(error);
                            }
                            
                            @Override
                            public void onComplete() {
                                emitter.onComplete();
                            }
                        });
                    });
                    observer.onNext(windowObservable);
                    firstWindow.set(false);
                }, 0, timespan, unit);
                
                Observable.this.subscribe(new Observer<T>() {
                    @Override
                    public void onNext(T value) {
                        for (Observer<T> windowObserver : currentWindow) {
                            windowObserver.onNext(value);
                        }
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        for (Observer<T> windowObserver : currentWindow) {
                            windowObserver.onError(error);
                        }
                        observer.onError(error);
                    }
                    
                    @Override
                    public void onComplete() {
                        for (Observer<T> windowObserver : currentWindow) {
                            windowObserver.onComplete();
                        }
                        observer.onComplete();
                    }
                });
            }
        };
    }
    
    /**
     * Emits a value only after a specified timespan has passed without another emission.
     * Useful for handling user input (e.g., search as you type).
     */
    public final Observable<T> debounce(long timeout, TimeUnit unit) {
        return debounce(timeout, unit, Schedulers.computation());
    }
    
    public final Observable<T> debounce(long timeout, TimeUnit unit, Scheduler scheduler) {
        return new com.reactive.operators.time.ObservableDebounce<>(this, timeout, unit, scheduler);
    }
    
    /**
     * Emits the first item, then ignores subsequent items for a specified duration.
     * Useful for preventing multiple rapid clicks.
     */
    public final Observable<T> throttleFirst(long windowDuration, TimeUnit unit) {
        return throttleFirst(windowDuration, unit, Schedulers.computation());
    }
    
    public final Observable<T> throttleFirst(long windowDuration, TimeUnit unit, Scheduler scheduler) {
        return new com.reactive.operators.time.ObservableThrottleFirst<>(this, windowDuration, unit, scheduler);
    }
    
    /**
     * Alias for sample. Emits the most recent item at specified time intervals.
     * Useful for rate limiting while keeping the most recent value.
     */
    public final Observable<T> throttleLast(long intervalDuration, TimeUnit unit) {
        return throttleLast(intervalDuration, unit, Schedulers.computation());
    }
    
    public final Observable<T> throttleLast(long intervalDuration, TimeUnit unit, Scheduler scheduler) {
        return new com.reactive.operators.time.ObservableThrottleLast<>(this, intervalDuration, unit, scheduler);
    }
    
    /**
     * Emits the most recent item at specified time intervals.
     */
    public final Observable<T> sample(long period, TimeUnit unit) {
        return sample(period, unit, Schedulers.computation());
    }
    
    public final Observable<T> sample(long period, TimeUnit unit, Scheduler scheduler) {
        return new com.reactive.operators.time.ObservableSample<>(this, period, unit, scheduler);
    }
    
    // ============ Aggregation Operators ============
    
    /**
     * Reduces items using an accumulator function and emits the final result.
     * 
     * <p>Unlike {@link #scan}, this emits only the final accumulated value,
     * not the intermediate values.
     * 
     * <p>Example:
     * <pre>{@code
     * Observable.just(1, 2, 3, 4, 5)
     *     .reduce((acc, x) -> acc + x)
     *     .subscribe(result -> System.out.println("Sum: " + result));
     * // Emits: Sum: 15
     * }</pre>
     * 
     * @param accumulator function to combine two values
     * @return an Observable that emits the final reduced value
     * @see #scan(Object, BiFunction)
     * @see #reduce(Object, BiFunction)
     */
    public final Observable<T> reduce(BiFunction<T, T, T> accumulator) {
        return new Observable<T>() {
            @Override
            public void subscribe(Observer<? super T> observer) {
                AtomicReference<T> accumRef = new AtomicReference<>();
                AtomicBoolean hasValue = new AtomicBoolean(false);
                
                Observable.this.subscribe(new Observer<T>() {
                    @Override
                    public void onNext(T value) {
                        if (!hasValue.get()) {
                            accumRef.set(value);
                            hasValue.set(true);
                        } else {
                            try {
                                T newValue = accumulator.apply(accumRef.get(), value);
                                accumRef.set(newValue);
                            } catch (Exception e) {
                                observer.onError(e);
                            }
                        }
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        observer.onError(error);
                    }
                    
                    @Override
                    public void onComplete() {
                        if (hasValue.get()) {
                            observer.onNext(accumRef.get());
                        }
                        observer.onComplete();
                    }
                });
            }
        };
    }
    
    /**
     * Reduces items using an accumulator function with an initial seed value.
     */
    public final <R> Observable<R> reduce(R seed, BiFunction<R, T, R> accumulator) {
        return new Observable<R>() {
            @Override
            public void subscribe(Observer<? super R> observer) {
                AtomicReference<R> accumRef = new AtomicReference<>(seed);
                
                Observable.this.subscribe(new Observer<T>() {
                    @Override
                    public void onNext(T value) {
                        try {
                            R newValue = accumulator.apply(accumRef.get(), value);
                            accumRef.set(newValue);
                        } catch (Exception e) {
                            observer.onError(e);
                        }
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        observer.onError(error);
                    }
                    
                    @Override
                    public void onComplete() {
                        observer.onNext(accumRef.get());
                        observer.onComplete();
                    }
                });
            }
        };
    }
    
    /**
     * Collects all items into a List and emits it when the source completes.
     * 
     * <p>Example:
     * <pre>{@code
     * Observable.just(1, 2, 3, 4, 5)
     *     .toList()
     *     .subscribe(list -> System.out.println("List: " + list));
     * // Emits: List: [1, 2, 3, 4, 5]
     * }</pre>
     * 
     * @return an Observable that emits a single List containing all items
     * @see #toSet()
     * @see #toMap(Function, Function)
     */
    public final Observable<List<T>> toList() {
        return new Observable<List<T>>() {
            @Override
            public void subscribe(Observer<? super List<T>> observer) {
                List<T> list = new ArrayList<>();
                
                Observable.this.subscribe(new Observer<T>() {
                    @Override
                    public void onNext(T value) {
                        list.add(value);
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        observer.onError(error);
                    }
                    
                    @Override
                    public void onComplete() {
                        observer.onNext(list);
                        observer.onComplete();
                    }
                });
            }
        };
    }
    
    /**
     * Collects items into a Set, eliminating duplicates.
     */
    public final Observable<Set<T>> toSet() {
        return new Observable<Set<T>>() {
            @Override
            public void subscribe(Observer<? super Set<T>> observer) {
                Set<T> set = new HashSet<>();
                
                Observable.this.subscribe(new Observer<T>() {
                    @Override
                    public void onNext(T value) {
                        set.add(value);
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        observer.onError(error);
                    }
                    
                    @Override
                    public void onComplete() {
                        observer.onNext(set);
                        observer.onComplete();
                    }
                });
            }
        };
    }
    
    /**
     * Collects items into a Map using key and value selectors.
     */
    public final <K, V> Observable<Map<K, V>> toMap(
            Function<T, K> keySelector,
            Function<T, V> valueSelector) {
        return new Observable<Map<K, V>>() {
            @Override
            public void subscribe(Observer<? super Map<K, V>> observer) {
                Map<K, V> map = new HashMap<>();
                
                Observable.this.subscribe(new Observer<T>() {
                    @Override
                    public void onNext(T item) {
                        try {
                            K key = keySelector.apply(item);
                            V value = valueSelector.apply(item);
                            map.put(key, value);
                        } catch (Exception e) {
                            observer.onError(e);
                        }
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        observer.onError(error);
                    }
                    
                    @Override
                    public void onComplete() {
                        observer.onNext(map);
                        observer.onComplete();
                    }
                });
            }
        };
    }
    
    /**
     * Collects items into a Map using only a key selector (value is the item itself).
     */
    public final <K> Observable<Map<K, T>> toMap(Function<T, K> keySelector) {
        return toMap(keySelector, item -> item);
    }
    
    /**
     * Collects items using a custom collector with supplier and accumulator.
     */
    public final <R> Observable<R> collect(
            Supplier<R> collectionSupplier,
            BiConsumer<R, T> collector) {
        return new Observable<R>() {
            @Override
            public void subscribe(Observer<? super R> observer) {
                try {
                    R collection = collectionSupplier.get();
                    
                    Observable.this.subscribe(new Observer<T>() {
                        @Override
                        public void onNext(T value) {
                            try {
                                collector.accept(collection, value);
                            } catch (Exception e) {
                                observer.onError(e);
                            }
                        }
                        
                        @Override
                        public void onError(Throwable error) {
                            observer.onError(error);
                        }
                        
                        @Override
                        public void onComplete() {
                            observer.onNext(collection);
                            observer.onComplete();
                        }
                    });
                } catch (Exception e) {
                    observer.onError(e);
                }
            }
        };
    }
    
    /**
     * Collects items using a Java Stream Collector.
     */
    public final <R, A> Observable<R> collect(java.util.stream.Collector<T, A, R> collector) {
        return new Observable<R>() {
            @Override
            public void subscribe(Observer<? super R> observer) {
                try {
                    A container = collector.supplier().get();
                    BiConsumer<A, T> accumulator = collector.accumulator();
                    
                    Observable.this.subscribe(new Observer<T>() {
                        @Override
                        public void onNext(T value) {
                            try {
                                accumulator.accept(container, value);
                            } catch (Exception e) {
                                observer.onError(e);
                            }
                        }
                        
                        @Override
                        public void onError(Throwable error) {
                            observer.onError(error);
                        }
                        
                        @Override
                        public void onComplete() {
                            try {
                                R result = collector.finisher().apply(container);
                                observer.onNext(result);
                                observer.onComplete();
                            } catch (Exception e) {
                                observer.onError(e);
                            }
                        }
                    });
                } catch (Exception e) {
                    observer.onError(e);
                }
            }
        };
    }
    
    // ============ Combination Operators ============
    
    /**
     * Merges multiple Observables into one by emitting all items concurrently.
     * 
     * <p>Items are emitted as soon as they arrive from any source.
     * Order is not guaranteed.
     * 
     * <p>Example:
     * <pre>{@code
     * Observable<Integer> odds = Observable.just(1, 3, 5);
     * Observable<Integer> evens = Observable.just(2, 4, 6);
     * 
     * Observable.merge(odds, evens)
     *     .subscribe(System.out::println);
     * // Emits all items (order may vary): 1, 2, 3, 4, 5, 6
     * }</pre>
     * 
     * @param <T> the type of items
     * @param sources the Observables to merge
     * @return an Observable that emits all items from all sources
     * @see #zip(Observable, Observable, BiFunction)
     */
    public static <T> Observable<T> merge(Observable<? extends T>... sources) {
        return new Observable<T>() {
            @Override
            public void subscribe(Observer<? super T> observer) {
                AtomicInteger remaining = new AtomicInteger(sources.length);
                AtomicBoolean errorOccurred = new AtomicBoolean(false);
                
                for (Observable<? extends T> source : sources) {
                    source.subscribe(new Observer<T>() {
                        @Override
                        public void onNext(T value) {
                            if (!errorOccurred.get()) {
                                observer.onNext(value);
                            }
                        }
                        
                        @Override
                        public void onError(Throwable error) {
                            if (errorOccurred.compareAndSet(false, true)) {
                                observer.onError(error);
                            }
                        }
                        
                        @Override
                        public void onComplete() {
                            if (remaining.decrementAndGet() == 0 && !errorOccurred.get()) {
                                observer.onComplete();
                            }
                        }
                    });
                }
            }
        };
    }
    
    public static <T1, T2, R> Observable<R> zip(Observable<T1> source1, Observable<T2> source2, 
                                                  BiFunction<T1, T2, R> zipper) {
        return new Observable<R>() {
            @Override
            public void subscribe(Observer<? super R> observer) {
                Queue<T1> queue1 = new ConcurrentLinkedQueue<>();
                Queue<T2> queue2 = new ConcurrentLinkedQueue<>();
                AtomicBoolean completed1 = new AtomicBoolean(false);
                AtomicBoolean completed2 = new AtomicBoolean(false);
                
                Runnable tryZip = () -> {
                    while (!queue1.isEmpty() && !queue2.isEmpty()) {
                        try {
                            R result = zipper.apply(queue1.poll(), queue2.poll());
                            observer.onNext(result);
                        } catch (Exception e) {
                            observer.onError(e);
                            return;
                        }
                    }
                    
                    if ((completed1.get() && queue1.isEmpty()) || 
                        (completed2.get() && queue2.isEmpty())) {
                        observer.onComplete();
                    }
                };
                
                source1.subscribe(new Observer<T1>() {
                    @Override
                    public void onNext(T1 value) {
                        queue1.offer(value);
                        tryZip.run();
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        observer.onError(error);
                    }
                    
                    @Override
                    public void onComplete() {
                        completed1.set(true);
                        tryZip.run();
                    }
                });
                
                source2.subscribe(new Observer<T2>() {
                    @Override
                    public void onNext(T2 value) {
                        queue2.offer(value);
                        tryZip.run();
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        observer.onError(error);
                    }
                    
                    @Override
                    public void onComplete() {
                        completed2.set(true);
                        tryZip.run();
                    }
                });
            }
        };
    }
    
    /**
     * Returns an Observable that emits items that combine this Observable's elements
     * with those of another ObservableSource, using a zipper function.
     * <p>
     * The resulting Observable completes when either source completes.
     * <p>
     * Example:
     * <pre>{@code
     * Observable.just(1, 2, 3)
     *     .zipWith(Observable.just("a", "b", "c"), (num, str) -> num + str)
     *     // emits: "1a", "2b", "3c"
     * }</pre>
     *
     * @param <U> the type of items emitted by the other ObservableSource
     * @param <R> the type of items emitted by the resulting Observable
     * @param other the other ObservableSource to zip with
     * @param zipper the function that combines items from both sources
     * @return an Observable that emits combined items
     * @see #zip(Observable, Observable, BiFunction)
     */
    public final <U, R> Observable<R> zipWith(ObservableSource<? extends U> other, 
                                               BiFunction<? super T, ? super U, ? extends R> zipper) {
        Objects.requireNonNull(other, "other is null");
        Objects.requireNonNull(zipper, "zipper is null");
        Observable<T> source = this;
        return new Observable<R>() {
            @Override
            public void subscribe(Observer<? super R> observer) {
                Queue<T> queue1 = new ConcurrentLinkedQueue<>();
                Queue<U> queue2 = new ConcurrentLinkedQueue<>();
                AtomicBoolean completed1 = new AtomicBoolean(false);
                AtomicBoolean completed2 = new AtomicBoolean(false);
                AtomicBoolean disposed = new AtomicBoolean(false);
                
                Runnable tryZip = () -> {
                    while (!queue1.isEmpty() && !queue2.isEmpty() && !disposed.get()) {
                        try {
                            R result = zipper.apply(queue1.poll(), queue2.poll());
                            observer.onNext(result);
                        } catch (Exception e) {
                            disposed.set(true);
                            observer.onError(e);
                            return;
                        }
                    }
                    
                    if (!disposed.get() && 
                        ((completed1.get() && queue1.isEmpty()) || 
                         (completed2.get() && queue2.isEmpty()))) {
                        disposed.set(true);
                        observer.onComplete();
                    }
                };
                
                source.subscribe(new Observer<T>() {
                    @Override
                    public void onNext(T value) {
                        if (!disposed.get()) {
                            queue1.offer(value);
                            tryZip.run();
                        }
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        if (!disposed.get()) {
                            disposed.set(true);
                            observer.onError(error);
                        }
                    }
                    
                    @Override
                    public void onComplete() {
                        completed1.set(true);
                        tryZip.run();
                    }
                });
                
                other.subscribe(new Observer<U>() {
                    @Override
                    public void onNext(U value) {
                        if (!disposed.get()) {
                            queue2.offer(value);
                            tryZip.run();
                        }
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        if (!disposed.get()) {
                            disposed.set(true);
                            observer.onError(error);
                        }
                    }
                    
                    @Override
                    public void onComplete() {
                        completed2.set(true);
                        tryZip.run();
                    }
                });
            }
        };
    }
    
    public static <T1, T2, R> Observable<R> combineLatest(Observable<T1> source1, Observable<T2> source2,
                                                            BiFunction<T1, T2, R> combiner) {
        return new Observable<R>() {
            @Override
            public void subscribe(Observer<? super R> observer) {
                AtomicReference<T1> latest1 = new AtomicReference<>();
                AtomicReference<T2> latest2 = new AtomicReference<>();
                AtomicBoolean has1 = new AtomicBoolean(false);
                AtomicBoolean has2 = new AtomicBoolean(false);
                
                Runnable tryCombine = () -> {
                    if (has1.get() && has2.get()) {
                        try {
                            R result = combiner.apply(latest1.get(), latest2.get());
                            observer.onNext(result);
                        } catch (Exception e) {
                            observer.onError(e);
                        }
                    }
                };
                
                source1.subscribe(new Observer<T1>() {
                    @Override
                    public void onNext(T1 value) {
                        latest1.set(value);
                        has1.set(true);
                        tryCombine.run();
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        observer.onError(error);
                    }
                    
                    @Override
                    public void onComplete() {
                    }
                });
                
                source2.subscribe(new Observer<T2>() {
                    @Override
                    public void onNext(T2 value) {
                        latest2.set(value);
                        has2.set(true);
                        tryCombine.run();
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
     * Combines latest values from three Observables.
     */
    public static <T1, T2, T3, R> Observable<R> combineLatest(
            Observable<T1> source1,
            Observable<T2> source2,
            Observable<T3> source3,
            Function3<T1, T2, T3, R> combiner) {
        return new Observable<R>() {
            @Override
            public void subscribe(Observer<? super R> observer) {
                AtomicReference<T1> latest1 = new AtomicReference<>();
                AtomicReference<T2> latest2 = new AtomicReference<>();
                AtomicReference<T3> latest3 = new AtomicReference<>();
                AtomicBoolean has1 = new AtomicBoolean(false);
                AtomicBoolean has2 = new AtomicBoolean(false);
                AtomicBoolean has3 = new AtomicBoolean(false);
                
                Runnable tryCombine = () -> {
                    if (has1.get() && has2.get() && has3.get()) {
                        try {
                            R result = combiner.apply(latest1.get(), latest2.get(), latest3.get());
                            observer.onNext(result);
                        } catch (Exception e) {
                            observer.onError(e);
                        }
                    }
                };
                
                source1.subscribe(new Observer<T1>() {
                    @Override
                    public void onNext(T1 value) {
                        latest1.set(value);
                        has1.set(true);
                        tryCombine.run();
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        observer.onError(error);
                    }
                    
                    @Override
                    public void onComplete() {
                    }
                });
                
                source2.subscribe(new Observer<T2>() {
                    @Override
                    public void onNext(T2 value) {
                        latest2.set(value);
                        has2.set(true);
                        tryCombine.run();
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        observer.onError(error);
                    }
                    
                    @Override
                    public void onComplete() {
                    }
                });
                
                source3.subscribe(new Observer<T3>() {
                    @Override
                    public void onNext(T3 value) {
                        latest3.set(value);
                        has3.set(true);
                        tryCombine.run();
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
     * Combines each value from this Observable with the latest value from another Observable.
     */
    public final <U, R> Observable<R> withLatestFrom(Observable<U> other, BiFunction<T, U, R> combiner) {
        return new Observable<R>() {
            @Override
            public void subscribe(Observer<? super R> observer) {
                AtomicReference<U> latestOther = new AtomicReference<>();
                AtomicBoolean hasOther = new AtomicBoolean(false);
                
                // Subscribe to other first to get its latest value
                other.subscribe(new Observer<U>() {
                    @Override
                    public void onNext(U value) {
                        latestOther.set(value);
                        hasOther.set(true);
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        observer.onError(error);
                    }
                    
                    @Override
                    public void onComplete() {
                        // Other completes, but we continue with this
                    }
                });
                
                // Subscribe to this Observable
                Observable.this.subscribe(new Observer<T>() {
                    @Override
                    public void onNext(T value) {
                        if (hasOther.get()) {
                            try {
                                R result = combiner.apply(value, latestOther.get());
                                observer.onNext(result);
                            } catch (Exception e) {
                                observer.onError(e);
                            }
                        }
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
     * Prepends values to the beginning of this Observable.
     */
    @SafeVarargs
    public final Observable<T> startWith(T... values) {
        return new Observable<T>() {
            @Override
            public void subscribe(Observer<? super T> observer) {
                // First emit the starting values
                for (T value : values) {
                    observer.onNext(value);
                }
                
                // Then subscribe to the source
                Observable.this.subscribe(observer);
            }
        };
    }
    
    /**
     * Prepends another Observable before this Observable.
     */
    public final Observable<T> startWith(Observable<T> other) {
        return new Observable<T>() {
            @Override
            public void subscribe(Observer<? super T> observer) {
                AtomicBoolean otherComplete = new AtomicBoolean(false);
                
                // First subscribe to other
                other.subscribe(new Observer<T>() {
                    @Override
                    public void onNext(T value) {
                        observer.onNext(value);
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        observer.onError(error);
                    }
                    
                    @Override
                    public void onComplete() {
                        otherComplete.set(true);
                        // Then subscribe to this Observable
                        Observable.this.subscribe(observer);
                    }
                });
            }
        };
    }
    
    /**
     * Compares two Observable sequences for equality.
     */
    public static <T> Observable<Boolean> sequenceEqual(Observable<T> source1, Observable<T> source2) {
        return new Observable<Boolean>() {
            @Override
            public void subscribe(Observer<? super Boolean> observer) {
                List<T> list1 = Collections.synchronizedList(new ArrayList<>());
                List<T> list2 = Collections.synchronizedList(new ArrayList<>());
                AtomicInteger completed = new AtomicInteger(0);
                AtomicBoolean errorOccurred = new AtomicBoolean(false);
                
                Runnable checkEquality = () -> {
                    if (completed.get() == 2 && !errorOccurred.get()) {
                        boolean equal = list1.equals(list2);
                        observer.onNext(equal);
                        observer.onComplete();
                    }
                };
                
                source1.subscribe(new Observer<T>() {
                    @Override
                    public void onNext(T value) {
                        list1.add(value);
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        if (errorOccurred.compareAndSet(false, true)) {
                            observer.onError(error);
                        }
                    }
                    
                    @Override
                    public void onComplete() {
                        completed.incrementAndGet();
                        checkEquality.run();
                    }
                });
                
                source2.subscribe(new Observer<T>() {
                    @Override
                    public void onNext(T value) {
                        list2.add(value);
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        if (errorOccurred.compareAndSet(false, true)) {
                            observer.onError(error);
                        }
                    }
                    
                    @Override
                    public void onComplete() {
                        completed.incrementAndGet();
                        checkEquality.run();
                    }
                });
            }
        };
    }
    
    // ============ Error Handling Operators ============
    
    /**
     * Catches errors and returns an alternative value.
     * 
     * <p>When an error occurs, instead of propagating it, this operator
     * emits the specified fallback value and completes normally.
     * 
     * <p>Example:
     * <pre>{@code
     * Observable.just(1, 2, 3)
     *     .map(x -> {
     *         if (x == 2) throw new RuntimeException("Error!");
     *         return x;
     *     })
     *     .onErrorReturn(error -> -1)
     *     .subscribe(System.out::println);
     * // Emits: 1, -1 (then completes)
     * }</pre>
     * 
     * @param valueSupplier function that returns a fallback value based on the error
     * @return an Observable that emits a fallback value on error
     * @see #onErrorResumeNext(Function)
     * @see #retry(int)
     */
    public final Observable<T> onErrorReturn(Function<Throwable, T> valueSupplier) {
        return new Observable<T>() {
            @Override
            public void subscribe(Observer<? super T> observer) {
                Observable.this.subscribe(new Observer<T>() {
                    @Override
                    public void onNext(T value) {
                        observer.onNext(value);
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        try {
                            T fallbackValue = valueSupplier.apply(error);
                            observer.onNext(fallbackValue);
                            observer.onComplete();
                        } catch (Exception e) {
                            observer.onError(e);
                        }
                    }
                    
                    @Override
                    public void onComplete() {
                        observer.onComplete();
                    }
                });
            }
        };
    }
    
    public final Observable<T> onErrorResumeNext(Function<Throwable, Observable<T>> resumeFunction) {
        return new Observable<T>() {
            @Override
            public void subscribe(Observer<? super T> observer) {
                Observable.this.subscribe(new Observer<T>() {
                    @Override
                    public void onNext(T value) {
                        observer.onNext(value);
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        try {
                            Observable<T> fallback = resumeFunction.apply(error);
                            fallback.subscribe(observer);
                        } catch (Exception e) {
                            observer.onError(e);
                        }
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
     * Retries the subscription to the source Observable when an error occurs.
     * 
     * <p>Attempts to resubscribe up to {@code count} times before giving up.
     * 
     * <p>Example:
     * <pre>{@code
     * AtomicInteger attempt = new AtomicInteger(0);
     * Observable.defer(() -> {
     *     int num = attempt.incrementAndGet();
     *     if (num < 3) {
     *         return Observable.error(new RuntimeException("Attempt " + num));
     *     }
     *     return Observable.just("Success!");
     * })
     * .retry(3)
     * .subscribe(System.out::println);
     * // Eventually emits: Success!
     * }</pre>
     * 
     * @param count the number of retry attempts
     * @return an Observable that retries on error
     * @see #onErrorReturn(Function)
     */
    public final Observable<T> retry(int count) {
        return new Observable<T>() {
            @Override
            public void subscribe(Observer<? super T> observer) {
                AtomicInteger attempts = new AtomicInteger(0);
                
                class RetryObserver implements Observer<T> {
                    @Override
                    public void onNext(T value) {
                        observer.onNext(value);
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        if (attempts.incrementAndGet() <= count) {
                            Observable.this.subscribe(this);
                        } else {
                            observer.onError(error);
                        }
                    }
                    
                    @Override
                    public void onComplete() {
                        observer.onComplete();
                    }
                }
                
                Observable.this.subscribe(new RetryObserver());
            }
        };
    }
    
    /**
     * Returns an Observable that re-subscribes to this Observable when the source Observable
     * calls onError, based on the signals from the Observable returned by the handler function.
     * <p>
     * The handler function receives an Observable of Throwable representing the errors, and
     * returns an Observable. When this returned Observable emits an item, a retry is triggered.
     * When it completes, the resulting Observable completes. When it errors, the error is
     * propagated to the downstream observer.
     * <p>
     * Example usage with exponential backoff:
     * <pre>{@code
     * source.retryWhen(errors -> errors
     *     .zipWith(Observable.range(1, 3), (error, retryCount) -> retryCount)
     *     .flatMap(retryCount -> Observable.timer(
     *         (long) Math.pow(2, retryCount), TimeUnit.SECONDS)));
     * }</pre>
     *
     * @param handler the function that receives an Observable of errors and returns an Observable
     *                that signals when to retry
     * @return an Observable that retries based on the signals from the handler
     * @see #retry(int)
     * @see #onErrorReturn(Function)
     */
    public final Observable<T> retryWhen(Function<? super Observable<Throwable>, ? extends ObservableSource<?>> handler) {
        Objects.requireNonNull(handler, "handler is null");
        Observable<T> source = this;
        return new Observable<T>() {
            @Override
            public void subscribe(Observer<? super T> observer) {
                // Subject to emit errors to the handler
                PublishSubject<Throwable> errorSubject = PublishSubject.create();
                AtomicBoolean disposed = new AtomicBoolean(false);
                
                // Runnable to subscribe to source - declared first so it can be referenced
                final Runnable[] subscribeToSource = new Runnable[1];
                subscribeToSource[0] = () -> {
                    if (disposed.get()) return;
                    
                    source.subscribe(new Observer<T>() {
                        @Override
                        public void onNext(T value) {
                            if (!disposed.get()) {
                                observer.onNext(value);
                            }
                        }
                        
                        @Override
                        public void onError(Throwable error) {
                            if (!disposed.get()) {
                                // Emit error to handler, which may trigger retry
                                errorSubject.onNext(error);
                            }
                        }
                        
                        @Override
                        public void onComplete() {
                            if (!disposed.get()) {
                                disposed.set(true);
                                observer.onComplete();
                            }
                        }
                    });
                };
                
                // Apply handler to get retry signal Observable
                ObservableSource<?> retrySignals;
                try {
                    retrySignals = handler.apply(errorSubject);
                } catch (Exception e) {
                    observer.onError(e);
                    return;
                }
                
                if (retrySignals == null) {
                    observer.onError(new NullPointerException("The handler returned a null ObservableSource"));
                    return;
                }
                
                // Subscribe to retry signals
                retrySignals.subscribe(new Observer<Object>() {
                    @Override
                    public void onNext(Object signal) {
                        // Retry signal received - resubscribe to source
                        if (!disposed.get()) {
                            subscribeToSource[0].run();
                        }
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        if (!disposed.get()) {
                            disposed.set(true);
                            observer.onError(error);
                        }
                    }
                    
                    @Override
                    public void onComplete() {
                        if (!disposed.get()) {
                            disposed.set(true);
                            observer.onComplete();
                        }
                    }
                });
                
                // Initial subscription
                subscribeToSource[0].run();
            }
        };
    }
    
    // ============ Utility Operators ============
    
    public final Observable<T> delay(long delay, TimeUnit unit) {
        return delay(delay, unit, Schedulers.computation());
    }
    
    public final Observable<T> delay(long delay, TimeUnit unit, Scheduler scheduler) {
        return new com.reactive.operators.time.ObservableDelay<>(this, delay, unit, scheduler);
    }
    
    /**
     * Delays the subscription to the source Observable by the specified time.
     * Unlike delay which delays each item, this delays when the subscription happens.
     */
    public final Observable<T> delaySubscription(long delay, TimeUnit unit) {
        return delaySubscription(delay, unit, Schedulers.computation());
    }
    
    public final Observable<T> delaySubscription(long delay, TimeUnit unit, Scheduler scheduler) {
        return new com.reactive.operators.time.ObservableDelaySubscription<>(this, delay, unit, scheduler);
    }
    
    public final Observable<T> timeout(long timeout, TimeUnit unit) {
        return timeout(timeout, unit, Schedulers.computation());
    }
    
    public final Observable<T> timeout(long timeout, TimeUnit unit, Scheduler scheduler) {
        return new Observable<T>() {
            @Override
            public void subscribe(Observer<? super T> observer) {
                AtomicBoolean timedOut = new AtomicBoolean(false);
                AtomicReference<Disposable> timeoutDisposable = new AtomicReference<>();
                
                timeoutDisposable.set(scheduler.scheduleDirect(() -> {
                    if (timedOut.compareAndSet(false, true)) {
                        observer.onError(new TimeoutException());
                    }
                }, timeout, unit));
                
                Observable.this.subscribe(new Observer<T>() {
                    @Override
                    public void onNext(T value) {
                        if (!timedOut.get()) {
                            observer.onNext(value);
                        }
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        if (timedOut.compareAndSet(false, true)) {
                            Disposable disposable = timeoutDisposable.get();
                            if (disposable != null) {
                                disposable.dispose();
                            }
                            observer.onError(error);
                        }
                    }
                    
                    @Override
                    public void onComplete() {
                        if (timedOut.compareAndSet(false, true)) {
                            Disposable disposable = timeoutDisposable.get();
                            if (disposable != null) {
                                disposable.dispose();
                            }
                            observer.onComplete();
                        }
                    }
                });
            }
        };
    }
    
    /**
     * Specifies the Scheduler on which the Observer receives notifications.
     * 
     * <p>Use this to observe results on a specific thread (e.g., UI thread).
     * 
     * <p>Example:
     * <pre>{@code
     * Observable.just(1, 2, 3)
     *     .map(x -> x * 10)
     *     .observeOn(Schedulers.computation())
     *     .subscribe(value -> {
     *         System.out.println("On: " + Thread.currentThread().getName());
     *         System.out.println("Value: " + value);
     *     });
     * // Values are received on computation thread
     * }</pre>
     * 
     * @param scheduler the Scheduler to notify Observer on
     * @return an Observable that observes on the specified Scheduler
     * @see #subscribeOn(Scheduler)
     */
    public final Observable<T> observeOn(Scheduler scheduler) {
        return new Observable<T>() {
            @Override
            public void subscribe(Observer<? super T> observer) {
                Observable.this.subscribe(new Observer<T>() {
                    @Override
                    public void onNext(T value) {
                        scheduler.scheduleDirect(() -> observer.onNext(value));
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        scheduler.scheduleDirect(() -> observer.onError(error));
                    }
                    
                    @Override
                    public void onComplete() {
                        scheduler.scheduleDirect(observer::onComplete);
                    }
                });
            }
        };
    }
    
    // ============ Utility Operators ============
    
    /**
     * Specifies the Scheduler on which subscription and unsubscription occur.
     * 
     * <p>By default, subscription occurs on the calling thread. Use this to
     * offload subscription to a different thread.
     * 
     * <p>Example:
     * <pre>{@code
     * Observable.fromCallable(() -> {
     *     System.out.println("Running on: " + Thread.currentThread().getName());
     *     return expensiveComputation();
     * })
     * .subscribeOn(Schedulers.io())
     * .subscribe(result -> System.out.println("Result: " + result));
     * // Computation runs on IO thread
     * }</pre>
     * 
     * @param scheduler the Scheduler to perform subscription on
     * @return an Observable that subscribes on the specified Scheduler
     * @see #observeOn(Scheduler)
     */
    public final Observable<T> subscribeOn(Scheduler scheduler) {
        return new Observable<T>() {
            @Override
            public void subscribe(Observer<? super T> observer) {
                SubscribeOnObserver<T> wrapper = new SubscribeOnObserver<>(observer);
                observer.onSubscribe(wrapper);
                
                // Schedule the subscription and capture the disposable
                Disposable schedulerDisposable = scheduler.scheduleDirect(() -> {
                    Observable.this.subscribe(wrapper);
                });
                
                wrapper.setSchedulerDisposable(schedulerDisposable);
            }
        };
    }
    
    /**
     * Performs a side effect action for each emitted item without modifying the stream.
     * 
     * <p>Useful for debugging or logging without changing the Observable's behavior.
     * 
     * <p>Example:
     * <pre>{@code
     * Observable.just(1, 2, 3)
     *     .doOnNext(x -> System.out.println("Processing: " + x))
     *     .map(x -> x * 10)
     *     .subscribe(System.out::println);
     * // Logs: Processing: 1
     * // Then emits: 10
     * // Logs: Processing: 2
     * // Then emits: 20
     * // ...
     * }</pre>
     * 
     * @param action the action to perform for each item
     * @return the source Observable with the side effect applied
     * @see #doOnError(Consumer)
     * @see #doOnComplete(Runnable)
     */
    public final Observable<T> doOnNext(Consumer<T> action) {
        return new Observable<T>() {
            @Override
            public void subscribe(Observer<? super T> observer) {
                Observable.this.subscribe(new Observer<T>() {
                    @Override
                    public void onNext(T value) {
                        try {
                            action.accept(value);
                        } catch (Exception e) {
                            observer.onError(e);
                            return;
                        }
                        observer.onNext(value);
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
     * Performs a side effect action when an error occurs.
     * 
     * <p>The error is still propagated downstream after the action executes.
     * 
     * <p>Example:
     * <pre>{@code
     * Observable.error(new RuntimeException("Oops!"))
     *     .doOnError(error -> System.err.println("Logged: " + error))
     *     .subscribe(
     *         value -> System.out.println(value),
     *         error -> System.err.println("Caught: " + error)
     *     );
     * // Logs error, then propagates it
     * }</pre>
     * 
     * @param action the action to perform on error
     * @return the source Observable with the side effect applied
     * @see #doOnNext(Consumer)
     */
    public final Observable<T> doOnError(Consumer<Throwable> action) {
        return new Observable<T>() {
            @Override
            public void subscribe(Observer<? super T> observer) {
                Observable.this.subscribe(new Observer<T>() {
                    @Override
                    public void onNext(T value) {
                        observer.onNext(value);
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        try {
                            action.accept(error);
                        } catch (Exception e) {
                            // Ignore
                        }
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
     * Performs a side effect action when the sequence completes successfully.
     * 
     * <p>Example:
     * <pre>{@code
     * Observable.just(1, 2, 3)
     *     .doOnComplete(() -> System.out.println("Processing complete!"))
     *     .subscribe(System.out::println);
     * // Emits: 1, 2, 3
     * // Then logs: Processing complete!
     * }</pre>
     * 
     * @param action the action to perform on completion
     * @return the source Observable with the side effect applied
     * @see #doOnNext(Consumer)
     */
    public final Observable<T> doOnComplete(Runnable action) {
        return new Observable<T>() {
            @Override
            public void subscribe(Observer<? super T> observer) {
                Observable.this.subscribe(new Observer<T>() {
                    @Override
                    public void onNext(T value) {
                        observer.onNext(value);
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        observer.onError(error);
                    }
                    
                    @Override
                    public void onComplete() {
                        try {
                            action.run();
                        } catch (Exception e) {
                            // Ignore
                        }
                        observer.onComplete();
                    }
                });
            }
        };
    }
    
    // ============ Conversion Operators ============
    
    /**
     * Converts this Observable to a Flowable with the specified backpressure strategy.
     * 
     * @param strategy the backpressure strategy to use
     * @return a Flowable with backpressure support
     */
    public final Flowable<T> toFlowable(BackpressureStrategy strategy) {
        Observable<T> source = this;
        return Flowable.create(emitter -> {
            source.subscribe(new Observer<T>() {
                @Override
                public void onSubscribe(Disposable disposable) {
                    // Link disposable with subscription
                }
                
                @Override
                public void onNext(T value) {
                    emitter.onNext(value);
                }
                
                @Override
                public void onError(Throwable error) {
                    emitter.onError(error);
                }
                
                @Override
                public void onComplete() {
                    emitter.onComplete();
                }
            });
        }, strategy);
    }
    
    // ============ Buffer Operators ============
    
    public final Observable<List<T>> buffer(int count) {
        return new Observable<List<T>>() {
            @Override
            public void subscribe(Observer<? super List<T>> observer) {
                List<T> buffer = new ArrayList<>();
                
                Observable.this.subscribe(new Observer<T>() {
                    @Override
                    public void onNext(T value) {
                        buffer.add(value);
                        if (buffer.size() >= count) {
                            observer.onNext(new ArrayList<>(buffer));
                            buffer.clear();
                        }
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        observer.onError(error);
                    }
                    
                    @Override
                    public void onComplete() {
                        if (!buffer.isEmpty()) {
                            observer.onNext(new ArrayList<>(buffer));
                        }
                        observer.onComplete();
                    }
                });
            }
        };
    }
    
    /**
     * Groups items by key into separate GroupedObservables.
     * Each GroupedObservable emits items that share the same key.
     * 
     * @param <K> the type of the key
     * @param keySelector function to extract the key from each item
     * @return an Observable of GroupedObservables
     */
    public final <K> Observable<GroupedObservable<K, T>> groupBy(Function<T, K> keySelector) {
        return new Observable<GroupedObservable<K, T>>() {
            @Override
            public void subscribe(Observer<? super GroupedObservable<K, T>> observer) {
                Map<K, Subject<T>> groups = new ConcurrentHashMap<>();
                
                Observable.this.subscribe(new Observer<T>() {
                    @Override
                    public void onNext(T value) {
                        try {
                            K key = keySelector.apply(value);
                            Subject<T> group = groups.computeIfAbsent(key, k -> {
                                // Create new group
                                Subject<T> newGroup = PublishSubject.create();
                                
                                // Emit the GroupedObservable
                                observer.onNext(new GroupedObservable<K, T>() {
                                    @Override
                                    public K getKey() {
                                        return k;
                                    }
                                    
                                    @Override
                                    public void subscribe(Observer<? super T> groupObserver) {
                                        newGroup.subscribe(groupObserver);
                                    }
                                });
                                
                                return newGroup;
                            });
                            
                            // Emit value to the group
                            group.onNext(value);
                        } catch (Exception e) {
                            observer.onError(e);
                        }
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        // Propagate error to all groups
                        for (Subject<T> group : groups.values()) {
                            group.onError(error);
                        }
                        observer.onError(error);
                    }
                    
                    @Override
                    public void onComplete() {
                        // Complete all groups
                        for (Subject<T> group : groups.values()) {
                            group.onComplete();
                        }
                        observer.onComplete();
                    }
                });
            }
        };
    }
    
    /**
     * Buffers items with a skip count.
     * 
     * @param count number of items in each buffer
     * @param skip number of items to skip before starting a new buffer
     * @return Observable of Lists
     */
    public final Observable<List<T>> buffer(int count, int skip) {
        return new Observable<List<T>>() {
            @Override
            public void subscribe(Observer<? super List<T>> observer) {
                List<List<T>> buffers = Collections.synchronizedList(new ArrayList<>());
                AtomicInteger index = new AtomicInteger(0);
                
                Observable.this.subscribe(new Observer<T>() {
                    @Override
                    public void onNext(T value) {
                        int idx = index.getAndIncrement();
                        
                        // Add to existing buffers
                        synchronized (buffers) {
                            for (Iterator<List<T>> it = buffers.iterator(); it.hasNext();) {
                                List<T> buffer = it.next();
                                buffer.add(value);
                                if (buffer.size() >= count) {
                                    observer.onNext(new ArrayList<>(buffer));
                                    it.remove();
                                }
                            }
                        }
                        
                        // Start new buffer if needed
                        if (idx % skip == 0) {
                            List<T> newBuffer = new ArrayList<>();
                            newBuffer.add(value);
                            if (count == 1) {
                                observer.onNext(new ArrayList<>(newBuffer));
                            } else {
                                buffers.add(newBuffer);
                            }
                        }
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        observer.onError(error);
                    }
                    
                    @Override
                    public void onComplete() {
                        // Emit remaining buffers
                        synchronized (buffers) {
                            for (List<T> buffer : buffers) {
                                if (!buffer.isEmpty()) {
                                    observer.onNext(new ArrayList<>(buffer));
                                }
                            }
                        }
                        observer.onComplete();
                    }
                });
            }
        };
    }
    
    /**
     * Buffers items based on time.
     * 
     * @param timespan the time duration for each buffer
     * @param unit the time unit
     * @return Observable of Lists
     */
    public final Observable<List<T>> buffer(long timespan, TimeUnit unit) {
        return buffer(timespan, unit, Schedulers.computation());
    }
    
    /**
     * Buffers items based on time with a specific scheduler.
     * 
     * @param timespan the time duration for each buffer
     * @param unit the time unit
     * @param scheduler the scheduler to use
     * @return Observable of Lists
     */
    public final Observable<List<T>> buffer(long timespan, TimeUnit unit, Scheduler scheduler) {
        return new Observable<List<T>>() {
            @Override
            public void subscribe(Observer<? super List<T>> observer) {
                List<T> buffer = Collections.synchronizedList(new ArrayList<>());
                
                // Schedule periodic buffer emission
                scheduler.schedulePeriodic(() -> {
                    synchronized (buffer) {
                        if (!buffer.isEmpty()) {
                            observer.onNext(new ArrayList<>(buffer));
                            buffer.clear();
                        }
                    }
                }, timespan, timespan, unit);
                
                Observable.this.subscribe(new Observer<T>() {
                    @Override
                    public void onNext(T value) {
                        buffer.add(value);
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        observer.onError(error);
                    }
                    
                    @Override
                    public void onComplete() {
                        synchronized (buffer) {
                            if (!buffer.isEmpty()) {
                                observer.onNext(new ArrayList<>(buffer));
                            }
                        }
                        observer.onComplete();
                    }
                });
            }
        };
    }
    
    // ============ Connectable Operators ============
    
    /**
     * Converts this Observable into a ConnectableObservable using a PublishSubject.
     * 
     * <p>A ConnectableObservable resembles an ordinary Observable, except that it does not
     * begin emitting items when it is subscribed to, but only when its connect() method
     * is called.</p>
     * 
     * <p>This allows multiple observers to subscribe before emissions begin, enabling
     * multicasting of a single subscription to the underlying source Observable.</p>
     * 
     * <h3>Example:</h3>
     * <pre>{@code
     * ConnectableObservable<Integer> connectable = Observable.range(1, 5).publish();
     * connectable.subscribe(x -> System.out.println("Sub1: " + x));
     * connectable.subscribe(x -> System.out.println("Sub2: " + x));
     * connectable.connect(); // Both subscribers receive the same emissions
     * }</pre>
     * 
     * @return a ConnectableObservable that multicasts emissions
     * @see #share()
     * @see #replay()
     */
    public final ConnectableObservable<T> publish() {
        return ObservablePublish.create(this);
    }
    
    /**
     * Converts this Observable into a ConnectableObservable using a ReplaySubject
     * that buffers all emitted items.
     * 
     * <p>Unlike publish(), replay() buffers emitted items so that new observers
     * that subscribe after emissions have started can receive previously emitted items.</p>
     * 
     * <h3>Example:</h3>
     * <pre>{@code
     * ConnectableObservable<Integer> connectable = Observable.range(1, 3).replay();
     * connectable.connect(); // Start emitting
     * Thread.sleep(100);
     * connectable.subscribe(x -> System.out.println("Late: " + x)); // Still receives 1, 2, 3
     * }</pre>
     * 
     * @return a ConnectableObservable with unbounded replay buffer
     * @see #replay(int)
     */
    public final ConnectableObservable<T> replay() {
        return ObservableReplay.create(this);
    }
    
    /**
     * Converts this Observable into a ConnectableObservable using a ReplaySubject
     * that buffers up to the specified number of items.
     * 
     * <p>When the buffer is full, the oldest items are discarded to make room for new ones.</p>
     * 
     * <h3>Example:</h3>
     * <pre>{@code
     * ConnectableObservable<Integer> connectable = Observable.range(1, 5).replay(2);
     * connectable.connect();
     * Thread.sleep(100);
     * connectable.subscribe(x -> System.out.println("Late: " + x)); // Receives only 4, 5
     * }</pre>
     * 
     * @param bufferSize the maximum number of items to buffer
     * @return a ConnectableObservable with size-bounded replay buffer
     */
    public final ConnectableObservable<T> replay(int bufferSize) {
        return ObservableReplay.createWithSize(this, bufferSize);
    }
    
    /**
     * Returns an Observable that shares a single subscription to the underlying source.
     * 
     * <p>This is a convenience method equivalent to calling publish().refCount().</p>
     * 
     * <p><b>Behavior:</b></p>
     * <ul>
     * <li>First subscriber triggers connection to the source</li>
     * <li>Additional subscribers share the same connection</li>
     * <li>When all subscribers unsubscribe, the connection is disposed</li>
     * <li>New subscribers after complete disposal will trigger a new connection</li>
     * </ul>
     * 
     * <p>This operator is useful for converting a cold Observable into a hot Observable
     * with automatic connection management.</p>
     * 
     * <h3>Example:</h3>
     * <pre>{@code
     * Observable<Long> shared = Observable.interval(1, TimeUnit.SECONDS).share();
     * 
     * // First subscriber triggers connection
     * Disposable d1 = shared.subscribe(x -> System.out.println("Sub1: " + x));
     * 
     * Thread.sleep(2500);
     * 
     * // Second subscriber joins existing stream
     * Disposable d2 = shared.subscribe(x -> System.out.println("Sub2: " + x));
     * 
     * // Both subscribers receive emissions 3, 4, 5...
     * }</pre>
     * 
     * @return an Observable with automatic connection management
     * @see #publish()
     * @see ConnectableObservable#refCount()
     */
    public final Observable<T> share() {
        return publish().refCount();
    }
    
    // ============ Aggregation Operators ============
    
    /**
     * Counts the number of items emitted by the source Observable and emits this count.
     * 
     * <p>The count is emitted when the source completes. If the source emits an error,
     * the error is propagated without emitting a count.</p>
     * 
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Observable.just(1, 2, 3, 4, 5)
     *     .count()
     *     .subscribe(count -> System.out.println("Count: " + count));
     * // Output: Count: 5
     * }</pre>
     * 
     * @return an Observable that emits a single Long value representing the count
     */
    public final Observable<Long> count() {
        return com.reactive.observables.internal.operators.ObservableCount.create(this);
    }
    
    /**
     * Determines whether all items emitted by the source satisfy a given predicate.
     * 
     * <p>Emits true if all items pass the predicate test or if the source is empty.
     * Emits false immediately upon the first item that fails the test.</p>
     * 
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Observable.just(2, 4, 6, 8)
     *     .all(x -> x % 2 == 0)
     *     .subscribe(result -> System.out.println("All even: " + result));
     * // Output: All even: true
     * }</pre>
     * 
     * @param predicate the predicate to test each item
     * @return an Observable that emits true if all items pass, false otherwise
     */
    public final Observable<Boolean> all(java.util.function.Predicate<? super T> predicate) {
        return com.reactive.observables.internal.operators.ObservableAll.create(this, predicate);
    }
    
    /**
     * Determines whether any item emitted by the source satisfies a given predicate.
     * 
     * <p>Emits true immediately upon the first item that passes the predicate test.
     * Emits false if no items pass or if the source is empty.</p>
     * 
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Observable.just(1, 3, 4, 5)
     *     .any(x -> x % 2 == 0)
     *     .subscribe(result -> System.out.println("Any even: " + result));
     * // Output: Any even: true
     * }</pre>
     * 
     * @param predicate the predicate to test each item
     * @return an Observable that emits true if any item passes, false otherwise
     */
    public final Observable<Boolean> any(java.util.function.Predicate<? super T> predicate) {
        return com.reactive.observables.internal.operators.ObservableAny.create(this, predicate);
    }
    
    /**
     * Determines whether the source Observable emits a specific item.
     * 
     * <p>Emits true immediately upon finding the item (using Objects.equals for comparison).
     * Emits false if the item is not found or if the source is empty.</p>
     * 
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Observable.just("apple", "banana", "cherry")
     *     .contains("banana")
     *     .subscribe(result -> System.out.println("Contains banana: " + result));
     * // Output: Contains banana: true
     * }</pre>
     * 
     * @param value the value to search for
     * @return an Observable that emits true if the value is found, false otherwise
     */
    public final Observable<Boolean> contains(T value) {
        return com.reactive.observables.internal.operators.ObservableContains.create(this, value);
    }
    
    /**
     * Determines whether the source Observable emits no items.
     * 
     * <p>Emits false immediately upon the first item emission.
     * Emits true if the source completes without emitting any items.</p>
     * 
     * <p><b>Examples:</b></p>
     * <pre>{@code
     * Observable.empty()
     *     .isEmpty()
     *     .subscribe(result -> System.out.println("Is empty: " + result));
     * // Output: Is empty: true
     * 
     * Observable.just(1)
     *     .isEmpty()
     *     .subscribe(result -> System.out.println("Is empty: " + result));
     * // Output: Is empty: false
     * }</pre>
     * 
     * @return an Observable that emits true if the source is empty, false otherwise
     */
    public final Observable<Boolean> isEmpty() {
        return com.reactive.observables.internal.operators.ObservableIsEmpty.create(this);
    }
    
    // ============ Java Standard Conversions ============
    
    /**
     * Converts this Observable into a Future that completes with the first emitted item.
     * If the Observable completes without emitting any items, the Future completes with null.
     * If the Observable emits an error, the Future completes exceptionally.
     * 
     * <p>Example:
     * <pre>{@code
     * Future<Integer> future = Observable.just(1, 2, 3)
     *     .toFuture();
     * Integer result = future.get(); // Returns 1
     * }</pre>
     * 
     * @return a Future that completes with the first emitted item
     */
    public final java.util.concurrent.Future<T> toFuture() {
        return toCompletableFuture();
    }
    
    /**
     * Converts this Observable into a CompletableFuture that completes with the first emitted item.
     * If the Observable completes without emitting any items, the CompletableFuture completes with null.
     * If the Observable emits an error, the CompletableFuture completes exceptionally.
     * 
     * <p>Example:
     * <pre>{@code
     * CompletableFuture<Integer> future = Observable.just(1, 2, 3)
     *     .toCompletableFuture();
     * future.thenAccept(value -> System.out.println("Value: " + value));
     * }</pre>
     * 
     * @return a CompletableFuture that completes with the first emitted item
     */
    public final java.util.concurrent.CompletableFuture<T> toCompletableFuture() {
        java.util.concurrent.CompletableFuture<T> future = new java.util.concurrent.CompletableFuture<>();
        
        subscribe(new Observer<T>() {
            private T value;
            private boolean hasValue = false;
            
            @Override
            public void onSubscribe(Disposable d) {
                // Store disposable if needed
            }
            
            @Override
            public void onNext(T item) {
                if (!hasValue) {
                    value = item;
                    hasValue = true;
                }
            }
            
            @Override
            public void onError(Throwable error) {
                future.completeExceptionally(error);
            }
            
            @Override
            public void onComplete() {
                if (hasValue) {
                    future.complete(value);
                } else {
                    future.complete(null);
                }
            }
        });
        
        return future;
    }
    
    /**
     * Converts this Observable into a Java Stream.
     * The stream will block on terminal operations until the Observable completes.
     * 
     * <p>Example:
     * <pre>{@code
     * Stream<Integer> stream = Observable.just(1, 2, 3, 4, 5)
     *     .toStream();
     * long count = stream.filter(x -> x > 2).count(); // Returns 3
     * }</pre>
     * 
     * @return a Stream containing all emitted items
     */
    public final java.util.stream.Stream<T> toStream() {
        java.util.List<T> list = new java.util.ArrayList<>();
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<Throwable> error = new java.util.concurrent.atomic.AtomicReference<>();
        
        subscribe(new Observer<T>() {
            @Override
            public void onSubscribe(Disposable d) {
                // Store disposable if needed
            }
            
            @Override
            public void onNext(T value) {
                synchronized (list) {
                    list.add(value);
                }
            }
            
            @Override
            public void onError(Throwable e) {
                error.set(e);
                latch.countDown();
            }
            
            @Override
            public void onComplete() {
                latch.countDown();
            }
        });
        
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for Observable to complete", e);
        }
        
        if (error.get() != null) {
            throw new RuntimeException("Observable completed with error", error.get());
        }
        
        return list.stream();
    }
    
    /**
     * Converts this Observable into a blocking Iterable.
     * The iterator will block when calling next() until the next item is available.
     * 
     * <p>Example:
     * <pre>{@code
     * Iterable<Integer> iterable = Observable.just(1, 2, 3)
     *     .blockingIterable();
     * for (Integer value : iterable) {
     *     System.out.println(value);
     * }
     * }</pre>
     * 
     * @return a blocking Iterable
     */
    public final Iterable<T> blockingIterable() {
        return () -> {
            java.util.concurrent.BlockingQueue<Object> queue = new java.util.concurrent.LinkedBlockingQueue<>();
            Object COMPLETE = new Object();
            
            subscribe(new Observer<T>() {
                @Override
                public void onSubscribe(Disposable d) {
                    // Store disposable if needed
                }
                
                @Override
                public void onNext(T value) {
                    try {
                        queue.put(value);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                
                @Override
                public void onError(Throwable error) {
                    try {
                        queue.put(error);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                
                @Override
                public void onComplete() {
                    try {
                        queue.put(COMPLETE);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
            
            return new java.util.Iterator<T>() {
                private Object next;
                private boolean hasNext = true;
                
                @Override
                public boolean hasNext() {
                    if (!hasNext) {
                        return false;
                    }
                    
                    if (next == null) {
                        try {
                            next = queue.take();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            hasNext = false;
                            return false;
                        }
                    }
                    
                    if (next == COMPLETE) {
                        hasNext = false;
                        return false;
                    }
                    
                    if (next instanceof Throwable) {
                        hasNext = false;
                        throw new RuntimeException("Observable error", (Throwable) next);
                    }
                    
                    return true;
                }
                
                @Override
                @SuppressWarnings("unchecked")
                public T next() {
                    if (!hasNext()) {
                        throw new java.util.NoSuchElementException();
                    }
                    
                    T value = (T) next;
                    next = null;
                    return value;
                }
            };
        };
    }
    
    /**
     * Blocks until the Observable emits its first item, then returns that item.
     * Throws NoSuchElementException if the Observable completes without emitting any items.
     * 
     * <p>Example:
     * <pre>{@code
     * Integer first = Observable.just(1, 2, 3)
     *     .blockingFirst();
     * System.out.println(first); // Prints: 1
     * }</pre>
     * 
     * @return the first emitted item
     * @throws NoSuchElementException if the Observable completes without emitting
     * @throws RuntimeException if the Observable emits an error
     */
    public final T blockingFirst() {
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<T> result = new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.atomic.AtomicReference<Throwable> error = new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.atomic.AtomicBoolean hasValue = new java.util.concurrent.atomic.AtomicBoolean(false);
        
        subscribe(new Observer<T>() {
            @Override
            public void onSubscribe(Disposable d) {
                // Store disposable if needed
            }
            
            @Override
            public void onNext(T value) {
                if (hasValue.compareAndSet(false, true)) {
                    result.set(value);
                    latch.countDown();
                }
            }
            
            @Override
            public void onError(Throwable e) {
                error.set(e);
                latch.countDown();
            }
            
            @Override
            public void onComplete() {
                latch.countDown();
            }
        });
        
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for first item", e);
        }
        
        if (error.get() != null) {
            throw new RuntimeException("Observable completed with error", error.get());
        }
        
        if (!hasValue.get()) {
            throw new java.util.NoSuchElementException("Observable completed without emitting any items");
        }
        
        return result.get();
    }
    
    /**
     * Blocks until the Observable emits its first item, then returns that item.
     * Returns the default value if the Observable completes without emitting any items.
     * 
     * <p>Example:
     * <pre>{@code
     * Integer first = Observable.<Integer>empty()
     *     .blockingFirst(42);
     * System.out.println(first); // Prints: 42
     * }</pre>
     * 
     * @param defaultValue the value to return if the Observable is empty
     * @return the first emitted item, or the default value if empty
     * @throws RuntimeException if the Observable emits an error
     */
    public final T blockingFirst(T defaultValue) {
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<T> result = new java.util.concurrent.atomic.AtomicReference<>(defaultValue);
        java.util.concurrent.atomic.AtomicReference<Throwable> error = new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.atomic.AtomicBoolean hasValue = new java.util.concurrent.atomic.AtomicBoolean(false);
        
        subscribe(new Observer<T>() {
            @Override
            public void onSubscribe(Disposable d) {
                // Store disposable if needed
            }
            
            @Override
            public void onNext(T value) {
                if (hasValue.compareAndSet(false, true)) {
                    result.set(value);
                    latch.countDown();
                }
            }
            
            @Override
            public void onError(Throwable e) {
                error.set(e);
                latch.countDown();
            }
            
            @Override
            public void onComplete() {
                latch.countDown();
            }
        });
        
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for first item", e);
        }
        
        if (error.get() != null) {
            throw new RuntimeException("Observable completed with error", error.get());
        }
        
        return result.get();
    }
    
    /**
     * Blocks until the Observable completes, then returns the last emitted item.
     * Throws NoSuchElementException if the Observable completes without emitting any items.
     * 
     * <p>Example:
     * <pre>{@code
     * Integer last = Observable.just(1, 2, 3)
     *     .blockingLast();
     * System.out.println(last); // Prints: 3
     * }</pre>
     * 
     * @return the last emitted item
     * @throws NoSuchElementException if the Observable completes without emitting
     * @throws RuntimeException if the Observable emits an error
     */
    public final T blockingLast() {
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<T> result = new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.atomic.AtomicReference<Throwable> error = new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.atomic.AtomicBoolean hasValue = new java.util.concurrent.atomic.AtomicBoolean(false);
        
        subscribe(new Observer<T>() {
            @Override
            public void onSubscribe(Disposable d) {
                // Store disposable if needed
            }
            
            @Override
            public void onNext(T value) {
                result.set(value);
                hasValue.set(true);
            }
            
            @Override
            public void onError(Throwable e) {
                error.set(e);
                latch.countDown();
            }
            
            @Override
            public void onComplete() {
                latch.countDown();
            }
        });
        
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for last item", e);
        }
        
        if (error.get() != null) {
            throw new RuntimeException("Observable completed with error", error.get());
        }
        
        if (!hasValue.get()) {
            throw new java.util.NoSuchElementException("Observable completed without emitting any items");
        }
        
        return result.get();
    }
    
    /**
     * Blocks until the Observable completes, then returns the last emitted item.
     * Returns the default value if the Observable completes without emitting any items.
     * 
     * <p>Example:
     * <pre>{@code
     * Integer last = Observable.<Integer>empty()
     *     .blockingLast(42);
     * System.out.println(last); // Prints: 42
     * }</pre>
     * 
     * @param defaultValue the value to return if the Observable is empty
     * @return the last emitted item, or the default value if empty
     * @throws RuntimeException if the Observable emits an error
     */
    public final T blockingLast(T defaultValue) {
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<T> result = new java.util.concurrent.atomic.AtomicReference<>(defaultValue);
        java.util.concurrent.atomic.AtomicReference<Throwable> error = new java.util.concurrent.atomic.AtomicReference<>();
        
        subscribe(new Observer<T>() {
            @Override
            public void onSubscribe(Disposable d) {
                // Store disposable if needed
            }
            
            @Override
            public void onNext(T value) {
                result.set(value);
            }
            
            @Override
            public void onError(Throwable e) {
                error.set(e);
                latch.countDown();
            }
            
            @Override
            public void onComplete() {
                latch.countDown();
            }
        });
        
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for last item", e);
        }
        
        if (error.get() != null) {
            throw new RuntimeException("Observable completed with error", error.get());
        }
        
        return result.get();
    }
    
    // ============ Reactive Streams Integration ============
    
    /**
     * Converts this Observable into a Reactive Streams Publisher.
     * This allows interoperability with any library that supports the Reactive Streams specification.
     * 
     * <p>Example:
     * <pre>{@code
     * Publisher<Integer> publisher = Observable.just(1, 2, 3).toPublisher();
     * publisher.subscribe(new Subscriber<Integer>() {
     *     public void onSubscribe(Subscription s) { s.request(Long.MAX_VALUE); }
     *     public void onNext(Integer value) { System.out.println(value); }
     *     public void onError(Throwable t) { t.printStackTrace(); }
     *     public void onComplete() { System.out.println("Done"); }
     * });
     * }</pre>
     * 
     * @return a Publisher representing this Observable
     */
    public final org.reactivestreams.Publisher<T> toPublisher() {
        return subscriber -> {
            subscriber.onSubscribe(new org.reactivestreams.Subscription() {
                private volatile boolean cancelled = false;
                private volatile long requested = 0;
                private final AtomicLong pending = new AtomicLong(0);
                private final Queue<T> buffer = new ConcurrentLinkedQueue<>();
                private volatile boolean completed = false;
                private volatile Throwable error = null;
                
                {
                    // Subscribe to Observable
                    Observable.this.subscribe(new Observer<T>() {
                        @Override
                        public void onSubscribe(Disposable d) {
                            // Link disposable to subscription cancellation
                        }
                        
                        @Override
                        public void onNext(T value) {
                            if (!cancelled) {
                                buffer.offer(value);
                                drain();
                            }
                        }
                        
                        @Override
                        public void onError(Throwable e) {
                            if (!cancelled) {
                                error = e;
                                drain();
                            }
                        }
                        
                        @Override
                        public void onComplete() {
                            if (!cancelled) {
                                completed = true;
                                drain();
                            }
                        }
                    });
                }
                
                private void drain() {
                    synchronized (this) {
                        while (requested > 0 && !buffer.isEmpty() && !cancelled) {
                            T value = buffer.poll();
                            if (value != null) {
                                requested--;
                                subscriber.onNext(value);
                            }
                        }
                        
                        if (!cancelled && buffer.isEmpty()) {
                            if (error != null) {
                                subscriber.onError(error);
                                cancelled = true;
                            } else if (completed) {
                                subscriber.onComplete();
                                cancelled = true;
                            }
                        }
                    }
                }
                
                @Override
                public void request(long n) {
                    if (n <= 0) {
                        subscriber.onError(new IllegalArgumentException("Requested amount must be positive"));
                        return;
                    }
                    
                    synchronized (this) {
                        long current = requested;
                        if (current == Long.MAX_VALUE) {
                            return; // Already unbounded
                        }
                        
                        if (n == Long.MAX_VALUE || current + n < 0) {
                            requested = Long.MAX_VALUE;
                        } else {
                            requested = current + n;
                        }
                    }
                    
                    drain();
                }
                
                @Override
                public void cancel() {
                    cancelled = true;
                }
            });
        };
    }
    
    /**
     * Creates an Observable from a Reactive Streams Publisher.
     * This allows conversion from any Reactive Streams compliant source.
     * 
     * <p>Example:
     * <pre>{@code
     * Publisher<Integer> publisher = // some publisher
     * Observable<Integer> observable = Observable.fromPublisher(publisher);
     * observable.subscribe(value -> System.out.println(value));
     * }</pre>
     * 
     * @param publisher the Publisher to convert
     * @param <T> the type of elements emitted
     * @return an Observable that emits the same elements as the publisher
     */
    public static <T> Observable<T> fromPublisher(org.reactivestreams.Publisher<T> publisher) {
        return create(emitter -> {
            publisher.subscribe(new org.reactivestreams.Subscriber<T>() {
                private org.reactivestreams.Subscription subscription;
                
                @Override
                public void onSubscribe(org.reactivestreams.Subscription s) {
                    this.subscription = s;
                    s.request(Long.MAX_VALUE); // Request all items
                }
                
                @Override
                public void onNext(T value) {
                    if (!emitter.isDisposed()) {
                        emitter.onNext(value);
                    }
                }
                
                @Override
                public void onError(Throwable t) {
                    if (!emitter.isDisposed()) {
                        emitter.onError(t);
                    }
                }
                
                @Override
                public void onComplete() {
                    if (!emitter.isDisposed()) {
                        emitter.onComplete();
                    }
                }
            });
        });
    }
    
    /**
     * Converts this Observable into a Project Reactor Flux.
     * Flux is a publisher of 0 to N elements.
     * 
     * <p>Requires reactor-core dependency.
     * 
     * <p>Example:
     * <pre>{@code
     * Flux<Integer> flux = Observable.just(1, 2, 3, 4, 5).toFlux();
     * flux.subscribe(value -> System.out.println(value));
     * }</pre>
     * 
     * @return a Flux representing this Observable
     * @throws UnsupportedOperationException if reactor-core is not available
     */
    public final Object toFlux() {
        try {
            Class<?> fluxClass = Class.forName("reactor.core.publisher.Flux");
            java.lang.reflect.Method fromMethod = fluxClass.getMethod("from", org.reactivestreams.Publisher.class);
            return fromMethod.invoke(null, toPublisher());
        } catch (ClassNotFoundException e) {
            throw new UnsupportedOperationException("reactor-core dependency not found. Add io.projectreactor:reactor-core to use this method.", e);
        } catch (Exception e) {
            throw new RuntimeException("Error converting to Flux", e);
        }
    }
    
    /**
     * Converts this Observable into a Project Reactor Mono.
     * Mono is a publisher of 0 or 1 element.
     * Takes only the first element emitted by this Observable.
     * 
     * <p>Requires reactor-core dependency.
     * 
     * <p>Example:
     * <pre>{@code
     * Mono<Integer> mono = Observable.just(1, 2, 3).toMono();
     * mono.subscribe(value -> System.out.println(value)); // Prints: 1
     * }</pre>
     * 
     * @return a Mono that emits the first element of this Observable
     * @throws UnsupportedOperationException if reactor-core is not available
     */
    public final Object toMono() {
        try {
            Class<?> monoClass = Class.forName("reactor.core.publisher.Mono");
            java.lang.reflect.Method fromMethod = monoClass.getMethod("from", org.reactivestreams.Publisher.class);
            return fromMethod.invoke(null, toPublisher());
        } catch (ClassNotFoundException e) {
            throw new UnsupportedOperationException("reactor-core dependency not found. Add io.projectreactor:reactor-core to use this method.", e);
        } catch (Exception e) {
            throw new RuntimeException("Error converting to Mono", e);
        }
    }
    
    /**
     * Creates an Observable from a Project Reactor Flux.
     * 
     * <p>Requires reactor-core dependency.
     * 
     * <p>Example:
     * <pre>{@code
     * Flux<Integer> flux = Flux.just(1, 2, 3, 4, 5);
     * Observable<Integer> observable = Observable.fromFlux(flux);
     * }</pre>
     * 
     * @param flux the Flux to convert (as Object to avoid compile-time dependency)
     * @param <T> the type of elements emitted
     * @return an Observable that emits the same elements as the Flux
     * @throws UnsupportedOperationException if reactor-core is not available
     */
    @SuppressWarnings("unchecked")
    public static <T> Observable<T> fromFlux(Object flux) {
        try {
            if (flux instanceof org.reactivestreams.Publisher) {
                return fromPublisher((org.reactivestreams.Publisher<T>) flux);
            }
            throw new IllegalArgumentException("Provided object is not a Flux (Publisher)");
        } catch (ClassCastException e) {
            throw new UnsupportedOperationException("reactor-core dependency not found. Add io.projectreactor:reactor-core to use this method.", e);
        }
    }
    
    /**
     * Creates an Observable from a Project Reactor Mono.
     * 
     * <p>Requires reactor-core dependency.
     * 
     * <p>Example:
     * <pre>{@code
     * Mono<Integer> mono = Mono.just(42);
     * Observable<Integer> observable = Observable.fromMono(mono);
     * }</pre>
     * 
     * @param mono the Mono to convert (as Object to avoid compile-time dependency)
     * @param <T> the type of elements emitted
     * @return an Observable that emits the same element as the Mono
     * @throws UnsupportedOperationException if reactor-core is not available
     */
    @SuppressWarnings("unchecked")
    public static <T> Observable<T> fromMono(Object mono) {
        try {
            if (mono instanceof org.reactivestreams.Publisher) {
                return fromPublisher((org.reactivestreams.Publisher<T>) mono);
            }
            throw new IllegalArgumentException("Provided object is not a Mono (Publisher)");
        } catch (ClassCastException e) {
            throw new UnsupportedOperationException("reactor-core dependency not found. Add io.projectreactor:reactor-core to use this method.", e);
        }
    }
    
    // ============ Helper Classes ============
    
    static class EmitterImpl<T> implements Emitter<T>, Disposable {
        private final Observer<? super T> observer;
        private volatile boolean disposed;
        private Disposable resource;
        
        EmitterImpl(Observer<? super T> observer) {
            this.observer = observer;
        }
        
        @Override
        public void onNext(T value) {
            if (!disposed) {
                observer.onNext(value);
            }
        }
        
        @Override
        public void onError(Throwable error) {
            if (!disposed) {
                disposed = true;
                observer.onError(error);
            }
        }
        
        @Override
        public void onComplete() {
            if (!disposed) {
                disposed = true;
                observer.onComplete();
            }
        }
        
        @Override
        public boolean isDisposed() {
            return disposed;
        }
        
        @Override
        public void setDisposable(Disposable disposable) {
            this.resource = disposable;
        }
        
        @Override
        public void dispose() {
            if (!disposed) {
                disposed = true;
                if (resource != null) {
                    resource.dispose();
                }
            }
        }
    }
    
    static class DisposableObserver<T> implements Observer<T>, Disposable {
        private final Consumer<T> onNext;
        private final Consumer<Throwable> onError;
        private final Runnable onComplete;
        private volatile boolean disposed;
        private Disposable upstream;
        
        DisposableObserver(Consumer<T> onNext, Consumer<Throwable> onError, Runnable onComplete) {
            this.onNext = onNext;
            this.onError = onError;
            this.onComplete = onComplete;
        }
        
        @Override
        public void onSubscribe(Disposable d) {
            this.upstream = d;
        }
        
        @Override
        public void onNext(T value) {
            if (!disposed) {
                try {
                    onNext.accept(value);
                } catch (Exception e) {
                    onError(e);
                }
            }
        }
        
        @Override
        public void onError(Throwable error) {
            if (!disposed) {
                disposed = true;
                try {
                    onError.accept(error);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        
        @Override
        public void onComplete() {
            if (!disposed) {
                disposed = true;
                try {
                    onComplete.run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        
        @Override
        public void dispose() {
            disposed = true;
            if (upstream != null) {
                upstream.dispose();
            }
        }
        
        @Override
        public boolean isDisposed() {
            return disposed;
        }
    }
    
    /**
     * Observer wrapper for subscribeOn operator that manages disposal across schedulers.
     */
    static class SubscribeOnObserver<T> implements Observer<T>, Disposable {
        private final Observer<? super T> downstream;
        private Disposable upstream;
        private Disposable schedulerDisposable;
        private volatile boolean disposed;
        
        SubscribeOnObserver(Observer<? super T> downstream) {
            this.downstream = downstream;
        }
        
        void setSchedulerDisposable(Disposable d) {
            this.schedulerDisposable = d;
        }
        
        @Override
        public void onSubscribe(Disposable d) {
            this.upstream = d;
        }
        
        @Override
        public void onNext(T value) {
            if (!disposed) {
                downstream.onNext(value);
            }
        }
        
        @Override
        public void onError(Throwable error) {
            if (!disposed) {
                disposed = true;
                downstream.onError(error);
            }
        }
        
        @Override
        public void onComplete() {
            if (!disposed) {
                disposed = true;
                downstream.onComplete();
            }
        }
        
        @Override
        public void dispose() {
            if (!disposed) {
                disposed = true;
                if (upstream != null) {
                    upstream.dispose();
                }
                if (schedulerDisposable != null) {
                    schedulerDisposable.dispose();
                }
            }
        }
        
        @Override
        public boolean isDisposed() {
            return disposed;
        }
    }
}
