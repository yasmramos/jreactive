package com.reactive.operators;
import com.reactive.core.*;

public class ObservableConcat<T> extends Observable<T> {
    private final ObservableSource<? extends T>[] sources;
    private int index = 0;
    
    @SafeVarargs
    public ObservableConcat(ObservableSource<? extends T>... sources) {
        this.sources = sources;
    }
    
    @Override
    public void subscribe(Observer<? super T> observer) {
        observer.onSubscribe(Disposable.EMPTY);
        subscribeNext(observer, 0);
    }
    
    private void subscribeNext(Observer<? super T> observer, int idx) {
        if (idx >= sources.length) {
            observer.onComplete();
            return;
        }
        
        sources[idx].subscribe(new Observer<T>() {
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
                subscribeNext(observer, idx + 1);
            }
        });
    }
}
