package com.reactive.conversions;

import com.reactive.core.Observable;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for Java standard conversions (Phase 2).
 * Tests all blocking and asynchronous conversion methods.
 */
public class JavaConversionsTest {
    
    // ============ toFuture() Tests ============
    
    @Test
    public void testToFuture_WithSingleValue() throws Exception {
        Future<Integer> future = Observable.just(42).toFuture();
        
        Integer result = future.get(1, TimeUnit.SECONDS);
        assertEquals(Integer.valueOf(42), result);
        assertTrue(future.isDone());
    }
    
    @Test
    public void testToFuture_WithMultipleValues_ReturnsFirst() throws Exception {
        Future<Integer> future = Observable.just(1, 2, 3, 4, 5).toFuture();
        
        Integer result = future.get(1, TimeUnit.SECONDS);
        assertEquals(Integer.valueOf(1), result);
    }
    
    @Test
    public void testToFuture_WithEmptyObservable() throws Exception {
        Future<Integer> future = Observable.<Integer>empty().toFuture();
        
        Integer result = future.get(1, TimeUnit.SECONDS);
        assertNull(result);
    }
    
    @Test
    public void testToFuture_WithError() throws Exception {
        RuntimeException error = new RuntimeException("Test error");
        Future<Integer> future = Observable.<Integer>error(error).toFuture();
        
        try {
            future.get(1, TimeUnit.SECONDS);
            fail("Expected ExecutionException");
        } catch (ExecutionException e) {
            assertEquals(error, e.getCause());
        }
    }
    
    // ============ toCompletableFuture() Tests ============
    
    @Test
    public void testToCompletableFuture_WithSingleValue() throws Exception {
        CompletableFuture<Integer> future = Observable.just(42).toCompletableFuture();
        
        Integer result = future.get(1, TimeUnit.SECONDS);
        assertEquals(Integer.valueOf(42), result);
        assertTrue(future.isDone());
    }
    
    @Test
    public void testToCompletableFuture_WithMultipleValues_ReturnsFirst() throws Exception {
        CompletableFuture<Integer> future = Observable.just(1, 2, 3, 4, 5).toCompletableFuture();
        
        Integer result = future.get(1, TimeUnit.SECONDS);
        assertEquals(Integer.valueOf(1), result);
    }
    
    @Test
    public void testToCompletableFuture_ChainWithThenApply() throws Exception {
        CompletableFuture<String> future = Observable.just(42)
            .toCompletableFuture()
            .thenApply(x -> "Value: " + x);
        
        String result = future.get(1, TimeUnit.SECONDS);
        assertEquals("Value: 42", result);
    }
    
    @Test
    public void testToCompletableFuture_WithEmpty() throws Exception {
        CompletableFuture<Integer> future = Observable.<Integer>empty().toCompletableFuture();
        
        Integer result = future.get(1, TimeUnit.SECONDS);
        assertNull(result);
    }
    
    @Test
    public void testToCompletableFuture_WithError() {
        RuntimeException error = new RuntimeException("Test error");
        CompletableFuture<Integer> future = Observable.<Integer>error(error).toCompletableFuture();
        
        try {
            future.get(1, TimeUnit.SECONDS);
            fail("Expected ExecutionException");
        } catch (Exception e) {
            assertTrue(e instanceof ExecutionException);
            assertEquals(error, e.getCause());
        }
    }
    
    @Test
    public void testToCompletableFuture_Exceptionally() throws Exception {
        CompletableFuture<Integer> future = Observable.<Integer>error(new RuntimeException("Error"))
            .toCompletableFuture()
            .exceptionally(ex -> 999);
        
        Integer result = future.get(1, TimeUnit.SECONDS);
        assertEquals(Integer.valueOf(999), result);
    }
    
    // ============ toStream() Tests ============
    
    @Test
    public void testToStream_WithMultipleValues() {
        Stream<Integer> stream = Observable.just(1, 2, 3, 4, 5).toStream();
        
        List<Integer> result = stream.collect(Collectors.toList());
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }
    
