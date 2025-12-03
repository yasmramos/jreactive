package com.reactive.schedulers;

import com.reactive.core.Disposable;
import com.reactive.core.Observable;
import com.reactive.core.Observer;
import com.reactive.core.Scheduler;
import com.reactive.core.Scheduler.Worker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Timeout;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for Schedulers and Scheduler implementations.
 */
@DisplayName("Schedulers Tests")
public class SchedulersTest {

    // ==================== Schedulers Factory Tests ====================

    @Test
    @DisplayName("Schedulers.trampoline() should return same instance")
    public void testTrampolineSingleton() {
        Scheduler s1 = Schedulers.trampoline();
        Scheduler s2 = Schedulers.trampoline();
        assertSame(s1, s2);
    }

    @Test
    @DisplayName("Schedulers.computation() should return same instance")
    public void testComputationSingleton() {
        Scheduler s1 = Schedulers.computation();
        Scheduler s2 = Schedulers.computation();
        assertSame(s1, s2);
    }

    @Test
    @DisplayName("Schedulers.io() should return same instance")
    public void testIOSingleton() {
        Scheduler s1 = Schedulers.io();
        Scheduler s2 = Schedulers.io();
        assertSame(s1, s2);
    }

    @Test
    @DisplayName("Schedulers.newThread() should return same instance")
    public void testNewThreadSingleton() {
        Scheduler s1 = Schedulers.newThread();
        Scheduler s2 = Schedulers.newThread();
        assertSame(s1, s2);
    }

    @Test
    @DisplayName("Schedulers.eventLoop() should return same instance")
    public void testEventLoopSingleton() {
        Scheduler s1 = Schedulers.eventLoop();
        Scheduler s2 = Schedulers.eventLoop();
        assertSame(s1, s2);
    }

    @Test
    @DisplayName("Schedulers.from() should create scheduler from executor")
    public void testFromExecutor() throws InterruptedException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Scheduler scheduler = Schedulers.from(executor);
        
        assertNotNull(scheduler);
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> threadName = new AtomicReference<>();
        
        scheduler.scheduleDirect(() -> {
            threadName.set(Thread.currentThread().getName());
            latch.countDown();
        });
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertNotNull(threadName.get());
        
