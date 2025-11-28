package com.reactive.core;

import com.reactive.testing.TestObserver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests para BehaviorSubject.
 */
public class BehaviorSubjectTest {
    
    @Test
    public void testBasicBehavior() {
        BehaviorSubject<Integer> subject = BehaviorSubject.create();
        TestObserver<Integer> observer = new TestObserver<>();
        
        subject.onNext(1);
        subject.subscribe(observer);
        
        subject.onNext(2);
        subject.onNext(3);
        subject.onComplete();
        
        observer.assertValues(1, 2, 3);
        observer.assertComplete();
    }
    
    @Test
    public void testDefaultValue() {
        BehaviorSubject<String> subject = BehaviorSubject.createDefault("Initial");
        TestObserver<String> observer = new TestObserver<>();
        
        subject.subscribe(observer);
        
        observer.assertValues("Initial");
        observer.assertNotComplete();
        
        subject.onNext("A");
        subject.onComplete();
        
        observer.assertValues("Initial", "A");
        observer.assertComplete();
    }
    
    @Test
    public void testLateSubscriberGetsLastValue() {
        BehaviorSubject<Integer> subject = BehaviorSubject.create();
        TestObserver<Integer> observer1 = new TestObserver<>();
        
        subject.subscribe(observer1);
        
        subject.onNext(1);
        subject.onNext(2);
        
        TestObserver<Integer> observer2 = new TestObserver<>();
        subject.subscribe(observer2);
        
        subject.onNext(3);
        subject.onComplete();
        
        // Observer 1 recibe todos los valores desde el inicio
        observer1.assertValues(1, 2, 3);
        observer1.assertComplete();
        
        // Observer 2 recibe el último valor antes de suscribirse (2) y luego 3
        observer2.assertValues(2, 3);
        observer2.assertComplete();
    }
    
    @Test
    public void testMultipleObservers() {
        BehaviorSubject<String> subject = BehaviorSubject.createDefault("Start");
        TestObserver<String> observer1 = new TestObserver<>();
        TestObserver<String> observer2 = new TestObserver<>();
        
        subject.subscribe(observer1);
        subject.subscribe(observer2);
        
        subject.onNext("A");
        subject.onNext("B");
        subject.onComplete();
        
        observer1.assertValues("Start", "A", "B");
        observer1.assertComplete();
        
        observer2.assertValues("Start", "A", "B");
        observer2.assertComplete();
    }
    
    @Test
    public void testError() {
        BehaviorSubject<Integer> subject = BehaviorSubject.create();
        TestObserver<Integer> observer = new TestObserver<>();
        
        subject.onNext(1);
        subject.subscribe(observer);
        
        subject.onNext(2);
        subject.onError(new RuntimeException("Test error"));
        
        observer.assertValues(1, 2);
        observer.assertError(RuntimeException.class);
        observer.assertErrorMessage("Test error");
    }
    
    @Test
    public void testLateSubscriberAfterComplete() {
        BehaviorSubject<Integer> subject = BehaviorSubject.create();
        
        subject.onNext(1);
        subject.onNext(2);
        subject.onComplete();
        
        TestObserver<Integer> observer = new TestObserver<>();
        subject.subscribe(observer);
        
        // Observer tarde solo recibe complete (no hay valor porque está completado)
        observer.assertEmpty();
        observer.assertComplete();
    }
    
    @Test
    public void testLateSubscriberAfterError() {
        BehaviorSubject<Integer> subject = BehaviorSubject.create();
        RuntimeException error = new RuntimeException("Test error");
        
        subject.onNext(1);
        subject.onError(error);
        
        TestObserver<Integer> observer = new TestObserver<>();
        subject.subscribe(observer);
        
        // Observer tarde solo recibe error
        observer.assertEmpty();
        observer.assertError(RuntimeException.class);
    }
    
    @Test
    public void testGetValue() {
        BehaviorSubject<String> subject = BehaviorSubject.create();
        
        assertNull(subject.getValue());
        assertFalse(subject.hasValue());
        
        subject.onNext("A");
        
        assertEquals("A", subject.getValue());
        assertTrue(subject.hasValue());
        
        subject.onNext("B");
        
        assertEquals("B", subject.getValue());
        assertTrue(subject.hasValue());
        
        subject.onComplete();
        
        // BehaviorSubject mantiene su último valor incluso después de completar
        assertEquals("B", subject.getValue());
        assertTrue(subject.hasValue());
    }
    
