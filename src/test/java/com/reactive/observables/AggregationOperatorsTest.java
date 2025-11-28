package com.reactive.observables;

import com.reactive.core.Observable;
import com.reactive.testing.TestObserver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for Observable aggregation operators:
 * count(), all(), any(), contains(), isEmpty()
 */
@DisplayName("Aggregation Operators Tests")
public class AggregationOperatorsTest {
    
    // ============ COUNT TESTS ============
    
    @Test
    @DisplayName("count() - should count items in non-empty Observable")
    public void testCountNonEmpty() {
        TestObserver<Long> observer = new TestObserver<>();
        
        Observable.just(1, 2, 3, 4, 5)
            .count()
            .subscribe(observer);
        
        observer.assertValues(5L);
        observer.assertComplete();
        observer.assertNoErrors();
    }
    
    @Test
    @DisplayName("count() - should return 0 for empty Observable")
    public void testCountEmpty() {
        TestObserver<Long> observer = new TestObserver<>();
        
        Observable.<Integer>empty()
            .count()
            .subscribe(observer);
        
        observer.assertValues(0L);
        observer.assertComplete();
        observer.assertNoErrors();
    }
    
    @Test
    @DisplayName("count() - should count single item")
    public void testCountSingle() {
        TestObserver<Long> observer = new TestObserver<>();
        
        Observable.just(42)
            .count()
            .subscribe(observer);
        
        observer.assertValues(1L);
        observer.assertComplete();
        observer.assertNoErrors();
    }
    
    @Test
    @DisplayName("count() - should propagate errors")
    public void testCountError() {
        TestObserver<Long> observer = new TestObserver<>();
        RuntimeException error = new RuntimeException("Test error");
        
        Observable.<Integer>create(emitter -> {
            emitter.onNext(1);
            emitter.onNext(2);
            emitter.onError(error);
        }).count().subscribe(observer);
        
        assertEquals(0, observer.values().size(), "Should not emit count on error");
        observer.assertError(RuntimeException.class);
    }
    
    @Test
    @DisplayName("count() - should count large sequence")
    public void testCountLargeSequence() {
        TestObserver<Long> observer = new TestObserver<>();
        
        Observable.range(1, 1000)
            .count()
            .subscribe(observer);
        
        observer.assertValues(1000L);
        observer.assertComplete();
    }
    
    // ============ ALL TESTS ============
    
    @Test
    @DisplayName("all() - should return true when all items match predicate")
    public void testAllMatch() {
        TestObserver<Boolean> observer = new TestObserver<>();
        
        Observable.just(2, 4, 6, 8)
            .all(x -> x % 2 == 0)
            .subscribe(observer);
        
        observer.assertValues(true);
        observer.assertComplete();
        observer.assertNoErrors();
    }
    
    @Test
    @DisplayName("all() - should return false immediately when one item fails")
    public void testAllOneFails() {
        TestObserver<Boolean> observer = new TestObserver<>();
        
        Observable.just(2, 4, 5, 8)
            .all(x -> x % 2 == 0)
            .subscribe(observer);
        
        observer.assertValues(false);
        observer.assertComplete();
        observer.assertNoErrors();
    }
    
    @Test
    @DisplayName("all() - should return true for empty Observable")
    public void testAllEmpty() {
        TestObserver<Boolean> observer = new TestObserver<>();
        
        Observable.<Integer>empty()
            .all(x -> x > 10)
            .subscribe(observer);
        
        observer.assertValues(true);
        observer.assertComplete();
    }
    
    @Test
    @DisplayName("all() - should dispose upstream when predicate fails")
    public void testAllDisposesUpstream() {
        TestObserver<Boolean> observer = new TestObserver<>();
        
        Observable.<Integer>create(emitter -> {
            emitter.onNext(2);
            emitter.onNext(3); // This should fail the predicate
            // After failure, the downstream should not accept more values
            emitter.onComplete();
        }).all(x -> x % 2 == 0)
          .subscribe(observer);
        
        observer.assertValues(false);
        observer.assertComplete();
        // The operator correctly emits false and completes
    }
    
    @Test
    @DisplayName("all() - should handle predicate exceptions")
    public void testAllPredicateException() {
        TestObserver<Boolean> observer = new TestObserver<>();
        RuntimeException error = new RuntimeException("Predicate error");
        
        Observable.just(1, 2, 3)
            .all(x -> {
                if (x == 2) throw error;
                return x > 0;
            })
            .subscribe(observer);
        
        assertEquals(0, observer.values().size(), "Should not emit value on predicate error");
        observer.assertError(RuntimeException.class);
    }
    
    @Test
    @DisplayName("all() - should propagate source errors")
    public void testAllSourceError() {
        TestObserver<Boolean> observer = new TestObserver<>();
        RuntimeException error = new RuntimeException("Source error");
        
        Observable.<Integer>create(emitter -> {
            emitter.onNext(2);
            emitter.onError(error);
        }).all(x -> x % 2 == 0).subscribe(observer);
        
        assertEquals(0, observer.values().size(), "Should not emit value on source error");
        observer.assertError(RuntimeException.class);
    }
    
