package com.reactive.examples;

import com.reactive.backpressure.BackpressureStrategy;
import com.reactive.core.*;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Examples demonstrating Step 3 and 4 features:
 * - Advanced Grouping (GroupBy, Buffer, Window)
 * - Flowable with Backpressure
 */
public class AdvancedFeaturesExample {
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== JReactive: Advanced Features Demo ===\n");
        
        // Example 1: GroupBy with GroupedObservable
        example1_GroupBy();
        
        // Example 2: Buffer with Skip
        example2_BufferWithSkip();
        
        // Example 3: Flowable with Backpressure
        example3_FlowableBackpressure();
        
        // Example 4: Complex Pipeline
        example4_ComplexPipeline();
        
        System.out.println("\n=== Demo Complete ===");
    }
    
    /**
     * Example 1: GroupBy with streaming groups
     */
    private static void example1_GroupBy() {
        System.out.println("--- Example 1: GroupBy ---");
        
        Observable.just("apple", "apricot", "banana", "berry", "cherry", "avocado")
            .groupBy(word -> word.charAt(0))  // Group by first letter
            .subscribe(group -> {
                System.out.println("Group: " + group.getKey());
                group.subscribe(word -> 
                    System.out.println("  - " + word)
                );
            });
        
        System.out.println();
    }
    
    /**
     * Example 2: Buffer with overlapping windows
     */
    private static void example2_BufferWithSkip() {
        System.out.println("--- Example 2: Buffer with Skip (Overlapping) ---");
        
        // Create overlapping buffers: size=3, skip=2
        Observable.range(1, 10)
            .buffer(3, 2)
            .subscribe(buffer -> 
                System.out.println("Buffer: " + buffer)
            );
        
        System.out.println();
    }
    
    /**
     * Example 3: Flowable with backpressure control
     */
    private static void example3_FlowableBackpressure() {
        System.out.println("--- Example 3: Flowable with Backpressure ---");
        
        Flowable.range(1, 20)
            .subscribe(new Subscriber<Integer>() {
                private Subscription subscription;
                private int received = 0;
                
                @Override
                public void onSubscribe(Subscription subscription) {
                    this.subscription = subscription;
                    System.out.println("Subscribed! Requesting 3 items...");
                    subscription.request(3);  // Request 3 items initially
                }
                
                @Override
                public void onNext(Integer item) {
                    received++;
                    System.out.println("Received: " + item);
                    
                    // Request more items after processing 3
                    if (received % 3 == 0 && received < 20) {
                        System.out.println("Processed 3 items, requesting 3 more...");
                        subscription.request(3);
                    }
                }
                
                @Override
                public void onError(Throwable throwable) {
                    System.err.println("Error: " + throwable.getMessage());
                }
                
                @Override
                public void onComplete() {
                    System.out.println("Completed! Total received: " + received);
                }
            });
        
        System.out.println();
    }
    
    /**
     * Example 4: Complex pipeline combining multiple features
     */
    private static void example4_ComplexPipeline() {
        System.out.println("--- Example 4: Complex Pipeline ---");
        
        // Simulate sensor data processing
        Observable.range(1, 50)
            .map(n -> {
                // Simulate sensor readings
                return new SensorReading(n, n % 3, n * 1.5);
            })
            .groupBy(reading -> reading.sensorId)  // Group by sensor
            .subscribe(group -> {
                System.out.println("Processing Sensor " + group.getKey());
                
                group
                    .buffer(5)  // Buffer 5 readings per sensor
                    .subscribe(readings -> {
                        double avg = readings.stream()
                            .mapToDouble(r -> r.value)
                            .average()
                            .orElse(0);
                        
                        System.out.println("  Sensor " + group.getKey() + 
                                         " - Batch avg: " + String.format("%.2f", avg));
                    });
            });
        
        System.out.println();
    }
    
    /**
     * Example 5: Flowable with custom backpressure strategy
     */
    public static void example5_FlowableWithStrategy() {
        System.out.println("--- Example 5: Flowable with DROP Strategy ---");
        
        Flowable.<Integer>create(emitter -> {
            // Rapidly emit 100 items
            for (int i = 1; i <= 100; i++) {
                emitter.onNext(i);
            }
            emitter.onComplete();
        }, BackpressureStrategy.DROP)
        .subscribe(new Subscriber<Integer>() {
            @Override
            public void onSubscribe(Subscription subscription) {
                subscription.request(10);  // Only request 10, rest will be dropped
            }
            
            @Override
            public void onNext(Integer item) {
                System.out.println("Received: " + item);
            }
            
            @Override
            public void onError(Throwable throwable) {
                System.err.println("Error: " + throwable.getMessage());
            }
            
            @Override
            public void onComplete() {
                System.out.println("Completed (dropped items not received)");
            }
        });
        
        System.out.println();
    }
    
    /**
     * Example 6: Converting between Observable and Flowable
     */
    public static void example6_ObservableFlowableConversion() {
        System.out.println("--- Example 6: Observable <-> Flowable Conversion ---");
        
        // Observable to Flowable
        Observable<Integer> observable = Observable.range(1, 10);
        Flowable<Integer> flowable = observable.toFlowable(BackpressureStrategy.BUFFER);
        
        System.out.println("Converted Observable to Flowable");
        flowable.subscribe(new Subscriber<Integer>() {
            @Override
            public void onSubscribe(Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }
            
            @Override
            public void onNext(Integer item) {
                System.out.print(item + " ");
            }
            
            @Override
            public void onError(Throwable throwable) {
                System.err.println("Error: " + throwable.getMessage());
            }
            
            @Override
            public void onComplete() {
                System.out.println("\nConversion successful!");
            }
        });
        
        System.out.println();
        
        // Flowable to Observable
        Flowable<String> flowable2 = Flowable.just("A", "B", "C");
        Observable<String> observable2 = flowable2.toObservable();
        
        System.out.println("Converted Flowable to Observable");
        observable2.subscribe(
            item -> System.out.print(item + " "),
            Throwable::printStackTrace,
            () -> System.out.println("\nConversion successful!")
        );
        
        System.out.println();
    }
    
    /**
     * Example 7: Chaining operators with backpressure
     */
    public static void example7_ChainedOperators() {
        System.out.println("--- Example 7: Chained Operators with Backpressure ---");
        
        Flowable.range(1, 100)
            .filter(n -> n % 2 == 0)      // Only even numbers
            .map(n -> n * 2)               // Double them
            .take(10)                      // Take first 10
            .subscribe(new Subscriber<Integer>() {
                @Override
                public void onSubscribe(Subscription subscription) {
                    subscription.request(Long.MAX_VALUE);
                }
                
                @Override
                public void onNext(Integer item) {
                    System.out.print(item + " ");
                }
                
                @Override
                public void onError(Throwable throwable) {
                    System.err.println("Error: " + throwable.getMessage());
                }
                
                @Override
                public void onComplete() {
                    System.out.println("\nChained pipeline completed!");
                }
            });
        
        System.out.println();
    }
    
    // Helper class for sensor reading example
    static class SensorReading {
        final int id;
        final int sensorId;
        final double value;
        
        SensorReading(int id, int sensorId, double value) {
            this.id = id;
            this.sensorId = sensorId;
            this.value = value;
        }
        
        @Override
        public String toString() {
            return String.format("Reading{id=%d, sensor=%d, value=%.2f}", 
                               id, sensorId, value);
        }
    }
}
