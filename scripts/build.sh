#!/usr/bin/env bash
set -euo pipefail

# Resolve repository root regardless of current working directory.
PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$PROJECT_DIR"

mkdir -p build/classes

# Build the project with Maven and copy the packaged JAR to the legacy build path.
./mvnw -q -DskipTests package
cp target/wordcount-hadoop-1.0.0.jar build/wordcount.jar

echo "Build complete: $PROJECT_DIR/build/wordcount.jar"
