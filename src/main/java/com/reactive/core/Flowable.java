package com.reactive.core;

import com.reactive.backpressure.BackpressureStrategy;
import com.reactive.schedulers.Schedulers;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

/**
 * A reactive stream that supports backpressure.
 * Unlike Observable, Flowable allows subscribers to control the flow of items
 * by requesting specific amounts via the Subscription.
 *
 * @param <T> the type of items emitted by this Flowable
 */
public abstract class Flowable<T> {
    
    /**
     * Subscribe to this Flowable with a Subscriber.
     *
     * @param subscriber the subscriber to receive items
     */
    public abstract void subscribe(Subscriber<? super T> subscriber);
    
    // ============ Factory Methods ============
    
    /**
     * Creates a Flowable that emits the given items.
     */
    @SafeVarargs
    public static <T> Flowable<T> just(T... items) {
        return new Flowable<T>() {
            @Override
            public void subscribe(Subscriber<? super T> subscriber) {
                subscriber.onSubscribe(new Subscription() {
                    private final AtomicLong requested = new AtomicLong(0);
                    private final AtomicInteger index = new AtomicInteger(0);
                    private volatile boolean cancelled = false;
                    
                    @Override
                    public void request(long n) {
                        if (n <= 0 || cancelled) return;
                        
                        long current = requested.getAndAdd(n);
                        if (current == 0) {
                            emitItems(n);
                        }
                    }
                    
                    @Override
                    public void cancel() {
                        cancelled = true;
                    }
                    
                    private void emitItems(long n) {
                        long emitted = 0;
                        int idx = index.get();
                        
                        while (emitted < n && idx < items.length && !cancelled) {
                            subscriber.onNext(items[idx]);
                            idx++;
                            emitted++;
                            index.set(idx);
                        }
                        
                        long remaining = requested.addAndGet(-emitted);
                        
                        if (idx >= items.length && !cancelled) {
                            subscriber.onComplete();
                        } else if (remaining > 0 && !cancelled) {
                            emitItems(remaining);
                        }
                    }
                });
            }
        };
    }
    
    /**
     * Creates a Flowable from an Iterable.
     */
    public static <T> Flowable<T> fromIterable(Iterable<T> iterable) {
        return new Flowable<T>() {
            @Override
            public void subscribe(Subscriber<? super T> subscriber) {
                subscriber.onSubscribe(new Subscription() {
                    private final Iterator<T> iterator = iterable.iterator();
                    private final AtomicLong requested = new AtomicLong(0);
                    private final AtomicBoolean emitting = new AtomicBoolean(false);
                    private volatile boolean cancelled = false;
                    
                    @Override
                    public void request(long n) {
                        if (n <= 0 || cancelled) return;
                        
                        requested.addAndGet(n);
                        if (emitting.compareAndSet(false, true)) {
                            emitItems();
                        }
                    }
                    
                    @Override
                    public void cancel() {
                        cancelled = true;
                    }
                    
                    private void emitItems() {
                        try {
                            while (!cancelled) {
                                long r = requested.get();
                                if (r == 0) break;
                                
                                long emitted = 0;
                                while (emitted < r && iterator.hasNext() && !cancelled) {
                                    subscriber.onNext(iterator.next());
                                    emitted++;
                                }
                                
                                requested.addAndGet(-emitted);
                                
                                if (!iterator.hasNext() && !cancelled) {
                                    subscriber.onComplete();
                                    break;
                                }
                            }
                        } catch (Exception e) {
                            if (!cancelled) {
                                subscriber.onError(e);
                            }
                        } finally {
                            emitting.set(false);
                        }
                    }
                });
            }
        };
    }
    
