package com.reactive.operators;

import com.reactive.core.*;

/**
 * Observable que completa inmediatamente sin emitir elementos
 */
public class ObservableEmpty<T> extends Observable<T> {
    
    @Override
    public void subscribe(Observer<? super T> observer) {
        observer.onSubscribe(Disposable.EMPTY);
        observer.onComplete();
    }
}