    @Test
    public void testToStream_WithFilter() {
        Stream<Integer> stream = Observable.range(1, 10).toStream();
        
        List<Integer> result = stream
            .filter(x -> x % 2 == 0)
            .collect(Collectors.toList());
        
        assertEquals(Arrays.asList(2, 4, 6, 8, 10), result);
    }
    
    @Test
    public void testToStream_WithMap() {
        Stream<Integer> stream = Observable.just(1, 2, 3).toStream();
        
        List<String> result = stream
            .map(x -> "Value: " + x)
            .collect(Collectors.toList());
        
        assertEquals(Arrays.asList("Value: 1", "Value: 2", "Value: 3"), result);
    }
    
    @Test
    public void testToStream_WithEmpty() {
        Stream<Integer> stream = Observable.<Integer>empty().toStream();
        
        List<Integer> result = stream.collect(Collectors.toList());
        assertTrue(result.isEmpty());
    }
    
    @Test
    public void testToStream_Count() {
        Stream<Integer> stream = Observable.range(1, 100).toStream();
        
        long count = stream.count();
        assertEquals(100, count);
    }
    
    @Test
    public void testToStream_Reduce() {
        Stream<Integer> stream = Observable.just(1, 2, 3, 4, 5).toStream();
        
        int sum = stream.reduce(0, Integer::sum);
        assertEquals(15, sum);
    }
    
    @Test
    public void testToStream_WithError() {
        assertThrows(RuntimeException.class, () -> {
            Observable.<Integer>error(new RuntimeException("Test error")).toStream();
        });
    }
    
    // ============ blockingIterable() Tests ============
    
    @Test
    public void testBlockingIterable_WithMultipleValues() {
        Iterable<Integer> iterable = Observable.just(1, 2, 3, 4, 5).blockingIterable();
        
        List<Integer> result = new ArrayList<>();
        for (Integer value : iterable) {
            result.add(value);
        }
        
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }
    
    @Test
    public void testBlockingIterable_WithEmpty() {
        Iterable<Integer> iterable = Observable.<Integer>empty().blockingIterable();
        
        List<Integer> result = new ArrayList<>();
        for (Integer value : iterable) {
            result.add(value);
        }
        
        assertTrue(result.isEmpty());
    }
    
    @Test
    public void testBlockingIterable_MultipleIterations() {
        Iterable<Integer> iterable = Observable.just(1, 2, 3).blockingIterable();
        
        // First iteration
        List<Integer> result1 = new ArrayList<>();
        for (Integer value : iterable) {
            result1.add(value);
        }
        
        // Second iteration (should get new iterator)
        List<Integer> result2 = new ArrayList<>();
        for (Integer value : iterable) {
            result2.add(value);
        }
        
        assertEquals(Arrays.asList(1, 2, 3), result1);
        assertEquals(Arrays.asList(1, 2, 3), result2);
    }
    
    @Test
    public void testBlockingIterable_WithError() {
        assertThrows(RuntimeException.class, () -> {
            Iterable<Integer> iterable = Observable.<Integer>error(new RuntimeException("Test error"))
                .blockingIterable();
            
            for (Integer value : iterable) {
                // Should throw exception
            }
        });
    }
    
    @Test
    public void testBlockingIterable_WithRange() {
        Iterable<Integer> iterable = Observable.range(1, 5).blockingIterable();
        
        List<Integer> result = new ArrayList<>();
        Iterator<Integer> iterator = iterable.iterator();
        
        while (iterator.hasNext()) {
            result.add(iterator.next());
        }
        
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }
    
    @Test
    public void testBlockingIterable_NextAfterCompletion() {
        assertThrows(NoSuchElementException.class, () -> {
            Iterable<Integer> iterable = Observable.just(1).blockingIterable();
            Iterator<Integer> iterator = iterable.iterator();
            
            assertTrue(iterator.hasNext());
            assertEquals(Integer.valueOf(1), iterator.next());
            
            assertFalse(iterator.hasNext());
            iterator.next(); // Should throw NoSuchElementException
        });
    }
    
