#!/usr/bin/env bash
set -euo pipefail
BACKUP="${1:?Usage: restore-smoke.sh /absolute/backup/directory}"
BACKUP="$(cd "$BACKUP" && pwd)"
NAME="ladux-restore-$(date +%s)-$$"
cleanup() { docker rm -f "$NAME" >/dev/null 2>&1 || true; }
trap cleanup EXIT
(cd "$BACKUP" && sha256sum -c SHA256SUMS)
tar tzf "$BACKUP/uploads.tar.gz" >/dev/null
# Disposable database: no host ports and no production volume.
docker run --rm -d --name "$NAME" --network none --tmpfs /var/lib/postgresql/data -e POSTGRES_HOST_AUTH_METHOD=trust -e POSTGRES_DB=ladux_restore pgvector/pgvector:pg17 >/dev/null
READY=false
for attempt in {1..60}; do
  if docker exec "$NAME" pg_isready -U postgres -d ladux_restore >/dev/null 2>&1; then READY=true; break; fi
  sleep 1
done
[[ "$READY" == true ]] || { printf 'Restore database did not become ready\n' >&2; exit 1; }
docker exec -i "$NAME" pg_restore -U postgres -d ladux_restore --exit-on-error --no-owner --no-acl < "$BACKUP/database.dump"
docker exec "$NAME" psql -U postgres -d ladux_restore -v ON_ERROR_STOP=1 -c 'SELECT COUNT(*) AS orders FROM orders; SELECT COUNT(*) AS payments FROM payments; SELECT COUNT(*) AS products FROM products; SELECT version, success FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 3;'
printf 'Disposable restore passed. Check business totals/media references before declaring recovery verified.\n'
