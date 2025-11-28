package com.reactive.operators;
import com.reactive.core.*;

public class ObservableDoOnComplete<T> extends Observable<T> {
    private final ObservableSource<T> source;
    private final Runnable onComplete;
    
    public ObservableDoOnComplete(ObservableSource<T> source, Runnable onComplete) {
        this.source = source;
        this.onComplete = onComplete;
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
                observer.onError(error);
            }
            
            @Override
            public void onComplete() {
                try {
                    onComplete.run();
                } catch (Exception e) {
                    // Ignore
                }
                observer.onComplete();
            }
        });
    }
}
