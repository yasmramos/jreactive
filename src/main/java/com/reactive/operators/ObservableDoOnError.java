package com.reactive.operators;
import com.reactive.core.*;
import java.util.function.Consumer;

public class ObservableDoOnError<T> extends Observable<T> {
    private final ObservableSource<T> source;
    private final Consumer<? super Throwable> onError;
    
    public ObservableDoOnError(ObservableSource<T> source, Consumer<? super Throwable> onError) {
        this.source = source;
        this.onError = onError;
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
                    onError.accept(error);
                } catch (Exception e) {
                    // Ignore
                }
                observer.onError(error);
            }
            
            @Override
            public void onComplete() {
                observer.onComplete();
            }
        });
    }
}