    // ============ blockingFirst() Tests ============
    
    @Test
    public void testBlockingFirst_WithSingleValue() {
        Integer result = Observable.just(42).blockingFirst();
        assertEquals(Integer.valueOf(42), result);
    }
    
    @Test
    public void testBlockingFirst_WithMultipleValues_ReturnsFirst() {
        Integer result = Observable.just(1, 2, 3, 4, 5).blockingFirst();
        assertEquals(Integer.valueOf(1), result);
    }
    
    @Test
    public void testBlockingFirst_WithRange() {
        Integer result = Observable.range(10, 5).blockingFirst();
        assertEquals(Integer.valueOf(10), result);
    }
    
    @Test
    public void testBlockingFirst_WithEmpty_ThrowsException() {
        assertThrows(NoSuchElementException.class, () -> {
            Observable.<Integer>empty().blockingFirst();
        });
    }
    
    @Test
    public void testBlockingFirst_WithError_ThrowsException() {
        assertThrows(RuntimeException.class, () -> {
            Observable.<Integer>error(new RuntimeException("Test error")).blockingFirst();
        });
    }
    
    // ============ blockingFirst(defaultValue) Tests ============
    
    @Test
    public void testBlockingFirstWithDefault_WithSingleValue() {
        Integer result = Observable.just(42).blockingFirst(999);
        assertEquals(Integer.valueOf(42), result);
    }
    
    @Test
    public void testBlockingFirstWithDefault_WithMultipleValues() {
        Integer result = Observable.just(1, 2, 3).blockingFirst(999);
        assertEquals(Integer.valueOf(1), result);
    }
    
    @Test
    public void testBlockingFirstWithDefault_WithEmpty_ReturnsDefault() {
        Integer result = Observable.<Integer>empty().blockingFirst(999);
        assertEquals(Integer.valueOf(999), result);
    }
    
    @Test
    public void testBlockingFirstWithDefault_WithNullDefault() {
        Integer result = Observable.<Integer>empty().blockingFirst(null);
        assertNull(result);
    }
    
    @Test
    public void testBlockingFirstWithDefault_WithError_ThrowsException() {
        assertThrows(RuntimeException.class, () -> {
            Observable.<Integer>error(new RuntimeException("Test error")).blockingFirst(999);
        });
    }
    
    // ============ blockingLast() Tests ============
    
    @Test
    public void testBlockingLast_WithSingleValue() {
        Integer result = Observable.just(42).blockingLast();
        assertEquals(Integer.valueOf(42), result);
    }
    
    @Test
    public void testBlockingLast_WithMultipleValues_ReturnsLast() {
        Integer result = Observable.just(1, 2, 3, 4, 5).blockingLast();
        assertEquals(Integer.valueOf(5), result);
    }
    
    @Test
    public void testBlockingLast_WithRange() {
        Integer result = Observable.range(10, 5).blockingLast();
        assertEquals(Integer.valueOf(14), result);
    }
    
    @Test
    public void testBlockingLast_WithEmpty_ThrowsException() {
        assertThrows(NoSuchElementException.class, () -> {
            Observable.<Integer>empty().blockingLast();
        });
    }
    
    @Test
    public void testBlockingLast_WithError_ThrowsException() {
        assertThrows(RuntimeException.class, () -> {
            Observable.<Integer>error(new RuntimeException("Test error")).blockingLast();
        });
    }
    
    // ============ blockingLast(defaultValue) Tests ============
    
    @Test
    public void testBlockingLastWithDefault_WithSingleValue() {
        Integer result = Observable.just(42).blockingLast(999);
        assertEquals(Integer.valueOf(42), result);
    }
    
    @Test
    public void testBlockingLastWithDefault_WithMultipleValues() {
        Integer result = Observable.just(1, 2, 3, 4, 5).blockingLast(999);
        assertEquals(Integer.valueOf(5), result);
    }
    
