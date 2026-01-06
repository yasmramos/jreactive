package com.reactive.testing;

import com.reactive.core.Disposable;
import com.reactive.core.Observable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for TestObserver covering all assertion methods
 * and edge cases to achieve maximum code coverage.
 */
@DisplayName("TestObserver Comprehensive Tests")
public class TestObserverComprehensiveTest {

    // ==================== BASIC FUNCTIONALITY ====================
    
    @Nested
    @DisplayName("Basic Functionality")
    class BasicFunctionality {
        
        @Test
        @DisplayName("Should record all emitted values")
        void shouldRecordAllEmittedValues() {
            TestObserver<Integer> observer = new TestObserver<>();
            
            Observable.just(1, 2, 3, 4, 5)
                .subscribe(observer);
            
            assertThat(observer.values()).containsExactly(1, 2, 3, 4, 5);
            assertEquals(5, observer.values().size());
        }
        
        @Test
        @DisplayName("Should record completion")
        void shouldRecordCompletion() {
            TestObserver<String> observer = new TestObserver<>();
            
            Observable.just("a", "b")
                .subscribe(observer);
            
            assertTrue(observer.isCompleted());
            assertEquals(1, observer.getCompleteCount());
        }
        
        @Test
        @DisplayName("Should record errors")
        void shouldRecordErrors() {
            TestObserver<Integer> observer = new TestObserver<>();
            RuntimeException error = new RuntimeException("Test error");
            
            Observable.<Integer>error(error)
                .subscribe(observer);
            
            assertTrue(observer.hasErrors());
            assertThat(observer.errors()).hasSize(1);
            assertThat(observer.errors().get(0)).isEqualTo(error);
        }
        
        @Test
        @DisplayName("Should track terminated state correctly")
        void shouldTrackTerminatedState() {
            // Test with completion
            TestObserver<Integer> observer1 = new TestObserver<>();
            Observable.just(1).subscribe(observer1);
            assertTrue(observer1.isTerminated());
            
            // Test with error
            TestObserver<Integer> observer2 = new TestObserver<>();
            Observable.<Integer>error(new RuntimeException()).subscribe(observer2);
            assertTrue(observer2.isTerminated());
        }
        
        @Test
        @DisplayName("Should handle onSubscribe with Disposable")
        void shouldHandleOnSubscribe() {
            TestObserver<Integer> observer = new TestObserver<>();
            Disposable disposable = Disposable.empty();
            
            observer.onSubscribe(disposable);
            // onSubscribe should store the upstream
            assertFalse(observer.isDisposed());
        }
    }
    
    // ==================== ASSERTION METHODS ====================
    
    @Nested
    @DisplayName("Assertion Methods")
    class AssertionMethods {
        
        @Test
        @DisplayName("assertNoErrors should pass when no errors")
        void assertNoErrorsShouldPassWhenNoErrors() {
            TestObserver<Integer> observer = new TestObserver<>();
            Observable.just(1, 2, 3).subscribe(observer);
            
            assertDoesNotThrow(() -> observer.assertNoErrors());
        }
        
        @Test
        @DisplayName("assertNoErrors should fail when errors exist")
        void assertNoErrorsShouldFailWhenErrorsExist() {
            TestObserver<Integer> observer = new TestObserver<>();
            Observable.<Integer>error(new RuntimeException("Error")).subscribe(observer);
            
            assertThrows(AssertionError.class, () -> observer.assertNoErrors());
        }
        
        @Test
        @DisplayName("assertError should pass for correct error type")
        void assertErrorShouldPassForCorrectType() {
            TestObserver<Integer> observer = new TestObserver<>();
            Observable.<Integer>error(new IllegalArgumentException()).subscribe(observer);
            
            assertDoesNotThrow(() -> observer.assertError(IllegalArgumentException.class));
        }
        
        @Test
        @DisplayName("assertError should fail for wrong error type")
        void assertErrorShouldFailForWrongType() {
            TestObserver<Integer> observer = new TestObserver<>();
            Observable.<Integer>error(new RuntimeException()).subscribe(observer);
            
            assertThrows(AssertionError.class, 
                () -> observer.assertError(IllegalArgumentException.class));
        }
        
        @Test
        @DisplayName("assertError should fail when no errors")
        void assertErrorShouldFailWhenNoErrors() {
            TestObserver<Integer> observer = new TestObserver<>();
            Observable.just(1).subscribe(observer);
            
            assertThrows(AssertionError.class, 
                () -> observer.assertError(RuntimeException.class));
        }
        
