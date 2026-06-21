package benchmarks;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;

import org.apache.hadoop.io.IntWritable;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 250, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 250, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
public class WordCountBenchmark {

    // Per-thread state keeps benchmark inputs stable across iterations.
    @State(Scope.Thread)
    public static class BenchmarkState {
        private String shortLine;
        private String longLine;
        private List<IntWritable> smallValues;
        private List<IntWritable> largeValues;
        private Method normalizeLineMethod;
        private Method sumValuesMethod;

        @Setup(Level.Trial)
        public void setUp() throws Exception {
            // Use one short line and one realistic paragraph to compare input sizes.
            shortLine = "Hello, Hadoop! MAP_reduce 123";
            longLine = "Hadoop makes batch processing easier. "
                    + "WordCount keeps the example focused on tokenization, normalization, and aggregation. "
                    + "Performance benchmarks help us observe the cost of repeated text cleanup.";

            // Prebuild reusable reducer inputs so the benchmark focuses on aggregation cost.
            smallValues = buildValues(8);
            largeValues = buildValues(1_000);

            // WordCount lives in the default package, so the benchmark reaches it via reflection.
            Class<?> wordCountClass = Class.forName("WordCount");
            normalizeLineMethod = wordCountClass.getDeclaredMethod("normalizeLine", String.class);
            normalizeLineMethod.setAccessible(true);

            Class<?> reducerClass = Class.forName("WordCount$IntSumReducer");
            sumValuesMethod = reducerClass.getDeclaredMethod("sumValues", Iterable.class);
            sumValuesMethod.setAccessible(true);
        }

        private static List<IntWritable> buildValues(int size) {
            List<IntWritable> values = new ArrayList<>(size);
            for (int index = 0; index < size; index++) {
                values.add(new IntWritable((index % 5) + 1));
            }
            return values;
        }

        private String normalize(String line) throws Exception {
            return (String) normalizeLineMethod.invoke(null, line);
        }

        private int sum(List<IntWritable> values) throws Exception {
            return (Integer) sumValuesMethod.invoke(null, values);
        }
    }

    // Measures the simplest normalization path.
    @Benchmark
    public String normalizeShortLine(BenchmarkState state) throws Exception {
        return state.normalize(state.shortLine);
    }

    // Measures normalization on a longer line with more punctuation and words.
    @Benchmark
    public String normalizeLongLine(BenchmarkState state) throws Exception {
        return state.normalize(state.longLine);
    }

    // Approximates the mapper's preprocessing path: normalization plus tokenization.
    @Benchmark
    public int tokenizeNormalizedLongLine(BenchmarkState state) throws Exception {
        String normalized = state.normalize(state.longLine);
        StringTokenizer tokenizer = new StringTokenizer(normalized);
        int tokenCount = 0;

        while (tokenizer.hasMoreTokens()) {
            tokenizer.nextToken();
            tokenCount++;
        }

        return tokenCount;
    }

    // Measures reducer aggregation on a small set of IntWritable values.
    @Benchmark
    public int sumSmallValues(BenchmarkState state) throws Exception {
        return state.sum(state.smallValues);
    }

    // Measures reducer aggregation on a larger input set.
    @Benchmark
    public int sumLargeValues(BenchmarkState state) throws Exception {
        return state.sum(state.largeValues);
    }

    // Allows the benchmark to be launched directly without an extra runner class.
    public static void main(String[] args) throws Exception {
        org.openjdk.jmh.Main.main(args);
    }
}
