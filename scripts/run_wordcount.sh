#!/usr/bin/env bash
set -euo pipefail

# Expect explicit HDFS input file and HDFS output directory.
if [[ $# -ne 2 ]]; then
  echo "Usage: $0 <hdfs_input_file> <hdfs_output_dir>"
  echo "Example: $0 /user/hadoop/input/input.txt /user/hadoop/output_wordcount"
  exit 1
fi

INPUT_PATH="$1"
OUTPUT_PATH="$2"
PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
JAR_PATH="$PROJECT_DIR/build/wordcount.jar"

if [[ ! -f "$JAR_PATH" ]]; then
  echo "JAR not found at $JAR_PATH"
  echo "Run scripts/build.sh first."
  exit 1
fi

# Remove stale output dir because Hadoop jobs fail if output already exists.
hdfs dfs -rm -r -f "$OUTPUT_PATH" || true
hadoop jar "$JAR_PATH" "$INPUT_PATH" "$OUTPUT_PATH"

echo "MapReduce completed. Output: $OUTPUT_PATH"
