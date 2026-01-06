package com.reactive.testing;

import com.reactive.core.Disposable;
import com.reactive.core.Observable;
import com.reactive.core.Scheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for TestScheduler covering all functionality
 * and edge cases for maximum code coverage.
 */
@DisplayName("TestScheduler Comprehensive Tests")
public class TestSchedulerComprehensiveTest {

    private TestScheduler scheduler;
    
    @BeforeEach
    void setUp() {
        scheduler = new TestScheduler();
    }
    
    // ==================== VIRTUAL TIME ====================
    
    @Nested
    @DisplayName("Virtual Time Management")
    class VirtualTimeManagement {
        
        @Test
        @DisplayName("Should start at time zero")
        void shouldStartAtTimeZero() {
            assertEquals(0, scheduler.nowMillis());
            assertEquals(0, scheduler.now(TimeUnit.SECONDS));
            assertEquals(0, scheduler.now(TimeUnit.NANOSECONDS));
        }
        
        @Test
        @DisplayName("Should advance time correctly in milliseconds")
        void shouldAdvanceTimeCorrectlyInMilliseconds() {
            scheduler.advanceTimeBy(500, TimeUnit.MILLISECONDS);
            assertEquals(500, scheduler.nowMillis());
            
            scheduler.advanceTimeBy(500, TimeUnit.MILLISECONDS);
            assertEquals(1000, scheduler.nowMillis());
        }
        
        @Test
        @DisplayName("Should advance time correctly in seconds")
        void shouldAdvanceTimeCorrectlyInSeconds() {
            scheduler.advanceTimeBy(2, TimeUnit.SECONDS);
            assertEquals(2000, scheduler.nowMillis());
            assertEquals(2, scheduler.now(TimeUnit.SECONDS));
        }
        
        @Test
        @DisplayName("Should handle various time units")
        void shouldHandleVariousTimeUnits() {
            scheduler.advanceTimeBy(1, TimeUnit.HOURS);
            assertEquals(3600000, scheduler.nowMillis());
            assertEquals(3600, scheduler.now(TimeUnit.SECONDS));
            assertEquals(60, scheduler.now(TimeUnit.MINUTES));
            assertEquals(1, scheduler.now(TimeUnit.HOURS));
        }
        
        @Test
        @DisplayName("Should convert nanoseconds correctly")
        void shouldConvertNanosecondsCorrectly() {
            scheduler.advanceTimeBy(1000000, TimeUnit.NANOSECONDS); // 1 millisecond
            assertEquals(1, scheduler.nowMillis());
        }
    }
    
    // ==================== SCHEDULE DIRECT ====================
    
    @Nested
    @DisplayName("scheduleDirect")
    class ScheduleDirect {
        
        @Test
        @DisplayName("Should execute immediate task on advance")
        void shouldExecuteImmediateTaskOnAdvance() {
            AtomicInteger counter = new AtomicInteger(0);
            
            scheduler.scheduleDirect(() -> counter.incrementAndGet());
            
            assertEquals(0, counter.get()); // Not executed yet
            
            scheduler.advanceTimeBy(1, TimeUnit.NANOSECONDS);
            
            assertEquals(1, counter.get()); // Now executed
        }
        
        @Test
        @DisplayName("Should execute delayed task at correct time")
        void shouldExecuteDelayedTaskAtCorrectTime() {
            List<Long> executionTimes = new ArrayList<>();
            
            scheduler.scheduleDirect(() -> executionTimes.add(scheduler.nowMillis()), 
                100, TimeUnit.MILLISECONDS);
            
            scheduler.advanceTimeBy(50, TimeUnit.MILLISECONDS);
            assertTrue(executionTimes.isEmpty());
            
            scheduler.advanceTimeBy(50, TimeUnit.MILLISECONDS);
            assertEquals(1, executionTimes.size());
            assertEquals(100L, executionTimes.get(0));
        }
        
