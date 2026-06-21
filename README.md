# Java WordCount MapReduce on Hadoop

This project contains a complete Java MapReduce WordCount example for a Hadoop cluster. It features a production-ready implementation with comprehensive unit tests (19 tests, 100% pass rate), design patterns for testability, and automated CI/CD validation.

## Quick Start

```bash
# Build JAR
./mvnw clean package


# Run all tests (19 tests)
./mvnw clean test

# Full validation pipeline (tests + coverage)
./mvnw clean verify

# Generate coverage report
./mvnw test jacoco:report
# Open: target/site/jacoco/index.html

# Run performance benchmarks
bash scripts/run_benchmarks.sh
```

**Status**: ✅ All tests passing, build successful, ready for deployment.

## Project Structure

- `src/main/java/WordCount.java`: Core MapReduce implementation with Strategy and Factory design patterns
- `src/main/java/benchmarks/WordCountBenchmark.java`: JMH benchmarks for normalization and aggregation hot paths
- `src/test/java/WordCountTest.java`: 19 comprehensive unit tests covering all functionality
- `scripts/build.sh`: Compiles Java source and creates JAR
- `scripts/run_wordcount.sh`: Runs the job on HDFS input/output paths
- `scripts/run_local_containerized.sh`: Runs local containerized Hadoop validation
- `scripts/run_benchmarks.sh`: Runs JMH performance benchmarks
- `input.txt`: Sample input file
- `output.txt`: Expected output for the sample input
- `docs/C4.md`: C4 architecture documentation (context, container, component, deployment)
- `pom.xml`: Maven build configuration with JUnit 5, Mockito, JaCoCo coverage validation
- `mvnw` / `mvnw.cmd`: Maven wrapper (v3.9.16) for reproducible builds
- `.github/workflows/ci.yml`: GitHub Actions pipeline for automated testing and coverage validation
- `.github/workflows/containerized-validation.yml`: GitHub Actions containerized Hadoop smoke tests
- `docker-compose.hadoop.yml` and `.env.hadoop`: Local Hadoop 3.2.1 cluster for containerized runs
- `BUILD_SUMMARY.md`: Final build status and deliverables summary

## Architecture (C4)

This application architecture is documented using the C4 model.

- Full diagrams: [docs/C4.md](docs/C4.md)
- C4 Level 1 (Context): Operator runs the WordCount app, which interacts with HDFS and YARN.
- C4 Level 2 (Container): `WordCount.java`, build/run scripts, JAR artifact, and HDFS input/output flow.
- C4 Level 3 (Component): Driver (`main`), `TokenizerMapper`, and `IntSumReducer` (combiner + reducer).
- C4 Level 4 (Deployment): 4-node Hadoop cluster with one NameNode and three worker nodes.

Deployment view summary:

1. NameNode hosts Hadoop client tools and launches `wordcount.jar`.
2. YARN schedules map/reduce containers across `slave1`, `slave2`, and `slave3`.
3. HDFS stores input splits and final output (`part-r-00000`).
4. Student/Operator submits and monitors the job from the NameNode.

## Target Environment

This is designed for a 4-node Hadoop cluster:
- 1 NameNode (master)
- 3 DataNode/worker nodes (`slave1`, `slave2`, `slave3`)

Run the commands below from the NameNode where Hadoop client commands are available.

## Prerequisites

### For Local Development

1. **Java Development Kit (JDK 17+)**
   ```bash
   java -version
   ```
   On macOS with Homebrew: `brew install openjdk@17`

   The Maven build uses JDK 17 tooling, but it produces Java 8 compatible bytecode so the Docker Hadoop images can run the JAR.

2. **Maven 3.9+**
   ```bash
   mvn -v
   ```
   On macOS with Homebrew: `brew install maven`

3. **Docker (for containerized local testing)**
   ```bash
   docker --version
   docker compose version
   ```
   On macOS with Homebrew: `brew install docker`

### For Hadoop Cluster Deployment

1. Hadoop is installed and configured on all nodes.
2. HDFS and YARN are running.
3. Java (JDK 8 or compatible with your Hadoop version) is installed.
4. SSH connectivity among nodes is configured.

Useful cluster checks:

```bash
jps
hdfs dfsadmin -report
yarn node -list
```

You should see one active NameNode and three active DataNodes/NodeManagers.

## Maven Wrapper (Recommended)

This project includes a Maven wrapper (`mvnw`/`mvnw.cmd`) for consistent builds without requiring Maven installation:

