package com.reactive.core;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Observer que permite suscribirse usando lambdas simples
 * Implementa Disposable para facilitar la cancelación
 */
public class LambdaObserver<T> implements Observer<T>, Disposable {
    
    private final Consumer<? super T> onNext;
    private final Consumer<? super Throwable> onError;
    private final Runnable onComplete;
    private final AtomicBoolean disposed = new AtomicBoolean(false);
    private Disposable upstream;
    
    public LambdaObserver(Consumer<? super T> onNext,
                         Consumer<? super Throwable> onError,
                         Runnable onComplete) {
        this.onNext = onNext;
        this.onError = onError;
        this.onComplete = onComplete;
    }
    
    @Override
    public void onSubscribe(Disposable disposable) {
        this.upstream = disposable;
    }
    
    @Override
    public void onNext(T item) {
        if (!disposed.get()) {
            try {
                onNext.accept(item);
            } catch (Throwable e) {
                onError(e);
            }
        }
    }
    
    @Override
    public void onError(Throwable error) {
        if (disposed.compareAndSet(false, true)) {
            try {
                onError.accept(error);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }
    
    @Override
    public void onComplete() {
        if (disposed.compareAndSet(false, true)) {
            try {
                onComplete.run();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }
    
    @Override
    public void dispose() {
        if (disposed.compareAndSet(false, true)) {
            if (upstream != null) {
                upstream.dispose();
            }
        }
    }
    
    @Override
    public boolean isDisposed() {
        return disposed.get();
    }
}