    @Test
    public void testBlockingLastWithDefault_WithEmpty_ReturnsDefault() {
        Integer result = Observable.<Integer>empty().blockingLast(999);
        assertEquals(Integer.valueOf(999), result);
    }
    
    @Test
    public void testBlockingLastWithDefault_WithNullDefault() {
        Integer result = Observable.<Integer>empty().blockingLast(null);
        assertNull(result);
    }
    
    @Test
    public void testBlockingLastWithDefault_WithError_ThrowsException() {
        assertThrows(RuntimeException.class, () -> {
            Observable.<Integer>error(new RuntimeException("Test error")).blockingLast(999);
        });
    }
    
    // ============ Integration Tests ============
    
    @Test
    public void testConversion_ChainToStreamAndBackToObservable() {
        List<Integer> result = Observable.just(1, 2, 3, 4, 5)
            .toStream()
            .filter(x -> x > 2)
            .collect(Collectors.toList());
        
        assertEquals(Arrays.asList(3, 4, 5), result);
    }
    
    @Test
    public void testConversion_CompletableFutureComposition() throws Exception {
        CompletableFuture<Integer> future1 = Observable.just(10).toCompletableFuture();
        CompletableFuture<Integer> future2 = Observable.just(20).toCompletableFuture();
        
        CompletableFuture<Integer> combined = future1.thenCombine(future2, Integer::sum);
        
        Integer result = combined.get(1, TimeUnit.SECONDS);
        assertEquals(Integer.valueOf(30), result);
    }
    
    @Test
    public void testConversion_BlockingIterableWithStreamProcessing() {
        Iterable<Integer> iterable = Observable.range(1, 10).blockingIterable();
        
        List<Integer> result = new ArrayList<>();
        for (Integer value : iterable) {
            if (value % 2 == 0) {
                result.add(value * 2);
            }
        }
        
        assertEquals(Arrays.asList(4, 8, 12, 16, 20), result);
    }
    
    @Test
    public void testConversion_AllMethodsOnSameObservable() throws Exception {
        Observable<Integer> source = Observable.just(1, 2, 3, 4, 5);
        
        // All conversions should work independently
        Integer first = source.blockingFirst();
        Integer last = Observable.just(1, 2, 3, 4, 5).blockingLast();
        Future<Integer> future = Observable.just(1, 2, 3, 4, 5).toFuture();
        Stream<Integer> stream = Observable.just(1, 2, 3, 4, 5).toStream();
        
        assertEquals(Integer.valueOf(1), first);
        assertEquals(Integer.valueOf(5), last);
        assertEquals(Integer.valueOf(1), future.get(1, TimeUnit.SECONDS));
        assertEquals(5, stream.count());
    }
    
    @Test
    public void testConversion_ToStreamWithComplexOperations() {
        Stream<String> stream = Observable.range(1, 10)
            .toStream()
            .filter(x -> x % 2 == 0)
            .map(x -> "Number: " + x);
        
        String result = stream.collect(Collectors.joining(", "));
        assertEquals("Number: 2, Number: 4, Number: 6, Number: 8, Number: 10", result);
    }
    
    @Test
    public void testConversion_BlockingWithTimeout() throws Exception {
        // Simulate delayed observable
        CountDownLatch startLatch = new CountDownLatch(1);
        
        Observable<Integer> delayed = Observable.create(emitter -> {
            new Thread(() -> {
                try {
                    startLatch.await();
                    emitter.onNext(42);
                    emitter.onComplete();
                } catch (InterruptedException e) {
                    emitter.onError(e);
                }
            }).start();
        });
        
        CompletableFuture<Integer> future = delayed.toCompletableFuture();
        
        // Start emission
        startLatch.countDown();
        
        Integer result = future.get(2, TimeUnit.SECONDS);
        assertEquals(Integer.valueOf(42), result);
    }
}
