"""
Kaleo ingestion (owner: Jairelle).

Runs Geor's scraper and saves every post into the database, so the sync API
can serve it without scraping on every request.

    python ingest.py          # run once (put this on a 6-hour schedule later)

Afterwards, run Benedict's ai_extractor.py to fill in the AI fields.
"""

import hashlib
import re
import sys
from datetime import datetime

import db
from scraper import fetch_all_posts


def _iso_to_ms(value):
    """Geor's scraper gives ISO 8601 timestamps; the database uses epoch ms."""
    if not value:
        return None
    try:
        return int(datetime.fromisoformat(value.replace("Z", "+00:00")).timestamp() * 1000)
    except ValueError:
        return None


def _post_id(post: dict) -> str:
    if post.get("post_id"):
        return str(post["post_id"])
    # No guid/link in the feed: build a stable id from the content instead.
    basis = f"{post.get('source')}|{post.get('timestamp')}|{post.get('raw_caption')}"
    return "hash-" + hashlib.sha1(basis.encode("utf-8")).hexdigest()[:16]


# FetchRSS appends this tag to every post; it is not part of the real caption.
_FETCHRSS_TAG = re.compile(r"\(\s*Feed generated with FetchRSS\s*\)", re.IGNORECASE)


def is_fetchrss_notice(post: dict) -> bool:
    """FetchRSS sometimes puts its own messages in a feed (e.g. "This Feed is deleted
    by its creator"). Those link to fetchrss.com instead of a Facebook post."""
    for key in ("post_url", "post_id"):
        value = (post.get(key) or "").lower()
        if "fetchrss.com" in value and "facebook.com" not in value:
            return True
    return False


def clean_caption(text: str) -> str:
    return re.sub(r"\s+", " ", _FETCHRSS_TAG.sub("", text or "")).strip()


def to_announcement(post: dict) -> dict:
    """Map Geor's field names to the schema in SCHEMA.md."""
    record = {
        "id": _post_id(post),
        "page_name": post.get("source") or "Unknown Facebook Page",
        "permalink": post.get("post_url") or post.get("page_url"),
        # May be empty for image-only posts; the AI reads the text from the images.
        "raw_caption": clean_caption(post.get("raw_caption") or post.get("title") or ""),
        "image_urls": post.get("image_urls") or [],
    }
    created_at = _iso_to_ms(post.get("timestamp"))
    if created_at is not None:
        record["created_at"] = created_at
    return record


def run() -> int:
    db.init_db()
    result = fetch_all_posts()
    counts = {"inserted": 0, "updated": 0, "unchanged": 0, "failed": 0, "notices": 0}

    for post in result["announcements"]:
        if is_fetchrss_notice(post):
            counts["notices"] += 1
            print(f"[SKIP] {post.get('source')}: FetchRSS notice, not a post: "
                  f"{(post.get('raw_caption') or '')[:80]}", file=sys.stderr)
            db.mark_deleted(_post_id(post))   # hide it if an older run saved it
            continue
        try:
            counts[db.save_announcement(to_announcement(post))] += 1
        except ValueError as exc:
            counts["failed"] += 1
            print(f"[SKIP] {post.get('source')}: {exc}", file=sys.stderr)

    print(f"Ingest done: {counts['inserted']} new, {counts['updated']} updated, "
          f"{counts['unchanged']} unchanged, {counts['failed']} failed, "
          f"{counts['notices']} FetchRSS notice(s) skipped, "
          f"{len(result['errors'])} feed error(s).")
    return 0 if result["announcements"] or not result["errors"] else 1


if __name__ == "__main__":
    sys.exit(run())
