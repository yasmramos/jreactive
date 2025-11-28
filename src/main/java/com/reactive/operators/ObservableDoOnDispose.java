package com.reactive.operators;
import com.reactive.core.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class ObservableDoOnDispose<T> extends Observable<T> {
    private final ObservableSource<T> source;
    private final Runnable onDispose;
    
    public ObservableDoOnDispose(ObservableSource<T> source, Runnable onDispose) {
        this.source = source;
        this.onDispose = onDispose;
    }
    
    @Override
    public void subscribe(Observer<? super T> observer) {
        AtomicBoolean disposed = new AtomicBoolean(false);
        
        source.subscribe(new Observer<T>() {
            @Override
            public void onSubscribe(Disposable disposable) {
                observer.onSubscribe(new Disposable() {
                    @Override
                    public void dispose() {
                        if (disposed.compareAndSet(false, true)) {
                            try {
                                onDispose.run();
                            } catch (Exception e) {
                                // Ignore
                            }
                            disposable.dispose();
                        }
                    }
                    
                    @Override
                    public boolean isDisposed() {
                        return disposed.get();
                    }
                });
            }
            
            @Override
            public void onNext(T item) {
                observer.onNext(item);
            }
            
            @Override
            public void onError(Throwable error) {
                observer.onError(error);
            }
            
            @Override
            public void onComplete() {
                observer.onComplete();
            }
        });
    }
}