    @Test
    public void testGetValueWithDefault() {
        BehaviorSubject<String> subject = BehaviorSubject.createDefault("Initial");
        
        assertEquals("Initial", subject.getValue());
        assertTrue(subject.hasValue());
        
        subject.onNext("Updated");
        
        assertEquals("Updated", subject.getValue());
        assertTrue(subject.hasValue());
    }
    
    @Test
    public void testHasObservers() {
        BehaviorSubject<Integer> subject = BehaviorSubject.create();
        
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
    }
    
    @Test
    public void testHasComplete() {
        BehaviorSubject<Integer> subject = BehaviorSubject.create();
        
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
        BehaviorSubject<Integer> subject = BehaviorSubject.create();
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
    public void testNullOnNext() {
        BehaviorSubject<Integer> subject = BehaviorSubject.create();
        TestObserver<Integer> observer = new TestObserver<>();
        
        subject.subscribe(observer);
        subject.onNext(null);
        
        observer.assertError(NullPointerException.class);
    }
    
    @Test
    public void testNullDefaultValue() {
        assertThrows(NullPointerException.class, () -> {
            BehaviorSubject.createDefault(null);
        });
    }
    
    @Test
    public void testSubscribeAfterDispose() {
        BehaviorSubject<Integer> subject = BehaviorSubject.create();
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
    public void testAsObserver() {
        BehaviorSubject<Integer> subject = BehaviorSubject.create();
        TestObserver<Integer> finalObserver = new TestObserver<>();
        
        subject.subscribe(finalObserver);
        
        // Usamos el subject como Observer
        Observable.just(1, 2, 3).subscribe(subject);
        
        finalObserver.assertValues(1, 2, 3);
        finalObserver.assertComplete();
    }
    
    @Test
    public void testOperatorChaining() {
        BehaviorSubject<Integer> subject = BehaviorSubject.createDefault(0);
        TestObserver<String> observer = new TestObserver<>();
        
        subject
            .map(i -> i * 2)
            .filter(i -> i > 0)
            .map(String::valueOf)
            .subscribe(observer);
        
        subject.onNext(1);
        subject.onNext(2);
        subject.onNext(3);
        subject.onComplete();
        
        observer.assertValues("2", "4", "6");
        observer.assertComplete();
    }
    
    @Test
    public void testEmptyBehaviorSubject() {
        BehaviorSubject<Integer> subject = BehaviorSubject.create();
        TestObserver<Integer> observer = new TestObserver<>();
        
        subject.subscribe(observer);
        subject.onComplete();
        
        observer.assertEmpty();
        observer.assertComplete();
    }
    
    @Test
    public void testValuePersistence() {
        BehaviorSubject<Integer> subject = BehaviorSubject.createDefault(0);
        
        subject.onNext(1);
        assertEquals(1, subject.getValue().intValue());
        
        subject.onNext(2);
        assertEquals(2, subject.getValue().intValue());
        
        subject.onNext(3);
        assertEquals(3, subject.getValue().intValue());
        
        // Múltiples subscriptores obtienen el último valor
        TestObserver<Integer> observer1 = new TestObserver<>();
        TestObserver<Integer> observer2 = new TestObserver<>();
        
        subject.subscribe(observer1);
        subject.subscribe(observer2);
        
        observer1.assertValues(3);
        observer2.assertValues(3);
        
        subject.onNext(4);
        
        observer1.assertValues(3, 4);
        observer2.assertValues(3, 4);
    }
    
    @Test
    public void testOnNextAfterComplete() {
        BehaviorSubject<Integer> subject = BehaviorSubject.create();
        TestObserver<Integer> observer = new TestObserver<>();
        
        subject.subscribe(observer);
        
        subject.onNext(1);
        subject.onComplete();
        subject.onNext(2); // Debe ser ignorado
        
        observer.assertValues(1);
        observer.assertComplete();
        // BehaviorSubject mantiene su valor después de completar
        assertTrue(subject.hasValue());
    }
    
    @Test
    public void testOnNextAfterError() {
        BehaviorSubject<Integer> subject = BehaviorSubject.create();
        TestObserver<Integer> observer = new TestObserver<>();
        
        subject.subscribe(observer);
        
        subject.onNext(1);
        subject.onError(new RuntimeException("Error"));
        subject.onNext(2); // Debe ser ignorado
        
        observer.assertValues(1);
        observer.assertError(RuntimeException.class);
    }
}
