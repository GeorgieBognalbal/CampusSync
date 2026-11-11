#!/usr/bin/env bash
# One-time setup of the CampusSync backend on a fresh Debian/Ubuntu server
# (Google Cloud e2-micro, the Dell Wyse, or any small Linux box).
#
#   sudo ./setup.sh <duckdns-subdomain> <duckdns-token>
#   e.g. sudo ./setup.sh campussync-lspu 1234abcd-....
#
# Safe to run again: it keeps your existing API key and database.
set -euo pipefail

if [[ $EUID -ne 0 ]]; then echo "Run with sudo: sudo ./setup.sh <subdomain> <token>"; exit 1; fi
if [[ $# -ne 2 ]]; then echo "Usage: sudo ./setup.sh <duckdns-subdomain> <duckdns-token>"; exit 1; fi

SUBDOMAIN="${1%.duckdns.org}"
TOKEN="$2"
DOMAIN="$SUBDOMAIN.duckdns.org"
DEPLOY="$(cd "$(dirname "$0")" && pwd)"
BACKEND="$(dirname "$DEPLOY")"
USER_NAME="${SUDO_USER:-$(stat -c %U "$BACKEND")}"
DATA_DIR=/var/lib/campussync
ENV_FILE=/etc/campussync/campussync.env

step() { echo; echo "==> $*"; }

step "Installing system packages"
apt-get update -q
apt-get install -y -q python3-venv python3-pip git sqlite3 curl caddy

step "Adding 1 GB swap (helps the 1 GB e2-micro during installs)"
if ! swapon --show | grep -q .; then
    fallocate -l 1G /swapfile && chmod 600 /swapfile && mkswap /swapfile && swapon /swapfile
    grep -q '^/swapfile' /etc/fstab || echo '/swapfile none swap sw 0 0' >> /etc/fstab
else
    echo "Swap already on, skipping."
fi

step "Creating data folder and settings"
mkdir -p "$DATA_DIR" /etc/campussync
chown "$USER_NAME:$USER_NAME" "$DATA_DIR"
if [[ -f "$ENV_FILE" ]]; then
    echo "Keeping existing $ENV_FILE (API key unchanged)."
    sed -i "s/^DUCKDNS_SUBDOMAIN=.*/DUCKDNS_SUBDOMAIN=$SUBDOMAIN/; s/^DUCKDNS_TOKEN=.*/DUCKDNS_TOKEN=$TOKEN/" "$ENV_FILE"
else
    API_KEY="$(python3 -c 'import secrets; print(secrets.token_urlsafe(32))')"
    cat > "$ENV_FILE" <<EOF
CAMPUSSYNC_API_KEY=$API_KEY
CAMPUSSYNC_DB=$DATA_DIR/campussync.db
DUCKDNS_SUBDOMAIN=$SUBDOMAIN
DUCKDNS_TOKEN=$TOKEN
EOF
fi
chown "root:$USER_NAME" "$ENV_FILE"
chmod 640 "$ENV_FILE"

step "Installing Python packages"
sudo -u "$USER_NAME" python3 -m venv "$BACKEND/.venv"
sudo -u "$USER_NAME" "$BACKEND/.venv/bin/pip" install -q --upgrade pip
sudo -u "$USER_NAME" "$BACKEND/.venv/bin/pip" install -q -r "$DEPLOY/requirements-server.txt"

step "Installing background services"
for unit in "$DEPLOY"/systemd/*; do
    sed -e "s|@USER@|$USER_NAME|g" -e "s|@BACKEND@|$BACKEND|g" -e "s|@DEPLOY@|$DEPLOY|g" \
        "$unit" > "/etc/systemd/system/$(basename "$unit")"
done
chmod +x "$DEPLOY/backup.sh" "$DEPLOY/update.sh"
systemctl daemon-reload

step "Pointing $DOMAIN at this server"
systemctl enable --now duckdns.timer
systemctl start duckdns.service || echo "WARNING: DuckDNS update failed. Check the subdomain and token."

step "Setting up HTTPS (Caddy)"
sed "s|@DOMAIN@|$DOMAIN|g" "$DEPLOY/Caddyfile" > /etc/caddy/Caddyfile
systemctl enable caddy
systemctl restart caddy

step "Starting the server, the 6-hour ingest and the daily backup"
systemctl enable --now campussync-server.service
systemctl enable --now campussync-ingest.timer campussync-backup.timer

step "Fetching posts for the first time"
systemctl start campussync-ingest.service || echo "WARNING: first ingest failed. See: journalctl -u campussync-ingest"

API_KEY="$(grep '^CAMPUSSYNC_API_KEY=' "$ENV_FILE" | cut -d= -f2-)"
cat <<EOF

==================================================================
 CampusSync backend is set up.

 Test it (from any computer; HTTPS can take a minute the first time):
   curl https://$DOMAIN/api/health

 Put these two lines in local.properties on your laptop, then rebuild the app:
   campussync.apiBaseUrl=https://$DOMAIN
   campussync.apiKey=$API_KEY

 Useful commands:
   systemctl status campussync-server       is the server running?
   journalctl -u campussync-server -f       live server log
   journalctl -u campussync-ingest          last ingest results
   sudo $DEPLOY/update.sh                   pull new code from GitHub + restart
==================================================================
EOF
