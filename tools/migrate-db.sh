#!/usr/bin/env bash
# Run Flyway migrations via MigrateMain (Gradle 9-safe; no flyway plugin).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "${ROOT}"

DB_URL="${DB_URL:-jdbc:postgresql://localhost:5433/l2jdb}"
DB_USER="${DB_USER:-brproject}"
DB_PASSWORD="${DB_PASSWORD:-brproject}"

echo "Migrating database..."
echo "  url=${DB_URL}"
echo "  user=${DB_USER}"

./gradlew :db-migrate:run \
  -PdbUrl="${DB_URL}" \
  -PdbUser="${DB_USER}" \
  -PdbPassword="${DB_PASSWORD}" \
  --no-daemon

echo "Flyway migrate finished."
