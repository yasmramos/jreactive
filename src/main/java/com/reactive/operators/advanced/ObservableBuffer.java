package com.reactive.operators.advanced;

import com.reactive.core.Observable;
import com.reactive.core.Observer;
import java.util.ArrayList;
import java.util.List;

/**
 * Operador buffer: agrupa elementos en listas de tamaño N.
 * 
 * Útil para:
 * - Procesamiento por lotes (batch processing)
 * - Agrupar elementos para envío
 * - Reducir número de operaciones
 * 
 * Ejemplo:
 * [1, 2, 3, 4, 5, 6, 7] con buffer(3)
 * Emite: [1,2,3], [4,5,6], [7]
 * 
 * @param <T> Tipo de elementos
 */
public class ObservableBuffer<T> extends Observable<List<T>> {
    
    private final Observable<T> source;
    private final int count;
    private final int skip;
    
    /**
     * Crea un buffer con tamaño count y skip = count (no overlapping).
     */
    public ObservableBuffer(Observable<T> source, int count) {
        this(source, count, count);
    }
    
    /**
     * Crea un buffer con tamaño count y skip personalizado.
     * 
     * @param count Tamaño del buffer
     * @param skip Número de elementos a saltar entre buffers
     */
    public ObservableBuffer(Observable<T> source, int count, int skip) {
        this.source = source;
        this.count = count;
        this.skip = skip;
    }
    
    @Override
    public void subscribe(Observer<? super List<T>> observer) {
        source.subscribe(new Observer<T>() {
            private List<T> buffer = new ArrayList<>();
            private int index = 0;
            
            @Override
            public void onNext(T value) {
                buffer.add(value);
                index++;
                
                // Si el buffer alcanzó el tamaño, emitirlo
                if (buffer.size() == count) {
                    observer.onNext(new ArrayList<>(buffer));
                    
                    // Limpiar buffer según skip
                    if (skip >= count) {
                        buffer.clear();
                    } else {
                        // Overlapping: mantener algunos elementos
                        buffer = new ArrayList<>(buffer.subList(skip, buffer.size()));
                    }
                }
            }
            
            @Override
            public void onError(Throwable error) {
                observer.onError(error);
            }
            
            @Override
            public void onComplete() {
                // Emitir buffer restante si hay elementos
                if (!buffer.isEmpty()) {
                    observer.onNext(buffer);
                }
                observer.onComplete();
            }
        });
    }
}
