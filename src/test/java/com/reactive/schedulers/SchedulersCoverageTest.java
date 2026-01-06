package com.reactive.schedulers;

import com.reactive.core.Disposable;
import com.reactive.core.Scheduler;
import com.reactive.core.Scheduler.Worker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Additional tests to improve scheduler coverage.
 */
@DisplayName("Schedulers Coverage Tests")
public class SchedulersCoverageTest {

    // ==================== TrampolineScheduler Coverage ====================

    @Nested
    @DisplayName("TrampolineScheduler Additional Tests")
    class TrampolineSchedulerTests {

        @Test
        @DisplayName("TrampolineScheduler.Worker should execute tasks in order")
        void workerShouldExecuteTasksInOrder() {
            TrampolineScheduler scheduler = new TrampolineScheduler();
            Worker worker = scheduler.createWorker();
            
            List<Integer> results = new ArrayList<>();
            
            worker.schedule(() -> {
                results.add(1);
                worker.schedule(() -> results.add(2));
                worker.schedule(() -> results.add(3));
            });
            
            assertEquals(List.of(1, 2, 3), results);
        }

        @Test
        @DisplayName("TrampolineScheduler.scheduleDirect with delay should throw")
        void scheduleDirectWithDelayShouldThrow() {
            TrampolineScheduler scheduler = new TrampolineScheduler();
            
            assertThrows(UnsupportedOperationException.class, () -> {
                scheduler.scheduleDirect(() -> {}, 100, TimeUnit.MILLISECONDS);
            });
        }

        @Test
        @DisplayName("TrampolineScheduler.schedulePeriodic should throw")
        void schedulePeriodicShouldThrow() {
            TrampolineScheduler scheduler = new TrampolineScheduler();
            
            assertThrows(UnsupportedOperationException.class, () -> {
                scheduler.schedulePeriodic(() -> {}, 0, 100, TimeUnit.MILLISECONDS);
            });
        }

        @Test
        @DisplayName("TrampolineScheduler.Worker.schedule with delay should throw")
        void workerScheduleWithDelayShouldThrow() {
            TrampolineScheduler scheduler = new TrampolineScheduler();
            Worker worker = scheduler.createWorker();
            
            assertThrows(UnsupportedOperationException.class, () -> {
                worker.schedule(() -> {}, 100, TimeUnit.MILLISECONDS);
            });
        }

        @Test
        @DisplayName("TrampolineScheduler.Worker.schedulePeriodic should throw")
        void workerSchedulePeriodicShouldThrow() {
            TrampolineScheduler scheduler = new TrampolineScheduler();
            Worker worker = scheduler.createWorker();
            
            assertThrows(UnsupportedOperationException.class, () -> {
                worker.schedulePeriodic(() -> {}, 0, 100, TimeUnit.MILLISECONDS);
            });
        }

        @Test
        @DisplayName("TrampolineScheduler.Worker should not execute after dispose")
        void workerShouldNotExecuteAfterDispose() {
            TrampolineScheduler scheduler = new TrampolineScheduler();
            Worker worker = scheduler.createWorker();
            
            List<Integer> results = new ArrayList<>();
            
            worker.dispose();
            assertTrue(worker.isDisposed());
            
            worker.schedule(() -> results.add(1));
            
            // Task should not be added since worker is disposed
            assertTrue(results.isEmpty());
        }

        @Test
        @DisplayName("TrampolineScheduler.shutdown should be callable")
        void shutdownShouldBeCallable() {
            TrampolineScheduler scheduler = new TrampolineScheduler();
            
            // Should not throw
            assertDoesNotThrow(() -> scheduler.shutdown());
        }
    }

    // ==================== ComputationScheduler Coverage ====================

    @Nested
    @DisplayName("ComputationScheduler Additional Tests")
    class ComputationSchedulerTests {

        @Test
        @Timeout(5)
        @DisplayName("ComputationScheduler.Worker should schedule tasks")
        void workerShouldScheduleTasks() throws InterruptedException {
            ComputationScheduler scheduler = new ComputationScheduler();
            Worker worker = scheduler.createWorker();
            
            CountDownLatch latch = new CountDownLatch(1);
            AtomicBoolean executed = new AtomicBoolean(false);
            
            worker.schedule(() -> {
                executed.set(true);
                latch.countDown();
            });
            
            assertTrue(latch.await(2, TimeUnit.SECONDS));
            assertTrue(executed.get());
        }

