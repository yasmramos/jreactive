package com.reactive.operators;
import com.reactive.core.*;

public class ObservableDefaultIfEmpty<T> extends Observable<T> {
    private final ObservableSource<T> source;
    private final T defaultValue;
    
    public ObservableDefaultIfEmpty(ObservableSource<T> source, T defaultValue) {
        this.source = source;
        this.defaultValue = defaultValue;
    }
    
    @Override
    public void subscribe(Observer<? super T> observer) {
        source.subscribe(new DefaultIfEmptyObserver<>(observer, defaultValue));
    }
    
    static class DefaultIfEmptyObserver<T> implements Observer<T> {
        private final Observer<? super T> downstream;
        private final T defaultValue;
        private boolean isEmpty = true;
        
        DefaultIfEmptyObserver(Observer<? super T> downstream, T defaultValue) {
            this.downstream = downstream;
            this.defaultValue = defaultValue;
        }
        
        @Override
        public void onSubscribe(Disposable disposable) {
            downstream.onSubscribe(disposable);
        }
        
        @Override
        public void onNext(T item) {
            isEmpty = false;
            downstream.onNext(item);
        }
        
        @Override
        public void onError(Throwable error) {
            downstream.onError(error);
        }
        
        @Override
        public void onComplete() {
            if (isEmpty) {
                downstream.onNext(defaultValue);
            }
            downstream.onComplete();
        }
    }
}
