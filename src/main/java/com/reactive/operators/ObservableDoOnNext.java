package com.reactive.operators;
import com.reactive.core.*;
import java.util.function.Consumer;

public class ObservableDoOnNext<T> extends Observable<T> {
    private final ObservableSource<T> source;
    private final Consumer<? super T> onNext;
    
    public ObservableDoOnNext(ObservableSource<T> source, Consumer<? super T> onNext) {
        this.source = source;
        this.onNext = onNext;
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
                try {
                    onNext.accept(item);
                } catch (Exception e) {
                    onError(e);
                    return;
                }
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
