#!/usr/bin/env bash
set -euo pipefail
umask 077
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${LADUX_ENV_FILE:-$ROOT/.env.production}"
BACKUP_ROOT="${LADUX_BACKUP_DIR:?Set LADUX_BACKUP_DIR to a protected backup directory}"
STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
DEST="$BACKUP_ROOT/$STAMP"
mkdir -p "$DEST"
COMPOSE=(docker compose --env-file "$ENV_FILE" -f "$ROOT/backend/docker-compose.prod.yml")
APP_WAS_RUNNING="$("${COMPOSE[@]}" ps --status running -q app)"
resume() { if [[ -n "$APP_WAS_RUNNING" ]]; then "${COMPOSE[@]}" start app >/dev/null; fi; }
trap resume EXIT
# Quiesce writes so database and media are captured as one consistent set.
"${COMPOSE[@]}" stop app >/dev/null
"${COMPOSE[@]}" exec -T postgres sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc' > "$DEST/database.dump"
"${COMPOSE[@]}" run --rm -T --no-deps --entrypoint sh app -c 'tar czf - -C /app/uploads .' > "$DEST/uploads.tar.gz"
test -s "$DEST/database.dump"
test -s "$DEST/uploads.tar.gz"
(cd "$DEST" && sha256sum database.dump uploads.tar.gz > SHA256SUMS)
printf 'Backup complete: %s\nCopy this directory to encrypted off-host storage.\n' "$DEST"
