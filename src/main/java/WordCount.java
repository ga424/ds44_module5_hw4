import java.io.IOException;
import java.util.StringTokenizer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class WordCount {

    @FunctionalInterface
    interface TextNormalizationStrategy {
        String normalize(String line);
    }

    static class AlphanumericLowercaseStrategy implements TextNormalizationStrategy {
        @Override
        public String normalize(String line) {
            return line.toLowerCase().replaceAll("[^a-z0-9\\s]", " ");
        }
    }

    // Injection seam for tests and future variants of token pre-processing.
    static TextNormalizationStrategy normalizationStrategy = new AlphanumericLowercaseStrategy();

    // Factory pattern seam so run() can be verified without launching a real Hadoop job.
    @FunctionalInterface
    interface WordCountJobFactory {
        Job create(Configuration conf, String inputPath, String outputPath) throws IOException;
    }

    static WordCountJobFactory jobFactory = (conf, inputPath, outputPath) -> {
        Job job = Job.getInstance(conf, "word count");

        job.setJarByClass(WordCount.class);
        job.setMapperClass(TokenizerMapper.class);
        job.setCombinerClass(IntSumReducer.class);
        job.setReducerClass(IntSumReducer.class);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);

        FileInputFormat.addInputPath(job, new Path(inputPath));
        FileOutputFormat.setOutputPath(job, new Path(outputPath));
        return job;
    };

    // Delegates to the active strategy so normalization rules are swappable.
    static String normalizeLine(String line) {
        return normalizationStrategy.normalize(line);
    }

    public static class TokenizerMapper extends Mapper<Object, Text, Text, IntWritable> {
        private static final IntWritable ONE = new IntWritable(1);
        private final Text word = new Text();

        @Override
        public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
            String line = normalizeLine(value.toString());
            StringTokenizer tokenizer = new StringTokenizer(line);

            // Emit one occurrence per token. Reducer performs aggregation later.
            while (tokenizer.hasMoreTokens()) {
                word.set(tokenizer.nextToken());
                context.write(word, ONE);
            }
        }
    }

    public static class IntSumReducer extends Reducer<Text, IntWritable, Text, IntWritable> {
        private final IntWritable result = new IntWritable();

        // Shared pure helper enables direct unit testing of aggregation behavior.
        static int sumValues(Iterable<IntWritable> values) {
            int sum = 0;
            for (IntWritable val : values) {
                sum += val.get();
            }
            return sum;
        }

        @Override
        public void reduce(Text key, Iterable<IntWritable> values, Context context)
                throws IOException, InterruptedException {
            int sum = sumValues(values);
            result.set(sum);
            context.write(key, result);
        }
    }

    static int run(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: WordCount <input path> <output path>");
            return 2;
        }

        Configuration conf = new Configuration();
        Job job = jobFactory.create(conf, args[0], args[1]);

        // Aligns exit semantics with typical CLI behavior: 0 success, 1 failure.
        return job.waitForCompletion(true) ? 0 : 1;
    }

    public static void main(String[] args) throws Exception {
        System.exit(run(args));
    }
}
