package com.reactive.hooks;

import com.reactive.core.Observable;
import org.junit.jupiter.api.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for RxJavaHooks covering all hook types
 * and edge cases for maximum code coverage.
 */
@DisplayName("RxJavaHooks Comprehensive Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RxJavaHooksComprehensiveTest {

    @AfterEach
    void cleanup() {
        RxJavaHooks.reset();
    }

    // ==================== ERROR HANDLER ====================
    
    @Nested
    @DisplayName("Error Handler")
    class ErrorHandlerTests {
        
        @Test
        @Order(1)
        @DisplayName("Should set and get error handler")
        void shouldSetAndGetErrorHandler() {
            Consumer<Throwable> handler = e -> {};
            
            RxJavaHooks.setErrorHandler(handler);
            
            assertSame(handler, RxJavaHooks.getErrorHandler());
        }
        
        @Test
        @Order(2)
        @DisplayName("Should invoke error handler with error")
        void shouldInvokeErrorHandlerWithError() {
            List<Throwable> errors = new ArrayList<>();
            RxJavaHooks.setErrorHandler(errors::add);
            
            RuntimeException error1 = new RuntimeException("Error 1");
            RuntimeException error2 = new RuntimeException("Error 2");
            
            RxJavaHooks.onError(error1);
            RxJavaHooks.onError(error2);
            
            assertThat(errors).hasSize(2);
            assertThat(errors.get(0)).isEqualTo(error1);
            assertThat(errors.get(1)).isEqualTo(error2);
        }
        
        @Test
        @Order(3)
        @DisplayName("Should print stack trace when no handler set")
        void shouldPrintStackTraceWhenNoHandlerSet() {
            PrintStream originalErr = System.err;
            ByteArrayOutputStream errContent = new ByteArrayOutputStream();
            System.setErr(new PrintStream(errContent));
            
            try {
                RxJavaHooks.onError(new RuntimeException("Test error"));
                
                String output = errContent.toString();
                assertTrue(output.contains("RuntimeException") || output.contains("Test error"));
            } finally {
                System.setErr(originalErr);
            }
        }
        
        @Test
        @Order(4)
        @DisplayName("Should handle exception in error handler")
        void shouldHandleExceptionInErrorHandler() {
            PrintStream originalErr = System.err;
            ByteArrayOutputStream errContent = new ByteArrayOutputStream();
            System.setErr(new PrintStream(errContent));
            
            try {
                RxJavaHooks.setErrorHandler(e -> {
                    throw new RuntimeException("Handler error");
                });
                
                // Should not throw, but print the handler exception
                assertDoesNotThrow(() -> RxJavaHooks.onError(new RuntimeException("Original")));
            } finally {
                System.setErr(originalErr);
            }
        }
    }
    
    // ==================== SCHEDULE HANDLER ====================
    
    @Nested
    @DisplayName("Schedule Handler")
    class ScheduleHandlerTests {
        
        @Test
        @Order(5)
        @DisplayName("Should set and get schedule handler")
        void shouldSetAndGetScheduleHandler() {
            Function<Runnable, Runnable> handler = r -> r;
            
            RxJavaHooks.setScheduleHandler(handler);
            
            assertSame(handler, RxJavaHooks.getScheduleHandler());
        }
        
        @Test
        @Order(6)
        @DisplayName("Should wrap runnable with schedule handler")
        void shouldWrapRunnableWithScheduleHandler() {
            AtomicInteger wrapCount = new AtomicInteger(0);
            AtomicInteger runCount = new AtomicInteger(0);
            
            RxJavaHooks.setScheduleHandler(runnable -> {
                wrapCount.incrementAndGet();
                return () -> {
                    runCount.incrementAndGet();
                    runnable.run();
                };
            });
            
            Runnable original = () -> {};
            Runnable wrapped = RxJavaHooks.onSchedule(original);
            
            assertEquals(1, wrapCount.get());
            assertEquals(0, runCount.get());
            
            wrapped.run();
            
            assertEquals(1, runCount.get());
        }
        
        @Test
        @Order(7)
        @DisplayName("Should return original runnable when no handler")
        void shouldReturnOriginalRunnableWhenNoHandler() {
            Runnable original = () -> {};
            
            Runnable result = RxJavaHooks.onSchedule(original);
            
            assertSame(original, result);
        }
        
        @Test
        @Order(8)
        @DisplayName("Should track multiple scheduled tasks")
        void shouldTrackMultipleScheduledTasks() {
            List<String> schedule = new ArrayList<>();
            
            RxJavaHooks.setScheduleHandler(runnable -> {
                schedule.add("scheduled");
                return () -> {
                    schedule.add("executing");
                    runnable.run();
                    schedule.add("completed");
                };
            });
            
            Runnable task1 = RxJavaHooks.onSchedule(() -> schedule.add("task1"));
            Runnable task2 = RxJavaHooks.onSchedule(() -> schedule.add("task2"));
            
            task1.run();
            task2.run();
            
            assertThat(schedule).containsExactly(
                "scheduled", "scheduled",
                "executing", "task1", "completed",
                "executing", "task2", "completed"
            );
        }
    }
    
    // ==================== OBSERVABLE ASSEMBLY HOOK ====================
    
    @Nested
    @DisplayName("Observable Assembly Hook")
    class ObservableAssemblyHookTests {
        
        @Test
        @Order(9)
        @DisplayName("Should set and get observable assembly hook")
        void shouldSetAndGetObservableAssemblyHook() {
            Function<Observable, Observable> handler = o -> o;
            
            RxJavaHooks.setOnObservableAssembly(handler);
            
            assertSame(handler, RxJavaHooks.getOnObservableAssembly());
        }
        
        @Test
        @Order(10)
        @DisplayName("Should call assembly hook when applied")
        void shouldCallAssemblyHookWhenApplied() {
            AtomicBoolean hookCalled = new AtomicBoolean(false);
            AtomicReference<Observable> capturedObservable = new AtomicReference<>();
            
            RxJavaHooks.setOnObservableAssembly(observable -> {
                hookCalled.set(true);
                capturedObservable.set(observable);
                return observable;
            });
            
            Observable<Integer> original = Observable.just(1, 2, 3);
            Observable<Integer> result = RxJavaHooks.onAssembly(original);
            
            assertTrue(hookCalled.get());
            assertSame(original, capturedObservable.get());
            assertSame(original, result);
        }
        
        @Test
        @Order(11)
        @DisplayName("Should return original observable when no hook")
        void shouldReturnOriginalObservableWhenNoHook() {
            Observable<Integer> original = Observable.just(1, 2, 3);
            
            Observable<Integer> result = RxJavaHooks.onAssembly(original);
            
            assertSame(original, result);
        }
        
        @Test
        @Order(12)
        @DisplayName("Assembly hook can transform observable")
        void assemblyHookCanTransformObservable() {
            // Hook that adds a map operation
            RxJavaHooks.setOnObservableAssembly(observable -> 
                observable.map(x -> x) // Identity transform, but proves hook works
            );
            
            Observable<Integer> original = Observable.just(1);
            Observable<Integer> result = RxJavaHooks.onAssembly(original);
            
            // Result should be a different observable (the mapped one)
            assertNotSame(original, result);
        }
    }
    
    // ==================== OBSERVABLE SUBSCRIBE HOOK ====================
    
    @Nested
    @DisplayName("Observable Subscribe Hook")
    class ObservableSubscribeHookTests {
        
        @Test
        @Order(13)
        @DisplayName("Should set and get observable subscribe hook")
        void shouldSetAndGetObservableSubscribeHook() {
            BiFunction<Observable, Object, Observable> handler = (o, obs) -> o;
            
            RxJavaHooks.setOnObservableSubscribe(handler);
            
            assertSame(handler, RxJavaHooks.getOnObservableSubscribe());
        }
        
        @Test
        @Order(14)
        @DisplayName("Should call subscribe hook when applied")
        void shouldCallSubscribeHookWhenApplied() {
            AtomicBoolean hookCalled = new AtomicBoolean(false);
            AtomicReference<Observable> capturedObservable = new AtomicReference<>();
            AtomicReference<Object> capturedObserver = new AtomicReference<>();
            
            RxJavaHooks.setOnObservableSubscribe((observable, observer) -> {
                hookCalled.set(true);
                capturedObservable.set(observable);
                capturedObserver.set(observer);
                return observable;
            });
            
            Observable<Integer> original = Observable.just(1, 2, 3);
            Object mockObserver = new Object();
            Observable<Integer> result = RxJavaHooks.onSubscribe(original, mockObserver);
            
            assertTrue(hookCalled.get());
            assertSame(original, capturedObservable.get());
            assertSame(mockObserver, capturedObserver.get());
            assertSame(original, result);
        }
        
        @Test
        @Order(15)
        @DisplayName("Should return original observable when no subscribe hook")
        void shouldReturnOriginalObservableWhenNoSubscribeHook() {
            Observable<Integer> original = Observable.just(1, 2, 3);
            Object mockObserver = new Object();
            
            Observable<Integer> result = RxJavaHooks.onSubscribe(original, mockObserver);
            
            assertSame(original, result);
        }
    }
    
    // ==================== RESET ====================
    
    @Nested
    @DisplayName("Reset")
    class ResetTests {
        
        @Test
        @Order(16)
        @DisplayName("Should reset all hooks to null")
        void shouldResetAllHooksToNull() {
            RxJavaHooks.setErrorHandler(e -> {});
            RxJavaHooks.setScheduleHandler(r -> r);
            RxJavaHooks.setOnObservableAssembly(o -> o);
            RxJavaHooks.setOnObservableSubscribe((o, obs) -> o);
            
            // Verify all are set
            assertNotNull(RxJavaHooks.getErrorHandler());
            assertNotNull(RxJavaHooks.getScheduleHandler());
            assertNotNull(RxJavaHooks.getOnObservableAssembly());
            assertNotNull(RxJavaHooks.getOnObservableSubscribe());
            
            // Reset
            RxJavaHooks.reset();
            
            // Verify all are null
            assertNull(RxJavaHooks.getErrorHandler());
            assertNull(RxJavaHooks.getScheduleHandler());
            assertNull(RxJavaHooks.getOnObservableAssembly());
            assertNull(RxJavaHooks.getOnObservableSubscribe());
        }
    }
    
    // ==================== DEBUG MODE ====================
    
    @Nested
    @DisplayName("Debug Mode")
    class DebugModeTests {
        
        @Test
        @Order(17)
        @DisplayName("Should enable debug mode and set all handlers")
        void shouldEnableDebugModeAndSetAllHandlers() {
            RxJavaHooks.enableDebugMode();
            
            assertNotNull(RxJavaHooks.getErrorHandler());
            assertNotNull(RxJavaHooks.getScheduleHandler());
            assertNotNull(RxJavaHooks.getOnObservableAssembly());
            assertNotNull(RxJavaHooks.getOnObservableSubscribe());
        }
        
        @Test
        @Order(18)
        @DisplayName("Debug mode error handler should print error info")
        void debugModeErrorHandlerShouldPrintErrorInfo() {
            PrintStream originalErr = System.err;
            ByteArrayOutputStream errContent = new ByteArrayOutputStream();
            System.setErr(new PrintStream(errContent));
            
            try {
                RxJavaHooks.enableDebugMode();
                RxJavaHooks.onError(new RuntimeException("Debug test error"));
                
                String output = errContent.toString();
                assertTrue(output.contains("RxJava Error") || output.contains("RuntimeException"));
            } finally {
                System.setErr(originalErr);
            }
        }
        
        @Test
        @Order(19)
        @DisplayName("Debug mode assembly hook should log creation")
        void debugModeAssemblyHookShouldLogCreation() {
            PrintStream originalOut = System.out;
            ByteArrayOutputStream outContent = new ByteArrayOutputStream();
            System.setOut(new PrintStream(outContent));
            
            try {
                RxJavaHooks.enableDebugMode();
                
                Observable<Integer> observable = Observable.just(1, 2, 3);
                RxJavaHooks.onAssembly(observable);
                
                String output = outContent.toString();
                assertTrue(output.contains("RxJava Assembly") || output.contains("Observable"));
            } finally {
                System.setOut(originalOut);
            }
        }
        
        @Test
        @Order(20)
        @DisplayName("Debug mode subscribe hook should log subscription")
        void debugModeSubscribeHookShouldLogSubscription() {
            PrintStream originalOut = System.out;
            ByteArrayOutputStream outContent = new ByteArrayOutputStream();
            System.setOut(new PrintStream(outContent));
            
            try {
                RxJavaHooks.enableDebugMode();
                
                Observable<Integer> observable = Observable.just(1, 2, 3);
                Object mockObserver = new Object();
                RxJavaHooks.onSubscribe(observable, mockObserver);
                
                String output = outContent.toString();
                assertTrue(output.contains("RxJava Subscribe") || output.contains("Observer"));
            } finally {
                System.setOut(originalOut);
            }
        }
        
        @Test
        @Order(21)
        @DisplayName("Debug mode schedule hook should log task")
        void debugModeScheduleHookShouldLogTask() {
            PrintStream originalOut = System.out;
            ByteArrayOutputStream outContent = new ByteArrayOutputStream();
            System.setOut(new PrintStream(outContent));
            
            try {
                RxJavaHooks.enableDebugMode();
                
                Runnable task = () -> {};
                Runnable wrapped = RxJavaHooks.onSchedule(task);
                wrapped.run();
                
                String output = outContent.toString();
                assertTrue(output.contains("RxJava Schedule") || output.contains("thread"));
            } finally {
                System.setOut(originalOut);
            }
        }
    }
    
    // ==================== THREAD SAFETY ====================
    
    @Nested
    @DisplayName("Thread Safety")
    class ThreadSafetyTests {
        
        @Test
        @Order(22)
        @DisplayName("Should handle concurrent hook access safely")
        void shouldHandleConcurrentHookAccessSafely() throws InterruptedException {
            AtomicInteger errorCount = new AtomicInteger(0);
            AtomicInteger successCount = new AtomicInteger(0);
            
            RxJavaHooks.setErrorHandler(e -> errorCount.incrementAndGet());
            
            Thread[] threads = new Thread[10];
            for (int i = 0; i < threads.length; i++) {
                threads[i] = new Thread(() -> {
                    for (int j = 0; j < 100; j++) {
                        RxJavaHooks.onError(new RuntimeException("Concurrent error"));
                        successCount.incrementAndGet();
                    }
                });
            }
            
            for (Thread t : threads) {
                t.start();
            }
            
            for (Thread t : threads) {
                t.join();
            }
            
            assertEquals(1000, successCount.get());
            assertEquals(1000, errorCount.get());
        }
    }
    
    // ==================== INTEGRATION ====================
    
    @Nested
    @DisplayName("Integration")
    class IntegrationTests {
        
        @Test
        @Order(23)
        @DisplayName("Hooks should work together")
        void hooksShouldWorkTogether() {
            List<String> events = new ArrayList<>();
            
            RxJavaHooks.setOnObservableAssembly(observable -> {
                events.add("assembly");
                return observable;
            });
            
            RxJavaHooks.setOnObservableSubscribe((observable, observer) -> {
                events.add("subscribe");
                return observable;
            });
            
            RxJavaHooks.setScheduleHandler(runnable -> {
                events.add("schedule");
                return runnable;
            });
            
            Observable<Integer> observable = Observable.just(1, 2, 3);
            RxJavaHooks.onAssembly(observable);
            RxJavaHooks.onSubscribe(observable, new Object());
            RxJavaHooks.onSchedule(() -> {}).run();
            
            assertThat(events).containsExactly("assembly", "subscribe", "schedule");
        }
    }
}
