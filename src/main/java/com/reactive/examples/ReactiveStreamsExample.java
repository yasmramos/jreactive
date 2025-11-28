package com.reactive.examples;

import com.reactive.core.Observable;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * Examples of Reactive Streams integration (Phase 3).
 * Demonstrates interoperability with Reactive Streams and Project Reactor.
 */
public class ReactiveStreamsExample {
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Reactive Streams Integration Examples ===\n");
        
        // Example 1: Observable to Publisher
        publisherExample();
        
        // Example 2: Publisher to Observable
        publisherToObservableExample();
        
        // Example 3: Observable to Flux
        fluxExample();
        
        // Example 4: Observable to Mono
        monoExample();
        
        // Example 5: Flux to Observable
        fluxToObservableExample();
        
        // Example 6: Real-world integration example
        realWorldExample();
    }
    
    static void publisherExample() throws InterruptedException {
        System.out.println("--- Example 1: Observable to Publisher ---");
        
        // Create Observable and convert to Publisher
        Observable<Integer> observable = Observable.range(1, 10);
        Publisher<Integer> publisher = observable.toPublisher();
        
        System.out.println("Subscribing to Publisher with backpressure...");
        
        List<Integer> result = new ArrayList<>();
        
        publisher.subscribe(new Subscriber<Integer>() {
            private Subscription subscription;
            
            @Override
            public void onSubscribe(Subscription s) {
                this.subscription = s;
                System.out.println("Subscribed! Requesting 5 items...");
                s.request(5); // Request only 5 items
            }
            
            @Override
            public void onNext(Integer value) {
                result.add(value);
                System.out.println("  Received: " + value);
                
                // Request more items after processing each batch
                if (result.size() % 5 == 0 && result.size() < 10) {
                    System.out.println("  Requesting 5 more items...");
                    subscription.request(5);
                }
            }
            
            @Override
            public void onError(Throwable t) {
                System.err.println("Error: " + t.getMessage());
            }
            
            @Override
            public void onComplete() {
                System.out.println("Completed! Total items: " + result.size());
            }
        });
        
        Thread.sleep(100); // Wait for async processing
        System.out.println();
    }
    
    static void publisherToObservableExample() {
        System.out.println("--- Example 2: Publisher to Observable ---");
        
        // Create a custom Publisher
        Publisher<String> publisher = subscriber -> {
            subscriber.onSubscribe(new Subscription() {
                private boolean cancelled = false;
                
                @Override
                public void request(long n) {
                    if (!cancelled) {
                        subscriber.onNext("Hello");
                        subscriber.onNext("Reactive");
                        subscriber.onNext("Streams");
                        subscriber.onComplete();
                    }
                }
                
                @Override
                public void cancel() {
                    cancelled = true;
                }
            });
        };
        
        // Convert to Observable and use Observable operators
        Observable<String> observable = Observable.fromPublisher(publisher);
        
        System.out.println("Processing with Observable operators:");
        observable
            .map(String::toUpperCase)
            .subscribe(value -> System.out.println("  " + value));
        
        System.out.println();
    }
    
    static void fluxExample() {
        System.out.println("--- Example 3: Observable to Flux ---");
        
        // Create Observable and convert to Flux
        Observable<Integer> observable = Observable.just(1, 2, 3, 4, 5);
        
        @SuppressWarnings("unchecked")
        Flux<Integer> flux = (Flux<Integer>) observable.toFlux();
        
        System.out.println("Processing with Flux operators:");
        List<Integer> result = flux
            .filter(x -> x % 2 == 0)
            .map(x -> x * 10)
            .collectList()
            .block();
        
        System.out.println("  Result: " + result);
        System.out.println();
    }
    
    static void monoExample() {
        System.out.println("--- Example 4: Observable to Mono ---");
        
        // Create Observable and convert to Mono
        Observable<String> observable = Observable.just("Hello", "World");
        
        @SuppressWarnings("unchecked")
        Mono<String> mono = (Mono<String>) observable.toMono();
        
        System.out.println("Converting to Mono (takes first element):");
        String result = mono.block();
        System.out.println("  Result: " + result);
        System.out.println();
    }
    
    static void fluxToObservableExample() {
        System.out.println("--- Example 5: Flux to Observable ---");
        
        // Create Flux and convert to Observable
        Flux<Integer> flux = Flux.range(1, 10)
            .filter(x -> x % 2 == 0);
        
        Observable<Integer> observable = Observable.fromFlux(flux);
        
        System.out.println("Processing with Observable operators:");
        observable
            .map(x -> "Number: " + x)
            .subscribe(value -> System.out.println("  " + value));
        
        System.out.println();
    }
    
    static void realWorldExample() {
        System.out.println("--- Example 6: Real-World Integration ---");
        System.out.println("Data processing pipeline with mixed operators\n");
        
        // Scenario: Process user data from different sources
        
        // Source 1: User IDs from Observable
        Observable<Integer> userIds = Observable.range(1, 5);
        
        // Convert to Flux for reactive processing
        @SuppressWarnings("unchecked")
        Flux<Integer> userIdFlux = (Flux<Integer>) userIds.toFlux();
        
        // Simulate fetching user details (Flux operation)
        Flux<UserData> userDataFlux = userIdFlux
            .flatMap(id -> Flux.just(new UserData(id, "User" + id, id * 100)));
        
        // Convert back to Observable for business logic
        Observable<UserData> userDataObs = Observable.fromFlux(userDataFlux);
        
        // Apply business rules with Observable
        Observable<Report> reports = userDataObs
            .filter(user -> user.getScore() > 200)
            .map(user -> new Report(user.getId(), user.getName(), 
                                   "High score: " + user.getScore()));
        
        // Convert to Mono for final aggregation
        @SuppressWarnings("unchecked")
        Flux<Report> reportFlux = (Flux<Report>) reports.toFlux();
        List<Report> finalReports = reportFlux.collectList().block();
        
        System.out.println("Generated Reports:");
        finalReports.forEach(report -> 
            System.out.println("  " + report.getUserName() + ": " + report.getMessage()));
        
        System.out.println("\n=== End of Examples ===");
    }
    
    // Helper classes for the real-world example
    static class UserData {
        private final int id;
        private final String name;
        private final int score;
        
        public UserData(int id, String name, int score) {
            this.id = id;
            this.name = name;
            this.score = score;
        }
        
        public int getId() { return id; }
        public String getName() { return name; }
        public int getScore() { return score; }
    }
    
    static class Report {
        private final int userId;
        private final String userName;
        private final String message;
        
        public Report(int userId, String userName, String message) {
            this.userId = userId;
            this.userName = userName;
            this.message = message;
        }
        
        public int getUserId() { return userId; }
        public String getUserName() { return userName; }
        public String getMessage() { return message; }
    }
}
