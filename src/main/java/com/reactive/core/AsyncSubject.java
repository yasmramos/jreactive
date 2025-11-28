package com.reactive.core;

import java.util.concurrent.atomic.AtomicReference;

/**
 * AsyncSubject solo emite el último valor (y solo el último valor) cuando
 * la secuencia se completa.
 * 
 * Si la secuencia se completa sin haber emitido ningún valor, el AsyncSubject
 * solo emite la señal de completado. Si termina con error, solo emite el error.
 * 
 * Es thread-safe y permite múltiples observers.
 * 
 * <p>Ejemplo:
 * <pre>{@code
 * AsyncSubject<String> subject = AsyncSubject.create();
 * 
 * subject.subscribe(s -> System.out.println("Observer 1: " + s));
 * 
 * subject.onNext("A");
 * subject.onNext("B");
 * subject.onNext("C");
 * 
 * subject.subscribe(s -> System.out.println("Observer 2: " + s));
 * 
 * // Ningún observer recibe valores aún
 * 
 * subject.onComplete();
 * // Ahora ambos observers reciben "C" (último valor)
 * // seguido de onComplete()
 * }</pre>
 * 
 * <p>Sin valores:
 * <pre>{@code
 * AsyncSubject<String> subject = AsyncSubject.create();
 * 
 * subject.subscribe(
 *     s -> System.out.println("onNext: " + s),
 *     e -> System.out.println("onError: " + e),
 *     () -> System.out.println("onComplete")
 * );
 * 
 * subject.onComplete();
 * // Output: "onComplete" (sin onNext)
 * }</pre>
 * 
 * @param <T> Tipo de elementos que emite
 */
public final class AsyncSubject<T> extends Subject<T> {
    
    private final AtomicReference<AsyncDisposable<T>[]> observers;
    
    private T value;
    private Throwable error;
    private volatile boolean done;
    
    @SuppressWarnings("rawtypes")
    private static final AsyncDisposable[] EMPTY = new AsyncDisposable[0];
    
    @SuppressWarnings("rawtypes")
    private static final AsyncDisposable[] TERMINATED = new AsyncDisposable[0];
    
    private AsyncSubject() {
        this.observers = new AtomicReference<>(EMPTY);
    }
    
    /**
     * Crea un nuevo AsyncSubject.
     * 
     * @param <T> Tipo de elementos
     * @return Nuevo AsyncSubject
     */
    public static <T> AsyncSubject<T> create() {
        return new AsyncSubject<>();
    }
    
    @Override
    protected void subscribeActual(Observer<? super T> observer) {
        AsyncDisposable<T> ad = new AsyncDisposable<>(observer, this);
        observer.onSubscribe(ad);
        
        if (add(ad)) {
            if (ad.isDisposed()) {
                remove(ad);
            }
        } else {
            // Ya terminado
            Throwable ex = error;
            if (ex != null) {
                observer.onError(ex);
            } else {
                T v = value;
                if (v != null) {
                    observer.onNext(v);
                }
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
        
        // Solo almacenamos el valor, no lo emitimos hasta completar
        value = item;
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
        value = null;
        done = true;
        
        AsyncDisposable<T>[] array = observers.getAndSet(TERMINATED);
        for (AsyncDisposable<T> ad : array) {
            ad.onError(e);
        }
    }
    
    @Override
    public void onComplete() {
        if (done) {
            return;
        }
        
        done = true;
        T v = value;
        
        AsyncDisposable<T>[] array = observers.getAndSet(TERMINATED);
        
        if (v != null) {
            for (AsyncDisposable<T> ad : array) {
                ad.onNext(v);
                ad.onComplete();
            }
        } else {
            for (AsyncDisposable<T> ad : array) {
                ad.onComplete();
            }
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
     * Obtiene el último valor emitido, o null si no hay valor o no ha completado.
     * 
     * @return El último valor, o null
     */
    public T getValue() {
        return done ? value : null;
    }
    
    /**
     * Indica si el AsyncSubject tiene un valor.
     * 
     * @return true si tiene un valor y ha completado
     */
    public boolean hasValue() {
        return done && value != null;
    }
    
    @SuppressWarnings("unchecked")
    private boolean add(AsyncDisposable<T> ad) {
        for (;;) {
            AsyncDisposable<T>[] current = observers.get();
            if (current == TERMINATED) {
                return false;
            }
            
            int n = current.length;
            AsyncDisposable<T>[] next = new AsyncDisposable[n + 1];
            System.arraycopy(current, 0, next, 0, n);
            next[n] = ad;
            
            if (observers.compareAndSet(current, next)) {
                return true;
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    void remove(AsyncDisposable<T> ad) {
        for (;;) {
            AsyncDisposable<T>[] current = observers.get();
            if (current == TERMINATED || current == EMPTY) {
                return;
            }
            
            int n = current.length;
            int index = -1;
            
            for (int i = 0; i < n; i++) {
                if (current[i] == ad) {
                    index = i;
                    break;
                }
            }
            
            if (index < 0) {
                return;
            }
            
            AsyncDisposable<T>[] next;
            if (n == 1) {
                next = EMPTY;
            } else {
                next = new AsyncDisposable[n - 1];
                System.arraycopy(current, 0, next, 0, index);
                System.arraycopy(current, index + 1, next, index, n - index - 1);
            }
            
            if (observers.compareAndSet(current, next)) {
                return;
            }
        }
    }
    
    /**
     * Disposable para suscripciones a AsyncSubject.
     */
    static final class AsyncDisposable<T> implements Disposable {
        final Observer<? super T> downstream;
        final AsyncSubject<T> parent;
        
        volatile boolean disposed;
        
        AsyncDisposable(Observer<? super T> downstream, AsyncSubject<T> parent) {
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