        @Test
        @DisplayName("assertErrorMessage should pass for correct message")
        void assertErrorMessageShouldPassForCorrectMessage() {
            TestObserver<Integer> observer = new TestObserver<>();
            Observable.<Integer>error(new RuntimeException("Expected message")).subscribe(observer);
            
            assertDoesNotThrow(() -> observer.assertErrorMessage("Expected message"));
        }
        
        @Test
        @DisplayName("assertErrorMessage should fail for wrong message")
        void assertErrorMessageShouldFailForWrongMessage() {
            TestObserver<Integer> observer = new TestObserver<>();
            Observable.<Integer>error(new RuntimeException("Actual message")).subscribe(observer);
            
            assertThrows(AssertionError.class, 
                () -> observer.assertErrorMessage("Different message"));
        }
        
        @Test
        @DisplayName("assertErrorMessage should fail when no errors")
        void assertErrorMessageShouldFailWhenNoErrors() {
            TestObserver<Integer> observer = new TestObserver<>();
            Observable.just(1).subscribe(observer);
            
            assertThrows(AssertionError.class, 
                () -> observer.assertErrorMessage("Any message"));
        }
        
        @Test
        @DisplayName("assertComplete should pass when completed")
        void assertCompleteShouldPassWhenCompleted() {
            TestObserver<Integer> observer = new TestObserver<>();
            Observable.just(1).subscribe(observer);
            
            assertDoesNotThrow(() -> observer.assertComplete());
        }
        
        @Test
        @DisplayName("assertComplete should fail when not completed")
        void assertCompleteShouldFailWhenNotCompleted() {
            TestObserver<Integer> observer = new TestObserver<>();
            Observable.<Integer>error(new RuntimeException()).subscribe(observer);
            
            assertThrows(AssertionError.class, () -> observer.assertComplete());
        }
        
        @Test
        @DisplayName("assertNotComplete should pass when not completed")
        void assertNotCompleteShouldPassWhenNotCompleted() {
            TestObserver<Integer> observer = new TestObserver<>();
            Observable.<Integer>error(new RuntimeException()).subscribe(observer);
            
            assertDoesNotThrow(() -> observer.assertNotComplete());
        }
        
        @Test
        @DisplayName("assertNotComplete should fail when completed")
        void assertNotCompleteShouldFailWhenCompleted() {
            TestObserver<Integer> observer = new TestObserver<>();
            Observable.just(1).subscribe(observer);
            
            assertThrows(AssertionError.class, () -> observer.assertNotComplete());
        }
        
        @Test
        @DisplayName("assertValueCount should pass for correct count")
        void assertValueCountShouldPassForCorrectCount() {
            TestObserver<Integer> observer = new TestObserver<>();
            Observable.just(1, 2, 3).subscribe(observer);
            
            assertDoesNotThrow(() -> observer.assertValueCount(3));
        }
        
        @Test
        @DisplayName("assertValueCount should fail for wrong count")
        void assertValueCountShouldFailForWrongCount() {
            TestObserver<Integer> observer = new TestObserver<>();
            Observable.just(1, 2, 3).subscribe(observer);
            
            assertThrows(AssertionError.class, () -> observer.assertValueCount(5));
        }
        
        @Test
        @DisplayName("assertValue should pass for single correct value")
        void assertValueShouldPassForSingleCorrectValue() {
            TestObserver<String> observer = new TestObserver<>();
            Observable.just("expected").subscribe(observer);
            
            assertDoesNotThrow(() -> observer.assertValue("expected"));
        }
        
        @Test
        @DisplayName("assertValue should fail for wrong value")
        void assertValueShouldFailForWrongValue() {
            TestObserver<String> observer = new TestObserver<>();
            Observable.just("actual").subscribe(observer);
            
            assertThrows(AssertionError.class, () -> observer.assertValue("expected"));
        }
        
        @Test
        @DisplayName("assertValue should fail for multiple values")
        void assertValueShouldFailForMultipleValues() {
            TestObserver<Integer> observer = new TestObserver<>();
            Observable.just(1, 2).subscribe(observer);
            
            assertThrows(AssertionError.class, () -> observer.assertValue(1));
        }
        
        @Test
        @DisplayName("assertValues should pass for correct values in order")
        void assertValuesShouldPassForCorrectValuesInOrder() {
            TestObserver<Integer> observer = new TestObserver<>();
            Observable.just(1, 2, 3).subscribe(observer);
            
            assertDoesNotThrow(() -> observer.assertValues(1, 2, 3));
        }
        
        @Test
        @DisplayName("assertValues should fail for wrong order")
        void assertValuesShouldFailForWrongOrder() {
            TestObserver<Integer> observer = new TestObserver<>();
            Observable.just(1, 2, 3).subscribe(observer);
            
            assertThrows(AssertionError.class, () -> observer.assertValues(3, 2, 1));
        }
        
