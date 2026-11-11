"""
CampusSync backend database (owner: Jairelle).

This is the ONLY file that talks to the database directly.
Everyone else uses these functions:

    ingest.py  (runs Geor's scraper) -> save_announcement({...raw fields...})
    Benedict   (ai_extractor.py)     -> get_unprocessed(), save_announcement({...AI fields...})
    server.py                        -> get_changed_since(since, limit)

See SCHEMA.md for the meaning and format of every field.

Command line:
    python db.py init     # create the database file
    python db.py seed     # add sample announcements for testing
    python db.py list     # print what's stored
"""

import json
import os
import sqlite3
import sys
import time
from contextlib import contextmanager

DB_PATH = os.environ.get(
    "CAMPUSSYNC_DB",
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "campussync.db"),
)

ALLOWED_CATEGORIES = {
    "Suspension", "Scholarship", "Enrollment", "Events", "Exams", "General",
}
ALLOWED_URGENCY = {"High", "Medium", "Low"}

# Fields that are stored as JSON lists inside a TEXT column.
JSON_FIELDS = ("image_urls", "important_dates", "categories")

# Every column a caller may set. updated_at / deleted are managed here.
WRITABLE_FIELDS = (
    "page_name", "permalink", "raw_caption", "image_urls",
    "clean_text", "key_takeaway", "important_dates", "categories",
    "urgency", "created_at",
)

SCHEMA_SQL = """
CREATE TABLE IF NOT EXISTS announcements (
    id              TEXT PRIMARY KEY,
    page_name       TEXT NOT NULL,
    permalink       TEXT,
    raw_caption     TEXT,
    image_urls      TEXT NOT NULL DEFAULT '[]',
    clean_text      TEXT,
    key_takeaway    TEXT,
    important_dates TEXT NOT NULL DEFAULT '[]',
    categories      TEXT NOT NULL DEFAULT '[]',
    urgency         TEXT,
    created_at      INTEGER NOT NULL,
    updated_at      INTEGER NOT NULL,
    deleted         INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_announcements_updated_at
    ON announcements(updated_at);
"""


def now_ms() -> int:
    return int(time.time() * 1000)


@contextmanager
def connect():
    # Ingestion and phone sync can touch the database at the same time. WAL lets
    # readers continue during writes; the busy timeout gives short writes time to finish.
    conn = sqlite3.connect(DB_PATH, timeout=30)
    conn.execute("PRAGMA busy_timeout = 30000")
    conn.row_factory = sqlite3.Row
    try:
        yield conn
        conn.commit()
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()


def init_db() -> None:
    with connect() as conn:
        # WAL allows sync requests to read while ingestion commits new rows.
        conn.execute("PRAGMA journal_mode = WAL")
        conn.executescript(SCHEMA_SQL)


# ---------- validation ----------

def _normalize_categories(value):
    if value is None:
        return []
    if isinstance(value, str):
        value = [value]
    result = []
    for c in value:
        c = str(c).strip().lstrip("#").strip()
        match = next((a for a in ALLOWED_CATEGORIES if a.lower() == c.lower()), None)
        if match is None:
            raise ValueError(
                f"Unknown category {c!r}. Allowed: {sorted(ALLOWED_CATEGORIES)}")
        if match not in result:
            result.append(match)
    return result


def _normalize_urgency(value):
    if value is None:
        return None
    v = str(value).strip().capitalize()
    if v not in ALLOWED_URGENCY:
        raise ValueError(f"Unknown urgency {value!r}. Allowed: {sorted(ALLOWED_URGENCY)}")
    return v


def _normalize_dates(value):
    if value is None:
        return []
    result = []
    for d in value:
        if not isinstance(d, dict) or "date" not in d:
            raise ValueError(
                'important_dates must be a list like [{"label": "Deadline", "date": "2026-12-05"}]')
        date = str(d["date"])
        if len(date) != 10 or date[4] != "-" or date[7] != "-":
            raise ValueError(f"Date {date!r} must be YYYY-MM-DD")
        result.append({"label": str(d.get("label", "")), "date": date})
    return result


