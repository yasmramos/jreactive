package com.reactive.operators;
import com.reactive.core.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiFunction;

public class ObservableZip<T, U, R> extends Observable<R> {
    private final ObservableSource<? extends T> source1;
    private final ObservableSource<? extends U> source2;
    private final BiFunction<? super T, ? super U, ? extends R> zipper;
    
    public ObservableZip(ObservableSource<? extends T> source1,
                        ObservableSource<? extends U> source2,
                        BiFunction<? super T, ? super U, ? extends R> zipper) {
        this.source1 = source1;
        this.source2 = source2;
        this.zipper = zipper;
    }
    
    @Override
    public void subscribe(Observer<? super R> observer) {
        observer.onSubscribe(Disposable.EMPTY);
        
        ConcurrentLinkedQueue<T> queue1 = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<U> queue2 = new ConcurrentLinkedQueue<>();
        boolean[] done = new boolean[2];
        
        source1.subscribe(new Observer<T>() {
            @Override
            public void onNext(T item) {
                queue1.offer(item);
                drain();
            }
            
            @Override
            public void onError(Throwable error) {
                observer.onError(error);
            }
            
            @Override
            public void onComplete() {
                done[0] = true;
                drain();
            }
            
            private void drain() {
                while (!queue1.isEmpty() && !queue2.isEmpty()) {
                    T t = queue1.poll();
                    U u = queue2.poll();
                    try {
                        R result = zipper.apply(t, u);
                        observer.onNext(result);
                    } catch (Exception e) {
                        observer.onError(e);
                        return;
                    }
                }
                if (done[0] && done[1]) {
                    observer.onComplete();
                }
            }
        });
        
        source2.subscribe(new Observer<U>() {
            @Override
            public void onNext(U item) {
                queue2.offer(item);
                drain();
            }
            
            @Override
            public void onError(Throwable error) {
                observer.onError(error);
            }
            
            @Override
            public void onComplete() {
                done[1] = true;
                drain();
            }
            
            private void drain() {
                while (!queue1.isEmpty() && !queue2.isEmpty()) {
                    T t = queue1.poll();
                    U u = queue2.poll();
                    try {
                        R result = zipper.apply(t, u);
                        observer.onNext(result);
                    } catch (Exception e) {
                        observer.onError(e);
                        return;
                    }
                }
                if (done[0] && done[1]) {
                    observer.onComplete();
                }
            }
        });
    }
}