        @Test
        @DisplayName("assertValues should fail for wrong count")
        void assertValuesShouldFailForWrongCount() {
            TestObserver<Integer> observer = new TestObserver<>();
            Observable.just(1, 2).subscribe(observer);
            
            assertThrows(AssertionError.class, () -> observer.assertValues(1, 2, 3));
        }
        
        @Test
        @DisplayName("assertEmpty should pass when no values")
        void assertEmptyShouldPassWhenNoValues() {
            TestObserver<Integer> observer = new TestObserver<>();
            Observable.<Integer>empty().subscribe(observer);
            
            assertDoesNotThrow(() -> observer.assertEmpty());
        }
        
        @Test
        @DisplayName("assertEmpty should fail when values exist")
        void assertEmptyShouldFailWhenValuesExist() {
            TestObserver<Integer> observer = new TestObserver<>();
            Observable.just(1).subscribe(observer);
            
            assertThrows(AssertionError.class, () -> observer.assertEmpty());
        }
        
        @Test
        @DisplayName("assertTerminated should pass when completed")
        void assertTerminatedShouldPassWhenCompleted() {
            TestObserver<Integer> observer = new TestObserver<>();
            Observable.just(1).subscribe(observer);
            
            assertDoesNotThrow(() -> observer.assertTerminated());
        }
        
        @Test
        @DisplayName("assertTerminated should pass when errored")
        void assertTerminatedShouldPassWhenErrored() {
            TestObserver<Integer> observer = new TestObserver<>();
            Observable.<Integer>error(new RuntimeException()).subscribe(observer);
            
            assertDoesNotThrow(() -> observer.assertTerminated());
        }
        
        @Test
        @DisplayName("assertTerminated should fail when still active")
        void assertTerminatedShouldFailWhenStillActive() {
            TestObserver<Integer> observer = new TestObserver<>();
            // No subscription means not terminated
            
            assertThrows(AssertionError.class, () -> observer.assertTerminated());
        }
        
        @Test
        @DisplayName("assertNotTerminated should pass when still active")
        void assertNotTerminatedShouldPassWhenStillActive() {
            TestObserver<Integer> observer = new TestObserver<>();
            // No subscription means not terminated
            
            assertDoesNotThrow(() -> observer.assertNotTerminated());
        }
        
        @Test
        @DisplayName("assertNotTerminated should fail when completed")
        void assertNotTerminatedShouldFailWhenCompleted() {
            TestObserver<Integer> observer = new TestObserver<>();
            Observable.just(1).subscribe(observer);
            
            assertThrows(AssertionError.class, () -> observer.assertNotTerminated());
        }
    }
    
    // ==================== AWAIT METHODS ====================
    
    @Nested
    @DisplayName("Await Methods")
    class AwaitMethods {
        