```bash
./mvnw clean package          # Build only
./mvnw clean test             # Run unit tests
./mvnw clean verify           # Full pipeline: compile → test → coverage validation
```

The wrapper ensures all developers and CI environments use Maven 3.9.16 with JDK 17 compilation.

## Automated Testing and Coverage

Run the complete test and coverage validation pipeline:

```bash
./mvnw clean verify
```

### Test Suite

This project includes **19 comprehensive unit tests** with the following coverage:

1. **Normalization Tests (5 tests)**: Text lowercase, punctuation removal, symbol handling, edge cases
2. **Aggregation Tests (7 tests)**: Value summing across various inputs (empty, single, multiple, large, negative, zero)
3. **CLI Validation Tests (3 tests)**: Argument validation and usage code returns
4. **Design Pattern Tests (2 tests)**: Strategy pattern injection (custom normalizers) and Factory pattern (job creation)
5. **Edge Cases (2 tests)**: Numeric tokens, consecutive symbols, leading/trailing punctuation

### Code Coverage

- **Pure Functions** (`IntSumReducer.sumValues()`): **100% line coverage**
- **Text Normalization Logic**: Comprehensive edge case coverage
- **CLI Validation**: Full argument checking coverage
- **Overall Application**: ~76% line coverage (practical limit without live Hadoop cluster)

The JaCoCo coverage gate validates 100% coverage of the `IntSumReducer` aggregation function, which is fully testable without Hadoop infrastructure. Integration code (Job submission, Context interaction) requires a live cluster.

Generate detailed coverage report:

```bash
./mvnw test jacoco:report
# Open: target/site/jacoco/index.html
```

## Security Scanning (Optional)

Security scanning via OWASP Dependency-Check can be run manually (requires internet connectivity for vulnerability database):

```bash
./mvnw org.owasp:dependency-check-maven:check
```

Report is generated at: `target/dependency-check-report.html`

*Note: This is not part of the automated verify pipeline to support offline environments.*

## Performance Benchmarks (JMH)

This project includes JMH benchmarks in `src/main/java/benchmarks/WordCountBenchmark.java` for the most testable hot paths:

1. Text normalization on a short line.
2. Text normalization on a longer, more realistic line.
3. Normalization plus tokenization of the longer line, approximating mapper preprocessing.
4. Reducer aggregation on small and large `IntWritable` lists.

Run the benchmarks with:

```bash
bash scripts/run_benchmarks.sh
```

The helper script compiles the benchmark class, builds the runtime classpath, and launches JMH. Pass JMH options through the script if you want to narrow the run, for example:

```bash
bash scripts/run_benchmarks.sh normalizeShortLine
```

## GitHub Actions CI

GitHub Actions workflow is defined in `.github/workflows/ci.yml`.

On every push and pull request to `main`, it will:

1. Build the project with Maven.
2. Execute tests.
3. Enforce the JaCoCo coverage gate.
4. Upload JaCoCo HTML report as an artifact.

Containerized integration workflow is defined in `.github/workflows/containerized-validation.yml`.

1. Builds the JAR.
2. Starts local Hadoop with Docker Compose.
3. Runs the MapReduce job end-to-end.
4. Tears down containers automatically.

## Containerized Local Validation (No AWS Required)

You can run and validate the MapReduce job locally without AWS using Docker Compose.

1. Ensure Docker, Docker Compose, and Maven are installed.
2. Run:

```bash
chmod +x scripts/run_local_containerized.sh
./scripts/run_local_containerized.sh
```

What this does:

1. Builds the JAR with Maven.
2. Starts a local Hadoop stack in containers (`namenode`, `datanode`, `resourcemanager`, `nodemanager`, `historyserver`).
3. Uploads `input.txt` to HDFS.
4. Runs the WordCount MapReduce job.
5. Prints output from `part-r-00000`.

Stop and clean containers:

```bash
docker compose -f docker-compose.hadoop.yml down -v
```

## How to Validate the JAR

Use these commands to verify the packaged JAR and the end-to-end Hadoop flow:

1. Build the JAR and copy it to the legacy `build/` location:

```bash
bash scripts/build.sh
ls -l build/wordcount.jar
```

2. Run the local Hadoop smoke test, which uploads `input.txt`, executes the JAR in a containerized Hadoop cluster, and prints the final counts:

```bash
bash scripts/run_local_containerized.sh
```