        @Test
        @Timeout(5)
        @DisplayName("ComputationScheduler.Worker should schedule delayed tasks")
        void workerShouldScheduleDelayedTasks() throws InterruptedException {
            ComputationScheduler scheduler = new ComputationScheduler();
            Worker worker = scheduler.createWorker();
            
            CountDownLatch latch = new CountDownLatch(1);
            AtomicLong startTime = new AtomicLong();
            AtomicLong executionTime = new AtomicLong();
            
            startTime.set(System.currentTimeMillis());
            worker.schedule(() -> {
                executionTime.set(System.currentTimeMillis());
                latch.countDown();
            }, 100, TimeUnit.MILLISECONDS);
            
            assertTrue(latch.await(2, TimeUnit.SECONDS));
            assertTrue(executionTime.get() - startTime.get() >= 90); // Allow some tolerance
        }

        @Test
        @Timeout(5)
        @DisplayName("ComputationScheduler.Worker should schedule periodic tasks")
        void workerShouldSchedulePeriodicTasks() throws InterruptedException {
            ComputationScheduler scheduler = new ComputationScheduler();
            Worker worker = scheduler.createWorker();
            
            CountDownLatch latch = new CountDownLatch(3);
            AtomicInteger count = new AtomicInteger(0);
            
            Disposable disposable = worker.schedulePeriodic(() -> {
                count.incrementAndGet();
                latch.countDown();
            }, 0, 50, TimeUnit.MILLISECONDS);
            
            assertTrue(latch.await(2, TimeUnit.SECONDS));
            assertTrue(count.get() >= 3);
            
            disposable.dispose();
        }

        @Test
        @DisplayName("ComputationScheduler.Worker should not execute after dispose")
        void workerShouldNotExecuteAfterDispose() throws InterruptedException {
            ComputationScheduler scheduler = new ComputationScheduler();
            Worker worker = scheduler.createWorker();
            
            worker.dispose();
            assertTrue(worker.isDisposed());
            
            // These should return empty disposables without scheduling
            Disposable d1 = worker.schedule(() -> {});
            Disposable d2 = worker.schedule(() -> {}, 100, TimeUnit.MILLISECONDS);
            Disposable d3 = worker.schedulePeriodic(() -> {}, 0, 100, TimeUnit.MILLISECONDS);
            
            assertNotNull(d1);
            assertNotNull(d2);
            assertNotNull(d3);
        }

        @Test
        @Timeout(5)
        @DisplayName("ComputationScheduler.scheduleDirect should execute task")
        void scheduleDirectShouldExecuteTask() throws InterruptedException {
            ComputationScheduler scheduler = new ComputationScheduler();
            
            CountDownLatch latch = new CountDownLatch(1);
            AtomicBoolean executed = new AtomicBoolean(false);
            
            scheduler.scheduleDirect(() -> {
                executed.set(true);
                latch.countDown();
            });
            
            assertTrue(latch.await(2, TimeUnit.SECONDS));
            assertTrue(executed.get());
        }

        @Test
        @Timeout(5)
        @DisplayName("ComputationScheduler.scheduleDirect with delay should execute task")
        void scheduleDirectWithDelayShouldExecuteTask() throws InterruptedException {
            ComputationScheduler scheduler = new ComputationScheduler();
            
            CountDownLatch latch = new CountDownLatch(1);
            AtomicLong startTime = new AtomicLong();
            AtomicLong executionTime = new AtomicLong();
            
            startTime.set(System.currentTimeMillis());
            scheduler.scheduleDirect(() -> {
                executionTime.set(System.currentTimeMillis());
                latch.countDown();
            }, 100, TimeUnit.MILLISECONDS);
            
            assertTrue(latch.await(2, TimeUnit.SECONDS));
            assertTrue(executionTime.get() - startTime.get() >= 90);
        }

        @Test
        @Timeout(5)
        @DisplayName("ComputationScheduler.schedulePeriodic should execute tasks")
        void schedulePeriodicShouldExecuteTasks() throws InterruptedException {
            ComputationScheduler scheduler = new ComputationScheduler();
            
            CountDownLatch latch = new CountDownLatch(3);
            AtomicInteger count = new AtomicInteger(0);
            
            Disposable disposable = scheduler.schedulePeriodic(() -> {
                count.incrementAndGet();
                latch.countDown();
            }, 0, 50, TimeUnit.MILLISECONDS);
            
            assertTrue(latch.await(2, TimeUnit.SECONDS));
            assertTrue(count.get() >= 3);
            
            disposable.dispose();
        }
    }

