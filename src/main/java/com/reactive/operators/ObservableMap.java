package com.reactive.operators;

import com.reactive.core.*;

import java.util.function.Function;

/**
 * Transforma cada elemento emitido aplicando una función
 */
public class ObservableMap<T, R> extends Observable<R> {
    
    private final ObservableSource<T> source;
    private final Function<? super T, ? extends R> mapper;
    
    public ObservableMap(ObservableSource<T> source, Function<? super T, ? extends R> mapper) {
        this.source = source;
        this.mapper = mapper;
    }
    
    @Override
    public void subscribe(Observer<? super R> observer) {
        source.subscribe(new MapObserver<>(observer, mapper));
    }
    
    static class MapObserver<T, R> implements Observer<T> {
        private final Observer<? super R> downstream;
        private final Function<? super T, ? extends R> mapper;
        
        MapObserver(Observer<? super R> downstream, Function<? super T, ? extends R> mapper) {
            this.downstream = downstream;
            this.mapper = mapper;
        }
        
        @Override
        public void onSubscribe(Disposable disposable) {
            downstream.onSubscribe(disposable);
        }
        
        @Override
        public void onNext(T item) {
            try {
                R result = mapper.apply(item);
                downstream.onNext(result);
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