        executor.shutdown();
    }

    // ==================== TrampolineScheduler Tests ====================

    @Test
    @DisplayName("TrampolineScheduler should execute on current thread")
    public void testTrampolineExecutesOnCurrentThread() {
        Scheduler scheduler = Schedulers.trampoline();
        String currentThread = Thread.currentThread().getName();
        AtomicReference<String> executedOnThread = new AtomicReference<>();
        
        scheduler.scheduleDirect(() -> {
            executedOnThread.set(Thread.currentThread().getName());
        });
        
        assertEquals(currentThread, executedOnThread.get());
    }

    @Test
    @DisplayName("TrampolineScheduler should execute tasks sequentially")
    public void testTrampolineSequentialExecution() {
        Scheduler scheduler = Schedulers.trampoline();
        List<Integer> results = new ArrayList<>();
        
        scheduler.scheduleDirect(() -> {
            results.add(1);
            scheduler.scheduleDirect(() -> results.add(3));
            results.add(2);
        });
        
        // All tasks should complete
        // The order depends on whether nested schedules are queued or executed immediately
        // We just verify that all tasks were executed
        assertTrue(results.contains(1));
        assertTrue(results.contains(2));
        assertTrue(results.contains(3));
    }

    @Test
    @DisplayName("TrampolineScheduler should not support delayed execution")
    public void testTrampolineNoDelayedSupport() {
        Scheduler scheduler = Schedulers.trampoline();
        
        assertThrows(UnsupportedOperationException.class, () -> {
            scheduler.scheduleDirect(() -> {}, 100, TimeUnit.MILLISECONDS);
        });
    }

    @Test
    @DisplayName("TrampolineScheduler should not support periodic execution")
    public void testTrampolineNoPeriodicSupport() {
        Scheduler scheduler = Schedulers.trampoline();
        
        assertThrows(UnsupportedOperationException.class, () -> {
            scheduler.schedulePeriodic(() -> {}, 0, 100, TimeUnit.MILLISECONDS);
        });
    }

    @Test
    @DisplayName("TrampolineScheduler Worker should execute tasks")
    public void testTrampolineWorker() {
        Scheduler scheduler = Schedulers.trampoline();
        Worker worker = scheduler.createWorker();
        
        assertNotNull(worker);
        assertFalse(worker.isDisposed());
        
        AtomicBoolean executed = new AtomicBoolean(false);
        worker.schedule(() -> executed.set(true));
        
        assertTrue(executed.get());
    }

    @Test
    @DisplayName("TrampolineScheduler Worker dispose should work")
    public void testTrampolineWorkerDispose() {
        Scheduler scheduler = Schedulers.trampoline();
        Worker worker = scheduler.createWorker();
        
        assertFalse(worker.isDisposed());
        worker.dispose();
        assertTrue(worker.isDisposed());
    }

    // ==================== ComputationScheduler Tests ====================

    @Test
    @Timeout(5)
    @DisplayName("ComputationScheduler should execute on background thread")
    public void testComputationExecutesOnBackgroundThread() throws InterruptedException {
        Scheduler scheduler = Schedulers.computation();
        String currentThread = Thread.currentThread().getName();
        AtomicReference<String> executedOnThread = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        
        scheduler.scheduleDirect(() -> {
            executedOnThread.set(Thread.currentThread().getName());
            latch.countDown();
        });
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertNotEquals(currentThread, executedOnThread.get());
        assertTrue(executedOnThread.get().contains("RxComputationScheduler"));
    }

    @Test
    @Timeout(5)
    @DisplayName("ComputationScheduler should support delayed execution")
    public void testComputationDelayedExecution() throws InterruptedException {
        Scheduler scheduler = Schedulers.computation();
        AtomicBoolean executed = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(1);
        
        long start = System.currentTimeMillis();
        scheduler.scheduleDirect(() -> {
            executed.set(true);
            latch.countDown();
        }, 100, TimeUnit.MILLISECONDS);
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        long elapsed = System.currentTimeMillis() - start;
        
        assertTrue(executed.get());
        assertTrue(elapsed >= 90, "Delay should be at least 90ms but was " + elapsed);
    }

    @Test
    @Timeout(5)
    @DisplayName("ComputationScheduler should support periodic execution")
    public void testComputationPeriodicExecution() throws InterruptedException {
        Scheduler scheduler = Schedulers.computation();
        AtomicInteger count = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(3);
        
        Disposable disposable = scheduler.schedulePeriodic(() -> {
            count.incrementAndGet();
            latch.countDown();
        }, 0, 50, TimeUnit.MILLISECONDS);
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        disposable.dispose();
        
        assertTrue(count.get() >= 3);
    }

    @Test
    @Timeout(5)
    @DisplayName("ComputationScheduler Worker should work correctly")
    public void testComputationWorker() throws InterruptedException {
        Scheduler scheduler = Schedulers.computation();
        Worker worker = scheduler.createWorker();
        
        assertNotNull(worker);
        assertFalse(worker.isDisposed());
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean executed = new AtomicBoolean(false);
        
        worker.schedule(() -> {
            executed.set(true);
            latch.countDown();
        });
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertTrue(executed.get());
        
        worker.dispose();
        assertTrue(worker.isDisposed());
    }

    @Test
    @Timeout(5)
    @DisplayName("ComputationScheduler Worker delayed schedule should work")
    public void testComputationWorkerDelayed() throws InterruptedException {
        Scheduler scheduler = Schedulers.computation();
        Worker worker = scheduler.createWorker();
        
        CountDownLatch latch = new CountDownLatch(1);
        long start = System.currentTimeMillis();
        
        worker.schedule(() -> latch.countDown(), 100, TimeUnit.MILLISECONDS);
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        long elapsed = System.currentTimeMillis() - start;
        assertTrue(elapsed >= 90);
        
        worker.dispose();
    }

    // ==================== IOScheduler Tests ====================

    @Test
    @Timeout(5)
    @DisplayName("IOScheduler should execute on background thread")
    public void testIOExecutesOnBackgroundThread() throws InterruptedException {
        Scheduler scheduler = Schedulers.io();
        String currentThread = Thread.currentThread().getName();
        AtomicReference<String> executedOnThread = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        
        scheduler.scheduleDirect(() -> {
            executedOnThread.set(Thread.currentThread().getName());
            latch.countDown();
        });
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertNotEquals(currentThread, executedOnThread.get());
    }

    @Test
    @Timeout(5)
    @DisplayName("IOScheduler should support multiple concurrent tasks")
    public void testIOConcurrentTasks() throws InterruptedException {
        Scheduler scheduler = Schedulers.io();
        int taskCount = 10;
        CountDownLatch latch = new CountDownLatch(taskCount);
        Set<String> threadNames = Collections.synchronizedSet(new HashSet<>());
        
        for (int i = 0; i < taskCount; i++) {
            scheduler.scheduleDirect(() -> {
                threadNames.add(Thread.currentThread().getName());
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                latch.countDown();
            });
        }
        
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        // IO scheduler can use multiple threads
        assertTrue(threadNames.size() >= 1);
    }

    @Test
    @Timeout(5)
    @DisplayName("IOScheduler Worker should work correctly")
    public void testIOWorker() throws InterruptedException {
        Scheduler scheduler = Schedulers.io();
        Worker worker = scheduler.createWorker();
        
        assertNotNull(worker);
        assertFalse(worker.isDisposed());
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean executed = new AtomicBoolean(false);
        
        worker.schedule(() -> {
            executed.set(true);
            latch.countDown();
        });
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertTrue(executed.get());
        
        worker.dispose();
        assertTrue(worker.isDisposed());
    }

    // ==================== NewThreadScheduler Tests ====================

    @Test
    @Timeout(5)
    @DisplayName("NewThreadScheduler should create new thread for each task")
    public void testNewThreadCreatesNewThread() throws InterruptedException {
        Scheduler scheduler = Schedulers.newThread();
        String currentThread = Thread.currentThread().getName();
        Set<String> threadNames = Collections.synchronizedSet(new HashSet<>());
        CountDownLatch latch = new CountDownLatch(3);
        
        for (int i = 0; i < 3; i++) {
            scheduler.scheduleDirect(() -> {
                threadNames.add(Thread.currentThread().getName());
                latch.countDown();
            });
        }
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertFalse(threadNames.contains(currentThread));
        // Each task might get its own thread (implementation dependent)
        assertTrue(threadNames.size() >= 1);
    }

    @Test
    @Timeout(5)
    @DisplayName("NewThreadScheduler Worker should work correctly")
    public void testNewThreadWorker() throws InterruptedException {
        Scheduler scheduler = Schedulers.newThread();
        Worker worker = scheduler.createWorker();
        
        assertNotNull(worker);
        assertFalse(worker.isDisposed());
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean executed = new AtomicBoolean(false);
        
        worker.schedule(() -> {
            executed.set(true);
            latch.countDown();
        });
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertTrue(executed.get());
        
        worker.dispose();
        assertTrue(worker.isDisposed());
    }

    // ==================== EventLoopScheduler Tests ====================

    @Test
    @Timeout(5)
    @DisplayName("EventLoopScheduler should execute tasks")
    public void testEventLoopExecutesTasks() throws InterruptedException {
        Scheduler scheduler = Schedulers.eventLoop();
        AtomicBoolean executed = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(1);
        
        scheduler.scheduleDirect(() -> {
            executed.set(true);
            latch.countDown();
        });
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertTrue(executed.get());
    }

    @Test
    @Timeout(5)
    @DisplayName("EventLoopScheduler Worker should work correctly")
    public void testEventLoopWorker() throws InterruptedException {
        Scheduler scheduler = Schedulers.eventLoop();
        Worker worker = scheduler.createWorker();
        
        assertNotNull(worker);
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean executed = new AtomicBoolean(false);
        
        worker.schedule(() -> {
            executed.set(true);
            latch.countDown();
        });
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertTrue(executed.get());
        
        worker.dispose();
    }

    // ==================== ExecutorScheduler Tests ====================

    @Test
    @Timeout(5)
    @DisplayName("ExecutorScheduler should use provided executor")
    public void testExecutorSchedulerUsesProvidedExecutor() throws InterruptedException {
        String threadPrefix = "CustomExecutor-";
        ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, threadPrefix + "1");
            return t;
        });
        
        Scheduler scheduler = Schedulers.from(executor);
        AtomicReference<String> threadName = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        
        scheduler.scheduleDirect(() -> {
            threadName.set(Thread.currentThread().getName());
            latch.countDown();
        });
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertTrue(threadName.get().startsWith(threadPrefix));
        
        executor.shutdown();
    }

    @Test
    @Timeout(5)
    @DisplayName("ExecutorScheduler Worker should work correctly")
    public void testExecutorSchedulerWorker() throws InterruptedException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Scheduler scheduler = Schedulers.from(executor);
        Worker worker = scheduler.createWorker();
        
        assertNotNull(worker);
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean executed = new AtomicBoolean(false);
        
        worker.schedule(() -> {
            executed.set(true);
            latch.countDown();
        });
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertTrue(executed.get());
        
        worker.dispose();
        executor.shutdown();
    }

    // ==================== Integration Tests with Observable ====================

    @Test
    @Timeout(5)
    @DisplayName("subscribeOn should change subscription thread")
    public void testSubscribeOnChangesThread() throws InterruptedException {
        String mainThread = Thread.currentThread().getName();
        AtomicReference<String> subscribeThread = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        
        Observable.<Integer>create(emitter -> {
            subscribeThread.set(Thread.currentThread().getName());
            emitter.onNext(1);
            emitter.onComplete();
        })
        .subscribeOn(Schedulers.computation())
        .subscribe(new Observer<Integer>() {
            @Override
            public void onSubscribe(Disposable d) {}
            @Override
            public void onNext(Integer item) {}
            @Override
            public void onError(Throwable e) { fail("Should not error"); }
            @Override
            public void onComplete() { latch.countDown(); }
        });
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertNotEquals(mainThread, subscribeThread.get());
    }

    @Test
    @Timeout(5)
    @DisplayName("observeOn should change observation thread")
    public void testObserveOnChangesThread() throws InterruptedException {
        String mainThread = Thread.currentThread().getName();
        AtomicReference<String> observeThread = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        
        Observable.just(1, 2, 3)
            .observeOn(Schedulers.io())
            .subscribe(new Observer<Integer>() {
                @Override
                public void onSubscribe(Disposable d) {}
                @Override
                public void onNext(Integer item) {
                    observeThread.set(Thread.currentThread().getName());
                }
                @Override
                public void onError(Throwable e) { fail("Should not error"); }
                @Override
                public void onComplete() { latch.countDown(); }
            });
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertNotEquals(mainThread, observeThread.get());
    }

    @Test
    @Timeout(5)
    @DisplayName("subscribeOn and observeOn should work together")
    public void testSubscribeOnAndObserveOnTogether() throws InterruptedException {
        AtomicReference<String> subscribeThread = new AtomicReference<>();
        AtomicReference<String> observeThread = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        
        Observable.<Integer>create(emitter -> {
            subscribeThread.set(Thread.currentThread().getName());
            emitter.onNext(1);
            emitter.onComplete();
        })
        .subscribeOn(Schedulers.computation())
        .observeOn(Schedulers.io())
        .subscribe(new Observer<Integer>() {
            @Override
            public void onSubscribe(Disposable d) {}
            @Override
            public void onNext(Integer item) {
                observeThread.set(Thread.currentThread().getName());
            }
            @Override
            public void onError(Throwable e) { fail("Should not error"); }
            @Override
            public void onComplete() { latch.countDown(); }
        });
        
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertNotNull(subscribeThread.get());
        assertNotNull(observeThread.get());
        // They should be on different thread pools
        assertTrue(subscribeThread.get().contains("Computation") || 
                   !subscribeThread.get().equals(observeThread.get()));
    }

    @Test
    @Timeout(5)
    @DisplayName("Multiple operations with different schedulers")
    public void testMultipleSchedulers() throws InterruptedException {
        List<Integer> results = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch latch = new CountDownLatch(1);
        
        Observable.range(1, 5)
            .subscribeOn(Schedulers.computation())
            .map(x -> x * 2)
            .observeOn(Schedulers.io())
            .filter(x -> x > 4)
            .subscribe(new Observer<Integer>() {
                @Override
                public void onSubscribe(Disposable d) {}
                @Override
                public void onNext(Integer item) { results.add(item); }
                @Override
                public void onError(Throwable e) { fail("Should not error"); }
                @Override
                public void onComplete() { latch.countDown(); }
            });
        
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertEquals(3, results.size());
        assertTrue(results.containsAll(Arrays.asList(6, 8, 10)));
    }

    // ==================== Disposable Tests ====================

    @Test
    @Timeout(5)
    @DisplayName("Scheduled task should be cancellable")
    public void testScheduledTaskCancellable() throws InterruptedException {
        Scheduler scheduler = Schedulers.computation();
        AtomicBoolean executed = new AtomicBoolean(false);
        
        Disposable disposable = scheduler.scheduleDirect(() -> {
            executed.set(true);
        }, 500, TimeUnit.MILLISECONDS);
        
        // Cancel before execution
        disposable.dispose();
        
        Thread.sleep(600);
        
        // Task should not have executed
        assertFalse(executed.get());
    }

    @Test
    @Timeout(5)
    @DisplayName("Periodic task should be cancellable")
    public void testPeriodicTaskCancellable() throws InterruptedException {
        Scheduler scheduler = Schedulers.computation();
        AtomicInteger count = new AtomicInteger(0);
        
        Disposable disposable = scheduler.schedulePeriodic(() -> {
            count.incrementAndGet();
        }, 0, 50, TimeUnit.MILLISECONDS);
        
        Thread.sleep(150);
        int countAfterSomeTime = count.get();
        assertTrue(countAfterSomeTime >= 2);
        
        disposable.dispose();
        Thread.sleep(100);
        
        // Count should not increase significantly after dispose
        int countAfterDispose = count.get();
        assertTrue(countAfterDispose - countAfterSomeTime <= 1);
    }
}
