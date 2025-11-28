package com.reactive.operators;
import com.reactive.core.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;

public class ObservableMerge<T> extends Observable<T> {
    private final ObservableSource<? extends T>[] sources;
    
    @SafeVarargs
    public ObservableMerge(ObservableSource<? extends T>... sources) {
        this.sources = sources;
    }
    
    @Override
    public void subscribe(Observer<? super T> observer) {
        observer.onSubscribe(Disposable.EMPTY);
        
        AtomicInteger activeCount = new AtomicInteger(sources.length);
        AtomicBoolean terminated = new AtomicBoolean(false);
        
        for (ObservableSource<? extends T> source : sources) {
            source.subscribe(new Observer<T>() {
                @Override
                public void onNext(T item) {
                    if (!terminated.get()) {
                        observer.onNext(item);
                    }
                }
                
                @Override
                public void onError(Throwable error) {
                    if (terminated.compareAndSet(false, true)) {
                        observer.onError(error);
                    }
                }
                
                @Override
                public void onComplete() {
                    if (activeCount.decrementAndGet() == 0 && !terminated.get()) {
                        observer.onComplete();
                    }
                }
            });
        }
    }
}