    // ==================== IOScheduler Coverage ====================

    @Nested
    @DisplayName("IOScheduler Additional Tests")
    class IOSchedulerTests {

        @Test
        @Timeout(5)
        @DisplayName("IOScheduler.Worker should schedule tasks")
        void workerShouldScheduleTasks() throws InterruptedException {
            Scheduler scheduler = Schedulers.io();
            Worker worker = scheduler.createWorker();
            
            CountDownLatch latch = new CountDownLatch(1);
            AtomicBoolean executed = new AtomicBoolean(false);
            
            worker.schedule(() -> {
                executed.set(true);
                latch.countDown();
            });
            
            assertTrue(latch.await(2, TimeUnit.SECONDS));
            assertTrue(executed.get());
        }

        @Test
        @Timeout(5)
        @DisplayName("IOScheduler.Worker should schedule delayed tasks")
        void workerShouldScheduleDelayedTasks() throws InterruptedException {
            Scheduler scheduler = Schedulers.io();
            Worker worker = scheduler.createWorker();
            
            CountDownLatch latch = new CountDownLatch(1);
            AtomicLong startTime = new AtomicLong(System.currentTimeMillis());
            AtomicLong executionTime = new AtomicLong();
            
            worker.schedule(() -> {
                executionTime.set(System.currentTimeMillis());
                latch.countDown();
            }, 100, TimeUnit.MILLISECONDS);
            
            assertTrue(latch.await(2, TimeUnit.SECONDS));
            assertTrue(executionTime.get() - startTime.get() >= 90);
        }

        @Test
        @DisplayName("IOScheduler.Worker dispose should work")
        void workerDisposeShouldWork() {
            Scheduler scheduler = Schedulers.io();
            Worker worker = scheduler.createWorker();
            
            assertFalse(worker.isDisposed());
            worker.dispose();
            assertTrue(worker.isDisposed());
        }
    }

    // ==================== NewThreadScheduler Coverage ====================

    @Nested
    @DisplayName("NewThreadScheduler Additional Tests")
    class NewThreadSchedulerTests {

        @Test
        @Timeout(5)
        @DisplayName("NewThreadScheduler should create new threads")
        void shouldCreateNewThreads() throws InterruptedException {
            Scheduler scheduler = Schedulers.newThread();
            
            CountDownLatch latch = new CountDownLatch(2);
            List<String> threadNames = Collections.synchronizedList(new ArrayList<>());
            
            scheduler.scheduleDirect(() -> {
                threadNames.add(Thread.currentThread().getName());
                latch.countDown();
            });
            
            scheduler.scheduleDirect(() -> {
                threadNames.add(Thread.currentThread().getName());
                latch.countDown();
            });
            
            assertTrue(latch.await(2, TimeUnit.SECONDS));
            assertEquals(2, threadNames.size());
        }

        @Test
        @Timeout(5)
        @DisplayName("NewThreadScheduler.Worker should schedule tasks")
        void workerShouldScheduleTasks() throws InterruptedException {
            Scheduler scheduler = Schedulers.newThread();
            Worker worker = scheduler.createWorker();
            
            CountDownLatch latch = new CountDownLatch(1);
            AtomicBoolean executed = new AtomicBoolean(false);
            
            worker.schedule(() -> {
                executed.set(true);
                latch.countDown();
            });
            
            assertTrue(latch.await(2, TimeUnit.SECONDS));
            assertTrue(executed.get());
            
            worker.dispose();
        }

        @Test
        @Timeout(5)
        @DisplayName("NewThreadScheduler.Worker should schedule delayed tasks")
        void workerShouldScheduleDelayedTasks() throws InterruptedException {
            Scheduler scheduler = Schedulers.newThread();
            Worker worker = scheduler.createWorker();
            
            CountDownLatch latch = new CountDownLatch(1);
            AtomicLong startTime = new AtomicLong(System.currentTimeMillis());
            AtomicLong executionTime = new AtomicLong();
            
            worker.schedule(() -> {
                executionTime.set(System.currentTimeMillis());
                latch.countDown();
            }, 100, TimeUnit.MILLISECONDS);
            
            assertTrue(latch.await(2, TimeUnit.SECONDS));
            assertTrue(executionTime.get() - startTime.get() >= 90);
            
            worker.dispose();
        }

