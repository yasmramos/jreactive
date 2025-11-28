package com.reactive.core;

/**
 * Interfaz base para fuentes observables
 * @param <T> Tipo de datos que emite
 */
@FunctionalInterface
public interface ObservableSource<T> {
    
    /**
     * Suscribe un Observer a esta fuente
     */
    void subscribe(Observer<? super T> observer);
}