    /**
     * Creates a Flowable from a range of integers.
     */
    public static Flowable<Integer> range(int start, int count) {
        return new Flowable<Integer>() {
            @Override
            public void subscribe(Subscriber<? super Integer> subscriber) {
                subscriber.onSubscribe(new Subscription() {
                    private final AtomicLong requested = new AtomicLong(0);
                    private final AtomicInteger index = new AtomicInteger(0);
                    private volatile boolean cancelled = false;
                    
                    @Override
                    public void request(long n) {
                        if (n <= 0 || cancelled) return;
                        
                        long current = requested.getAndAdd(n);
                        if (current == 0) {
                            emitItems(n);
                        }
                    }
                    
                    @Override
                    public void cancel() {
                        cancelled = true;
                    }
                    
                    private void emitItems(long n) {
                        long emitted = 0;
                        int idx = index.get();
                        
                        while (emitted < n && idx < count && !cancelled) {
                            subscriber.onNext(start + idx);
                            idx++;
                            emitted++;
                            index.set(idx);
                        }
                        
                        long remaining = requested.addAndGet(-emitted);
                        
                        if (idx >= count && !cancelled) {
                            subscriber.onComplete();
                        } else if (remaining > 0 && !cancelled) {
                            emitItems(remaining);
                        }
                    }
                });
            }
        };
    }
    
    /**
     * Creates a Flowable using a generator function that respects backpressure.
     */
    public static <T> Flowable<T> create(Consumer<FlowableEmitter<T>> source, BackpressureStrategy strategy) {
        return new Flowable<T>() {
            @Override
            public void subscribe(Subscriber<? super T> subscriber) {
                FlowableEmitterImpl<T> emitter = new FlowableEmitterImpl<>(subscriber, strategy);
                subscriber.onSubscribe(emitter);
                try {
                    source.accept(emitter);
                } catch (Exception e) {
                    emitter.onError(e);
                }
            }
        };
    }
    
    /**
     * Creates an empty Flowable that completes immediately.
     */
    public static <T> Flowable<T> empty() {
        return new Flowable<T>() {
            @Override
            public void subscribe(Subscriber<? super T> subscriber) {
                subscriber.onSubscribe(new Subscription() {
                    @Override
                    public void request(long n) {
                        subscriber.onComplete();
                    }
                    
                    @Override
                    public void cancel() {
                    }
                });
            }
        };
    }
    
    /**
     * Creates a Flowable that never emits anything.
     */
    public static <T> Flowable<T> never() {
        return new Flowable<T>() {
            @Override
            public void subscribe(Subscriber<? super T> subscriber) {
                subscriber.onSubscribe(new Subscription() {
                    @Override
                    public void request(long n) {
                        // Never emits
                    }
                    
                    @Override
                    public void cancel() {
                    }
                });
            }
        };
    }
    
    /**
     * Creates a Flowable that emits an error.
     */
    public static <T> Flowable<T> error(Throwable error) {
        return new Flowable<T>() {
            @Override
            public void subscribe(Subscriber<? super T> subscriber) {
                subscriber.onSubscribe(new Subscription() {
                    @Override
                    public void request(long n) {
                        subscriber.onError(error);
                    }
                    
                    @Override
                    public void cancel() {
                    }
                });
            }
        };
    }
    
    // ============ Transformation Operators ============
    
    /**
     * Maps each item to another value.
     */
    public final <R> Flowable<R> map(Function<? super T, ? extends R> mapper) {
        return new Flowable<R>() {
            @Override
            public void subscribe(Subscriber<? super R> subscriber) {
                Flowable.this.subscribe(new Subscriber<T>() {
                    @Override
                    public void onSubscribe(Subscription subscription) {
                        subscriber.onSubscribe(subscription);
                    }
                    
                    @Override
                    public void onNext(T item) {
                        try {
                            R result = mapper.apply(item);
                            subscriber.onNext(result);
                        } catch (Exception e) {
                            onError(e);
                        }
                    }
                    
                    @Override
                    public void onError(Throwable throwable) {
                        subscriber.onError(throwable);
                    }
                    
                    @Override
                    public void onComplete() {
                        subscriber.onComplete();
                    }
                });
            }
        };
    }
    
