#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
project_dir="$(cd "$script_dir/.." && pwd)"
classpath_file="$project_dir/target/jmh-classpath.txt"

cd "$project_dir"

./mvnw -q -DskipTests test-compile dependency:build-classpath -Dmdep.pathSeparator=: -Dmdep.outputFile="$classpath_file"

java -cp "target/test-classes:target/classes:$(cat "$classpath_file")" benchmarks.WordCountBenchmark "$@"
