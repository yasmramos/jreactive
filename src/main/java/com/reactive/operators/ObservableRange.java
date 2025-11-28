package com.reactive.operators;

import com.reactive.core.*;

/**
 * Observable que emite un rango de números enteros
 */
public class ObservableRange extends Observable<Integer> {
    
    private final int start;
    private final int count;
    
    public ObservableRange(int start, int count) {
        this.start = start;
        this.count = count;
    }
    
    @Override
    public void subscribe(Observer<? super Integer> observer) {
        observer.onSubscribe(Disposable.EMPTY);
        
        try {
            for (int i = 0; i < count; i++) {
                observer.onNext(start + i);
            }
            observer.onComplete();
        } catch (Exception e) {
            observer.onError(e);
        }
    }
}