3. Confirm the output file in the project root matches the expected token counts generated from `input.txt`:

```bash
cat output.txt
```

Expected success indicators:

- `scripts/build.sh` finishes with `Build complete: .../build/wordcount.jar`
- `scripts/run_local_containerized.sh` finishes with `Containerized run completed successfully.`
- `output.txt` contains tab-separated `token<TAB>count` rows sorted alphabetically

## Build the Program

```bash
cd module5_hw4
./mvnw clean package
```

Output JAR:

```bash
target/wordcount-hadoop-1.0.0.jar
```

The JAR is executable and includes all necessary classes for MapReduce execution.

## Run the Job

1. Upload input file to HDFS:

```bash
hdfs dfs -mkdir -p /user/hadoop/input
hdfs dfs -put -f input.txt /user/hadoop/input/input.txt
hdfs dfs -ls /user/hadoop/input
```

2. Execute MapReduce:

```bash
./scripts/run_wordcount.sh /user/hadoop/input/input.txt /user/hadoop/output_wordcount
```

3. Read result from HDFS:

```bash
hdfs dfs -cat /user/hadoop/output_wordcount/part-r-00000
```

## Included Input / Output

- `input.txt`: A realistic multi-paragraph sample that exercises the tokenizer and normalization logic (punctuation, numbers, email/URL fragments, hyphenation, code-like snippets, repeated tokens).
- `output.txt`: A token-count listing produced using the same normalization rules as `WordCount` (lowercased, non-alphanumeric characters removed). The repository includes a precomputed `output.txt` matching `input.txt`.

To regenerate `output.txt` locally (without Hadoop), run the Java local generator that uses the same normalization rules:

```bash
# compile and run the LocalWordCount main
./mvnw -q compile exec:java -Dexec.mainClass=local.LocalWordCount -Dexec.cleanupDaemonThreads=false

# then view the regenerated output
cat output.txt | less
```

To produce the HDFS MapReduce output (runs the full Hadoop job against HDFS):

```bash
# upload input to HDFS (run on a node with Hadoop client available)
hdfs dfs -mkdir -p /user/hadoop/input
hdfs dfs -put -f input.txt /user/hadoop/input/input.txt

# run the MapReduce job (this script expects HDFS input and output paths)
./scripts/run_wordcount.sh /user/hadoop/input/input.txt /user/hadoop/output_wordcount

# read the result produced by the Hadoop job
hdfs dfs -cat /user/hadoop/output_wordcount/part-r-00000 | less
```

Note: `LocalWordCount` performs the same normalization as `WordCount` and writes a tab-separated `token\tcount` listing to `output.txt` in the project root. Use it when you want to validate tokenization/counting locally before running on a cluster.

## Design Patterns

This implementation uses two key design patterns to improve testability and maintainability:

### 1. Strategy Pattern (TextNormalizationStrategy)

The text normalization logic is pluggable via the `TextNormalizationStrategy` interface. The default implementation `AlphanumericLowercaseStrategy` converts text to lowercase and removes non-alphanumeric characters. This allows:

- Easy testing with different normalization rules
- Runtime strategy injection for different text preprocessing
- Clean separation of concern from the mapper logic

### 2. Factory Pattern (WordCountJobFactory)

Job configuration is abstracted behind the `WordCountJobFactory` interface. This enables:

- Testing the `run()` method without instantiating a real Hadoop Job
- Easy dependency injection for testing
- Decoupling of job setup from execution logic

## Submission Checklist

Include the following in your submission:

1. Java source file: `src/main/java/WordCount.java` (includes Strategy + Factory patterns)
2. JAR file: `target/wordcount-hadoop-1.0.0.jar` (build with `./mvnw clean package`)
3. Unit test file: `src/test/java/WordCountTest.java` (19 passing tests)
4. Input and output files: `input.txt` and `output.txt`
5. Three screenshots:
   - Running command and initial result
   - MapReduce processing to completion without errors
   - Output retrieval command and displayed result

## Screenshot Commands (Suggested)

Use these exact commands when capturing screenshots:

1. Run command screenshot:

```bash
./scripts/run_wordcount.sh /user/hadoop/input/input.txt /user/hadoop/output_wordcount
```

2. Process/progress screenshot:

Capture terminal lines that show map/reduce progress and final completion (for example `map 100% reduce 100%`).

3. Output screenshot:

```bash
hdfs dfs -cat /user/hadoop/output_wordcount/part-r-00000
```
