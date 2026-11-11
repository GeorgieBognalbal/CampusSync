# Kaleo Backend

```
scraper.py       (Geor)      FetchRSS feeds -> raw posts
ingest.py        (Jairelle)  runs the scraper, saves posts into the database
ai_extractor.py  (Benedict)  raw posts -> Gemini -> clean text, dates, tags, urgency
db.py            (Jairelle)  SQLite storage, save_announcement()
server.py        (Jairelle)  GET /api/announcements/sync for the phones
```

Field formats are in [SCHEMA.md](SCHEMA.md).

## Run it

```bash
cd Backend
python -m venv .venv && source .venv/bin/activate     # Windows: .venv\Scripts\activate
pip install -r requirements.txt
python ingest.py           # fetch the real posts into the database
python server.py           # starts on port 5000
```

Optional: `python db.py seed` adds 4 sample posts for testing.

Test:
```bash
curl http://localhost:5000/api/health
curl -H "X-API-Key: dev-key-change-me" "http://localhost:5000/api/announcements/sync?since=0"
```

Set your own key with `export CAMPUSSYNC_API_KEY=something-secret` (Windows: `set CAMPUSSYNC_API_KEY=...`).

## Phone access

- USB: `adb reverse tcp:5000 tcp:5000`, then the app uses `http://localhost:5000` (the default).
- Wi-Fi: set `campussync.apiBaseUrl=http://<laptop-ip>:5000` in `local.properties`.

## Other commands

```bash
python db.py list        # show stored posts and whether AI processed them
```
