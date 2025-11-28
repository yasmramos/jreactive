package com.reactive.operators;

import com.reactive.core.*;

import java.util.function.Predicate;

/**
 * Filtra elementos basándose en un predicado
 */
public class ObservableFilter<T> extends Observable<T> {
    
    private final ObservableSource<T> source;
    private final Predicate<? super T> predicate;
    
    public ObservableFilter(ObservableSource<T> source, Predicate<? super T> predicate) {
        this.source = source;
        this.predicate = predicate;
    }
    
    @Override
    public void subscribe(Observer<? super T> observer) {
        source.subscribe(new FilterObserver<>(observer, predicate));
    }
    
    static class FilterObserver<T> implements Observer<T> {
        private final Observer<? super T> downstream;
        private final Predicate<? super T> predicate;
        
        FilterObserver(Observer<? super T> downstream, Predicate<? super T> predicate) {
            this.downstream = downstream;
            this.predicate = predicate;
        }
        
        @Override
        public void onSubscribe(Disposable disposable) {
            downstream.onSubscribe(disposable);
        }
        
        @Override
        public void onNext(T item) {
            try {
                if (predicate.test(item)) {
                    downstream.onNext(item);
                }
            } catch (Exception e) {
                onError(e);
            }
        }
        
        @Override
        public void onError(Throwable error) {
            downstream.onError(error);
        }
        
        @Override
        public void onComplete() {
            downstream.onComplete();
        }
    }
}
