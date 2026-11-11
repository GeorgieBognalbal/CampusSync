#!/usr/bin/env bash
# Pull the latest code from GitHub and restart the server.
#   sudo ./update.sh
set -euo pipefail
DEPLOY="$(cd "$(dirname "$0")" && pwd)"
BACKEND="$(dirname "$DEPLOY")"
USER_NAME="$(stat -c %U "$BACKEND")"
sudo -u "$USER_NAME" git -C "$BACKEND" pull
sudo -u "$USER_NAME" "$BACKEND/.venv/bin/pip" install -q -r "$DEPLOY/requirements-server.txt"
systemctl restart campussync-server
echo "Updated and restarted."
systemctl --no-pager status campussync-server | head -5
