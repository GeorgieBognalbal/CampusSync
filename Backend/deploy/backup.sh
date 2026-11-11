#!/usr/bin/env bash
# Safe copy of the live database (works even while the server is using it).
# Keeps the last 14 daily backups.
set -euo pipefail
DB="${CAMPUSSYNC_DB:?CAMPUSSYNC_DB not set}"
DIR="$(dirname "$DB")/backups"
OUT="$DIR/campussync-$(date +%Y%m%d).db"
mkdir -p "$DIR"
python3 - "$DB" "$OUT" <<'PY'
import sqlite3, sys
src, dst = sqlite3.connect(sys.argv[1]), sqlite3.connect(sys.argv[2])
with dst:
    src.backup(dst)
src.close(); dst.close()
PY
ls -1t "$DIR"/campussync-*.db | tail -n +15 | xargs -r rm --
echo "Backup saved to $OUT"