        @Test
        @DisplayName("Should execute tasks in chronological order")
        void shouldExecuteTasksInChronologicalOrder() {
            List<Integer> order = Collections.synchronizedList(new ArrayList<>());
            
            scheduler.scheduleDirect(() -> order.add(3), 300, TimeUnit.MILLISECONDS);
            scheduler.scheduleDirect(() -> order.add(1), 100, TimeUnit.MILLISECONDS);
            scheduler.scheduleDirect(() -> order.add(2), 200, TimeUnit.MILLISECONDS);
            
            scheduler.advanceTimeBy(500, TimeUnit.MILLISECONDS);
            
            assertThat(order).containsExactly(1, 2, 3);
        }
        
        @Test
        @DisplayName("Should return disposable that cancels task")
        void shouldReturnDisposableThatCancelsTask() {
            AtomicInteger counter = new AtomicInteger(0);
            
            Disposable disposable = scheduler.scheduleDirect(
                () -> counter.incrementAndGet(), 
                100, TimeUnit.MILLISECONDS);
            
            disposable.dispose();
            
            scheduler.advanceTimeBy(200, TimeUnit.MILLISECONDS);
            
            assertEquals(0, counter.get()); // Task was cancelled
        }
    }
    
    // ==================== SCHEDULE PERIODIC ====================
    
    @Nested
    @DisplayName("schedulePeriodic")
    class SchedulePeriodic {
        
        @Test
        @DisplayName("Should execute periodic task repeatedly")
        void shouldExecutePeriodicTaskRepeatedly() {
            AtomicInteger counter = new AtomicInteger(0);
            
            scheduler.schedulePeriodic(
                () -> counter.incrementAndGet(),
                0, 100, TimeUnit.MILLISECONDS);
            
            scheduler.advanceTimeBy(350, TimeUnit.MILLISECONDS);
            
            assertTrue(counter.get() >= 3, "Should execute at least 3 times");
        }
        
        @Test
        @DisplayName("Should respect initial delay")
        void shouldRespectInitialDelay() {
            AtomicInteger counter = new AtomicInteger(0);
            
            scheduler.schedulePeriodic(
                () -> counter.incrementAndGet(),
                200, 100, TimeUnit.MILLISECONDS);
            
            scheduler.advanceTimeBy(150, TimeUnit.MILLISECONDS);
            assertEquals(0, counter.get());
            
            scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS); // Now at 250ms
            assertEquals(1, counter.get()); // First execution at 200ms
        }
        
        @Test
        @DisplayName("Should track execution times correctly")
        void shouldTrackExecutionTimesCorrectly() {
            List<Long> executionTimes = new ArrayList<>();
            
            scheduler.schedulePeriodic(
                () -> executionTimes.add(scheduler.nowMillis()),
                0, 100, TimeUnit.MILLISECONDS);
            
            scheduler.advanceTimeBy(350, TimeUnit.MILLISECONDS);
            
            assertThat(executionTimes).containsExactly(0L, 100L, 200L, 300L);
        }
        
