package com.reactive.core;

import com.reactive.testing.TestObserver;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests para PublishSubject.
 */
public class PublishSubjectTest {
    
    @Test
    public void testBasicBehavior() {
        PublishSubject<Integer> subject = PublishSubject.create();
        TestObserver<Integer> observer = new TestObserver<>();
        
        subject.subscribe(observer);
        
        subject.onNext(1);
        subject.onNext(2);
        subject.onNext(3);
        subject.onComplete();
        
        observer.assertValues(1, 2, 3);
        observer.assertComplete();
        observer.assertNoErrors();
    }
    
    @Test
    public void testLateSubscriber() {
        PublishSubject<Integer> subject = PublishSubject.create();
        TestObserver<Integer> observer1 = new TestObserver<>();
        TestObserver<Integer> observer2 = new TestObserver<>();
        
        subject.subscribe(observer1);
        
        subject.onNext(1);
        subject.onNext(2);
        
        // Observer 2 se suscribe tarde
        subject.subscribe(observer2);
        
        subject.onNext(3);
        subject.onComplete();
        
        // Observer 1 recibe todos los valores
        observer1.assertValues(1, 2, 3);
        observer1.assertComplete();
        
        // Observer 2 solo recibe valores después de suscribirse
        observer2.assertValues(3);
        observer2.assertComplete();
    }
    
    @Test
    public void testMultipleObservers() {
        PublishSubject<String> subject = PublishSubject.create();
        TestObserver<String> observer1 = new TestObserver<>();
        TestObserver<String> observer2 = new TestObserver<>();
        TestObserver<String> observer3 = new TestObserver<>();
        
        subject.subscribe(observer1);
        subject.subscribe(observer2);
        subject.subscribe(observer3);
        
        subject.onNext("A");
        subject.onNext("B");
        subject.onComplete();
        
        observer1.assertValues("A", "B");
        observer1.assertComplete();
        
        observer2.assertValues("A", "B");
        observer2.assertComplete();
        
        observer3.assertValues("A", "B");
        observer3.assertComplete();
    }
    
    @Test
    public void testError() {
        PublishSubject<Integer> subject = PublishSubject.create();
        TestObserver<Integer> observer = new TestObserver<>();
        
        subject.subscribe(observer);
        
        subject.onNext(1);
        subject.onError(new RuntimeException("Test error"));
        
        observer.assertValues(1);
        observer.assertError(RuntimeException.class);
        observer.assertErrorMessage("Test error");
    }
    
    @Test
    public void testLateSubscriberAfterComplete() {
        PublishSubject<Integer> subject = PublishSubject.create();
        
        subject.onNext(1);
        subject.onNext(2);
        subject.onComplete();
        
        TestObserver<Integer> observer = new TestObserver<>();
        subject.subscribe(observer);
        
        // Observer tarde solo recibe complete
        observer.assertEmpty();
        observer.assertComplete();
    }
    
