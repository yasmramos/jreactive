package com.reactive.core;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A Subject that stores all (or a limited number of) emitted items and replays them
 * to each new observer that subscribes.
 * 
 * <p>Each new observer receives all stored items in the buffer followed by all
 * subsequent items. This allows late subscribers to catch up on missed events.
 * 
 * <p><strong>Key characteristics:</strong>
 * <ul>
 *   <li>Replays history - new observers receive past events</li>
 *   <li>Configurable buffer - unlimited or size-bounded</li>
 *   <li>Thread-safe - supports concurrent subscriptions and emissions</li>
 *   <li>Hot observable - maintains event history regardless of subscriptions</li>
 *   <li>Memory consideration - unbounded replay can consume significant memory</li>
 * </ul>
 * 
 * <p><strong>Use cases:</strong>
 * <ul>
 *   <li>Event logs - maintaining full history of events</li>
 *   <li>Message history - chat messages, audit trails</li>
 *   <li>Debugging - capturing all events for analysis</li>
 *   <li>Late subscription - ensuring no events are missed</li>
 *   <li>Caching - recent items with bounded buffer</li>
 * </ul>
 * 
 * <p>Example - Unbounded replay:
 * <pre>{@code
 * ReplaySubject<String> subject = ReplaySubject.create();
 * 
 * subject.onNext("A");
 * subject.onNext("B");
 * 
 * subject.subscribe(s -> System.out.println("Observer 1: " + s));
 * // Observer 1 receives "A", "B"
 * 
 * subject.onNext("C");  // Observer 1 receives "C"
 * 
 * subject.subscribe(s -> System.out.println("Observer 2: " + s));
 * // Observer 2 receives "A", "B", "C"
 * }</pre>
 * 
 * <p>Example - Bounded buffer:
 * <pre>{@code
 * ReplaySubject<String> subject = ReplaySubject.createWithSize(2);
 * 
 * subject.onNext("A");
 * subject.onNext("B");
 * subject.onNext("C");
 * 
 * subject.subscribe(s -> System.out.println("Observer: " + s));
 * // Observer only receives "B", "C" (last 2)
 * }</pre>
 * 
 * <p>Example - Event log:
 * <pre>{@code
 * public class AuditLog {
 *     private final ReplaySubject<AuditEvent> events = ReplaySubject.create();
 *     
 *     public void log(AuditEvent event) {
 *         events.onNext(event);
 *     }
 *     
 *     public Observable<AuditEvent> getAllEvents() {
 *         return events;  // New subscribers get full history
 *     }
 * }
 * }</pre>
 * 
 * <p><strong>Warning:</strong> Unbounded ReplaySubject can lead to memory issues
 * if the event stream is long-lived. Consider using {@link #createWithSize(int)}
 * for bounded buffers.
 * 
 * @param <T> the type of items emitted
 * @see Subject
 * @see PublishSubject
 * @see BehaviorSubject
 */
public final class ReplaySubject<T> extends Subject<T> {
    
    private final AtomicReference<ReplayDisposable<T>[]> observers;
    private final ReplayBuffer<T> buffer;
    private volatile boolean done;
    
    @SuppressWarnings("rawtypes")
    private static final ReplayDisposable[] EMPTY = new ReplayDisposable[0];
    
    @SuppressWarnings("rawtypes")
    private static final ReplayDisposable[] TERMINATED = new ReplayDisposable[0];
    
    private ReplaySubject(ReplayBuffer<T> buffer) {
        this.buffer = buffer;
        this.observers = new AtomicReference<>(EMPTY);
    }
    
    /**
     * Creates a ReplaySubject that stores all values.
     * 
     * <p>All emitted items are cached and replayed to new subscribers.
     * 
     * <p><strong>Warning:</strong> This can lead to memory issues with long-lived
     * or high-frequency streams. Consider using {@link #createWithSize(int)} instead.
     * 
     * @param <T> the type of items to emit
     * @return a new ReplaySubject instance with unbounded buffer
     */
    public static <T> ReplaySubject<T> create() {
        return new ReplaySubject<>(new UnboundedReplayBuffer<>());
    }
    
    /**
     * Creates a ReplaySubject with a bounded buffer of specified size.
     * 
     * <p>Only the most recent {@code capacityHint} items are cached and replayed.
     * Older items are discarded when the buffer is full.
     * 
     * <p>This is useful for limiting memory usage while still providing recent history.
     * 
     * @param <T> the type of items to emit
     * @param capacityHint the maximum number of items to buffer (must be positive)
     * @return a new ReplaySubject instance with bounded buffer
     * @throws IllegalArgumentException if capacityHint is not positive
     */
    public static <T> ReplaySubject<T> createWithSize(int capacityHint) {
        if (capacityHint <= 0) {
            throw new IllegalArgumentException("capacityHint must be positive");
        }
        return new ReplaySubject<>(new SizeBoundReplayBuffer<>(capacityHint));
    }
    
    @Override
    protected void subscribeActual(Observer<? super T> observer) {
        ReplayDisposable<T> rd = new ReplayDisposable<>(observer, this);
        observer.onSubscribe(rd);
        
        if (add(rd)) {
            if (rd.isDisposed()) {
                remove(rd);
                return;
            }
        }
        
        buffer.replay(rd);
    }
    
    @Override
    public void onSubscribe(Disposable d) {
        if (done) {
            d.dispose();
        }
    }
    
    @Override
    public void onNext(T value) {
        if (value == null) {
            onError(new NullPointerException("onNext called with null"));
            return;
        }
        
        if (done) {
            return;
        }
        
        buffer.next(value);
        
        for (ReplayDisposable<T> rd : observers.get()) {
            buffer.replay(rd);
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
        
        done = true;
        buffer.error(e);
        
        ReplayDisposable<T>[] array = observers.getAndSet(TERMINATED);
        for (ReplayDisposable<T> rd : array) {
            buffer.replay(rd);
        }
    }
    
    @Override
    public void onComplete() {
        if (done) {
            return;
        }
        
        done = true;
        buffer.complete();
        
        ReplayDisposable<T>[] array = observers.getAndSet(TERMINATED);
        for (ReplayDisposable<T> rd : array) {
            buffer.replay(rd);
        }
    }
    
    @Override
    public boolean hasComplete() {
        return buffer.isDone() && buffer.getError() == null;
    }
    
    @Override
    public boolean hasThrowable() {
        return buffer.getError() != null;
    }
    
    @Override
    public Throwable getThrowable() {
        return buffer.getError();
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
     * Obtiene los valores actuales almacenados en el buffer.
     * 
     * @return Array con los valores actuales
     */
    @SuppressWarnings("unchecked")
    public T[] getValues(T[] array) {
        return buffer.getValues(array);
    }
    
    /**
     * Indica si el ReplaySubject tiene valores almacenados.
     * 
     * @return true si tiene al menos un valor
     */
    public boolean hasValue() {
        return buffer.size() > 0;
    }
    
    /**
     * Obtiene el número de valores almacenados.
     * 
     * @return Número de valores en el buffer
     */
    public int size() {
        return buffer.size();
    }
    
    @SuppressWarnings("unchecked")
    private boolean add(ReplayDisposable<T> rd) {
        for (;;) {
            ReplayDisposable<T>[] current = observers.get();
            if (current == TERMINATED) {
                return false;
            }
            
            int n = current.length;
            ReplayDisposable<T>[] next = new ReplayDisposable[n + 1];
            System.arraycopy(current, 0, next, 0, n);
            next[n] = rd;
            
            if (observers.compareAndSet(current, next)) {
                return true;
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    void remove(ReplayDisposable<T> rd) {
        for (;;) {
            ReplayDisposable<T>[] current = observers.get();
            if (current == TERMINATED || current == EMPTY) {
                return;
            }
            
            int n = current.length;
            int index = -1;
            
            for (int i = 0; i < n; i++) {
                if (current[i] == rd) {
                    index = i;
                    break;
                }
            }
            
            if (index < 0) {
                return;
            }
            
            ReplayDisposable<T>[] next;
            if (n == 1) {
                next = EMPTY;
            } else {
                next = new ReplayDisposable[n - 1];
                System.arraycopy(current, 0, next, 0, index);
                System.arraycopy(current, index + 1, next, index, n - index - 1);
            }
            
            if (observers.compareAndSet(current, next)) {
                return;
            }
        }
    }
    
    /**
     * Interface para diferentes estrategias de buffer.
     */
    interface ReplayBuffer<T> {
        void next(T value);
        void error(Throwable e);
        void complete();
        void replay(ReplayDisposable<T> rd);
        int size();
        T[] getValues(T[] array);
        boolean isDone();
        Throwable getError();
    }
    
    /**
     * Buffer sin límite que almacena todos los valores.
     */
    static final class UnboundedReplayBuffer<T> implements ReplayBuffer<T> {
        private final List<T> buffer;
        private volatile boolean done;
        private volatile Throwable error;
        
        UnboundedReplayBuffer() {
            this.buffer = new ArrayList<>();
        }
        
        @Override
        public void next(T value) {
            synchronized (buffer) {
                buffer.add(value);
            }
        }
        
        @Override
        public void error(Throwable e) {
            error = e;
            done = true;
        }
        
        @Override
        public void complete() {
            done = true;
        }
        
        @Override
        public void replay(ReplayDisposable<T> rd) {
            if (rd.isDisposed()) {
                return;
            }
            
            synchronized (buffer) {
                int index = rd.index;
                int size = buffer.size();
                
                while (index < size) {
                    if (rd.isDisposed()) {
                        return;
                    }
                    rd.downstream.onNext(buffer.get(index));
                    index++;
                }
                
                rd.index = index;
            }
            
            if (done) {
                if (rd.isDisposed()) {
                    return;
                }
                
                Throwable ex = error;
                if (ex != null) {
                    rd.downstream.onError(ex);
                } else {
                    rd.downstream.onComplete();
                }
            }
        }
        
        @Override
        public int size() {
            synchronized (buffer) {
                return buffer.size();
            }
        }
        
        @Override
        @SuppressWarnings("unchecked")
        public T[] getValues(T[] array) {
            synchronized (buffer) {
                int size = buffer.size();
                if (array.length < size) {
                    array = (T[]) java.lang.reflect.Array.newInstance(
                        array.getClass().getComponentType(), size);
                }
                return buffer.toArray(array);
            }
        }
        
        @Override
        public boolean isDone() {
            return done;
        }
        
        @Override
        public Throwable getError() {
            return error;
        }
    }
    
    /**
     * Buffer con tamaño limitado que solo mantiene los últimos N valores.
     */
    static final class SizeBoundReplayBuffer<T> implements ReplayBuffer<T> {
        private final int maxSize;
        private final List<T> buffer;
        private volatile boolean done;
        private volatile Throwable error;
        private int totalCount; // Total de elementos emitidos
        
        SizeBoundReplayBuffer(int maxSize) {
            this.maxSize = maxSize;
            this.buffer = new ArrayList<>(maxSize);
            this.totalCount = 0;
        }
        
        @Override
        public void next(T value) {
            synchronized (buffer) {
                if (buffer.size() >= maxSize) {
                    buffer.remove(0);
                }
                buffer.add(value);
                totalCount++;
            }
        }
        
        @Override
        public void error(Throwable e) {
            error = e;
            done = true;
        }
        
        @Override
        public void complete() {
            done = true;
        }
        
        @Override
        public void replay(ReplayDisposable<T> rd) {
            if (rd.isDisposed()) {
                return;
            }
            
            synchronized (buffer) {
                int size = buffer.size();
                int startIndex = totalCount - size; // Índice global del primer elemento en buffer
                
                // Calcular desde qué posición del buffer empezar
                int bufferIndex;
                if (rd.index < startIndex) {
                    // El observer está muy atrás, empezar desde el principio del buffer
                    bufferIndex = 0;
                    rd.index = startIndex;
                } else {
                    // Empezar desde donde el observer se quedó
                    bufferIndex = rd.index - startIndex;
                }
                
                // Enviar los elementos desde bufferIndex hasta el final
                while (bufferIndex < size) {
                    if (rd.isDisposed()) {
                        return;
                    }
                    rd.downstream.onNext(buffer.get(bufferIndex));
                    bufferIndex++;
                    rd.index++;
                }
            }
            
            if (done) {
                if (rd.isDisposed()) {
                    return;
                }
                
                Throwable ex = error;
                if (ex != null) {
                    rd.downstream.onError(ex);
                } else {
                    rd.downstream.onComplete();
                }
            }
        }
        
        @Override
        public int size() {
            synchronized (buffer) {
                return buffer.size();
            }
        }
        
        @Override
        @SuppressWarnings("unchecked")
        public T[] getValues(T[] array) {
            synchronized (buffer) {
                int size = buffer.size();
                if (array.length < size) {
                    array = (T[]) java.lang.reflect.Array.newInstance(
                        array.getClass().getComponentType(), size);
                }
                return buffer.toArray(array);
            }
        }
        
        @Override
        public boolean isDone() {
            return done;
        }
        
        @Override
        public Throwable getError() {
            return error;
        }
    }
    
    /**
     * Disposable para suscripciones a ReplaySubject.
     */
    static final class ReplayDisposable<T> implements Disposable {
        final Observer<? super T> downstream;
        final ReplaySubject<T> parent;
        
        volatile boolean disposed;
        int index;
        
        ReplayDisposable(Observer<? super T> downstream, ReplaySubject<T> parent) {
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
    }
}
