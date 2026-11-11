"""Extracts takeaways, dates, tags and urgency from scraped posts using Gemini."""

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
from email.utils import parsedate_to_datetime
from pathlib import Path
from typing import Any

from dotenv import load_dotenv

HERE = Path(__file__).resolve().parent
RAW_COPY = HERE / "raw_posts.json"
DEFAULT_OUTPUT = HERE / "extracted_posts.json"

DEFAULT_MODEL = "gemini-3.5-flash-lite"
PH_TIME = timezone(timedelta(hours=8), "PHT")
MAX_IMAGES = 3
MAX_IMAGE_BYTES = 7 * 1024 * 1024

TAGS: dict[str, str] = {
    "#Suspension": "classes or work suspended or cancelled, campus closures",
    "#Scholarship": "scholarships or financial aid that STUDENTS can apply for or must act on "
                    "(news about faculty or staff scholarships is #News, not #Scholarship)",
    "#Enrollment": "enrollment, registration, adding or dropping subjects, grades, "
                   "school records, registrar requirements",
    "#Events": "activities, contests, seminars, org recruitment, sports and cultural events",
    "#Advisory": "rules, policies, dress code, schedules, office hours, reminders, "
                 "health, safety or weather advisories, services and price lists",
    "#News": "reports of things that already happened: achievements, visits, results",
    "#Other": "greetings, celebrations and anything else not useful for students",
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

dates: the dates that matter to students: deadlines, event dates, effective dates,
  suspension dates. Use YYYY-MM-DD. Resolve words like "tomorrow" or "this Friday" using the
  post time given below. A date range becomes two entries (start and end). Put a time in
  "time" as HH:MM (24-hour) if one is given, otherwise "". Do NOT invent a date when the
  post only says something vague like "until the end of the semester"; leave it out.
  Use an empty list if there are no such dates.

tags: 1 to 3 tags, chosen ONLY from this list:
{chr(10).join(f"  {tag}: {meaning}" for tag, meaning in TAGS.items())}

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
        "dates": {
            "type": "array",
            "items": {
                "type": "object",
                "properties": {
                    "label": {"type": "string"},
                    "date": {"type": "string", "description": "YYYY-MM-DD"},
                    "time": {"type": "string", "description": "HH:MM or empty"},
                },
                "required": ["label", "date", "time"],
            },
        },
        "tags": {"type": "array", "items": {"type": "string", "enum": list(TAGS)}},
        "urgency": {"type": "string", "enum": list(URGENCY_LEVELS)},
        "urgency_reason": {"type": "string"},
    },
    "required": ["clean_text", "key_takeaway", "dates", "tags", "urgency", "urgency_reason"],
}



def keep_valid_posts(data: Any) -> list[dict[str, Any]]:
    if isinstance(data, dict):
        data = data.get("announcements", data.get("posts", []))
    if not isinstance(data, list):
        raise ValueError("expected a list of posts")
    posts = [
        p for p in data
        if isinstance(p, dict) and p.get("post_id")
        and "This Feed is deleted" not in (p.get("raw_caption") or "")
    ]
    skipped = len(data) - len(posts)
    if skipped:
        print(f"Note: skipped {skipped} entries (no post_id, or a deleted feed)")
    return posts


def load_posts(path: Path) -> list[dict[str, Any]]:
    return keep_valid_posts(json.loads(path.read_text(encoding="utf-8")))


def fetch_from_scraper() -> list[dict[str, Any]]:
    sys.path.insert(0, str(HERE))
    import scraper

    result = scraper.fetch_all_posts()
    RAW_COPY.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    return keep_valid_posts(result)


def clean_caption(raw: str | None) -> str:
    if not raw:
        return ""
    text = html.unescape(raw)
    text = re.sub(r"<br\s*/?>", "\n", text, flags=re.IGNORECASE)
    text = re.sub(r"\(Feed generated with.*?FetchRSS.*?\)", "", text, flags=re.IGNORECASE | re.DOTALL)
    text = re.sub(r"<[^>]+>", "", text)
    # turns facebook "bold" letters (𝗧𝗲𝘅𝘁) into normal text
    text = unicodedata.normalize("NFKC", text)
    text = re.sub(r"[ \t]+\n", "\n", text)
    text = re.sub(r"\n{3,}", "\n\n", text)
    return text.strip()


def parse_post_time(value: str | None) -> datetime | None:
    if not value:
        return None
    try:
        moment = datetime.fromisoformat(value.replace("Z", "+00:00"))
    except ValueError:
        try:
            moment = parsedate_to_datetime(value)
        except (TypeError, ValueError):
            return None
    if moment.tzinfo is None:
        moment = moment.replace(tzinfo=timezone.utc)
    return moment.astimezone(PH_TIME)


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



