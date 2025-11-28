package com.reactive.operators;
import com.reactive.core.*;
import java.util.function.Consumer;

public class ObservableDoOnSubscribe<T> extends Observable<T> {
    private final ObservableSource<T> source;
    private final Consumer<? super Disposable> onSubscribe;
    
    public ObservableDoOnSubscribe(ObservableSource<T> source, Consumer<? super Disposable> onSubscribe) {
        this.source = source;
        this.onSubscribe = onSubscribe;
    }
    
    @Override
    public void subscribe(Observer<? super T> observer) {
        source.subscribe(new Observer<T>() {
            @Override
            public void onSubscribe(Disposable disposable) {
                try {
                    onSubscribe.accept(disposable);
                } catch (Exception e) {
                    // Ignore
                }
                observer.onSubscribe(disposable);
            }
            
            @Override
            public void onNext(T item) {
                observer.onNext(item);
            }
            
            @Override
            public void onError(Throwable error) {
                observer.onError(error);
            }
            
            @Override
            public void onComplete() {
                observer.onComplete();
            }
        });
    }
}
