package com.reactive.core;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A Subject that emits the most recent item (or an initial value) and all subsequent
 * items to each subscribed observer.
 * 
 * <p>When an observer subscribes, it immediately receives the most recent value
 * (or the initial value if nothing has been emitted yet), followed by all subsequent values.
 * 
 * <p><strong>Key characteristics:</strong>
 * <ul>
 *   <li>Replays last value - new observers get the current state immediately</li>
 *   <li>Requires initial value or first emission before observers get anything</li>
 *   <li>Thread-safe - supports concurrent subscriptions and emissions</li>
 *   <li>Hot observable - maintains state regardless of subscriptions</li>
 *   <li>Stateful - always has a current value after initialization</li>
 * </ul>
 * 
 * <p><strong>Use cases:</strong>
 * <ul>
 *   <li>State management - current user, configuration, settings</li>
 *   <li>Caching - last known value of frequently accessed data</li>
 *   <li>Model-View binding - UI reflecting current model state</li>
 *   <li>Current values - temperature, location, connection status</li>
 * </ul>
 * 
 * <p>Example:
 * <pre>{@code
 * BehaviorSubject<String> subject = BehaviorSubject.createDefault("Initial");
 * 
 * subject.subscribe(s -> System.out.println("Observer 1: " + s));
 * // Observer 1 receives "Initial"
 * 
 * subject.onNext("A");  // Observer 1 receives "A"
 * 
 * subject.subscribe(s -> System.out.println("Observer 2: " + s));
 * // Observer 2 receives "A" (last value)
 * 
 * subject.onNext("B");  // Both observers receive "B"
 * }</pre>
 * 
 * <p>Example - User state management:
 * <pre>{@code
 * public class UserManager {
 *     private final BehaviorSubject<User> currentUser = 
 *         BehaviorSubject.createDefault(User.guest());
 *     
 *     public Observable<User> getCurrentUser() {
 *         return currentUser;
 *     }
 *     
 *     public void login(User user) {
 *         currentUser.onNext(user);
 *     }
 *     
 *     public void logout() {
 *         currentUser.onNext(User.guest());
 *     }
 * }
 * 
 * // UI automatically updates when user changes
 * userManager.getCurrentUser()
 *     .subscribe(user -> updateUI(user));
 * }</pre>
 * 
 * @param <T> the type of items emitted
 * @see Subject
 * @see PublishSubject
 * @see ReplaySubject
 */
public final class BehaviorSubject<T> extends Subject<T> {
    
    private final AtomicReference<BehaviorDisposable<T>[]> observers;
    private final ReadWriteLock lock;
    private final Lock readLock;
    private final Lock writeLock;
    
    private final AtomicReference<Object> value;
    private volatile Throwable error;
    private volatile boolean done;
    
    @SuppressWarnings("rawtypes")
    private static final BehaviorDisposable[] EMPTY = new BehaviorDisposable[0];
    
    @SuppressWarnings("rawtypes")
    private static final BehaviorDisposable[] TERMINATED = new BehaviorDisposable[0];
    
    private static final Object EMPTY_VALUE = new Object();
    
    private BehaviorSubject() {
        this.observers = new AtomicReference<>(EMPTY);
        this.lock = new ReentrantReadWriteLock();
        this.readLock = lock.readLock();
        this.writeLock = lock.writeLock();
        this.value = new AtomicReference<>(EMPTY_VALUE);
    }
    
    private BehaviorSubject(T defaultValue) {
        this();
        this.value.set(defaultValue);
    }
    
    /**
     * Creates a new BehaviorSubject without an initial value.
     * 
     * <p>Observers will not receive any value until {@link #onNext(Object)} is called.
     * 
     * @param <T> the type of items to emit
     * @return a new BehaviorSubject instance
     */
    public static <T> BehaviorSubject<T> create() {
        return new BehaviorSubject<>();
    }
    
    /**
     * Creates a new BehaviorSubject with an initial value.
     * 
     * <p>New observers will immediately receive the initial value upon subscription.
     * 
     * @param <T> the type of items to emit
     * @param defaultValue the initial value (must not be null)
     * @return a new BehaviorSubject instance
     * @throws NullPointerException if defaultValue is null
     */
    public static <T> BehaviorSubject<T> createDefault(T defaultValue) {
        if (defaultValue == null) {
            throw new NullPointerException("defaultValue is null");
        }
        return new BehaviorSubject<>(defaultValue);
    }
    
    @Override
    protected void subscribeActual(Observer<? super T> observer) {
        BehaviorDisposable<T> bd = new BehaviorDisposable<>(observer, this);
        observer.onSubscribe(bd);
        
        if (add(bd)) {
            if (bd.isDisposed()) {
                remove(bd);
            } else {
                bd.emitFirst();
            }
        } else {
            Throwable ex = error;
            if (ex != null) {
                observer.onError(ex);
            } else {
                observer.onComplete();
            }
        }
    }
    
    @Override
    public void onSubscribe(Disposable d) {
        if (done) {
            d.dispose();
        }
    }
    
    @Override
    public void onNext(T item) {
        if (item == null) {
            onError(new NullPointerException("onNext called with null"));
            return;
        }
        
        if (done) {
            return;
        }
        
        value.set(item);
        
        for (BehaviorDisposable<T> bd : observers.get()) {
            bd.onNext(item);
        }
    }
    