    /**
     * Filters items based on a predicate.
     */
    public final Flowable<T> filter(Predicate<? super T> predicate) {
        return new Flowable<T>() {
            @Override
            public void subscribe(Subscriber<? super T> subscriber) {
                Flowable.this.subscribe(new Subscriber<T>() {
                    @Override
                    public void onSubscribe(Subscription subscription) {
                        subscriber.onSubscribe(subscription);
                    }
                    
                    @Override
                    public void onNext(T item) {
                        try {
                            if (predicate.test(item)) {
                                subscriber.onNext(item);
                            }
                        } catch (Exception e) {
                            onError(e);
                        }
                    }
                    
                    @Override
                    public void onError(Throwable throwable) {
                        subscriber.onError(throwable);
                    }
                    
                    @Override
                    public void onComplete() {
                        subscriber.onComplete();
                    }
                });
            }
        };
    }
    
    /**
     * Takes only the first n items.
     */
    public final Flowable<T> take(long n) {
        return new Flowable<T>() {
            @Override
            public void subscribe(Subscriber<? super T> subscriber) {
                Flowable.this.subscribe(new Subscriber<T>() {
                    private final AtomicLong remaining = new AtomicLong(n);
                    private Subscription upstream;
                    
                    @Override
                    public void onSubscribe(Subscription subscription) {
                        this.upstream = subscription;
                        subscriber.onSubscribe(subscription);
                    }
                    
                    @Override
                    public void onNext(T item) {
                        long r = remaining.getAndDecrement();
                        if (r > 0) {
                            subscriber.onNext(item);
                            if (r == 1) {
                                upstream.cancel();
                                subscriber.onComplete();
                            }
                        }
                    }
                    
                    @Override
                    public void onError(Throwable throwable) {
                        if (remaining.get() > 0) {
                            subscriber.onError(throwable);
                        }
                    }
                    
                    @Override
                    public void onComplete() {
                        if (remaining.get() > 0) {
                            subscriber.onComplete();
                        }
                    }
                });
            }
        };
    }
    
    /**
     * Skips the first n items.
     */
    public final Flowable<T> skip(long n) {
        return new Flowable<T>() {
            @Override
            public void subscribe(Subscriber<? super T> subscriber) {
                Flowable.this.subscribe(new Subscriber<T>() {
                    private final AtomicLong remaining = new AtomicLong(n);
                    
                    @Override
                    public void onSubscribe(Subscription subscription) {
                        subscriber.onSubscribe(subscription);
                    }
                    
                    @Override
                    public void onNext(T item) {
                        if (remaining.getAndDecrement() <= 0) {
                            subscriber.onNext(item);
                        }
                    }
                    
                    @Override
                    public void onError(Throwable throwable) {
                        subscriber.onError(throwable);
                    }
                    
                    @Override
                    public void onComplete() {
                        subscriber.onComplete();
                    }
                });
            }
        };
    }
    
    /**
     * Converts this Flowable to an Observable (loses backpressure support).
     */
    public final Observable<T> toObservable() {
        return new Observable<T>() {
            @Override
            public void subscribe(Observer<? super T> observer) {
                Flowable.this.subscribe(new Subscriber<T>() {
                    @Override
                    public void onSubscribe(Subscription subscription) {
                        observer.onSubscribe(new Disposable() {
                            @Override
                            public void dispose() {
                                subscription.cancel();
                            }
                            
                            @Override
                            public boolean isDisposed() {
                                return false;
                            }
                        });
                        // Request unbounded
                        subscription.request(Long.MAX_VALUE);
                    }
                    
                    @Override
                    public void onNext(T item) {
                        observer.onNext(item);
                    }
                    
                    @Override
                    public void onError(Throwable throwable) {
                        observer.onError(throwable);
                    }
                    
                    @Override
                    public void onComplete() {
                        observer.onComplete();
                    }
                });
            }
        };
    }
    
    // ============ FlowableEmitter Interface ============
    
