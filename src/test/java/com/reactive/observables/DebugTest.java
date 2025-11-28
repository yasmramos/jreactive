package com.reactive.observables;

import com.reactive.core.*;
import com.reactive.testing.TestObserver;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DebugTest {
    
    @Test
    public void testSimplePublish() {
        System.out.println("=== Test Simple Publish ===");
        ConnectableObservable<Integer> pub = Observable.just(1, 2, 3).publish();
        
        TestObserver<Integer> obs = new TestObserver<>();
        System.out.println("Before subscribe");
        pub.subscribe(obs);
        System.out.println("After subscribe, before connect");
        pub.connect();
        System.out.println("After connect");
        System.out.println("Values: " + obs.values());
        
        obs.assertValues(1, 2, 3);
    }
    
    @Test
    public void testSimpleRefCount() {
        System.out.println("\n=== Test Simple RefCount ===");
        Observable<Integer> ref = Observable.just(1, 2, 3).publish().refCount();
        
        TestObserver<Integer> obs = new TestObserver<>();
        System.out.println("Before subscribe");
        ref.subscribe(obs);
        System.out.println("After subscribe");
        System.out.println("Values: " + obs.values());
        
        obs.assertValues(1, 2, 3);
    }
}
