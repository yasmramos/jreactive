package com.reactive.operators;
import com.reactive.core.*;
import java.util.function.Function;

public class ObservableOnErrorResumeNext<T> extends Observable<T> {
    private final ObservableSource<T> source;
    private final Function<? super Throwable, ? extends ObservableSource<? extends T>> fallback;
    
    public ObservableOnErrorResumeNext(ObservableSource<T> source, 
                                       Function<? super Throwable, ? extends ObservableSource<? extends T>> fallback) {
        this.source = source;
        this.fallback = fallback;
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
                try {
                    ObservableSource<? extends T> fallbackSource = fallback.apply(error);
                    fallbackSource.subscribe(new Observer<T>() {
                        @Override
                        public void onNext(T item) {
                            observer.onNext(item);
                        }
                        
                        @Override
                        public void onError(Throwable fallbackError) {
                            observer.onError(fallbackError);
                        }
                        
                        @Override
                        public void onComplete() {
                            observer.onComplete();
                        }
                    });
                } catch (Exception e) {
                    observer.onError(e);
                }
            }
            
            @Override
            public void onComplete() {
                observer.onComplete();
            }
        });
    }
}
