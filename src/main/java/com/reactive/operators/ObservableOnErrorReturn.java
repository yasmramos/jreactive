package com.reactive.operators;
import com.reactive.core.*;
import java.util.function.Function;

public class ObservableOnErrorReturn<T> extends Observable<T> {
    private final ObservableSource<T> source;
    private final Function<? super Throwable, ? extends T> valueSupplier;
    
    public ObservableOnErrorReturn(ObservableSource<T> source, 
                                   Function<? super Throwable, ? extends T> valueSupplier) {
        this.source = source;
        this.valueSupplier = valueSupplier;
    }
    
    @Override
    public void subscribe(Observer<? super T> observer) {
        source.subscribe(new Observer<T>() {
            @Override
            public void onSubscribe(Disposable disposable) {
                observer.onSubscribe(disposable);
            }
            
            @Override
            public void onNext(T item) {
                observer.onNext(item);
            }
            
            @Override
            public void onError(Throwable error) {
                try {
                    T value = valueSupplier.apply(error);
                    observer.onNext(value);
                    observer.onComplete();
                } catch (Exception e) {
                    observer.onError(e);
                }
            }
            
            @Override
            public void onComplete() {
                observer.onComplete();
            }
        });
    }
}
