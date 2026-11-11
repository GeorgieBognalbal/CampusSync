"""Fills in the AI fields (clean text, takeaway, dates, categories, urgency) using Gemini."""

from __future__ import annotations

import argparse
import html
import json
import os
import re
import sys
import time
import unicodedata
import urllib.request
from datetime import datetime, timedelta, timezone
from pathlib import Path
from typing import Any

from dotenv import load_dotenv

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))

import db  # noqa: E402

DEFAULT_MODEL = "gemini-3.5-flash-lite"
PH_TIME = timezone(timedelta(hours=8), "PHT")
MAX_IMAGES = 3
MAX_IMAGE_BYTES = 7 * 1024 * 1024

CATEGORIES: dict[str, str] = {
    "Suspension": "classes or work suspended or cancelled, campus closures, no office transactions",
    "Scholarship": "scholarships or financial aid that STUDENTS can apply for or must act on "
                   "(news about faculty or staff scholarships is General)",
    "Enrollment": "admission, enrollment, registration, adding or dropping subjects, grades, "
                  "school records, registrar requirements",
    "Events": "activities, contests, seminars, org recruitment, sports and cultural events",
    "Exams": "exam schedules, midterms, finals, exam permits and exam reminders",
    "General": "everything else: advisories, rules, dress code, services, price lists, "
               "mental health tips, news, greetings",
}
URGENCY_LEVELS = ("High", "Medium", "Low")

SYSTEM_INSTRUCTION = f"""
You read announcements that a Philippine state university (LSPU, San Pablo City Campus)
and related offices post on Facebook, and turn each one into structured data for a
student app. Posts may be in English, Filipino or Taglish. Some posts are only an image
(an infographic or poster); read all the text in the images.

Fill every field:

clean_text: the full announcement as clean, readable plain text, in its original language.
  Keep the meaning and every important detail (dates, times, places, requirements, links).
  Remove decorative emojis and the hashtag list at the end. If the images contain
  announcement text, include it after the caption, starting with "From the image:".

key_takeaway: ONE sentence in English, at most 25 words, saying what a student needs to
  know or do. Example: "No classes on Nov 12 at all levels due to the typhoon."

important_dates: the dates that matter to students: deadlines, event dates, effective
  dates, suspension dates. Use YYYY-MM-DD. Resolve words like "tomorrow" or "this Friday"
  using the post time given below. A date range becomes two entries (start and end). If a
  time is given, put it in the label, e.g. "Application deadline, 11:59 PM". Do NOT invent
  a date when the post only says something vague like "until the end of the semester";
  leave it out. Use an empty list if there are no such dates.

categories: 1 to 3 categories, chosen ONLY from this list:
{chr(10).join(f"  {name}: {meaning}" for name, meaning in CATEGORIES.items())}

urgency, compared with TODAY's date given below:
  High: a class or work suspension that is today or upcoming; an emergency or safety
        warning; or a deadline or required action within the next 3 days.
  Medium: a deadline, required action or event within the next 14 days; or a new rule
        or policy that is now in effect.
  Low: news, greetings, events or deadlines that have already passed, and anything that
        needs no action from students.

urgency_reason: a short phrase explaining the urgency, e.g. "deadline in 2 days".
""".strip()

RESPONSE_SCHEMA: dict[str, Any] = {
    "type": "object",
    "properties": {
        "clean_text": {"type": "string"},
        "key_takeaway": {"type": "string"},
        "important_dates": {
            "type": "array",
            "items": {
                "type": "object",
                "properties": {
                    "label": {"type": "string"},
                    "date": {"type": "string", "description": "YYYY-MM-DD"},
                },
                "required": ["label", "date"],
            },
        },
        "categories": {"type": "array", "items": {"type": "string", "enum": list(CATEGORIES)}},
        "urgency": {"type": "string", "enum": list(URGENCY_LEVELS)},
        "urgency_reason": {"type": "string"},
    },
    "required": ["clean_text", "key_takeaway", "important_dates", "categories",
                 "urgency", "urgency_reason"],
}