def _clean(data: dict) -> dict:
    out = {}
    for key in WRITABLE_FIELDS:
        if key not in data:
            continue
        value = data[key]
        if key == "categories":
            value = _normalize_categories(value)
        elif key == "urgency":
            value = _normalize_urgency(value)
        elif key == "important_dates":
            value = _normalize_dates(value)
        elif key == "image_urls":
            value = [str(u) for u in (value or [])]
        elif key == "created_at":
            value = int(value)
        out[key] = value
    return out


def _next_updated_at(conn) -> int:
    """A strictly increasing timestamp, so phones paging with `since` never
    skip a row that shares the same millisecond as another."""
    row = conn.execute("SELECT MAX(updated_at) FROM announcements").fetchone()
    last = row[0] or 0
    return max(now_ms(), last + 1)


# ---------- public functions ----------

def save_announcement(data: dict) -> str:
    """Insert a new announcement or update an existing one (matched by `id`).

    Only the fields you pass are changed, so Geor can save the raw post first
    and Benedict can add the AI fields later without erasing anything.

    If nothing actually changed, the row is left alone, so re-scraping the same
    post does NOT make every phone download it again.

    Returns "inserted", "updated" or "unchanged".
    """
    if not data.get("id"):
        raise ValueError("save_announcement() needs an 'id'")
    post_id = str(data["id"])
    fields = _clean(data)

    with connect() as conn:
        conn.execute("BEGIN IMMEDIATE")
        existing = conn.execute(
            "SELECT * FROM announcements WHERE id = ?", (post_id,)).fetchone()

        stored = {k: (json.dumps(v, ensure_ascii=False) if k in JSON_FIELDS else v)
                  for k, v in fields.items()}

        if existing is not None:
            changed = {k: v for k, v in stored.items() if existing[k] != v}
            if not changed and not existing["deleted"]:
                return "unchanged"
            stored = changed

        stamp = _next_updated_at(conn)

        if existing is None:
            if not fields.get("page_name"):
                raise ValueError("A new announcement needs 'page_name'")
            stored.setdefault("created_at", stamp)
            stored["id"] = post_id
            stored["updated_at"] = stamp
            stored["deleted"] = 0
            cols = ", ".join(stored)
            marks = ", ".join("?" for _ in stored)
            conn.execute(f"INSERT INTO announcements ({cols}) VALUES ({marks})",
                         list(stored.values()))
            return "inserted"
        else:
            stored["updated_at"] = stamp
            stored["deleted"] = 0
            sets = ", ".join(f"{k} = ?" for k in stored)
            conn.execute(f"UPDATE announcements SET {sets} WHERE id = ?",
                         list(stored.values()) + [post_id])
            return "updated"


def mark_deleted(post_id: str) -> None:
    """Hide a post. Phones remove it on their next sync."""
    with connect() as conn:
        conn.execute("BEGIN IMMEDIATE")
        stamp = _next_updated_at(conn)
        # "AND deleted = 0" so hiding an already-hidden post doesn't re-send it to phones.
        conn.execute("UPDATE announcements SET deleted = 1, updated_at = ? "
                     "WHERE id = ? AND deleted = 0", (stamp, post_id))


def get_unprocessed(limit: int = 20) -> list:
    """Posts the AI hasn't processed yet (for Benedict)."""
    with connect() as conn:
        rows = conn.execute(
            "SELECT * FROM announcements WHERE clean_text IS NULL AND deleted = 0 "
            "ORDER BY created_at ASC LIMIT ?", (limit,)).fetchall()
    return [_row_to_dict(r) for r in rows]


def get_changed_since(since: int, limit: int = 100):
    """Rows with updated_at > since, oldest change first.
    Returns (rows, has_more)."""
    with connect() as conn:
        rows = conn.execute(
            "SELECT * FROM announcements WHERE updated_at > ? "
            "ORDER BY updated_at ASC LIMIT ?", (since, limit + 1)).fetchall()
    has_more = len(rows) > limit
    return [_row_to_dict(r) for r in rows[:limit]], has_more