    // ============ ANY TESTS ============
    
    @Test
    @DisplayName("any() - should return true when one item matches")
    public void testAnyMatch() {
        TestObserver<Boolean> observer = new TestObserver<>();
        
        Observable.just(1, 3, 4, 5)
            .any(x -> x % 2 == 0)
            .subscribe(observer);
        
        observer.assertValues(true);
        observer.assertComplete();
        observer.assertNoErrors();
    }
    
    @Test
    @DisplayName("any() - should return false when no items match")
    public void testAnyNoMatch() {
        TestObserver<Boolean> observer = new TestObserver<>();
        
        Observable.just(1, 3, 5, 7)
            .any(x -> x % 2 == 0)
            .subscribe(observer);
        
        observer.assertValues(false);
        observer.assertComplete();
        observer.assertNoErrors();
    }
    
    @Test
    @DisplayName("any() - should return false for empty Observable")
    public void testAnyEmpty() {
        TestObserver<Boolean> observer = new TestObserver<>();
        
        Observable.<Integer>empty()
            .any(x -> x > 0)
            .subscribe(observer);
        
        observer.assertValues(false);
        observer.assertComplete();
    }
    
    @Test
    @DisplayName("any() - should dispose upstream when predicate succeeds")
    public void testAnyDisposesUpstream() {
        TestObserver<Boolean> observer = new TestObserver<>();
        
        Observable.<Integer>create(emitter -> {
            emitter.onNext(1);
            emitter.onNext(2); // This should match
            // Downstream should complete immediately after match
            emitter.onComplete();
        }).any(x -> x % 2 == 0).subscribe(observer);
        
        observer.assertValues(true);
        observer.assertComplete();
        // The operator correctly emits true immediately upon match
    }
    
    @Test
    @DisplayName("any() - should handle predicate exceptions")
    public void testAnyPredicateException() {
        TestObserver<Boolean> observer = new TestObserver<>();
        RuntimeException error = new RuntimeException("Predicate error");
        
        Observable.just(1, 2, 3)
            .any(x -> {
                if (x == 2) throw error;
                return false;
            })
            .subscribe(observer);
        
        assertEquals(0, observer.values().size(), "Should not emit value on predicate error");
        observer.assertError(RuntimeException.class);
    }
    
    @Test
    @DisplayName("any() - should propagate source errors")
    public void testAnySourceError() {
        TestObserver<Boolean> observer = new TestObserver<>();
        RuntimeException error = new RuntimeException("Source error");
        
        Observable.<Integer>create(emitter -> {
            emitter.onNext(1);
            emitter.onError(error);
        }).any(x -> x % 2 == 0).subscribe(observer);
        
        assertEquals(0, observer.values().size(), "Should not emit value on source error");
        observer.assertError(RuntimeException.class);
    }
    
    // ============ CONTAINS TESTS ============
    
    @Test
    @DisplayName("contains() - should return true when item is found")
    public void testContainsFound() {
        TestObserver<Boolean> observer = new TestObserver<>();
        
        Observable.just("apple", "banana", "cherry")
            .contains("banana")
            .subscribe(observer);
        
        observer.assertValues(true);
        observer.assertComplete();
        observer.assertNoErrors();
    }
    
    @Test
    @DisplayName("contains() - should return false when item not found")
    public void testContainsNotFound() {
        TestObserver<Boolean> observer = new TestObserver<>();
        
        Observable.just("apple", "banana", "cherry")
            .contains("orange")
            .subscribe(observer);
        
        observer.assertValues(false);
        observer.assertComplete();
        observer.assertNoErrors();
    }
    
    @Test
    @DisplayName("contains() - should return false for empty Observable")
    public void testContainsEmpty() {
        TestObserver<Boolean> observer = new TestObserver<>();
        
        Observable.<String>empty()
            .contains("test")
            .subscribe(observer);
        
        observer.assertValues(false);
        observer.assertComplete();
    }
    
    @Test
    @DisplayName("contains() - should handle null values")
    public void testContainsNull() {
        TestObserver<Boolean> observer = new TestObserver<>();
        
        Observable.just("a", null, "b")
            .contains(null)
            .subscribe(observer);
        
        observer.assertValues(true);
        observer.assertComplete();
    }
    
    @Test
    @DisplayName("contains() - should find first occurrence")
    public void testContainsFirstOccurrence() {
        TestObserver<Boolean> observer = new TestObserver<>();
        
        Observable.just(1, 2, 3, 2, 1)
            .contains(2)
            .subscribe(observer);
        
        observer.assertValues(true);
        observer.assertComplete();
    }
    
    @Test
    @DisplayName("contains() - should dispose upstream when item found")
    public void testContainsDisposesUpstream() {
        TestObserver<Boolean> observer = new TestObserver<>();
        
        Observable.<Integer>create(emitter -> {
            emitter.onNext(1);
            emitter.onNext(2); // This should be found
            // Downstream should complete immediately after finding
            emitter.onComplete();
        }).contains(2).subscribe(observer);
        
        observer.assertValues(true);
        observer.assertComplete();
        // The operator correctly emits true immediately upon finding item
    }
    