def clean_caption(raw: str | None) -> str:
    if not raw:
        return ""
    text = html.unescape(raw)
    text = re.sub(r"<br\s*/?>", "\n", text, flags=re.IGNORECASE)
    text = re.sub(r"\(\s*Feed generated with.*?FetchRSS.*?\)", "", text, flags=re.IGNORECASE | re.DOTALL)
    text = re.sub(r"<[^>]+>", "", text)
    # turns facebook "bold" letters (𝗧𝗲𝘅𝘁) into normal text
    text = unicodedata.normalize("NFKC", text)
    text = re.sub(r"[ \t]+\n", "\n", text)
    text = re.sub(r"\n{3,}", "\n\n", text)
    return text.strip()


def posted_time(created_at_ms: Any) -> datetime | None:
    try:
        return datetime.fromtimestamp(int(created_at_ms) / 1000, PH_TIME)
    except (TypeError, ValueError, OSError):
        return None


def download_images(urls: list[str]) -> list[tuple[bytes, str]]:
    images: list[tuple[bytes, str]] = []
    for url in (urls or [])[:MAX_IMAGES]:
        try:
            request = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0 (CampusSync extractor)"})
            with urllib.request.urlopen(request, timeout=20) as response:
                data = response.read(MAX_IMAGE_BYTES + 1)
                mime = response.headers.get_content_type() or "image/jpeg"
            if len(data) > MAX_IMAGE_BYTES or not mime.startswith("image/"):
                print(f"    skipped an image ({mime}, {len(data)} bytes)")
                continue
            images.append((data, mime))
        except Exception as exc:
            print(f"    could not download an image: {exc}")
    return images


def ask_gemini(client: Any, model: str, page_name: str, caption: str,
               posted_at: datetime | None, images: list[tuple[bytes, str]]) -> dict[str, Any]:
    from google.genai import errors, types

    today = datetime.now(PH_TIME)
    details = [
        f"TODAY: {today:%A, %B %d, %Y}",
        f"POSTED: {posted_at:%A, %B %d, %Y %I:%M %p} (Philippine time)" if posted_at else "POSTED: unknown",
        f"PAGE: {page_name or 'unknown'}",
        f"IMAGES ATTACHED: {len(images)}",
        "CAPTION:",
        caption or "(no caption - the content is only in the image)",
    ]
    contents: list[Any] = [types.Part.from_bytes(data=data, mime_type=mime) for data, mime in images]
    contents.append("\n".join(details))

    config = types.GenerateContentConfig(
        system_instruction=SYSTEM_INSTRUCTION,
        response_mime_type="application/json",
        response_json_schema=RESPONSE_SCHEMA,
        temperature=0.2,
    )

    for attempt in range(1, 4):
        try:
            response = client.models.generate_content(model=model, contents=contents, config=config)
            return json.loads(response.text)
        except errors.ClientError as exc:
            if exc.code == 429 and attempt < 3:
                wait = 30 * attempt
                print(f"    rate limited, waiting {wait}s...")
                time.sleep(wait)
                continue
            raise
        except Exception as exc:
            if attempt < 3:
                print(f"    {type(exc).__name__}, retrying...")
                time.sleep(5 * attempt)
                continue
            raise
    raise RuntimeError("Gemini did not answer after 3 attempts")


def tidy_result(result: dict[str, Any]) -> dict[str, Any]:
    categories = [c for c in result.get("categories", []) if c in db.ALLOWED_CATEGORIES][:3] or ["General"]
    urgency = result.get("urgency") if result.get("urgency") in URGENCY_LEVELS else "Low"

    dates = []
    for item in result.get("important_dates", []) or []:
        try:
            datetime.strptime(str(item.get("date", "")), "%Y-%m-%d")
        except ValueError:
            continue
        dates.append({"label": str(item.get("label", "")).strip(), "date": item["date"]})

    return {
        "clean_text": unicodedata.normalize("NFKC", str(result.get("clean_text", ""))).strip(),
        "key_takeaway": str(result.get("key_takeaway", "")).strip(),
        "important_dates": dates,
        "categories": categories,
        "urgency": urgency,
    }


