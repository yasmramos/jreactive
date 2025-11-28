package com.reactive.core;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Implementación básica del Emitter que conecta el Observable con el Observer
 */
public class BasicEmitter<T> implements Emitter<T>, Disposable {
    
    private final Observer<? super T> observer;
    private final AtomicBoolean disposed = new AtomicBoolean(false);
    private Disposable resource;
    
    public BasicEmitter(Observer<? super T> observer) {
        this.observer = observer;
    }
    
    @Override
    public void onNext(T item) {
        if (!isDisposed() && item != null) {
            observer.onNext(item);
        }
    }
    
    @Override
    public void onError(Throwable error) {
        if (disposed.compareAndSet(false, true)) {
            try {
                observer.onError(error);
            } finally {
                disposeResource();
            }
        }
    }
    
    @Override
    public void onComplete() {
        if (disposed.compareAndSet(false, true)) {
            try {
                observer.onComplete();
            } finally {
                disposeResource();
            }
        }
    }
    
    @Override
    public boolean isDisposed() {
        return disposed.get();
    }
    
    @Override
    public void setDisposable(Disposable disposable) {
        this.resource = disposable;
    }
    
    @Override
    public void dispose() {
        if (disposed.compareAndSet(false, true)) {
            disposeResource();
        }
    }
    
    private void disposeResource() {
        if (resource != null) {
            resource.dispose();
        }
    }
}