    /**
     * Emitter for creating Flowables with backpressure support.
     */
    public interface FlowableEmitter<T> extends Subscription {
        /**
         * Emit an item to the subscriber.
         */
        void onNext(T item);
        
        /**
         * Signal an error to the subscriber.
         */
        void onError(Throwable error);
        
        /**
         * Signal completion to the subscriber.
         */
        void onComplete();
        
        /**
         * Returns the current number of requested items.
         */
        long requested();
        
        /**
         * Returns true if the subscription was cancelled.
         */
        boolean isCancelled();
    }
    
    // ============ FlowableEmitter Implementation ============
    
    private static class FlowableEmitterImpl<T> implements FlowableEmitter<T> {
        private final Subscriber<? super T> subscriber;
        private final BackpressureStrategy strategy;
        private final AtomicLong requested = new AtomicLong(0);
        private final Queue<T> queue;
        private volatile boolean cancelled = false;
        private volatile boolean done = false;
        private Throwable error = null;
        
        public FlowableEmitterImpl(Subscriber<? super T> subscriber, BackpressureStrategy strategy) {
            this.subscriber = subscriber;
            this.strategy = strategy;
            
            switch (strategy) {
                case BUFFER:
                    this.queue = new ConcurrentLinkedQueue<>();
                    break;
                case DROP:
                case DROP_LATEST:
                case DROP_OLDEST:
                    this.queue = new LinkedBlockingQueue<>(128); // Bounded queue
                    break;
                case ERROR:
                    this.queue = new LinkedBlockingQueue<>(128); // Bounded queue
                    break;
                default:
                    this.queue = new ConcurrentLinkedQueue<>();
            }
        }
        
        @Override
        public void onNext(T item) {
            if (done || cancelled) return;
            
            long r = requested.get();
            
            if (r > 0) {
                // Can emit immediately
                if (requested.compareAndSet(r, r - 1)) {
                    subscriber.onNext(item);
                } else {
                    // Retry
                    onNext(item);
                }
            } else {
                // Need to buffer according to strategy
                handleBackpressure(item);
            }
        }
        
        private void handleBackpressure(T item) {
            switch (strategy) {
                case BUFFER:
                    queue.offer(item);
                    break;
                    
                case DROP:
                    // Drop the item
                    break;
                    
                case DROP_LATEST:
                    if (!queue.offer(item)) {
                        // Queue is full, drop this item
                    }
                    break;
                    
                case DROP_OLDEST:
                    if (!queue.offer(item)) {
                        // Queue is full, remove oldest and add new
                        queue.poll();
                        queue.offer(item);
                    }
                    break;
                    
                case ERROR:
                    if (!queue.offer(item)) {
                        onError(new IllegalStateException("Buffer overflow - backpressure not respected"));
                    }
                    break;
            }
        }
        
        @Override
        public void onError(Throwable error) {
            if (done || cancelled) return;
            this.error = error;
            this.done = true;
            drain();
        }
        
        @Override
        public void onComplete() {
            if (done || cancelled) return;
            this.done = true;
            drain();
        }
        
        @Override
        public void request(long n) {
            if (n <= 0 || cancelled) return;
            requested.addAndGet(n);
            drain();
        }
        
        @Override
        public void cancel() {
            cancelled = true;
            queue.clear();
        }
        
        @Override
        public long requested() {
            return requested.get();
        }
        
        @Override
        public boolean isCancelled() {
            return cancelled;
        }
        
        private void drain() {
            if (cancelled) return;
            
            while (!cancelled) {
                long r = requested.get();
                if (r == 0) break;
                
                T item = queue.poll();
                if (item != null) {
                    requested.decrementAndGet();
                    subscriber.onNext(item);
                } else {
                    break;
                }
            }
            
            if (done && queue.isEmpty() && !cancelled) {
                if (error != null) {
                    subscriber.onError(error);
                } else {
                    subscriber.onComplete();
                }
            }
        }
    }
}