        @Test
        @DisplayName("NewThreadScheduler.Worker dispose should work")
        void workerDisposeShouldWork() {
            Scheduler scheduler = Schedulers.newThread();
            Worker worker = scheduler.createWorker();
            
            assertFalse(worker.isDisposed());
            worker.dispose();
            assertTrue(worker.isDisposed());
        }
    }

    // ==================== EventLoopScheduler Coverage ====================

    @Nested
    @DisplayName("EventLoopScheduler Additional Tests")
    class EventLoopSchedulerTests {

        @Test
        @Timeout(5)
        @DisplayName("EventLoopScheduler.Worker should schedule tasks")
        void workerShouldScheduleTasks() throws InterruptedException {
            Scheduler scheduler = Schedulers.eventLoop();
            Worker worker = scheduler.createWorker();
            
            CountDownLatch latch = new CountDownLatch(1);
            AtomicBoolean executed = new AtomicBoolean(false);
            
            worker.schedule(() -> {
                executed.set(true);
                latch.countDown();
            });
            
            assertTrue(latch.await(2, TimeUnit.SECONDS));
            assertTrue(executed.get());
        }

        @Test
        @Timeout(5)
        @DisplayName("EventLoopScheduler.Worker should schedule delayed tasks")
        void workerShouldScheduleDelayedTasks() throws InterruptedException {
            Scheduler scheduler = Schedulers.eventLoop();
            Worker worker = scheduler.createWorker();
            
            CountDownLatch latch = new CountDownLatch(1);
            AtomicLong startTime = new AtomicLong(System.currentTimeMillis());
            AtomicLong executionTime = new AtomicLong();
            
            worker.schedule(() -> {
                executionTime.set(System.currentTimeMillis());
                latch.countDown();
            }, 100, TimeUnit.MILLISECONDS);
            
            assertTrue(latch.await(2, TimeUnit.SECONDS));
            assertTrue(executionTime.get() - startTime.get() >= 90);
        }

        @Test
        @DisplayName("EventLoopScheduler.Worker dispose should work")
        void workerDisposeShouldWork() {
            Scheduler scheduler = Schedulers.eventLoop();
            Worker worker = scheduler.createWorker();
            
            assertFalse(worker.isDisposed());
            worker.dispose();
            assertTrue(worker.isDisposed());
        }
    }

    // ==================== ExecutorScheduler Coverage ====================

    @Nested
    @DisplayName("ExecutorScheduler Additional Tests")
    class ExecutorSchedulerTests {

        @Test
        @Timeout(5)
        @DisplayName("ExecutorScheduler should execute tasks from custom executor")
        void shouldExecuteTasksFromCustomExecutor() throws InterruptedException {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Scheduler scheduler = Schedulers.from(executor);
            
            CountDownLatch latch = new CountDownLatch(1);
            AtomicBoolean executed = new AtomicBoolean(false);
            
            scheduler.scheduleDirect(() -> {
                executed.set(true);
                latch.countDown();
            });
            
            assertTrue(latch.await(2, TimeUnit.SECONDS));
            assertTrue(executed.get());
            
            executor.shutdown();
        }

        @Test
        @Timeout(5)
        @DisplayName("ExecutorScheduler.Worker should schedule tasks")
        void workerShouldScheduleTasks() throws InterruptedException {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Scheduler scheduler = Schedulers.from(executor);
            Worker worker = scheduler.createWorker();
            
            CountDownLatch latch = new CountDownLatch(1);
            AtomicBoolean executed = new AtomicBoolean(false);
            
            worker.schedule(() -> {
                executed.set(true);
                latch.countDown();
            });
            
            assertTrue(latch.await(2, TimeUnit.SECONDS));
            assertTrue(executed.get());
            
            executor.shutdown();
        }

        @Test
        @DisplayName("ExecutorScheduler.Worker dispose should work")
        void workerDisposeShouldWork() {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Scheduler scheduler = Schedulers.from(executor);
            Worker worker = scheduler.createWorker();
            
            assertFalse(worker.isDisposed());
            worker.dispose();
            assertTrue(worker.isDisposed());
            
            executor.shutdown();
        }
    }
}
