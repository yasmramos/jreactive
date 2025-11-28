package com.reactive.operators;
import com.reactive.core.*;
import java.util.concurrent.atomic.AtomicLong;

public class ObservableRetry<T> extends Observable<T> {
    private final ObservableSource<T> source;
    private final long times;
    
    public ObservableRetry(ObservableSource<T> source, long times) {
        this.source = source;
        this.times = times;
    }
    
    @Override
    public void subscribe(Observer<? super T> observer) {
        observer.onSubscribe(Disposable.EMPTY);
        subscribeActual(observer, new AtomicLong(times));
    }
    
    private void subscribeActual(Observer<? super T> observer, AtomicLong remaining) {
        source.subscribe(new Observer<T>() {
            @Override
            public void onNext(T item) {
                observer.onNext(item);
            }
            
            @Override
            public void onError(Throwable error) {
                long r = remaining.decrementAndGet();
                if (r >= 0) {
                    subscribeActual(observer, remaining);
                } else {
                    observer.onError(error);
                }
            }
            
            @Override
            public void onComplete() {
                observer.onComplete();
            }
        });
    }
}
