#!/usr/bin/env bash
# Runs the CampusSync backend on THIS laptop as background services (Arch / any systemd Linux).
#
#   ./install-laptop.sh                      server + 6-hour ingest (no public link)
#   ./install-laptop.sh my-name.ngrok-free.app    also start an ngrok public link
#   ./install-laptop.sh --uninstall          stop and remove everything
#
# No sudo needed. Your existing database (Backend/campussync.db) is kept.
set -euo pipefail

LAPTOP="$(cd "$(dirname "$0")" && pwd)"
DEPLOY="$(dirname "$LAPTOP")"
BACKEND="$(dirname "$DEPLOY")"
UNIT_DIR="$HOME/.config/systemd/user"
ENV_DIR="$HOME/.config/campussync"
ENV_FILE="$ENV_DIR/campussync.env"
UNITS=(campussync-server.service campussync-ingest.service campussync-ingest.timer campussync-ngrok.service)

step() { echo; echo "==> $*"; }

if [[ "${1:-}" == "--uninstall" ]]; then
    step "Stopping and removing CampusSync services"
    systemctl --user disable --now campussync-server.service campussync-ingest.timer campussync-ngrok.service 2>/dev/null || true
    for u in "${UNITS[@]}"; do rm -f "$UNIT_DIR/$u"; done
    systemctl --user daemon-reload
    echo "Removed. Your database and $ENV_FILE were kept."
    exit 0
fi

NGROK_DOMAIN="${1:-}"
NGROK_DOMAIN="${NGROK_DOMAIN#https://}"
NGROK_DOMAIN="${NGROK_DOMAIN%/}"

step "Checking port 5000"
if ss -ltn 2>/dev/null | grep -q ':5000 ' && ! systemctl --user is-active -q campussync-server.service; then
    echo "Something is already using port 5000 (probably 'python server.py' in a terminal)."
    echo "Stop it with Ctrl+C in that terminal, then run this script again."
    exit 1
fi

step "Python environment"
if [[ ! -x "$BACKEND/.venv/bin/python" ]]; then
    python -m venv "$BACKEND/.venv"
fi
"$BACKEND/.venv/bin/pip" install -q -r "$DEPLOY/requirements-server.txt"

step "Settings ($ENV_FILE)"
mkdir -p "$ENV_DIR"
if [[ -f "$ENV_FILE" ]]; then
    echo "Keeping existing settings (API key unchanged)."
else
    API_KEY="$("$BACKEND/.venv/bin/python" -c 'import secrets; print(secrets.token_urlsafe(32))')"
    cat > "$ENV_FILE" <<EOF
CAMPUSSYNC_API_KEY=$API_KEY
CAMPUSSYNC_DB=$BACKEND/campussync.db
EOF
fi
chmod 600 "$ENV_FILE"

step "Installing background services"
mkdir -p "$UNIT_DIR"
for u in campussync-server.service campussync-ingest.service campussync-ingest.timer; do
    sed "s|@BACKEND@|$BACKEND|g" "$LAPTOP/$u" > "$UNIT_DIR/$u"
done

if [[ -n "$NGROK_DOMAIN" ]]; then
    NGROK="$(command -v ngrok || true)"
    if [[ -z "$NGROK" ]]; then
        echo "ngrok is not installed. Install it (Arch: yay -S ngrok), run"
        echo "  ngrok config add-authtoken <your token>"
        echo "then run this script again. Continuing without a public link for now."
        NGROK_DOMAIN=""
    else
        # Newer ngrok uses --url, older versions use --domain.
        if "$NGROK" http --help 2>&1 | grep -q -- '--url'; then
            NGROK_CMD="$NGROK http 5000 --url=https://$NGROK_DOMAIN --log=stdout"
        else
            NGROK_CMD="$NGROK http 5000 --domain=$NGROK_DOMAIN --log=stdout"
        fi
        sed "s|@NGROK_CMD@|$NGROK_CMD|" "$LAPTOP/campussync-ngrok.service" > "$UNIT_DIR/campussync-ngrok.service"
    fi
fi

systemctl --user daemon-reload
systemctl --user enable --now campussync-server.service campussync-ingest.timer
# Pick up any changes to the service file (e.g. the log format) right away.
systemctl --user restart campussync-server.service
if [[ -n "$NGROK_DOMAIN" ]]; then
    systemctl --user enable --now campussync-ngrok.service
fi

step "Letting the services start at boot (before you log in)"
loginctl enable-linger "$USER" 2>/dev/null \
    && echo "Done." \
    || echo "Skipped (needs permission). They will still start whenever you log in."

step "Fetching posts now"
systemctl --user start campussync-ingest.service \
    || echo "WARNING: ingest failed. See: journalctl --user -u campussync-ingest"

sleep 2
API_KEY="$(grep '^CAMPUSSYNC_API_KEY=' "$ENV_FILE" | cut -d= -f2-)"
LAN_IP="$(ip -4 route get 1.1.1.1 2>/dev/null | awk '{for(i=1;i<=NF;i++) if($i=="src") print $(i+1)}')"

if curl -fsS http://localhost:5000/api/health >/dev/null 2>&1; then
    HEALTH="running"
else
    HEALTH="NOT responding yet (check: systemctl --user status campussync-server)"
fi

cat <<EOF

==================================================================
 CampusSync backend on this laptop: $HEALTH

 Put ONE of these in local.properties (yours and your teammates'),
 together with the key line, then Sync Now + Run:

EOF
if [[ -n "$NGROK_DOMAIN" ]]; then
    echo "   From anywhere (ngrok):     campussync.apiBaseUrl=https://$NGROK_DOMAIN"
fi
cat <<EOF
   Same Wi-Fi:                campussync.apiBaseUrl=http://${LAN_IP:-<laptop-ip>}:5000
   Your phone over USB:       campussync.apiBaseUrl=http://localhost:5000   (+ adb reverse tcp:5000 tcp:5000)
   Emulator on this laptop:   campussync.apiBaseUrl=http://10.0.2.2:5000

   campussync.apiKey=$API_KEY

 Everyday commands:
   systemctl --user status campussync-server     is it running?
   journalctl --user -u campussync-server -f     watch phones syncing
   systemctl --user restart campussync-server    restart after code changes
   systemctl --user start campussync-ingest      fetch posts right now
   $LAPTOP/keep-awake.sh       keep the laptop awake during a demo
   $LAPTOP/install-laptop.sh --uninstall         remove it all
==================================================================
EOF
