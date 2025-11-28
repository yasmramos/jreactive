package com.reactive.core;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A Subject that emits all items to all subscribed observers from the point of subscription.
 * 
 * <p>PublishSubject does not store any values - observers only receive items emitted
 * after they subscribe. Items emitted before subscription are not received.
 * 
 * <p><strong>Key characteristics:</strong>
 * <ul>
 *   <li>No replay - observers only get future events</li>
 *   <li>Thread-safe - supports concurrent subscriptions and emissions</li>
 *   <li>Hot observable - emits regardless of subscriptions</li>
 *   <li>Multicasting - distributes events to all current observers</li>
 * </ul>
 * 
 * <p><strong>Use cases:</strong>
 * <ul>
 *   <li>Event buses - distributing application events</li>
 *   <li>UI events - button clicks, key presses</li>
 *   <li>Real-time data streams - sensor data, stock ticks</li>
 *   <li>Notifications - push notifications, alerts</li>
 * </ul>
 * 
 * <p>Example:
 * <pre>{@code
 * PublishSubject<String> subject = PublishSubject.create();
 * 
 * subject.subscribe(s -> System.out.println("Observer 1: " + s));
 * 
 * subject.onNext("A");  // Observer 1 receives "A"
 * subject.onNext("B");  // Observer 1 receives "B"
 * 
 * subject.subscribe(s -> System.out.println("Observer 2: " + s));
 * 
 * subject.onNext("C");  // Both observers receive "C"
 * subject.onComplete();
 * }</pre>
 * 
 * <p>Example - Event bus pattern:
 * <pre>{@code
 * // Central event bus
 * public class EventBus {
 *     private static final PublishSubject<Event> events = PublishSubject.create();
 *     
 *     public static Observable<Event> events() {
 *         return events;
 *     }
 *     
 *     public static void post(Event event) {
 *         events.onNext(event);
 *     }
 * }
 * 
 * // Subscribe to events
 * EventBus.events()
 *     .filter(e -> e.getType() == EventType.USER_LOGIN)
 *     .subscribe(e -> handleLogin(e));
 * }</pre>
 * 
 * @param <T> the type of items emitted
 * @see Subject
 * @see BehaviorSubject
 * @see ReplaySubject
 */
public final class PublishSubject<T> extends Subject<T> {
    
    private final AtomicReference<PublishDisposable<T>[]> observers;
    private final AtomicBoolean terminated;
    private volatile Throwable error;
    
    @SuppressWarnings("rawtypes")
    private static final PublishDisposable[] EMPTY = new PublishDisposable[0];
    
    @SuppressWarnings("rawtypes")
    private static final PublishDisposable[] TERMINATED = new PublishDisposable[0];
    
    private PublishSubject() {
        this.observers = new AtomicReference<>(EMPTY);
        this.terminated = new AtomicBoolean(false);
    }
    
    /**
     * Creates a new PublishSubject.
     * 
     * @param <T> the type of items to emit
     * @return a new PublishSubject instance
     */
    public static <T> PublishSubject<T> create() {
        return new PublishSubject<>();
    }
    
    @Override
    protected void subscribeActual(Observer<? super T> observer) {
        PublishDisposable<T> pd = new PublishDisposable<>(observer, this);
        observer.onSubscribe(pd);
        
        if (add(pd)) {
            // Suscripción exitosa
            if (pd.isDisposed()) {
                remove(pd);
            }
        } else {
            // Ya terminado
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
        if (terminated.get()) {
            d.dispose();
        }
    }
    
    @Override
    public void onNext(T value) {
        if (value == null) {
            onError(new NullPointerException("onNext called with null"));
            return;
        }
        
        if (terminated.get()) {
            return;
        }
        
        for (PublishDisposable<T> pd : observers.get()) {
            pd.onNext(value);
        }
    }
    
    @Override
    public void onError(Throwable e) {
        if (e == null) {
            e = new NullPointerException("onError called with null");
        }
        
        if (terminated.compareAndSet(false, true)) {
            error = e;
            PublishDisposable<T>[] array = observers.getAndSet(TERMINATED);
            for (PublishDisposable<T> pd : array) {
                pd.onError(e);
            }
        }
    }
    
    @Override
    public void onComplete() {
        if (terminated.compareAndSet(false, true)) {
            PublishDisposable<T>[] array = observers.getAndSet(TERMINATED);
            for (PublishDisposable<T> pd : array) {
                pd.onComplete();
            }
        }
    }
    
    @Override
    public boolean hasComplete() {
        return terminated.get() && error == null;
    }
    
    @Override
    public boolean hasThrowable() {
        return terminated.get() && error != null;
    }
    
    @Override
    public Throwable getThrowable() {
        return terminated.get() ? error : null;
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
     * Agrega un observer al subject.
     * 
     * @param pd El PublishDisposable a agregar
     * @return true si se agregó exitosamente, false si el subject ya terminó
     */
    @SuppressWarnings("unchecked")
    private boolean add(PublishDisposable<T> pd) {
        for (;;) {
            PublishDisposable<T>[] current = observers.get();
            if (current == TERMINATED) {
                return false;
            }
            
            int n = current.length;
            PublishDisposable<T>[] next = new PublishDisposable[n + 1];
            System.arraycopy(current, 0, next, 0, n);
            next[n] = pd;
            
            if (observers.compareAndSet(current, next)) {
                return true;
            }
        }
    }
    
    /**
     * Remueve un observer del subject.
     * 
     * @param pd El PublishDisposable a remover
     */
    @SuppressWarnings("unchecked")
    private void remove(PublishDisposable<T> pd) {
        for (;;) {
            PublishDisposable<T>[] current = observers.get();
            if (current == TERMINATED || current == EMPTY) {
                return;
            }
            
            int n = current.length;
            int index = -1;
            
            for (int i = 0; i < n; i++) {
                if (current[i] == pd) {
                    index = i;
                    break;
                }
            }
            
            if (index < 0) {
                return;
            }
            
            PublishDisposable<T>[] next;
            if (n == 1) {
                next = EMPTY;
            } else {
                next = new PublishDisposable[n - 1];
                System.arraycopy(current, 0, next, 0, index);
                System.arraycopy(current, index + 1, next, index, n - index - 1);
            }
            
            if (observers.compareAndSet(current, next)) {
                return;
            }
        }
    }
    
    /**
     * Disposable que representa la suscripción de un observer al PublishSubject.
     */
    static final class PublishDisposable<T> implements Disposable {
        final Observer<? super T> downstream;
        final PublishSubject<T> parent;
        volatile boolean disposed;
        
        PublishDisposable(Observer<? super T> downstream, PublishSubject<T> parent) {
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
