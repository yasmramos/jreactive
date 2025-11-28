package com.reactive.operators;

import com.reactive.core.*;

/**
 * Observable que permite crear streams personalizados usando un callback
 */
public class ObservableCreate<T> extends Observable<T> {
    
    private final OnSubscribe<T> onSubscribe;
    
    public ObservableCreate(OnSubscribe<T> onSubscribe) {
        this.onSubscribe = onSubscribe;
    }
    
    @Override
    public void subscribe(Observer<? super T> observer) {
        BasicEmitter<T> emitter = new BasicEmitter<>(observer);
        observer.onSubscribe(emitter);
        
        try {
            onSubscribe.subscribe(emitter);
        } catch (Exception e) {
            emitter.onError(e);
        }
    }
}
