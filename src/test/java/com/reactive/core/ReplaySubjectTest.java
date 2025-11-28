package com.reactive.core;

import com.reactive.testing.TestObserver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests para ReplaySubject.
 */
public class ReplaySubjectTest {
    
    @Test
    public void testBasicBehavior() {
        ReplaySubject<Integer> subject = ReplaySubject.create();
        TestObserver<Integer> observer = new TestObserver<>();
        
        subject.subscribe(observer);
        
        subject.onNext(1);
        subject.onNext(2);
        subject.onNext(3);
        subject.onComplete();
        
        observer.assertValues(1, 2, 3);
        observer.assertComplete();
    }
    
    @Test
    public void testReplayToLateSubscriber() {
        ReplaySubject<Integer> subject = ReplaySubject.create();
        TestObserver<Integer> observer1 = new TestObserver<>();
        
        subject.subscribe(observer1);
        
        subject.onNext(1);
        subject.onNext(2);
        
        TestObserver<Integer> observer2 = new TestObserver<>();
        subject.subscribe(observer2);
        
        subject.onNext(3);
        subject.onComplete();
        
        // Observer 1 recibe todos los valores en tiempo real
        observer1.assertValues(1, 2, 3);
        observer1.assertComplete();
        
        // Observer 2 recibe el replay de 1, 2 y luego 3
        observer2.assertValues(1, 2, 3);
        observer2.assertComplete();
    }
    
    @Test
    public void testReplayAfterComplete() {
        ReplaySubject<String> subject = ReplaySubject.create();
        
        subject.onNext("A");
        subject.onNext("B");
        subject.onNext("C");
        subject.onComplete();
        
        // Subscriptor después de complete recibe todo el replay
        TestObserver<String> observer1 = new TestObserver<>();
        subject.subscribe(observer1);
        
        observer1.assertValues("A", "B", "C");
        observer1.assertComplete();
        
        // Otro subscriptor también recibe el replay completo
        TestObserver<String> observer2 = new TestObserver<>();
        subject.subscribe(observer2);
        
        observer2.assertValues("A", "B", "C");
        observer2.assertComplete();
    }
    
    @Test
    public void testSizeBoundReplay() {
        ReplaySubject<Integer> subject = ReplaySubject.createWithSize(2);
        
        subject.onNext(1);
        subject.onNext(2);
        subject.onNext(3);
        subject.onNext(4);
        
        TestObserver<Integer> observer = new TestObserver<>();
        subject.subscribe(observer);
        
        // Solo recibe los últimos 2 valores
        observer.assertValues(3, 4);
        observer.assertNotComplete();
        
        subject.onNext(5);
        subject.onComplete();
        
        observer.assertValues(3, 4, 5);
        observer.assertComplete();
    }
    
    @Test
    public void testSizeBoundReplayAfterComplete() {
        ReplaySubject<Integer> subject = ReplaySubject.createWithSize(3);
        
        subject.onNext(1);
        subject.onNext(2);
        subject.onNext(3);
        subject.onNext(4);
        subject.onNext(5);
        subject.onComplete();
        
        TestObserver<Integer> observer = new TestObserver<>();
        subject.subscribe(observer);
        
        // Solo recibe los últimos 3 valores
        observer.assertValues(3, 4, 5);
        observer.assertComplete();
    }
    
    @Test
    public void testMultipleObservers() {
        ReplaySubject<String> subject = ReplaySubject.create();
        
        subject.onNext("A");
        subject.onNext("B");
        
        TestObserver<String> observer1 = new TestObserver<>();
        TestObserver<String> observer2 = new TestObserver<>();
        TestObserver<String> observer3 = new TestObserver<>();
        
        subject.subscribe(observer1);
        subject.subscribe(observer2);
        
        subject.onNext("C");
        
        subject.subscribe(observer3);
        
        subject.onNext("D");
        subject.onComplete();
        
        // Observer 1 y 2 reciben replay de A, B y luego C, D
        observer1.assertValues("A", "B", "C", "D");
        observer1.assertComplete();
        
        observer2.assertValues("A", "B", "C", "D");
        observer2.assertComplete();
        
        // Observer 3 recibe replay de A, B, C y luego D
        observer3.assertValues("A", "B", "C", "D");
        observer3.assertComplete();
    }
    