def add_posts_from_file(path: Path) -> int:
    import ingest

    data = json.loads(path.read_text(encoding="utf-8"))
    if isinstance(data, dict):
        data = data.get("announcements", data.get("posts", []))
    added = 0
    for post in data:
        if not isinstance(post, dict) or ingest.is_fetchrss_notice(post):
            continue
        db.save_announcement(ingest.to_announcement(post))
        added += 1
    return added


def main() -> int:
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")

    parser = argparse.ArgumentParser(description="Fill in the AI fields for posts saved by ingest.py.")
    parser.add_argument("--input", type=Path, help="also add posts from a JSON file (scraper format) first")
    parser.add_argument("--limit", type=int, default=100, help="most posts to process in one run")
    parser.add_argument("--dry-run", action="store_true", help="show what would be processed, without calling Gemini")
    args = parser.parse_args()

    db.init_db()

    if args.input:
        path = args.input if args.input.exists() else HERE / args.input
        if not path.exists():
            print(f"Input file not found: {args.input}")
            return 1
        print(f"Added {add_posts_from_file(path)} posts from {path.name}")

    rows = db.get_unprocessed(limit=args.limit)
    print(f"{len(rows)} posts waiting for AI processing")
    if not rows:
        print("Nothing to do. Run ingest.py to fetch new posts.")
        return 0

    client = None
    load_dotenv(HERE / ".env")
    model = os.getenv("GEMINI_MODEL", DEFAULT_MODEL)
    if not args.dry_run:
        api_key = os.getenv("GEMINI_API_KEY") or os.getenv("GOOGLE_API_KEY")
        if not api_key:
            print("No API key. Create Backend/.env with a line: GEMINI_API_KEY=your-key")
            return 1
        from google import genai
        from google.genai import types
        client = genai.Client(api_key=api_key, http_options=types.HttpOptions(timeout=90_000))
        print(f"Using model {model}")

    delay = float(os.getenv("GEMINI_DELAY_SECONDS", "4"))
    ok = failed = 0
    for index, row in enumerate(rows, start=1):
        caption = clean_caption(row.get("raw_caption"))
        posted_at = posted_time(row.get("created_at"))
        print(f"[{index}/{len(rows)}] {row.get('page_name', '?')} | {row['id']}")
        images = download_images(row.get("image_urls") or [])

        if args.dry_run:
            when = f"{posted_at:%b %d, %Y %I:%M %p} PHT" if posted_at else "no time"
            preview = caption[:160].replace("\n", " / ")
            print(f"    {when} | {len(images)} image(s) downloaded | caption: {preview or '(empty)'}")
            continue

        if not caption and not images:
            fields = {"clean_text": "", "key_takeaway": "", "important_dates": [],
                      "categories": ["General"], "urgency": "Low"}
            reason = "no caption and no image could be downloaded"
        else:
            try:
                result = ask_gemini(client, model, row.get("page_name", ""), caption, posted_at, images)
            except Exception as exc:
                failed += 1
                print(f"    FAILED (will retry next run): {type(exc).__name__}: {str(exc)[:300]}")
                continue
            fields = tidy_result(result)
            reason = str(result.get("urgency_reason", "")).strip()

        try:
            db.save_announcement({"id": row["id"], **fields})
        except ValueError as exc:
            failed += 1
            print(f"    FAILED to save: {exc}")
            continue
        ok += 1
        print(f"    {fields['urgency']} {', '.join(fields['categories'])} | {fields['key_takeaway'] or reason}")
        if index < len(rows):
            time.sleep(delay)

    if not args.dry_run:
        print(f"Done: {ok} saved, {failed} failed.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
