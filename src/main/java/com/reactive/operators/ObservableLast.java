package com.reactive.operators;
import com.reactive.core.*;

public class ObservableLast<T> extends Observable<T> {
    private final ObservableSource<T> source;
    private final T defaultValue;
    
    public ObservableLast(ObservableSource<T> source, T defaultValue) {
        this.source = source;
        this.defaultValue = defaultValue;
    }
    
    @Override
    public void subscribe(Observer<? super T> observer) {
        source.subscribe(new LastObserver<>(observer, defaultValue));
    }
    
    static class LastObserver<T> implements Observer<T> {
        private final Observer<? super T> downstream;
        private final T defaultValue;
        private T last;
        private boolean hasValue;
        
        LastObserver(Observer<? super T> downstream, T defaultValue) {
            this.downstream = downstream;
            this.defaultValue = defaultValue;
        }
        
        @Override
        public void onSubscribe(Disposable disposable) {
            downstream.onSubscribe(disposable);
        }
        
        @Override
        public void onNext(T item) {
            last = item;
            hasValue = true;
        }
        
        @Override
        public void onError(Throwable error) {
            downstream.onError(error);
        }
        
        @Override
        public void onComplete() {
            if (hasValue) {
                downstream.onNext(last);
            } else if (defaultValue != null) {
                downstream.onNext(defaultValue);
            }
            downstream.onComplete();
        }
    }
}
