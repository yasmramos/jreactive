package com.reactive.operators;

import com.reactive.core.*;

/**
 * Observable que nunca emite nada ni completa
 */
public class ObservableNever<T> extends Observable<T> {
    
    @Override
    public void subscribe(Observer<? super T> observer) {
        observer.onSubscribe(Disposable.EMPTY);
        // Nunca emite nada
    }
}