def _row_to_dict(row) -> dict:
    d = dict(row)
    for key in JSON_FIELDS:
        d[key] = json.loads(d[key]) if d[key] else []
    d["deleted"] = bool(d["deleted"])
    return d


# ---------- sample data for testing ----------

def seed_sample_data() -> None:
    day = 24 * 3600 * 1000
    t = now_ms()
    samples = [
        {
            "id": "sample-001",
            "page_name": "LSPU Official",
            "permalink": "https://www.facebook.com/LSPUOfficial",
            "raw_caption": "ANNOUNCEMENT: Classes are suspended tomorrow, all levels, due to Typhoon Signal No. 2. Walang pasok! Stay safe.",
            "clean_text": "Classes are suspended tomorrow in all levels due to Typhoon Signal No. 2. Walang pasok.",
            "key_takeaway": "Walang pasok bukas (all levels) dahil sa Signal No. 2.",
            "important_dates": [{"label": "Suspension", "date": time.strftime("%Y-%m-%d", time.localtime((t + day) / 1000))}],
            "categories": ["Suspension"],
            "urgency": "High",
            "created_at": t - 1 * 3600 * 1000,
        },
        {
            "id": "sample-002",
            "page_name": "SPCC OSAS",
            "permalink": "https://www.facebook.com/SPCC.OSAS",
            "raw_caption": "SCHOLARSHIP: Requirements - certificate of grades, certificate of indigency, 2x2 ID photo, application form. Submit to OSAS before Dec 5.",
            "image_urls": ["https://example.com/scholarship-poster.jpg"],
            "clean_text": "Scholarship requirements: certificate of grades, certificate of indigency, 2x2 ID photo, and a filled-out application form. Submit to OSAS before December 5.",
            "key_takeaway": "Submit scholarship requirements to OSAS before Dec 5.",
            "important_dates": [{"label": "Submission deadline", "date": "2026-12-05"}],
            "categories": ["Scholarship"],
            "urgency": "Medium",
            "created_at": t - 2 * day,
        },
        {
            "id": "sample-003",
            "page_name": "Office of the Registrar",
            "permalink": "https://www.facebook.com/LSPUSPCCRegistrar/",
            "raw_caption": "Midterm examinations: Nov 24-28. Bring your exam permit.",
            "clean_text": "Midterm examinations are scheduled for November 24 to 28. Bring your exam permit.",
            "key_takeaway": "Midterms Nov 24–28 — bring your exam permit.",
            "important_dates": [{"label": "Midterms start", "date": "2026-11-24"},
                                {"label": "Midterms end", "date": "2026-11-28"}],
            "categories": ["Exams"],
            "urgency": "Medium",
            "created_at": t - 3 * day,
        },
        {
            # Raw post with no AI fields yet -> shows up in get_unprocessed()
            "id": "sample-004",
            "page_name": "LSPU SPCC Sports & Cultural Affairs",
            "permalink": "https://www.facebook.com/LSPUSanPabloSCA/",
            "raw_caption": "Intramurals 2026 opening ceremony this Friday at the gym, 8AM!",
            "created_at": t - 4 * day,
        },
    ]
    for s in samples:
        save_announcement(s)


if __name__ == "__main__":
    cmd = sys.argv[1] if len(sys.argv) > 1 else "init"
    init_db()
    if cmd == "seed":
        seed_sample_data()
        print(f"Seeded sample data into {DB_PATH}")
    elif cmd == "list":
        rows, _ = get_changed_since(0, 1000)
        for r in rows:
            flag = " [deleted]" if r["deleted"] else ""
            ai = "AI done" if r["clean_text"] else "needs AI"
            print(f"{r['id']:<14} {r['page_name']:<38} {ai}{flag}")
        print(f"{len(rows)} announcement(s)")
    else:
        print(f"Database ready at {DB_PATH}")
