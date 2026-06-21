#!/usr/bin/env bash
set -euo pipefail

# Resolve repository root regardless of current working directory.
PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$PROJECT_DIR"

mkdir -p build/classes

# Compile with Hadoop libraries from the active Hadoop installation.
javac -classpath "$(hadoop classpath)" -d build/classes src/WordCount.java

# Build runnable JAR.
jar -cvf build/wordcount.jar -C build/classes .

echo "Build complete: $PROJECT_DIR/build/wordcount.jar"
