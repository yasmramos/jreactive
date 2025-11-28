package com.reactive.operators;

import com.reactive.core.*;

/**
 * Observable que emite un error inmediatamente
 */
public class ObservableError<T> extends Observable<T> {
    
    private final Throwable error;
    
    public ObservableError(Throwable error) {
        this.error = error;
    }
    
    @Override
    public void subscribe(Observer<? super T> observer) {
        observer.onSubscribe(Disposable.EMPTY);
        observer.onError(error);
    }
}