    @Test
    public void testError() {
        ReplaySubject<Integer> subject = ReplaySubject.create();
        TestObserver<Integer> observer1 = new TestObserver<>();
        
        subject.subscribe(observer1);
        
        subject.onNext(1);
        subject.onNext(2);
        subject.onError(new RuntimeException("Test error"));
        
        observer1.assertValues(1, 2);
        observer1.assertError(RuntimeException.class);
        
        // Late subscriber también recibe el replay con error
        TestObserver<Integer> observer2 = new TestObserver<>();
        subject.subscribe(observer2);
        
        observer2.assertValues(1, 2);
        observer2.assertError(RuntimeException.class);
    }
    
    @Test
    public void testHasObservers() {
        ReplaySubject<Integer> subject = ReplaySubject.create();
        
        assertFalse(subject.hasObservers());
        assertEquals(0, subject.observerCount());
        
        TestObserver<Integer> observer1 = new TestObserver<>();
        subject.subscribe(observer1);
        
        assertTrue(subject.hasObservers());
        assertEquals(1, subject.observerCount());
        
        TestObserver<Integer> observer2 = new TestObserver<>();
        subject.subscribe(observer2);
        
        assertEquals(2, subject.observerCount());
        
        observer1.dispose();
        
        assertEquals(1, subject.observerCount());
        
        observer2.dispose();
        
        assertEquals(0, subject.observerCount());
    }
    
    @Test
    public void testHasComplete() {
        ReplaySubject<Integer> subject = ReplaySubject.create();
        
        assertFalse(subject.hasComplete());
        assertFalse(subject.hasThrowable());
        
        subject.onNext(1);
        
        assertFalse(subject.hasComplete());
        
        subject.onComplete();
        
        assertTrue(subject.hasComplete());
        assertFalse(subject.hasThrowable());
    }
    
    @Test
    public void testHasThrowable() {
        ReplaySubject<Integer> subject = ReplaySubject.create();
        RuntimeException error = new RuntimeException("Test");
        
        assertFalse(subject.hasThrowable());
        assertNull(subject.getThrowable());
        
        subject.onNext(1);
        subject.onError(error);
        
        assertTrue(subject.hasThrowable());
        assertEquals(error, subject.getThrowable());
        assertFalse(subject.hasComplete());
    }
    
    @Test
    public void testHasValue() {
        ReplaySubject<Integer> subject = ReplaySubject.create();
        
        assertFalse(subject.hasValue());
        assertEquals(0, subject.size());
        
        subject.onNext(1);
        
        assertTrue(subject.hasValue());
        assertEquals(1, subject.size());
        
        subject.onNext(2);
        subject.onNext(3);
        
        assertTrue(subject.hasValue());
        assertEquals(3, subject.size());
    }
    
    @Test
    public void testGetValues() {
        ReplaySubject<Integer> subject = ReplaySubject.create();
        
        subject.onNext(1);
        subject.onNext(2);
        subject.onNext(3);
        
        Integer[] values = subject.getValues(new Integer[0]);
        
        assertEquals(3, values.length);
        assertEquals(1, values[0].intValue());
        assertEquals(2, values[1].intValue());
        assertEquals(3, values[2].intValue());
    }
    
    @Test
    public void testGetValuesSizeBound() {
        ReplaySubject<Integer> subject = ReplaySubject.createWithSize(2);
        
        subject.onNext(1);
        subject.onNext(2);
        subject.onNext(3);
        subject.onNext(4);
        
        Integer[] values = subject.getValues(new Integer[0]);
        
        assertEquals(2, values.length);
        assertEquals(3, values[0].intValue());
        assertEquals(4, values[1].intValue());
    }
    
