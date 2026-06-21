#!/usr/bin/env bash
set -euo pipefail

# Resolve repository root to keep paths stable for docker volume mounts.
PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$PROJECT_DIR"

# Tooling pre-flight checks keep failures actionable for new contributors.
if [[ ! -x "$PROJECT_DIR/mvnw" ]]; then
  echo "The Maven wrapper (mvnw) is required to build the jar for the local containerized test."
  exit 1
fi

if [[ -z "$(command -v docker || true)" ]]; then
  echo "Docker is required for local containerized execution."
  exit 1
fi

if [[ -z "$(command -v docker-compose || true)" && -z "$(docker compose version >/dev/null 2>&1 && echo yes || true)" ]]; then
  echo "Docker Compose is required."
  exit 1
fi

./mvnw -q -DskipTests clean package

# Support both docker-compose (v1) and docker compose (v2 plugin).
if command -v docker-compose >/dev/null 2>&1; then
  COMPOSE_CMD="docker-compose"
else
  COMPOSE_CMD="docker compose"
fi

# Reset any partially started Hadoop stack before bringing it back up.
$COMPOSE_CMD -f docker-compose.hadoop.yml down -v --remove-orphans >/dev/null 2>&1 || true

$COMPOSE_CMD -f docker-compose.hadoop.yml up -d

echo "Waiting for Hadoop services to become ready..."
ready=0
for _ in {1..30}; do
  # Namenode readiness is used as the cluster health gate.
  if $COMPOSE_CMD -f docker-compose.hadoop.yml exec -T namenode hdfs dfsadmin -report >/dev/null 2>&1; then
    ready=1
    break
  fi
  sleep 5
done

if [[ "$ready" -ne 1 ]]; then
  echo "Hadoop services did not become ready in time."
  exit 1
fi

# Run an end-to-end smoke test entirely inside the namenode container.
$COMPOSE_CMD -f docker-compose.hadoop.yml exec -T namenode bash --noprofile --norc -lc '
  set -e
  hdfs dfs -mkdir -p /user/root/input
  hdfs dfs -put -f /workspace/input.txt /user/root/input/input.txt
  hdfs dfs -rm -r -f /user/root/output_wordcount || true
  hadoop jar /workspace/target/wordcount-hadoop-1.0.0.jar /user/root/input/input.txt /user/root/output_wordcount
  hdfs dfs -cat /user/root/output_wordcount/part-r-00000
'

echo "Containerized run completed successfully."
echo "Use '$COMPOSE_CMD -f docker-compose.hadoop.yml down -v' to stop and clean up."
