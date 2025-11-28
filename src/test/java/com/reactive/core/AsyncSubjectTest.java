package com.reactive.core;

import com.reactive.testing.TestObserver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests para AsyncSubject.
 */
public class AsyncSubjectTest {
    
    @Test
    public void testBasicBehavior() {
        AsyncSubject<Integer> subject = AsyncSubject.create();
        TestObserver<Integer> observer = new TestObserver<>();
        
        subject.subscribe(observer);
        
        subject.onNext(1);
        subject.onNext(2);
        subject.onNext(3);
        
        // Nada emitido aún
        observer.assertEmpty();
        observer.assertNotComplete();
        
        subject.onComplete();
        
        // Solo recibe el último valor
        observer.assertValues(3);
        observer.assertComplete();
    }
    
    @Test
    public void testLateSubscriber() {
        AsyncSubject<Integer> subject = AsyncSubject.create();
        
        subject.onNext(1);
        subject.onNext(2);
        subject.onNext(3);
        subject.onComplete();
        
        TestObserver<Integer> observer = new TestObserver<>();
        subject.subscribe(observer);
        
        // Late subscriber recibe inmediatamente el último valor
        observer.assertValues(3);
        observer.assertComplete();
    }
    
    @Test
    public void testMultipleObservers() {
        AsyncSubject<String> subject = AsyncSubject.create();
        TestObserver<String> observer1 = new TestObserver<>();
        TestObserver<String> observer2 = new TestObserver<>();
        
        subject.subscribe(observer1);
        
        subject.onNext("A");
        subject.onNext("B");
        
        subject.subscribe(observer2);
        
        subject.onNext("C");
        subject.onComplete();
        
        // Ambos observers reciben solo el último valor
        observer1.assertValues("C");
        observer1.assertComplete();
        
        observer2.assertValues("C");
        observer2.assertComplete();
    }
    
    @Test
    public void testCompleteWithoutValues() {
        AsyncSubject<Integer> subject = AsyncSubject.create();
        TestObserver<Integer> observer = new TestObserver<>();
        
        subject.subscribe(observer);
        subject.onComplete();
        
        // Sin valores, solo complete
        observer.assertEmpty();
        observer.assertComplete();
    }
    
    @Test
    public void testLateSubscriberWithoutValues() {
        AsyncSubject<Integer> subject = AsyncSubject.create();
        
        subject.onComplete();
        
        TestObserver<Integer> observer = new TestObserver<>();
        subject.subscribe(observer);
        
        // Late subscriber recibe solo complete
        observer.assertEmpty();
        observer.assertComplete();
    }
    
    @Test
    public void testError() {
        AsyncSubject<Integer> subject = AsyncSubject.create();
        TestObserver<Integer> observer = new TestObserver<>();
        
        subject.subscribe(observer);
        
        subject.onNext(1);
        subject.onNext(2);
        subject.onError(new RuntimeException("Test error"));
        
        // Solo recibe error, sin valores
        observer.assertEmpty();
        observer.assertError(RuntimeException.class);
        observer.assertErrorMessage("Test error");
    }
    
    @Test
    public void testLateSubscriberAfterError() {
        AsyncSubject<Integer> subject = AsyncSubject.create();
        RuntimeException error = new RuntimeException("Test error");
        
        subject.onNext(1);
        subject.onNext(2);
        subject.onError(error);
        
        TestObserver<Integer> observer = new TestObserver<>();
        subject.subscribe(observer);
        
        // Late subscriber solo recibe error
        observer.assertEmpty();
        observer.assertError(RuntimeException.class);
    }
    
    @Test
    public void testOnNextAfterComplete() {
        AsyncSubject<Integer> subject = AsyncSubject.create();
        TestObserver<Integer> observer = new TestObserver<>();
        
        subject.subscribe(observer);
        
        subject.onNext(1);
        subject.onNext(2);
        subject.onComplete();
        subject.onNext(3); // Debe ser ignorado
        
        // Solo recibe el último valor antes de complete (2)
        observer.assertValues(2);
        observer.assertComplete();
    }
    
    @Test
    public void testOnNextAfterError() {
        AsyncSubject<Integer> subject = AsyncSubject.create();
        TestObserver<Integer> observer = new TestObserver<>();
        
        subject.subscribe(observer);
        
        subject.onNext(1);
        subject.onError(new RuntimeException("Error"));
        subject.onNext(2); // Debe ser ignorado
        
        observer.assertEmpty();
        observer.assertError(RuntimeException.class);
    }
    
    @Test
    public void testOnCompleteAfterComplete() {
        AsyncSubject<Integer> subject = AsyncSubject.create();
        TestObserver<Integer> observer = new TestObserver<>();
        
        subject.subscribe(observer);
        
        subject.onNext(1);
        subject.onComplete();
        subject.onComplete(); // Segundo complete debe ser ignorado
        
        observer.assertValues(1);
        observer.assertComplete();
    }
    
    @Test
    public void testDispose() {
        AsyncSubject<Integer> subject = AsyncSubject.create();
        TestObserver<Integer> observer = new TestObserver<>();
        
        subject.subscribe(observer);
        
        subject.onNext(1);
        observer.dispose();
        
        subject.onNext(2);
        subject.onComplete();
        
        // Observer disposed, no recibe nada
        observer.assertEmpty();
        observer.assertNotComplete();
    }
    
