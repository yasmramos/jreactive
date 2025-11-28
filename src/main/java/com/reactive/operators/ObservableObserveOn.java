package com.reactive.operators;
import com.reactive.core.*;
import com.reactive.schedulers.Scheduler;

public class ObservableObserveOn<T> extends Observable<T> {
    private final ObservableSource<T> source;
    private final Scheduler scheduler;
    
    public ObservableObserveOn(ObservableSource<T> source, Scheduler scheduler) {
        this.source = source;
        this.scheduler = scheduler;
    }
    
    @Override
    public void subscribe(Observer<? super T> observer) {
        source.subscribe(new Observer<T>() {
            @Override
            public void onSubscribe(Disposable disposable) {
                scheduler.execute(() -> observer.onSubscribe(disposable));
            }
            
            @Override
            public void onNext(T item) {
                scheduler.execute(() -> observer.onNext(item));
            }
            
            @Override
            public void onError(Throwable error) {
                scheduler.execute(() -> observer.onError(error));
            }
            
            @Override
            public void onComplete() {
                scheduler.execute(() -> observer.onComplete());
            }
        });
    }
}