    @Test
    public void testDispose() {
        ReplaySubject<Integer> subject = ReplaySubject.create();
        TestObserver<Integer> observer = new TestObserver<>();
        
        subject.onNext(1);
        subject.subscribe(observer);
        
        subject.onNext(2);
        observer.dispose();
        
        subject.onNext(3);
        subject.onComplete();
        
        observer.assertValues(1, 2);
        observer.assertNotComplete();
    }
    
    @Test
    public void testNullOnNext() {
        ReplaySubject<Integer> subject = ReplaySubject.create();
        TestObserver<Integer> observer = new TestObserver<>();
        
        subject.subscribe(observer);
        subject.onNext(null);
        
        observer.assertError(NullPointerException.class);
    }
    
    @Test
    public void testAsObserver() {
        ReplaySubject<Integer> subject = ReplaySubject.create();
        
        // Usamos el subject como Observer
        Observable.just(1, 2, 3).subscribe(subject);
        
        TestObserver<Integer> observer = new TestObserver<>();
        subject.subscribe(observer);
        
        observer.assertValues(1, 2, 3);
        observer.assertComplete();
    }
    
    @Test
    public void testOperatorChaining() {
        ReplaySubject<Integer> subject = ReplaySubject.create();
        
        subject.onNext(1);
        subject.onNext(2);
        subject.onNext(3);
        
        TestObserver<String> observer = new TestObserver<>();
        
        subject
            .map(i -> i * 2)
            .filter(i -> i > 2)
            .map(String::valueOf)
            .subscribe(observer);
        
        subject.onNext(4);
        subject.onComplete();
        
        observer.assertValues("4", "6", "8");
        observer.assertComplete();
    }
    
    @Test
    public void testEmptyReplaySubject() {
        ReplaySubject<Integer> subject = ReplaySubject.create();
        TestObserver<Integer> observer = new TestObserver<>();
        
        subject.subscribe(observer);
        subject.onComplete();
        
        observer.assertEmpty();
        observer.assertComplete();
        
        // Late subscriber también recibe solo complete
        TestObserver<Integer> observer2 = new TestObserver<>();
        subject.subscribe(observer2);
        
        observer2.assertEmpty();
        observer2.assertComplete();
    }
    
    @Test
    public void testOnNextAfterComplete() {
        ReplaySubject<Integer> subject = ReplaySubject.create();
        TestObserver<Integer> observer = new TestObserver<>();
        
        subject.subscribe(observer);
        
        subject.onNext(1);
        subject.onComplete();
        subject.onNext(2); // Debe ser ignorado
        
        observer.assertValues(1);
        observer.assertComplete();
        
        // Verificar que el buffer no fue modificado
        assertEquals(1, subject.size());
    }
    
    @Test
    public void testOnNextAfterError() {
        ReplaySubject<Integer> subject = ReplaySubject.create();
        TestObserver<Integer> observer = new TestObserver<>();
        
        subject.subscribe(observer);
        
        subject.onNext(1);
        subject.onError(new RuntimeException("Error"));
        subject.onNext(2); // Debe ser ignorado
        
        observer.assertValues(1);
        observer.assertError(RuntimeException.class);
        
        // Verificar que el buffer no fue modificado
        assertEquals(1, subject.size());
    }
    
    @Test
    public void testCreateWithInvalidSize() {
        assertThrows(IllegalArgumentException.class, () -> {
            ReplaySubject.createWithSize(0);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            ReplaySubject.createWithSize(-1);
        });
    }
    
    @Test
    public void testLargeBuffer() {
        ReplaySubject<Integer> subject = ReplaySubject.create();
        
        // Emitir muchos valores
        for (int i = 0; i < 1000; i++) {
            subject.onNext(i);
        }
        
        subject.onComplete();
        
        TestObserver<Integer> observer = new TestObserver<>();
        subject.subscribe(observer);
        
        // Verificar que recibió todos los 1000 valores
        assertEquals(1000, observer.values().size());
        assertEquals(0, observer.values().get(0).intValue());
        assertEquals(999, observer.values().get(999).intValue());
    }
}
