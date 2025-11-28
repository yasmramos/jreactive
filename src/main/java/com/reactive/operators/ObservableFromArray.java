package com.reactive.operators;

import com.reactive.core.*;

/**
 * Observable que emite elementos de un array
 */
public class ObservableFromArray<T> extends Observable<T> {
    
    private final T[] items;
    
    public ObservableFromArray(T[] items) {
        this.items = items;
    }
    
    @Override
    public void subscribe(Observer<? super T> observer) {
        observer.onSubscribe(Disposable.EMPTY);
        
        try {
            for (T item : items) {
                observer.onNext(item);
            }
            observer.onComplete();
        } catch (Exception e) {
            observer.onError(e);
        }
    }
}