    @Test
    public void testLateSubscriberAfterError() {
        PublishSubject<Integer> subject = PublishSubject.create();
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
    public void testOnNextAfterComplete() {
        PublishSubject<Integer> subject = PublishSubject.create();
        TestObserver<Integer> observer = new TestObserver<>();
        
        subject.subscribe(observer);
        
        subject.onNext(1);
        subject.onComplete();
        subject.onNext(2); // Esto debe ser ignorado
        
        observer.assertValues(1);
        observer.assertComplete();
    }
    
    @Test
    public void testOnNextAfterError() {
        PublishSubject<Integer> subject = PublishSubject.create();
        TestObserver<Integer> observer = new TestObserver<>();
        
        subject.subscribe(observer);
        
        subject.onNext(1);
        subject.onError(new RuntimeException("Error"));
        subject.onNext(2); // Esto debe ser ignorado
        
        observer.assertValues(1);
        observer.assertError(RuntimeException.class);
    }
    
    @Test
    public void testOnCompleteAfterComplete() {
        PublishSubject<Integer> subject = PublishSubject.create();
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
        PublishSubject<Integer> subject = PublishSubject.create();
        TestObserver<Integer> observer = new TestObserver<>();
        
        subject.subscribe(observer);
        
        subject.onNext(1);
        observer.dispose();
        subject.onNext(2);
        subject.onComplete();
        
        observer.assertValues(1);
        observer.assertNotComplete();
    }
    
    @Test
    public void testHasObservers() {
        PublishSubject<Integer> subject = PublishSubject.create();
        
        assertFalse(subject.hasObservers());
        assertEquals(0, subject.observerCount());
        
        TestObserver<Integer> observer1 = new TestObserver<>();
        subject.subscribe(observer1);
        
        assertTrue(subject.hasObservers());
        assertEquals(1, subject.observerCount());
        
        TestObserver<Integer> observer2 = new TestObserver<>();
        subject.subscribe(observer2);
        
        assertTrue(subject.hasObservers());
        assertEquals(2, subject.observerCount());
        
        observer1.dispose();
        
        assertTrue(subject.hasObservers());
        assertEquals(1, subject.observerCount());
        
        observer2.dispose();
        
        assertFalse(subject.hasObservers());
        assertEquals(0, subject.observerCount());
    }
    
    @Test
    public void testHasComplete() {
        PublishSubject<Integer> subject = PublishSubject.create();
        
        assertFalse(subject.hasComplete());
        assertFalse(subject.hasThrowable());
        
        subject.onNext(1);
        
        assertFalse(subject.hasComplete());
        assertFalse(subject.hasThrowable());
        
        subject.onComplete();
        
        assertTrue(subject.hasComplete());
        assertFalse(subject.hasThrowable());
    }
    
    @Test
    public void testHasThrowable() {
        PublishSubject<Integer> subject = PublishSubject.create();
        RuntimeException error = new RuntimeException("Test");
        
        assertFalse(subject.hasComplete());
        assertFalse(subject.hasThrowable());
        assertNull(subject.getThrowable());
        
        subject.onNext(1);
        
        assertFalse(subject.hasComplete());
        assertFalse(subject.hasThrowable());
        
        subject.onError(error);
        
        assertFalse(subject.hasComplete());
        assertTrue(subject.hasThrowable());
        assertEquals(error, subject.getThrowable());
    }
    
    @Test
    public void testNullOnNext() {
        PublishSubject<Integer> subject = PublishSubject.create();
        TestObserver<Integer> observer = new TestObserver<>();
        
        subject.subscribe(observer);
        subject.onNext(null);
        
        observer.assertEmpty();
        observer.assertError(NullPointerException.class);
    }
    
    @Test
    public void testNullOnError() {
        PublishSubject<Integer> subject = PublishSubject.create();
        TestObserver<Integer> observer = new TestObserver<>();
        
        subject.subscribe(observer);
        subject.onError(null);
        
        observer.assertError(NullPointerException.class);
    }
    
    @Test
    public void testAsObserver() {
        PublishSubject<Integer> subject = PublishSubject.create();
        TestObserver<Integer> finalObserver = new TestObserver<>();
        
        subject.subscribe(finalObserver);
        
        // Usamos el subject como Observer
        Observable.just(1, 2, 3).subscribe(subject);
        
        finalObserver.assertValues(1, 2, 3);
        finalObserver.assertComplete();
    }
    
    @Test
    public void testThreadSafety() throws InterruptedException {
        PublishSubject<Integer> subject = PublishSubject.create();
        AtomicInteger count = new AtomicInteger();
        
        // Múltiples observers
        for (int i = 0; i < 10; i++) {
            subject.subscribe(value -> count.incrementAndGet());
        }
        
        // Múltiples threads emitiendo
        Thread[] threads = new Thread[10];
        for (int i = 0; i < threads.length; i++) {
            final int value = i;
            threads[i] = new Thread(() -> subject.onNext(value));
        }
        
        for (Thread thread : threads) {
            thread.start();
        }
        
        for (Thread thread : threads) {
            thread.join();
        }
        
        subject.onComplete();
        
        // Debería haber 10 valores * 10 observers = 100 notificaciones
        assertEquals(100, count.get());
    }
    
    @Test
    public void testOperatorChaining() {
        PublishSubject<Integer> subject = PublishSubject.create();
        TestObserver<String> observer = new TestObserver<>();
        
        subject
            .map(i -> i * 2)
            .filter(i -> i > 5)
            .map(String::valueOf)
            .subscribe(observer);
        
        subject.onNext(1);
        subject.onNext(2);
        subject.onNext(3);
        subject.onNext(4);
        subject.onNext(5);
        subject.onComplete();
        
        observer.assertValues("6", "8", "10");
        observer.assertComplete();
    }
}