        @Test
        @DisplayName("Should cancel periodic task when disposed")
        void shouldCancelPeriodicTaskWhenDisposed() {
            AtomicInteger counter = new AtomicInteger(0);
            
            Disposable disposable = scheduler.schedulePeriodic(
                () -> counter.incrementAndGet(),
                0, 100, TimeUnit.MILLISECONDS);
            
            scheduler.advanceTimeBy(250, TimeUnit.MILLISECONDS);
            int countBefore = counter.get();
            
            disposable.dispose();
            
            scheduler.advanceTimeBy(500, TimeUnit.MILLISECONDS);
            assertEquals(countBefore, counter.get()); // No more executions
        }
    }
    
    // ==================== TRIGGER ACTIONS ====================
    
    @Nested
    @DisplayName("triggerActions")
    class TriggerActions {
        
        @Test
        @DisplayName("Should execute next pending task")
        void shouldExecuteNextPendingTask() {
            List<Integer> order = new ArrayList<>();
            
            scheduler.scheduleDirect(() -> order.add(1), 100, TimeUnit.MILLISECONDS);
            scheduler.scheduleDirect(() -> order.add(2), 200, TimeUnit.MILLISECONDS);
            
            scheduler.triggerActions(); // Execute first
            assertThat(order).containsExactly(1);
            
            scheduler.triggerActions(); // Execute second
            assertThat(order).containsExactly(1, 2);
        }
        
        @Test
        @DisplayName("Should jump to task time")
        void shouldJumpToTaskTime() {
            scheduler.scheduleDirect(() -> {}, 1000, TimeUnit.MILLISECONDS);
            
            assertEquals(0, scheduler.nowMillis());
            
            scheduler.triggerActions();
            
            assertEquals(1000, scheduler.nowMillis());
        }
        
        @Test
        @DisplayName("Should do nothing when no tasks")
        void shouldDoNothingWhenNoTasks() {
            scheduler.triggerActions();
            assertEquals(0, scheduler.nowMillis());
        }
        
        @Test
        @DisplayName("Should reschedule periodic tasks")
        void shouldReschedulePeriodicTasks() {
            AtomicInteger counter = new AtomicInteger(0);
            
            scheduler.schedulePeriodic(
                () -> counter.incrementAndGet(),
                100, 100, TimeUnit.MILLISECONDS);
            
            scheduler.triggerActions();
            assertEquals(1, counter.get());
            assertEquals(100, scheduler.nowMillis());
            
            scheduler.triggerActions();
            assertEquals(2, counter.get());
            assertEquals(200, scheduler.nowMillis());
        }
    }
    
    // ==================== PENDING TASKS ====================
    
    @Nested
    @DisplayName("Pending Tasks")
    class PendingTasks {
        
        @Test
        @DisplayName("Should track pending task count")
        void shouldTrackPendingTaskCount() {
            assertEquals(0, scheduler.getPendingTaskCount());
            
            scheduler.scheduleDirect(() -> {}, 100, TimeUnit.MILLISECONDS);
            assertEquals(1, scheduler.getPendingTaskCount());
            
            scheduler.scheduleDirect(() -> {}, 200, TimeUnit.MILLISECONDS);
            assertEquals(2, scheduler.getPendingTaskCount());
            
            scheduler.advanceTimeBy(150, TimeUnit.MILLISECONDS);
            assertEquals(1, scheduler.getPendingTaskCount());
            
            scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);
            assertEquals(0, scheduler.getPendingTaskCount());
        }
        
        @Test
        @DisplayName("Should decrement count when task cancelled")
        void shouldDecrementCountWhenTaskCancelled() {
            Disposable d1 = scheduler.scheduleDirect(() -> {}, 100, TimeUnit.MILLISECONDS);
            Disposable d2 = scheduler.scheduleDirect(() -> {}, 200, TimeUnit.MILLISECONDS);
            
            assertEquals(2, scheduler.getPendingTaskCount());
            
            d1.dispose();
            assertEquals(1, scheduler.getPendingTaskCount());
            
            d2.dispose();
            assertEquals(0, scheduler.getPendingTaskCount());
        }
    }
    
    // ==================== WORKER ====================
    
    @Nested
    @DisplayName("Worker")
    class WorkerTests {
        
        @Test
        @DisplayName("Should create worker")
        void shouldCreateWorker() {
            Scheduler.Worker worker = scheduler.createWorker();
            assertNotNull(worker);
            assertFalse(worker.isDisposed());
        }
        
        @Test
        @DisplayName("Worker should schedule immediate tasks")
        void workerShouldScheduleImmediateTasks() {
            Scheduler.Worker worker = scheduler.createWorker();
            AtomicInteger counter = new AtomicInteger(0);
            
            worker.schedule(() -> counter.incrementAndGet());
            
            scheduler.advanceTimeBy(1, TimeUnit.NANOSECONDS);
            
            assertEquals(1, counter.get());
        }
        
        @Test
        @DisplayName("Worker should schedule delayed tasks")
        void workerShouldScheduleDelayedTasks() {
            Scheduler.Worker worker = scheduler.createWorker();
            AtomicInteger counter = new AtomicInteger(0);
            
            worker.schedule(() -> counter.incrementAndGet(), 100, TimeUnit.MILLISECONDS);
            
            scheduler.advanceTimeBy(50, TimeUnit.MILLISECONDS);
            assertEquals(0, counter.get());
            
            scheduler.advanceTimeBy(60, TimeUnit.MILLISECONDS);
            assertEquals(1, counter.get());
        }
        
        @Test
        @DisplayName("Worker should schedule periodic tasks")
        void workerShouldSchedulePeriodicTasks() {
            Scheduler.Worker worker = scheduler.createWorker();
            AtomicInteger counter = new AtomicInteger(0);
            
            worker.schedulePeriodic(() -> counter.incrementAndGet(), 0, 50, TimeUnit.MILLISECONDS);
            
            scheduler.advanceTimeBy(200, TimeUnit.MILLISECONDS);
            
            assertTrue(counter.get() >= 4);
        }
        
        @Test
        @DisplayName("Disposed worker should not schedule tasks")
        void disposedWorkerShouldNotScheduleTasks() {
            Scheduler.Worker worker = scheduler.createWorker();
            AtomicInteger counter = new AtomicInteger(0);
            
            worker.dispose();
            assertTrue(worker.isDisposed());
            
            Disposable d = worker.schedule(() -> counter.incrementAndGet());
            
            scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);
            
            assertEquals(0, counter.get());
        }
        
        @Test
        @DisplayName("Disposed worker schedule should return empty disposable")
        void disposedWorkerScheduleShouldReturnEmptyDisposable() {
            Scheduler.Worker worker = scheduler.createWorker();
            worker.dispose();
            
            Disposable d1 = worker.schedule(() -> {});
            Disposable d2 = worker.schedule(() -> {}, 100, TimeUnit.MILLISECONDS);
            Disposable d3 = worker.schedulePeriodic(() -> {}, 0, 100, TimeUnit.MILLISECONDS);
            
            assertNotNull(d1);
            assertNotNull(d2);
            assertNotNull(d3);
        }
    }
    
    // ==================== SHUTDOWN ====================
    
    @Nested
    @DisplayName("Shutdown")
    class Shutdown {
        
        @Test
        @DisplayName("Shutdown should clear all tasks")
        void shutdownShouldClearAllTasks() {
            scheduler.scheduleDirect(() -> {}, 100, TimeUnit.MILLISECONDS);
            scheduler.scheduleDirect(() -> {}, 200, TimeUnit.MILLISECONDS);
            
            assertEquals(2, scheduler.getPendingTaskCount());
            
            scheduler.shutdown();
            
            assertEquals(0, scheduler.getPendingTaskCount());
        }
        
        @Test
        @DisplayName("Shutdown should prevent task execution")
        void shutdownShouldPreventTaskExecution() {
            AtomicInteger counter = new AtomicInteger(0);
            
            scheduler.scheduleDirect(() -> counter.incrementAndGet(), 100, TimeUnit.MILLISECONDS);
            
            scheduler.shutdown();
            scheduler.advanceTimeBy(200, TimeUnit.MILLISECONDS);
            
            assertEquals(0, counter.get());
        }
        
        @Test
        @DisplayName("Shutdown should prevent triggerActions")
        void shutdownShouldPreventTriggerActions() {
            AtomicInteger counter = new AtomicInteger(0);
            
            scheduler.scheduleDirect(() -> counter.incrementAndGet(), 100, TimeUnit.MILLISECONDS);
            
            scheduler.shutdown();
            scheduler.triggerActions();
            
            assertEquals(0, counter.get());
        }
    }
    
    // ==================== INTEGRATION WITH OBSERVABLE ====================
    
    @Nested
    @DisplayName("Observable Integration")
    class ObservableIntegration {
        
        @Test
        @DisplayName("Should work with interval operator")
        void shouldWorkWithIntervalOperator() {
            TestObserver<Long> observer = new TestObserver<>();
            
            Observable.interval(100, TimeUnit.MILLISECONDS, scheduler)
                .take(5)
                .subscribe(observer);
            
            observer.assertEmpty();
            
            scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);
            observer.assertValueCount(1);
            
            scheduler.advanceTimeBy(400, TimeUnit.MILLISECONDS);
            observer.assertValueCount(5)
                    .assertComplete()
                    .assertValues(0L, 1L, 2L, 3L, 4L);
        }
        
        @Test
        @DisplayName("Should work with timer operator")
        void shouldWorkWithTimerOperator() {
            TestObserver<Long> observer = new TestObserver<>();
            
            Observable.timer(500, TimeUnit.MILLISECONDS, scheduler)
                .subscribe(observer);
            
            scheduler.advanceTimeBy(400, TimeUnit.MILLISECONDS);
            observer.assertEmpty();
            
            scheduler.advanceTimeBy(200, TimeUnit.MILLISECONDS);
            observer.assertValue(0L)
                    .assertComplete();
        }
        
        @Test
        @DisplayName("Should work with delay operator")
        void shouldWorkWithDelayOperator() {
            TestObserver<Integer> observer = new TestObserver<>();
            
            Observable.just(1, 2, 3)
                .delay(200, TimeUnit.MILLISECONDS, scheduler)
                .subscribe(observer);
            
            scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);
            observer.assertEmpty();
            
            scheduler.advanceTimeBy(200, TimeUnit.MILLISECONDS);
            // After delay, values should be emitted (order may vary due to async scheduling)
            observer.assertValueCount(3);
        }
    }
    
    // ==================== EDGE CASES ====================
    
    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {
        
        @Test
        @DisplayName("Should handle zero delay")
        void shouldHandleZeroDelay() {
            AtomicInteger counter = new AtomicInteger(0);
            
            scheduler.scheduleDirect(() -> counter.incrementAndGet(), 0, TimeUnit.MILLISECONDS);
            
            scheduler.advanceTimeBy(1, TimeUnit.NANOSECONDS);
            
            assertEquals(1, counter.get());
        }
        
        @Test
        @DisplayName("Should handle very large time advance")
        void shouldHandleVeryLargeTimeAdvance() {
            AtomicInteger counter = new AtomicInteger(0);
            
            scheduler.scheduleDirect(() -> counter.incrementAndGet(), 1, TimeUnit.HOURS);
            
            scheduler.advanceTimeBy(2, TimeUnit.HOURS);
            
            assertEquals(1, counter.get());
            assertEquals(7200000, scheduler.nowMillis());
        }
        
        @Test
        @DisplayName("Should handle tasks scheduled at same time")
        void shouldHandleTasksScheduledAtSameTime() {
            List<Integer> order = Collections.synchronizedList(new ArrayList<>());
            
            scheduler.scheduleDirect(() -> order.add(1), 100, TimeUnit.MILLISECONDS);
            scheduler.scheduleDirect(() -> order.add(2), 100, TimeUnit.MILLISECONDS);
            scheduler.scheduleDirect(() -> order.add(3), 100, TimeUnit.MILLISECONDS);
            
            scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);
            
            assertEquals(3, order.size());
        }
        
        @Test
        @DisplayName("Should handle task scheduling from within task")
        void shouldHandleTaskSchedulingFromWithinTask() {
            List<Long> times = new ArrayList<>();
            
            scheduler.scheduleDirect(() -> {
                times.add(scheduler.nowMillis());
                scheduler.scheduleDirect(() -> times.add(scheduler.nowMillis()), 
                    100, TimeUnit.MILLISECONDS);
            }, 100, TimeUnit.MILLISECONDS);
            
            scheduler.advanceTimeBy(300, TimeUnit.MILLISECONDS);
            
            assertThat(times).containsExactly(100L, 200L);
        }
    }
}
