package com.reactive.core;

public interface Emitter<T> {
    void onNext(T value);
    void onError(Throwable error);
    void onComplete();
    boolean isDisposed();
    default void setDisposable(Disposable disposable) {
        // Default implementation does nothing
    }
}
