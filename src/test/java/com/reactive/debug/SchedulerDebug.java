package com.reactive.debug;

import com.reactive.core.Completable;
import com.reactive.schedulers.Schedulers;
import java.util.concurrent.atomic.AtomicReference;

public class SchedulerDebug {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Testing subscribeOn ===");
        AtomicReference<String> threadName1 = new AtomicReference<>();
        
        Completable.fromAction(() -> {
            threadName1.set(Thread.currentThread().getName());
            System.out.println("subscribeOn thread: " + Thread.currentThread().getName());
        })
        .subscribeOn(Schedulers.io())
        .subscribe(
            () -> System.out.println("Completed"),
            error -> System.out.println("Error: " + error)
        );
        
        Thread.sleep(300);
        System.out.println("Thread name captured: " + threadName1.get());
        System.out.println("Contains 'RxIoScheduler': " + (threadName1.get() != null && threadName1.get().contains("RxIoScheduler")));
        
        System.out.println("\n=== Testing observeOn ===");
        AtomicReference<String> threadName2 = new AtomicReference<>();
        
        Completable.complete()
            .observeOn(Schedulers.computation())
            .subscribe(
                () -> {
                    threadName2.set(Thread.currentThread().getName());
                    System.out.println("observeOn thread: " + Thread.currentThread().getName());
                },
                error -> System.out.println("Error: " + error)
            );
        
        Thread.sleep(300);
        System.out.println("Thread name captured: " + threadName2.get());
        System.out.println("Contains 'RxComputationScheduler': " + (threadName2.get() != null && threadName2.get().contains("RxComputationScheduler")));
    }
}