        @Test
        @Timeout(2)
        @DisplayName("await should block until completion")
        void awaitShouldBlockUntilCompletion() throws InterruptedException {
            TestObserver<Integer> observer = new TestObserver<>();
            
            // Start async emission
            new Thread(() -> {
                try {
                    Thread.sleep(100);
                    observer.onNext(1);
                    observer.onComplete();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
            
            observer.await();
            
            assertTrue(observer.isCompleted());
            observer.assertValueCount(1);
        }
        
        @Test
        @Timeout(2)
        @DisplayName("await with timeout should respect timeout")
        void awaitWithTimeoutShouldRespectTimeout() throws InterruptedException {
            TestObserver<Integer> observer = new TestObserver<>();
            
            // Complete quickly
            new Thread(() -> {
                try {
                    Thread.sleep(50);
                    observer.onComplete();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
            
            observer.await(1, TimeUnit.SECONDS);
            assertTrue(observer.isCompleted());
        }
        
        @Test
        @DisplayName("await with timeout should throw when timeout expires")
        void awaitWithTimeoutShouldThrowWhenTimeoutExpires() {
            TestObserver<Integer> observer = new TestObserver<>();
            // Never completes
            
            assertThrows(AssertionError.class, 
                () -> observer.await(50, TimeUnit.MILLISECONDS));
        }
        
        @Test
        @Timeout(2)
        @DisplayName("awaitTerminalEvent should block until terminal")
        void awaitTerminalEventShouldBlockUntilTerminal() {
            TestObserver<Integer> observer = new TestObserver<>();
            
            new Thread(() -> {
                try {
                    Thread.sleep(50);
                    observer.onError(new RuntimeException());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
            
            observer.awaitTerminalEvent();
            assertTrue(observer.hasErrors());
        }
        
        @Test
        @Timeout(2)
        @DisplayName("awaitTerminalEvent with timeout should work")
        void awaitTerminalEventWithTimeoutShouldWork() {
            TestObserver<Integer> observer = new TestObserver<>();
            
            new Thread(() -> {
                try {
                    Thread.sleep(50);
                    observer.onComplete();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
            
            observer.awaitTerminalEvent(1, TimeUnit.SECONDS);
            assertTrue(observer.isCompleted());
        }
        
        @Test
        @DisplayName("awaitTerminalEvent with timeout should throw on timeout")
        void awaitTerminalEventWithTimeoutShouldThrowOnTimeout() {
            TestObserver<Integer> observer = new TestObserver<>();
            
            assertThrows(AssertionError.class, 
                () -> observer.awaitTerminalEvent(50, TimeUnit.MILLISECONDS));
        }
    }
    
    // ==================== DISPOSAL ====================
    
    @Nested
    @DisplayName("Disposal")
    class Disposal {
        
        @Test
        @DisplayName("dispose should mark as disposed")
        void disposeShouldMarkAsDisposed() {
            TestObserver<Integer> observer = new TestObserver<>();
            
            assertFalse(observer.isDisposed());
            observer.dispose();
            assertTrue(observer.isDisposed());
        }
        
        @Test
        @DisplayName("dispose should prevent further emissions")
        void disposeShouldPreventFurtherEmissions() {
            TestObserver<Integer> observer = new TestObserver<>();
            
            observer.onNext(1);
            observer.dispose();
            observer.onNext(2); // Should be ignored
            observer.onComplete(); // Should be ignored
            
            assertThat(observer.values()).containsExactly(1);
            assertFalse(observer.isCompleted());
        }
        
        @Test
        @DisplayName("dispose should dispose upstream")
        void disposeShouldDisposeUpstream() {
            TestObserver<Integer> observer = new TestObserver<>();
            AtomicBoolean upstreamDisposed = new AtomicBoolean(false);
            
            Disposable upstream = new Disposable() {
                @Override
                public void dispose() {
                    upstreamDisposed.set(true);
                }
                
                @Override
                public boolean isDisposed() {
                    return upstreamDisposed.get();
                }
            };
            
            observer.onSubscribe(upstream);
            observer.dispose();
            
            assertTrue(upstreamDisposed.get());
        }
        
        @Test
        @DisplayName("disposed observer should ignore errors")
        void disposedObserverShouldIgnoreErrors() {
            TestObserver<Integer> observer = new TestObserver<>();
            
            observer.dispose();
            observer.onError(new RuntimeException());
            
            assertFalse(observer.hasErrors());
        }
    }
    
    // ==================== METHOD CHAINING ====================
    
    @Nested
    @DisplayName("Method Chaining")
    class MethodChaining {
        
        @Test
        @DisplayName("All assertion methods should return this for chaining")
        void allAssertionMethodsShouldReturnThis() {
            TestObserver<Integer> observer = new TestObserver<>();
            Observable.just(1, 2, 3).subscribe(observer);
            
            TestObserver<Integer> result = observer
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(3)
                .assertValues(1, 2, 3)
                .assertTerminated();
            
            assertSame(observer, result);
        }
        
        @Test
        @Timeout(2)
        @DisplayName("Await methods should return this for chaining")
        void awaitMethodsShouldReturnThis() throws InterruptedException {
            TestObserver<Integer> observer = new TestObserver<>();
            
            new Thread(() -> {
                try {
                    Thread.sleep(50);
                    observer.onNext(1);
                    observer.onComplete();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
            
            TestObserver<Integer> result = observer
                .await(1, TimeUnit.SECONDS)
                .assertComplete();
            
            assertSame(observer, result);
        }
    }
    
    // ==================== THREAD SAFETY ====================
    
    @Nested
    @DisplayName("Thread Safety")
    class ThreadSafety {
        
        @Test
        @Timeout(5)
        @DisplayName("Should handle concurrent emissions safely")
        void shouldHandleConcurrentEmissionsSafely() throws InterruptedException {
            TestObserver<Integer> observer = new TestObserver<>();
            int threadCount = 10;
            int emissionsPerThread = 100;
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch endLatch = new CountDownLatch(threadCount);
            
            for (int t = 0; t < threadCount; t++) {
                final int threadId = t;
                new Thread(() -> {
                    try {
                        startLatch.await();
                        for (int i = 0; i < emissionsPerThread; i++) {
                            observer.onNext(threadId * 1000 + i);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        endLatch.countDown();
                    }
                }).start();
            }
            
            startLatch.countDown();
            endLatch.await();
            
            assertEquals(threadCount * emissionsPerThread, observer.values().size());
        }
    }
}