def ask_gemini(client: Any, model: str, post: dict[str, Any], caption: str,
               posted_at: datetime | None, images: list[tuple[bytes, str]]) -> dict[str, Any]:
    from google.genai import errors, types

    today = datetime.now(PH_TIME)
    details = [
        f"TODAY: {today:%A, %B %d, %Y}",
        f"POSTED: {posted_at:%A, %B %d, %Y %I:%M %p} (Philippine time)" if posted_at else "POSTED: unknown",
        f"PAGE: {post.get('source') or 'unknown'}",
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
    tags = [t for t in result.get("tags", []) if t in TAGS][:3] or ["#Other"]
    urgency = result.get("urgency") if result.get("urgency") in URGENCY_LEVELS else "Low"

    dates = []
    for item in result.get("dates", []) or []:
        try:
            datetime.strptime(str(item.get("date", "")), "%Y-%m-%d")
        except ValueError:
            continue
        time_text = str(item.get("time") or "").strip()
        if not re.fullmatch(r"\d{2}:\d{2}", time_text):
            time_text = ""
        dates.append({"label": str(item.get("label", "")).strip(), "date": item["date"], "time": time_text})

    return {
        "clean_text": unicodedata.normalize("NFKC", str(result.get("clean_text", ""))).strip(),
        "key_takeaway": str(result.get("key_takeaway", "")).strip(),
        "dates": dates,
        "tags": tags,
        "urgency": urgency,
        "urgency_reason": str(result.get("urgency_reason", "")).strip(),
    }



def base_record(post: dict[str, Any], caption: str, posted_at: datetime | None) -> dict[str, Any]:
    return {
        "post_id": str(post["post_id"]),
        "source": post.get("source", ""),
        "post_url": post.get("post_url", ""),
        "posted_at": posted_at.isoformat() if posted_at else "",
        "image_urls": list(post.get("image_urls") or []),
        "clean_text": caption,
        "key_takeaway": "",
        "dates": [],
        "tags": ["#Other"],
        "urgency": "Low",
        "urgency_reason": "",
        "status": "failed",
        "error": "",
        "model": "",
        "extracted_at": "",
    }


def save_results(path: Path, records: dict[str, dict[str, Any]]) -> None:
    ordered = sorted(records.values(), key=lambda r: r.get("posted_at", ""), reverse=True)
    temp = path.with_suffix(".tmp")
    temp.write_text(json.dumps(ordered, ensure_ascii=False, indent=2), encoding="utf-8")
    temp.replace(path)


def load_results(path: Path) -> dict[str, dict[str, Any]]:
    if not path.exists():
        return {}
    data = json.loads(path.read_text(encoding="utf-8"))
    return {str(r["post_id"]): r for r in data if isinstance(r, dict) and r.get("post_id")}



def main() -> int:
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")

    parser = argparse.ArgumentParser(description="Extract structured announcements from scraped posts.")
    parser.add_argument("--input", type=Path, help="read posts from a JSON file instead of running scraper.py")
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT, help="results file (default: extracted_posts.json)")
    parser.add_argument("--dry-run", action="store_true", help="check the input and images without calling Gemini")
    parser.add_argument("--force", action="store_true", help="redo posts that were already extracted")
    parser.add_argument("--limit", type=int, default=0, help="only process this many posts")
    args = parser.parse_args()

    if args.input:
        input_path = args.input if args.input.is_absolute() else (Path.cwd() / args.input)
        if not input_path.exists() and (HERE / args.input).exists():
            input_path = HERE / args.input
        if not input_path.exists():
            print(f"Input file not found: {input_path}")
            return 1
        posts = load_posts(input_path)
        origin = input_path.name
    else:
        print("Fetching posts with scraper.py...")
        posts = fetch_from_scraper()
        origin = "the scraper"

    results = load_results(args.output)
    todo = [p for p in posts if args.force or results.get(str(p["post_id"]), {}).get("status") != "ok"]
    if args.limit:
        todo = todo[: args.limit]
    print(f"{len(posts)} posts from {origin}; {len(todo)} to process")

    client = None
    model = os.getenv("GEMINI_MODEL", DEFAULT_MODEL)
    if not args.dry_run:
        load_dotenv(HERE / ".env")
        model = os.getenv("GEMINI_MODEL", DEFAULT_MODEL)
        api_key = os.getenv("GEMINI_API_KEY") or os.getenv("GOOGLE_API_KEY")
        if not api_key:
            print("No API key. Create backend/.env with a line: GEMINI_API_KEY=your-key")
            return 1
        from google import genai
        from google.genai import types
        client = genai.Client(api_key=api_key, http_options=types.HttpOptions(timeout=90_000))
        print(f"Using model {model}")

    delay = float(os.getenv("GEMINI_DELAY_SECONDS", "4"))
    ok = failed = 0
    for index, post in enumerate(todo, start=1):
        caption = clean_caption(post.get("raw_caption"))
        title = clean_caption(post.get("title"))
        if not caption and title and not re.match(r"(A post|Photos) from ", title):
            caption = title
        posted_at = parse_post_time(post.get("timestamp"))
        print(f"[{index}/{len(todo)}] {post.get('source', '?')} | {post['post_id']}")
        images = download_images(post.get("image_urls") or [])

        if args.dry_run:
            preview = caption[:160].replace("\n", " / ")
            when = f"{posted_at:%b %d, %Y %I:%M %p} PHT" if posted_at else "no time"
            print(f"    {when} | {len(images)} image(s) downloaded | caption: {preview or '(empty)'}")
            continue

        record = base_record(post, caption, posted_at)
        record["model"] = model
        record["extracted_at"] = datetime.now(PH_TIME).isoformat(timespec="seconds")
        if not caption and not images:
            record["error"] = "no caption and no image could be downloaded"
        else:
            try:
                record.update(tidy_result(ask_gemini(client, model, post, caption, posted_at, images)))
                record["status"] = "ok"
                record["error"] = ""
            except Exception as exc:
                record["error"] = f"{type(exc).__name__}: {exc}"[:500]

        results[record["post_id"]] = record
        save_results(args.output, results)
        if record["status"] == "ok":
            ok += 1
            print(f"    {record['urgency']} {' '.join(record['tags'])} | {record['key_takeaway']}")
        else:
            failed += 1
            print(f"    FAILED: {record['error']}")
        if index < len(todo):
            time.sleep(delay)

    if not args.dry_run:
        print(f"Done: {ok} ok, {failed} failed. Saved to {args.output}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
