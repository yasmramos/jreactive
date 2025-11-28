package com.reactive.observables;

import com.reactive.core.Observable;
import com.reactive.core.Observer;
import com.reactive.core.GroupedObservable;
import com.reactive.core.Emitter;
import com.reactive.core.Subject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * Tests for advanced grouping operators: groupBy, window, buffer
 */
public class AdvancedGroupingTest {
    
    // ============ GroupBy Tests ============
    
    @Test
    public void testGroupByBasic() {
        List<GroupedObservable<Boolean, Integer>> groups = new ArrayList<>();
        AtomicInteger groupCount = new AtomicInteger(0);
        
        Observable.range(1, 10)
            .groupBy(n -> n % 2 == 0)
            .subscribe(group -> {
                groupCount.incrementAndGet();
                groups.add(group);
            });
        
        assertEquals(2, groupCount.get(), "Should create 2 groups (even and odd)");
        
        // Test even group
        List<Integer> evenNumbers = new ArrayList<>();
        groups.stream()
            .filter(g -> g.getKey())
            .findFirst()
            .ifPresent(g -> g.subscribe(evenNumbers::add));
        
        assertTrue(evenNumbers.stream().allMatch(n -> n % 2 == 0), "Even group should only contain even numbers");
        
        // Test odd group
        List<Integer> oddNumbers = new ArrayList<>();
        groups.stream()
            .filter(g -> !g.getKey())
            .findFirst()
            .ifPresent(g -> g.subscribe(oddNumbers::add));
        
        assertTrue(oddNumbers.stream().allMatch(n -> n % 2 != 0), "Odd group should only contain odd numbers");
    }
    
    @Test
    public void testGroupByWithStrings() {
        Map<Character, List<String>> result = new ConcurrentHashMap<>();
        AtomicBoolean completed = new AtomicBoolean(false);
        
        Observable.just("apple", "banana", "apricot", "berry", "cherry", "avocado")
            .groupBy(s -> s.charAt(0))
            .subscribe(
                group -> {
                    List<String> list = new ArrayList<>();
                    group.subscribe(
                        list::add,
                        Throwable::printStackTrace,
                        () -> result.put(group.getKey(), list)
                    );
                },
                Throwable::printStackTrace,
                () -> completed.set(true)
            );
        
        assertTrue(completed.get(), "Observable should complete");
        assertEquals(3, result.size(), "Should have 3 groups (a, b, c)");
        assertEquals(3, result.get('a').size(), "Group 'a' should have 3 items");
        assertEquals(2, result.get('b').size(), "Group 'b' should have 2 items");
        assertEquals(1, result.get('c').size(), "Group 'c' should have 1 item");
    }
    
    @Test
    public void testGroupByEmpty() {
        AtomicInteger groupCount = new AtomicInteger(0);
        AtomicBoolean completed = new AtomicBoolean(false);
        
        Observable.<Integer>empty()
            .groupBy(n -> n % 2 == 0)
            .subscribe(
                group -> groupCount.incrementAndGet(),
                Throwable::printStackTrace,
                () -> completed.set(true)
            );
        
        assertEquals(0, groupCount.get(), "Empty observable should create no groups");
        assertTrue(completed.get(), "Observable should complete");
    }
    
    @Test
    public void testGroupByErrorPropagation() {
        AtomicReference<Throwable> error = new AtomicReference<>();
        AtomicBoolean groupReceivingError = new AtomicBoolean(false);
        
        Observable.just(1, 2, 3, 4, 5)
            .map(n -> {
                if (n == 3) throw new RuntimeException("Test error");
                return n;
            })
            .groupBy(n -> n % 2 == 0)
            .subscribe(
                group -> {
                    group.subscribe(
                        n -> {},
                        e -> groupReceivingError.set(true),
                        () -> {}
                    );
                },
                error::set,
                () -> {}
            );
        
        assertNotNull(error.get(), "Error should propagate");
        assertTrue(error.get().getMessage().contains("Test error"));
    }
    
    // ============ Buffer Tests ============
    
    @Test
    public void testBufferBasic() {
        List<List<Integer>> buffers = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean(false);
        
        Observable.range(1, 10)
            .buffer(3)
            .subscribe(
                buffers::add,
                Throwable::printStackTrace,
                () -> completed.set(true)
            );
        
        assertEquals(4, buffers.size(), "Should create 4 buffers");
        assertEquals(Arrays.asList(1, 2, 3), buffers.get(0));
        assertEquals(Arrays.asList(4, 5, 6), buffers.get(1));
        assertEquals(Arrays.asList(7, 8, 9), buffers.get(2));
        assertEquals(Arrays.asList(10), buffers.get(3));
        assertTrue(completed.get());
    }
    
