package com.reactive.operators;

import com.reactive.core.*;

/**
 * Observable que emite elementos de un Iterable
 */
public class ObservableFromIterable<T> extends Observable<T> {
    
    private final Iterable<T> iterable;
    
    public ObservableFromIterable(Iterable<T> iterable) {
        this.iterable = iterable;
    }
    
    @Override
    public void subscribe(Observer<? super T> observer) {
        observer.onSubscribe(Disposable.EMPTY);
        
        try {
            for (T item : iterable) {
                observer.onNext(item);
            }
            observer.onComplete();
        } catch (Exception e) {
            observer.onError(e);
        }
    }
}
