# Hosting the CampusSync backend on your laptop

The server starts by itself (no terminal to keep open), fetches posts every 6 hours,
and restarts if it crashes. Phones can sync while the laptop is **on, awake and online**.
When it's off, the app still shows the posts it already saved.

## 1. Install (one time)

Stop any `python server.py` you have running in a terminal (Ctrl+C) first, then:

```bash
cd ~/StudioProjects/CampusSync/Backend/deploy/laptop
chmod +x install-laptop.sh keep-awake.sh
./install-laptop.sh
```

It prints the `local.properties` lines (server address + a **new secret API key**).
Put them in `local.properties`, then **Sync Now** and **Run ▶** in Android Studio.

## 2. Optional: a public link with ngrok (teammates anywhere)

Without this, only devices on your Wi-Fi (or your phone over USB) can sync.

1. Sign up at <https://ngrok.com> (free, no card).
2. In the ngrok dashboard, open **Domains** and claim your free static domain
   (something like `xxxx.ngrok-free.app`). Copy your **Authtoken** too.
3. Install and log in:
   ```bash
   yay -S ngrok
   ngrok config add-authtoken <your-authtoken>
   ```
4. Run the installer again with your domain:
   ```bash
   ./install-laptop.sh xxxx.ngrok-free.app
   ```
5. Everyone puts this in their `local.properties` (share the key privately, not on GitHub):
   ```
   campussync.apiBaseUrl=https://xxxx.ngrok-free.app
   campussync.apiKey=<the key the installer printed>
   ```

Check it from any browser: `https://xxxx.ngrok-free.app/api/health`.
(ngrok's free plan has monthly limits and may change; syncs are tiny, but check the dashboard.)

## Everyday commands

| I want to… | Command |
|---|---|
| Check the server is running | `systemctl --user status campussync-server` |
| Watch phones syncing live | `journalctl --user -u campussync-server -f` |
| Restart after pulling new code | `systemctl --user restart campussync-server` |
| Fetch posts right now | `systemctl --user start campussync-ingest` |
| See the last ingest results | `journalctl --user -u campussync-ingest -n 30` |
| Check the ngrok link | `systemctl --user status campussync-ngrok` |
| Keep the laptop awake for a demo (lid closed too) | `./keep-awake.sh` (Ctrl+C to stop) |
| Remove everything | `./install-laptop.sh --uninstall` |

Your API key and database path are in `~/.config/campussync/campussync.env`.

## If something goes wrong

- **"Couldn't sync" on a phone**: is the laptop awake? Run the status command above.
  Same Wi-Fi? IP changed? (re-run the installer, it prints the current IP).
- **Port 5000 already in use**: a `python server.py` is still running in some terminal. Close it.
- **Sync says "Bad response" only through ngrok**: tell Jairelle; ngrok's free plan can show
  a warning page instead of the API, which needs a one-line header in the app.