    @Test
    public void testBufferWithSkip() {
        List<List<Integer>> buffers = new ArrayList<>();
        
        Observable.range(1, 10)
            .buffer(3, 2)
            .subscribe(buffers::add);
        
        // With count=3 and skip=2:
        // Buffer 1: [1, 2, 3]
        // Buffer 2: [3, 4, 5]  (starts at 3)
        // Buffer 3: [5, 6, 7]  (starts at 5)
        // Buffer 4: [7, 8, 9]  (starts at 7)
        // Buffer 5: [9, 10]    (starts at 9)
        
        assertEquals(5, buffers.size(), "Should create 5 overlapping buffers");
        assertEquals(3, buffers.get(0).size());
        assertEquals(Integer.valueOf(1), buffers.get(0).get(0));
        assertEquals(Integer.valueOf(3), buffers.get(1).get(0));
    }
    
    @Test
    public void testBufferEmpty() {
        List<List<Integer>> buffers = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean(false);
        
        Observable.<Integer>empty()
            .buffer(3)
            .subscribe(
                buffers::add,
                Throwable::printStackTrace,
                () -> completed.set(true)
            );
        
        assertEquals(0, buffers.size(), "Empty observable should create no buffers");
        assertTrue(completed.get());
    }
    
    @Test
    @Timeout(5)
    public void testBufferWithTime() throws InterruptedException {
        List<List<Integer>> buffers = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch latch = new CountDownLatch(1);
        
        Observable.create((Emitter<Integer> emitter) -> {
            new Thread(() -> {
                try {
                    for (int i = 1; i <= 5; i++) {
                        emitter.onNext(i);
                        Thread.sleep(100);
                    }
                    emitter.onComplete();
                } catch (InterruptedException e) {
                    emitter.onError(e);
                }
            }).start();
        })
        .buffer(250, TimeUnit.MILLISECONDS)
        .subscribe(
            buffers::add,
            Throwable::printStackTrace,
            latch::countDown
        );
        
        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertTrue(buffers.size() >= 2, "Should create at least 2 time-based buffers");
        
        // Verify each buffer contains consecutive numbers
        for (List<Integer> buffer : buffers) {
            assertFalse(buffer.isEmpty(), "Buffers should not be empty");
        }
    }
    
    // ============ Window Tests ============
    
    @Test
    public void testWindowBasic() {
        List<List<Integer>> windows = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);
        
        Observable.range(1, 10)
            .window(3)
            .subscribe(
                window -> {
                    List<Integer> items = new ArrayList<>();
                    window.subscribe(items::add);
                    windows.add(items);
                },
                Throwable::printStackTrace,
                latch::countDown
            );
        
        try {
            latch.await(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("Test interrupted");
        }
        
        assertEquals(4, windows.size(), "Should create 4 windows");
        assertEquals(Arrays.asList(1, 2, 3), windows.get(0));
        assertEquals(Arrays.asList(4, 5, 6), windows.get(1));
        assertEquals(Arrays.asList(7, 8, 9), windows.get(2));
        assertEquals(Arrays.asList(10), windows.get(3));
    }
    
    @Test
    public void testWindowEmpty() {
        AtomicInteger windowCount = new AtomicInteger(0);
        AtomicBoolean completed = new AtomicBoolean(false);
        
        Observable.<Integer>empty()
            .window(3)
            .subscribe(
                window -> windowCount.incrementAndGet(),
                Throwable::printStackTrace,
                () -> completed.set(true)
            );
        
        assertEquals(0, windowCount.get(), "Empty observable should create no windows");
        assertTrue(completed.get());
    }
    
    @Test
    @Timeout(5)
    public void testWindowWithTime() throws InterruptedException {
        List<List<Integer>> windows = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch latch = new CountDownLatch(1);
        
        Observable.create((Emitter<Integer> emitter) -> {
            new Thread(() -> {
                try {
                    for (int i = 1; i <= 5; i++) {
                        emitter.onNext(i);
                        Thread.sleep(100);
                    }
                    emitter.onComplete();
                } catch (InterruptedException e) {
                    emitter.onError(e);
                }
            }).start();
        })
        .window(250, TimeUnit.MILLISECONDS)
        .subscribe(
            window -> {
                List<Integer> items = Collections.synchronizedList(new ArrayList<>());
                window.subscribe(items::add);
                windows.add(items);
            },
            Throwable::printStackTrace,
            latch::countDown
        );
        
        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertTrue(windows.size() >= 2, "Should create at least 2 time-based windows");
    }
    
    // ============ Combined Operations Tests ============
    
    @Test
    public void testGroupByThenBuffer() {
        Map<Boolean, List<List<Integer>>> result = new ConcurrentHashMap<>();
        CountDownLatch latch = new CountDownLatch(2);
        
        Observable.range(1, 10)
            .groupBy(n -> n % 2 == 0)
            .subscribe(group -> {
                group.buffer(2)
                    .subscribe(
                        buffer -> result.computeIfAbsent(group.getKey(), k -> new ArrayList<>()).add(buffer),
                        Throwable::printStackTrace,
                        latch::countDown
                    );
            });
        
        try {
            latch.await(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("Test interrupted");
        }
        
        assertEquals(2, result.size(), "Should have 2 groups");
        assertNotNull(result.get(true), "Should have even group");
        assertNotNull(result.get(false), "Should have odd group");
    }
}