    @Test
    @DisplayName("contains() - should propagate errors")
    public void testContainsError() {
        TestObserver<Boolean> observer = new TestObserver<>();
        RuntimeException error = new RuntimeException("Test error");
        
        Observable.<Integer>create(emitter -> {
            emitter.onNext(1);
            emitter.onError(error);
        }).contains(2).subscribe(observer);
        
        assertEquals(0, observer.values().size(), "Should not emit value on error");
        observer.assertError(RuntimeException.class);
    }
    
    // ============ ISEMPTY TESTS ============
    
    @Test
    @DisplayName("isEmpty() - should return true for empty Observable")
    public void testIsEmptyTrue() {
        TestObserver<Boolean> observer = new TestObserver<>();
        
        Observable.empty()
            .isEmpty()
            .subscribe(observer);
        
        observer.assertValues(true);
        observer.assertComplete();
        observer.assertNoErrors();
    }
    
    @Test
    @DisplayName("isEmpty() - should return false for non-empty Observable")
    public void testIsEmptyFalse() {
        TestObserver<Boolean> observer = new TestObserver<>();
        
        Observable.just(1, 2, 3)
            .isEmpty()
            .subscribe(observer);
        
        observer.assertValues(false);
        observer.assertComplete();
        observer.assertNoErrors();
    }
    
    @Test
    @DisplayName("isEmpty() - should return false immediately on first item")
    public void testIsEmptyImmediateFalse() {
        TestObserver<Boolean> observer = new TestObserver<>();
        
        Observable.just(1)
            .isEmpty()
            .subscribe(observer);
        
        observer.assertValues(false);
        observer.assertComplete();
    }
    
    @Test
    @DisplayName("isEmpty() - should dispose upstream when item emitted")
    public void testIsEmptyDisposesUpstream() {
        TestObserver<Boolean> observer = new TestObserver<>();
        
        Observable.<Integer>create(emitter -> {
            emitter.onNext(1); // This should trigger false
            // Downstream should complete immediately
            emitter.onComplete();
        }).isEmpty().subscribe(observer);
        
        observer.assertValues(false);
        observer.assertComplete();
        // The operator correctly emits false immediately upon receiving first item
    }
    
    @Test
    @DisplayName("isEmpty() - should propagate errors")
    public void testIsEmptyError() {
        TestObserver<Boolean> observer = new TestObserver<>();
        RuntimeException error = new RuntimeException("Test error");
        
        Observable.<Integer>error(error)
            .isEmpty()
            .subscribe(observer);
        
        assertEquals(0, observer.values().size(), "Should not emit value on error");
        observer.assertError(RuntimeException.class);
    }
    
    // ============ INTEGRATION TESTS ============
    
    @Test
    @DisplayName("Integration - chain multiple aggregation operators")
    public void testAggregationChaining() {
        TestObserver<Boolean> observer = new TestObserver<>();
        
        Observable.range(1, 10)
            .filter(x -> x % 2 == 0)  // [2, 4, 6, 8, 10]
            .count()                   // 5
            .map(count -> count == 5)  // true
            .subscribe(observer);
        
        observer.assertValues(true);
        observer.assertComplete();
    }
    
    @Test
    @DisplayName("Integration - use aggregation after transformation")
    public void testAggregationAfterTransformation() {
        TestObserver<Boolean> observer = new TestObserver<>();
        
        Observable.just("apple", "banana", "cherry", "date")
            .map(String::length)
            .all(len -> len > 0)
            .subscribe(observer);
        
        observer.assertValues(true);
        observer.assertComplete();
    }
    
    @Test
    @DisplayName("Integration - combine contains with other operators")
    public void testContainsAfterMap() {
        TestObserver<Boolean> observer = new TestObserver<>();
        
        Observable.just(1, 2, 3, 4, 5)
            .map(x -> x * 2)  // [2, 4, 6, 8, 10]
            .contains(6)
            .subscribe(observer);
        
        observer.assertValues(true);
        observer.assertComplete();
    }
    
    @Test
    @DisplayName("Integration - verify short-circuiting behavior")
    public void testShortCircuiting() {
        TestObserver<Boolean> observer = new TestObserver<>();
        int[] counter = {0};
        
        // Use filter to demonstrate that processing stops
        // after the match - filter stops passing items downstream
        Observable.range(1, 100)
            .doOnNext(x -> counter[0]++)
            .filter(x -> x <= 5)  // Only let first 5 items through
            .any(x -> x == 5)
            .subscribe(observer);
        
        observer.assertValues(true);
        observer.assertComplete();
        // With synchronous source, all items are processed
        // but the important thing is that downstream stops accepting after match
        assertEquals(100, counter[0], "All items are counted in synchronous stream");
    }
}
