# Deploying the CampusSync backend to Google Cloud (free)

Result: the backend runs 24/7 at `https://<your-name>.duckdns.org`, fetches new
posts every 6 hours, backs up the database daily, and phones sync from anywhere.
No laptop, no `adb reverse`.

What runs on the server:

| Service | What it does |
|---|---|
| `campussync-server` | gunicorn + Flask `server.py` on 127.0.0.1:5000 |
| `caddy` | Free HTTPS certificate, forwards the internet to the server |
| `campussync-ingest.timer` | `ingest.py` every 6 hours (+ `ai_extractor.py` once it's ready) |
| `campussync-backup.timer` | Daily database backup, keeps 14 days |
| `duckdns.timer` | Keeps your free domain pointed at the server's IP |

---

## 1. Create the free server (about 15 minutes)

1. Go to <https://console.cloud.google.com>, sign in, and add your card when asked
   (it is for verification; the e2-micro below is free).
2. **Set a budget alert first:** Billing → Budgets & alerts → Create budget → amount **$1**.
   You'll get an email if anything ever starts costing money.
3. Compute Engine → VM instances → **Create instance** (enable the API if asked). Use exactly:
   - **Name:** `campussync`
   - **Region:** `us-central1` (Iowa), `us-west1` (Oregon) or `us-east1` (South Carolina).
     **Only these 3 are free.** Do not pick Asia even though it's closer.
   - **Machine type:** `e2-micro`. Any other type costs money.
   - **Boot disk:** Change → **Debian 12**, disk type **Standard persistent disk**
     (not SSD / balanced), size **30 GB**.
   - **Firewall:** tick **Allow HTTP traffic** and **Allow HTTPS traffic**.
   - Leave everything else as default. Don't reserve a static IP (DuckDNS handles a changing IP).
4. Click **Create**. When it's running, click **SSH** next to it. A terminal opens in your browser.

## 2. Get a free domain (2 minutes)

1. Go to <https://www.duckdns.org> and sign in (Google or GitHub).
2. Type a subdomain, e.g. `campussync-lspu`, and click **add domain**.
3. Copy your **token** (shown at the top of the page). Keep it private.

## 3. Install CampusSync (one command)

In the SSH window:

```bash
sudo apt-get update && sudo apt-get install -y git
git clone https://github.com/GeorgieBognalbal/CampusSync.git
cd CampusSync/Backend/deploy
sudo ./setup.sh campussync-lspu YOUR-DUCKDNS-TOKEN
```

At the end it prints your **server URL** and a **new secret API key**.

Check it from your laptop's browser:
`https://campussync-lspu.duckdns.org/api/health` should show `"status": "online"`.
(The first HTTPS request can take a minute while Caddy gets the certificate.)

## 4. Point the app at the server

In `local.properties` on your laptop (this file is NOT pushed to GitHub):

```
campussync.apiBaseUrl=https://campussync-lspu.duckdns.org
campussync.apiKey=<the key setup.sh printed>
```

Then in Android Studio: **Sync Now**, **Run ▶**. The phone now syncs over the internet.

Share the URL and key with teammates **privately** (chat, not GitHub).

---

## Day-to-day

| I want to… | Command (in the SSH window) |
|---|---|
| Check the server is running | `systemctl status campussync-server` |
| Watch phones syncing live | `journalctl -u campussync-server -f` |
| See the last ingest results | `journalctl -u campussync-ingest -n 30` |
| Fetch posts right now | `sudo systemctl start campussync-ingest` |
| Get the newest code from GitHub | `sudo ~/CampusSync/Backend/deploy/update.sh` |
| See when timers run next | `systemctl list-timers 'campussync*'` |
| List stored posts | `cd ~/CampusSync/Backend && sudo -u $USER env $(sudo cat /etc/campussync/campussync.env) .venv/bin/python db.py list` |

**When Benedict's `ai_extractor.py` is ready:** in
`/etc/systemd/system/campussync-ingest.service`, remove the `#` in front of the
`ExecStartPost=` line and add his Gemini key to `/etc/campussync/campussync.env`, then run
`sudo systemctl daemon-reload`.

## Staying free

- Keep **one** e2-micro, in us-central1 / us-west1 / us-east1, with a standard disk ≤ 30 GB.
- The free tier includes 1 GB/month of outgoing traffic. Syncs are a few KB to a few
  hundred KB each, so this covers thousands of syncs. The budget alert will warn you first.
- If you ever delete the VM, also delete its disk (and any reserved IP) so nothing is left billing.

## Files here

```
setup.sh                 one-time install (safe to re-run)
update.sh                git pull + restart
backup.sh                daily database backup (run by the timer)
Caddyfile                HTTPS config template
requirements-server.txt  Flask + gunicorn
systemd/                 the background services and timers
```
