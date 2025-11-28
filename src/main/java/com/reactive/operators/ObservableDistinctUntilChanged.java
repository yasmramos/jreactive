package com.reactive.operators;
import com.reactive.core.*;

public class ObservableDistinctUntilChanged<T> extends Observable<T> {
    private final ObservableSource<T> source;
    
    public ObservableDistinctUntilChanged(ObservableSource<T> source) {
        this.source = source;
    }
    
    @Override
    public void subscribe(Observer<? super T> observer) {
        source.subscribe(new DistinctObserver<>(observer));
    }
    
    static class DistinctObserver<T> implements Observer<T> {
        private final Observer<? super T> downstream;
        private T last;
        private boolean hasValue;
        
        DistinctObserver(Observer<? super T> downstream) {
            this.downstream = downstream;
        }
        
        @Override
        public void onSubscribe(Disposable disposable) {
            downstream.onSubscribe(disposable);
        }
        
        @Override
        public void onNext(T item) {
            if (!hasValue) {
                hasValue = true;
                last = item;
                downstream.onNext(item);
            } else {
                boolean equals = (item == null && last == null) || (item != null && item.equals(last));
                if (!equals) {
                    last = item;
                    downstream.onNext(item);
                }
            }
        }
        
        @Override
        public void onError(Throwable error) { downstream.onError(error); }
        
        @Override
        public void onComplete() { downstream.onComplete(); }
    }
}