    @Test
    public void testHasObservers() {
        AsyncSubject<Integer> subject = AsyncSubject.create();
        
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
        AsyncSubject<Integer> subject = AsyncSubject.create();
        
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
        AsyncSubject<Integer> subject = AsyncSubject.create();
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
    public void testGetValue() {
        AsyncSubject<String> subject = AsyncSubject.create();
        
        assertNull(subject.getValue());
        assertFalse(subject.hasValue());
        
        subject.onNext("A");
        
        // Valor no disponible hasta completar
        assertNull(subject.getValue());
        assertFalse(subject.hasValue());
        
        subject.onNext("B");
        subject.onNext("C");
        subject.onComplete();
        
        // Ahora el valor está disponible
        assertEquals("C", subject.getValue());
        assertTrue(subject.hasValue());
    }
    
    @Test
    public void testGetValueAfterError() {
        AsyncSubject<String> subject = AsyncSubject.create();
        
        subject.onNext("A");
        subject.onError(new RuntimeException("Error"));
        
        // No hay valor disponible después de error
        assertNull(subject.getValue());
        assertFalse(subject.hasValue());
    }
    
    @Test
    public void testNullOnNext() {
        AsyncSubject<Integer> subject = AsyncSubject.create();
        TestObserver<Integer> observer = new TestObserver<>();
        
        subject.subscribe(observer);
        subject.onNext(null);
        
        observer.assertEmpty();
        observer.assertError(NullPointerException.class);
    }
    
    @Test
    public void testNullOnError() {
        AsyncSubject<Integer> subject = AsyncSubject.create();
        TestObserver<Integer> observer = new TestObserver<>();
        
        subject.subscribe(observer);
        subject.onError(null);
        
        observer.assertError(NullPointerException.class);
    }
    
    @Test
    public void testAsObserver() {
        AsyncSubject<Integer> subject = AsyncSubject.create();
        
        // Usamos el subject como Observer
        Observable.just(1, 2, 3).subscribe(subject);
        
        TestObserver<Integer> observer = new TestObserver<>();
        subject.subscribe(observer);
        
        // Solo recibe el último valor (3)
        observer.assertValues(3);
        observer.assertComplete();
    }
    
    @Test
    public void testOperatorChaining() {
        AsyncSubject<Integer> subject = AsyncSubject.create();
        TestObserver<String> observer = new TestObserver<>();
        
        subject
            .map(i -> i * 2)
            .map(String::valueOf)
            .subscribe(observer);
        
        subject.onNext(1);
        subject.onNext(2);
        subject.onNext(3);
        
        observer.assertEmpty();
        
        subject.onComplete();
        
        // Solo recibe el resultado del último valor transformado
        observer.assertValues("6");
        observer.assertComplete();
    }
    
    @Test
    public void testSingleValue() {
        AsyncSubject<Integer> subject = AsyncSubject.create();
        TestObserver<Integer> observer = new TestObserver<>();
        
        subject.subscribe(observer);
        
        subject.onNext(42);
        subject.onComplete();
        
        observer.assertValues(42);
        observer.assertComplete();
    }
    
    @Test
    public void testMultipleLateSubscribers() {
        AsyncSubject<Integer> subject = AsyncSubject.create();
        
        subject.onNext(1);
        subject.onNext(2);
        subject.onNext(3);
        subject.onComplete();
        
        // Múltiples late subscribers
        TestObserver<Integer> observer1 = new TestObserver<>();
        TestObserver<Integer> observer2 = new TestObserver<>();
        TestObserver<Integer> observer3 = new TestObserver<>();
        
        subject.subscribe(observer1);
        subject.subscribe(observer2);
        subject.subscribe(observer3);
        
        // Todos reciben el último valor
        observer1.assertValues(3);
        observer1.assertComplete();
        
        observer2.assertValues(3);
        observer2.assertComplete();
        
        observer3.assertValues(3);
        observer3.assertComplete();
    }
    
    @Test
    public void testMixedSubscribers() {
        AsyncSubject<String> subject = AsyncSubject.create();
        TestObserver<String> earlyObserver = new TestObserver<>();
        
        // Observer temprano
        subject.subscribe(earlyObserver);
        
        subject.onNext("A");
        subject.onNext("B");
        
        earlyObserver.assertEmpty();
        
        subject.onNext("C");
        subject.onComplete();
        
        // Observer temprano recibe el último valor
        earlyObserver.assertValues("C");
        earlyObserver.assertComplete();
        
        // Observer tardío también recibe el último valor
        TestObserver<String> lateObserver = new TestObserver<>();
        subject.subscribe(lateObserver);
        
        lateObserver.assertValues("C");
        lateObserver.assertComplete();
    }
    
    @Test
    public void testValueOverwrite() {
        AsyncSubject<Integer> subject = AsyncSubject.create();
        TestObserver<Integer> observer = new TestObserver<>();
        
        subject.subscribe(observer);
        
        // Emitir múltiples valores
        for (int i = 1; i <= 100; i++) {
            subject.onNext(i);
        }
        
        subject.onComplete();
        
        // Solo recibe el último valor (100)
        observer.assertValues(100);
        observer.assertComplete();
    }
}
