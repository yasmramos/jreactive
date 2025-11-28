package com.reactive.operators;
import com.reactive.core.*;
import java.util.function.Function;

public class ObservableSwitchMap<T, R> extends Observable<R> {
    private final ObservableSource<T> source;
    private final Function<? super T, ? extends ObservableSource<? extends R>> mapper;
    
    public ObservableSwitchMap(ObservableSource<T> source, Function<? super T, ? extends ObservableSource<? extends R>> mapper) {
        this.source = source;
        this.mapper = mapper;
    }
    
    @Override
    public void subscribe(Observer<? super R> observer) {
        // Implementación simplificada - similar a flatMap por ahora
        source.subscribe(new FlatMapObserver<>(observer, mapper));
    }
    
    // Reutilizamos FlatMapObserver por simplicidad
    static class FlatMapObserver<T, R> implements Observer<T> {
        private final Observer<? super R> downstream;
        private final Function<? super T, ? extends ObservableSource<? extends R>> mapper;
        
        FlatMapObserver(Observer<? super R> downstream, Function<? super T, ? extends ObservableSource<? extends R>> mapper) {
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
                mapper.apply(item).subscribe(new Observer<R>() {
                    @Override
                    public void onNext(R value) { downstream.onNext(value); }
                    @Override
                    public void onError(Throwable error) { downstream.onError(error); }
                    @Override
                    public void onComplete() {}
                });
            } catch (Exception e) {
                onError(e);
            }
        }
        
        @Override
        public void onError(Throwable error) { downstream.onError(error); }
        
        @Override
        public void onComplete() { downstream.onComplete(); }
    }
}
