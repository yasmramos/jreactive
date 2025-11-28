package com.reactive.operators;
import com.reactive.core.*;
import com.reactive.schedulers.Scheduler;

public class ObservableSubscribeOn<T> extends Observable<T> {
    private final ObservableSource<T> source;
    private final Scheduler scheduler;
    
    public ObservableSubscribeOn(ObservableSource<T> source, Scheduler scheduler) {
        this.source = source;
        this.scheduler = scheduler;
    }
    
    @Override
    public void subscribe(Observer<? super T> observer) {
        scheduler.execute(() -> source.subscribe(observer));
    }
}