    @Override
    public void onError(Throwable e) {
        if (e == null) {
            e = new NullPointerException("onError called with null");
        }
        
        if (done) {
            return;
        }
        
        error = e;
        value.set(e);
        done = true;
        
        BehaviorDisposable<T>[] array = observers.getAndSet(TERMINATED);
        for (BehaviorDisposable<T> bd : array) {
            bd.onError(e);
        }
    }
    
    @Override
    public void onComplete() {
        if (done) {
            return;
        }
        
        done = true;
        
        BehaviorDisposable<T>[] array = observers.getAndSet(TERMINATED);
        for (BehaviorDisposable<T> bd : array) {
            bd.onComplete();
        }
    }
    
    @Override
    public boolean hasComplete() {
        return done && error == null;
    }
    
    @Override
    public boolean hasThrowable() {
        return done && error != null;
    }
    
    @Override
    public Throwable getThrowable() {
        return done ? error : null;
    }
    
    @Override
    public boolean hasObservers() {
        return observers.get().length > 0;
    }
    
    @Override
    public int observerCount() {
        return observers.get().length;
    }
    
    /**
     * Obtiene el valor actual del BehaviorSubject.
     * 
     * @return El valor actual, o null si no hay valor o ha terminado
     */
    @SuppressWarnings("unchecked")
    public T getValue() {
        Object o = value.get();
        if (o == EMPTY_VALUE || o instanceof Throwable) {
            return null;
        }
        return (T) o;
    }
    
    /**
     * Indica si el BehaviorSubject tiene un valor.
     * 
     * @return true si tiene un valor (no vacío, no error, no completado)
     */
    public boolean hasValue() {
        Object o = value.get();
        return o != EMPTY_VALUE && !(o instanceof Throwable);
    }
    
    /**
     * Obtiene el valor actual, puede ser null, un valor, o un error.
     * 
     * @return El valor actual encapsulado
     */
    Object getValueInternal() {
        return value.get();
    }
    
    /**
     * Agrega un observer al subject.
     */
    @SuppressWarnings("unchecked")
    private boolean add(BehaviorDisposable<T> bd) {
        for (;;) {
            BehaviorDisposable<T>[] current = observers.get();
            if (current == TERMINATED) {
                return false;
            }
            
            int n = current.length;
            BehaviorDisposable<T>[] next = new BehaviorDisposable[n + 1];
            System.arraycopy(current, 0, next, 0, n);
            next[n] = bd;
            
            if (observers.compareAndSet(current, next)) {
                return true;
            }
        }
    }
    
    /**
     * Remueve un observer del subject.
     */
    @SuppressWarnings("unchecked")
    void remove(BehaviorDisposable<T> bd) {
        for (;;) {
            BehaviorDisposable<T>[] current = observers.get();
            if (current == TERMINATED || current == EMPTY) {
                return;
            }
            
            int n = current.length;
            int index = -1;
            
            for (int i = 0; i < n; i++) {
                if (current[i] == bd) {
                    index = i;
                    break;
                }
            }
            
            if (index < 0) {
                return;
            }
            
            BehaviorDisposable<T>[] next;
            if (n == 1) {
                next = EMPTY;
            } else {
                next = new BehaviorDisposable[n - 1];
                System.arraycopy(current, 0, next, 0, index);
                System.arraycopy(current, index + 1, next, index, n - index - 1);
            }
            
            if (observers.compareAndSet(current, next)) {
                return;
            }
        }
    }
    
    /**
     * Disposable que representa la suscripción de un observer al BehaviorSubject.
     */
    static final class BehaviorDisposable<T> implements Disposable {
        final Observer<? super T> downstream;
        final BehaviorSubject<T> parent;
        
        boolean emitting;
        boolean fastPath;
        volatile boolean disposed;
        boolean next;
        
        BehaviorDisposable(Observer<? super T> downstream, BehaviorSubject<T> parent) {
            this.downstream = downstream;
            this.parent = parent;
        }
        
        @Override
        public void dispose() {
            if (!disposed) {
                disposed = true;
                parent.remove(this);
            }
        }
        
        @Override
        public boolean isDisposed() {
            return disposed;
        }
        
        void emitFirst() {
            if (disposed) {
                return;
            }
            
            Object o;
            synchronized (this) {
                if (disposed) {
                    return;
                }
                if (next) {
                    return;
                }
                
                o = parent.getValueInternal();
                emitting = true;
                next = true;
            }
            
            emitValue(o);
        }
        
        @SuppressWarnings("unchecked")
        void emitValue(Object value) {
            if (disposed) {
                return;
            }
            
            if (value == EMPTY_VALUE) {
                return;
            }
            
            if (value instanceof Throwable) {
                downstream.onError((Throwable) value);
                return;
            }
            
            downstream.onNext((T) value);
        }
        
        void onNext(T value) {
            if (!disposed) {
                downstream.onNext(value);
            }
        }
        
        void onError(Throwable e) {
            if (!disposed) {
                disposed = true;
                downstream.onError(e);
            }
        }
        
        void onComplete() {
            if (!disposed) {
                disposed = true;
                downstream.onComplete();
            }
        }
    }
}
