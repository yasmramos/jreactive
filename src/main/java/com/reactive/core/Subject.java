package com.reactive.core;

/**
 * A Subject is both an {@link Observable} and an {@link Observer}.
 * 
 * <p>A Subject can subscribe to an Observable (acts as an Observer) and simultaneously
 * emit events to multiple subscribers (acts as an Observable). This makes Subjects
 * useful for multicasting - distributing a single source to multiple observers.
 * 
 * <p><strong>Key characteristics:</strong>
 * <ul>
 *   <li>Subjects are "hot" - they emit events immediately, regardless of subscriptions</li>
 *   <li>They can multicast - one source to multiple observers</li>
 *   <li>They bridge imperative and reactive code (can manually call onNext/onError/onComplete)</li>
 *   <li>Thread-safety varies by implementation</li>
 * </ul>
 * 
 * <p><strong>Subject types:</strong>
 * <ul>
 *   <li>{@link PublishSubject} - Emits only events that occur after subscription
 *       <br>Use when: You only care about future events (like event buses)</li>
 *   
 *   <li>{@link BehaviorSubject} - Emits the most recent value + all subsequent events
 *       <br>Use when: Subscribers need the current state (like current user, configuration)</li>
 *   
 *   <li>{@link ReplaySubject} - Replays all (or N most recent) events to new subscribers
 *       <br>Use when: Late subscribers need full event history (like message logs)</li>
 *   
 *   <li>{@link AsyncSubject} - Only emits the last value when the source completes
 *       <br>Use when: You only need the final result (like last calculation)</li>
 * </ul>
 * 
 * <p>Example - Basic multicasting:
 * <pre>{@code
 * PublishSubject<String> subject = PublishSubject.create();
 * 
 * // Multiple subscribers
 * subject.subscribe(value -> System.out.println("Observer 1: " + value));
 * subject.subscribe(value -> System.out.println("Observer 2: " + value));
 * 
 * // Manually emit events
 * subject.onNext("Hello");  // Both observers receive "Hello"
 * subject.onNext("World");  // Both observers receive "World"
 * subject.onComplete();
 * }</pre>
 * 
 * <p>Example - Bridging callback APIs:
 * <pre>{@code
 * PublishSubject<String> subject = PublishSubject.create();
 * 
 * // Bridge imperative callback to reactive stream
 * button.setOnClickListener(event -> subject.onNext("clicked"));
 * 
 * // Reactive consumption
 * subject
 *     .debounce(300, TimeUnit.MILLISECONDS)
 *     .subscribe(event -> handleClick());
 * }</pre>
 * 
 * <p><strong>Warning:</strong> Subjects can break referential transparency and should
 * be used sparingly. Prefer operators like {@code publish()}, {@code share()}, or
 * {@code replay()} when possible.
 * 
 * @param <T> the type of items emitted and observed
 * @see PublishSubject
 * @see BehaviorSubject
 * @see ReplaySubject
 * @see AsyncSubject
 * @see Observable
 * @see Observer
 */
public abstract class Subject<T> extends Observable<T> implements Observer<T> {
    
    @Override
    public void subscribe(Observer<? super T> observer) {
        subscribeActual(observer);
    }
    
    /**
     * Internal subscription method implemented by subclasses.
     * 
     * <p>This method handles the actual subscription logic specific to each
     * Subject type (e.g., emitting cached values, registering observers).
     * 
     * @param observer the Observer to notify of events
     */
    protected abstract void subscribeActual(Observer<? super T> observer);
    
    /**
     * Indicates whether the Subject has completed or terminated with an error.
     * 
     * @return true if the Subject has terminated (completed or errored)
     */
    public abstract boolean hasComplete();
    
    /**
     * Indicates whether the Subject has terminated with an error.
     * 
     * @return true if the Subject terminated with an error
     */
    public abstract boolean hasThrowable();
    
    /**
     * Returns the error if the Subject terminated with an error.
     * 
     * @return the Throwable error, or null if no error occurred
     */
    public abstract Throwable getThrowable();
    
    /**
     * Indicates whether the Subject has any subscribed observers.
     * 
     * @return true if at least one observer is subscribed
     */
    public abstract boolean hasObservers();
    
    /**
     * Returns the number of currently subscribed observers.
     * 
     * @return the count of subscribed observers
     */
    public abstract int observerCount();
}
