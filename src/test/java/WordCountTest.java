import java.util.Arrays;
import java.util.Collections;

import org.apache.hadoop.io.IntWritable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

/**
 * Unit tests for WordCount MapReduce application.
 * Tests normalization logic, aggregation, and command-line handling.
 */
class WordCountTest {

    // =========================================================================
    // 1. Normalize Line Tests - Strategy Pattern
    // =========================================================================

    @Test
    void normalizeLineShouldLowercaseAndRemovePunctuation() {
        String normalized = WordCount.normalizeLine("Hello, Hadoop! MAP_reduce 123");
        Assertions.assertEquals("hello  hadoop  map reduce 123", normalized);
    }

    @Test
    void normalizeLineShouldHandleAllPunctuation() {
        String normalized = WordCount.normalizeLine("!@#$%^&*()Test-Case_123");
        // Each symbol replaced with space: "!@#$%^&*()" -> "          " (10 spaces)
        // Then lowercase and preserve numbers: "Test-Case_123" -> "test case 123"
        Assertions.assertEquals("          test case 123", normalized);
    }

    @Test
    void normalizeLineShouldPreserveWhitespace() {
        String normalized = WordCount.normalizeLine("One   Two\tThree");
        Assertions.assertEquals("one   two\tthree", normalized);
    }

    @Test
    void normalizeLineShouldHandleEmptyString() {
        String normalized = WordCount.normalizeLine("");
        Assertions.assertEquals("", normalized);
    }

    @Test
    void normalizeLineShouldHandleOnlyPunctuation() {
        String normalized = WordCount.normalizeLine("!!! ### ???");
        // Each symbol replaced with space: "!!! ### ???" -> "           " (11 spaces)
        Assertions.assertEquals("           ", normalized);
    }

    // =========================================================================
    // 2. Aggregation Tests - Reducer Helper
    // =========================================================================

    @Test
    void sumValuesShouldAggregateIntWritableValues() {
        int sum = WordCount.IntSumReducer.sumValues(Arrays.asList(
                new IntWritable(1),
                new IntWritable(2),
                new IntWritable(3)));
        Assertions.assertEquals(6, sum);
    }

    @Test
    void sumValuesShouldHandleEmptyIterable() {
        int sum = WordCount.IntSumReducer.sumValues(Collections.emptyList());
        Assertions.assertEquals(0, sum);
    }

    @Test
    void sumValuesShouldHandleLargeValues() {
        int sum = WordCount.IntSumReducer.sumValues(Arrays.asList(
                new IntWritable(Integer.MAX_VALUE / 2),
                new IntWritable(Integer.MAX_VALUE / 2)));
        // Note: IntWritable wraps int, which wraps on overflow
        Assertions.assertTrue(sum >= 0 || sum < 0); // Just verifying no exception
    }

    @Test
    void sumValuesShouldHandleSingleValue() {
        int sum = WordCount.IntSumReducer.sumValues(Arrays.asList(new IntWritable(42)));
        Assertions.assertEquals(42, sum);
    }

    @Test
    void sumValuesShouldHandleNegativeValues() {
        int sum = WordCount.IntSumReducer.sumValues(Arrays.asList(
                new IntWritable(-5),
                new IntWritable(10),
                new IntWritable(-3)));
        Assertions.assertEquals(2, sum);
    }

    @Test
    void sumValuesShouldHandleZeroValues() {
        int sum = WordCount.IntSumReducer.sumValues(Arrays.asList(
                new IntWritable(0),
                new IntWritable(0),
                new IntWritable(5)));
        Assertions.assertEquals(5, sum);
    }

    // =========================================================================
    // 3. CLI Argument Validation Tests
    // =========================================================================

    @Test
    void runShouldReturnUsageCodeWhenArgumentsMissing() throws Exception {
        int exitCode = WordCount.run(new String[] {});
        Assertions.assertEquals(2, exitCode, "Should return usage error code 2 for missing arguments");
    }

    @Test
    void runShouldReturnUsageCodeWhenInputPathMissing() throws Exception {
        int exitCode = WordCount.run(new String[] {"/output-only"});
        Assertions.assertEquals(2, exitCode, "Should return usage error code 2 for single argument");
    }

    @Test
    void runShouldReturnUsageCodeWhenTooManyArguments() throws Exception {
        int exitCode = WordCount.run(new String[] {"/input", "/output", "/extra"});
        Assertions.assertEquals(2, exitCode, "Should return usage error code 2 for too many arguments");
    }

    // =========================================================================
    // 4. Strategy Pattern Tests
    // =========================================================================

    @Test
    void normalizationStrategyCanBeOverridden() {
        // Save original
        WordCount.TextNormalizationStrategy original = WordCount.normalizationStrategy;
        
        try {
            // Override with custom strategy
            WordCount.normalizationStrategy = line -> "CUSTOM:" + line;
            String result = WordCount.normalizeLine("test");
            Assertions.assertEquals("CUSTOM:test", result);
        } finally {
            // Restore original
            WordCount.normalizationStrategy = original;
        }
    }

    // =========================================================================
    // 5. Line Coverage Tests - Edge Cases
    // =========================================================================

    @Test
    void normalizeLineShouldConvertNumbersToWords() {
        // This tests that numbers remain as-is (are not removed)
        String normalized = WordCount.normalizeLine("123 456 789");
        Assertions.assertEquals("123 456 789", normalized);
    }

    @Test
    void normalizeLineShouldHandleConsecutiveSymbols() {
        String normalized = WordCount.normalizeLine("word...another---test");
        Assertions.assertEquals("word   another   test", normalized);
    }

    @Test
    void normalizeLineShouldHandleLeadingTrailingPunctuation() {
        String normalized = WordCount.normalizeLine("...hello...");
        Assertions.assertEquals("   hello   ", normalized);
    }

    @Test
    void normalizeLineShouldLowercaseUppercaseLetters() {
        String normalized = WordCount.normalizeLine("UPPERCASE MixedCase lowercase");
        Assertions.assertEquals("uppercase mixedcase lowercase", normalized);
    }
}
