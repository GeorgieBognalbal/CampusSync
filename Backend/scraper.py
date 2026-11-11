from __future__ import annotations

import json
import os
import re
import sys
import urllib.error
import urllib.request
import xml.etree.ElementTree as ET

from datetime import timezone
from email.utils import parsedate_to_datetime
from html.parser import HTMLParser
from typing import Any

SOURCES: list[dict[str, str]] = [
    {
        "name": "LSPU Official",
        "page_url": "https://www.facebook.com/LSPUOfficial",
        "rss_url": "https://fetchrss.com/feed/1xR6PCARzFXO1xR6PiCqr9lC.rss",
    },
    {
        "name": "Guidance Office",
        "page_url": "https://www.facebook.com/lspuspcc.guidance/",
        "rss_url": "https://fetchrss.com/feed/1xR5OK99rF9f1xR75U75FGmY.rss",
    },
    {
        "name": "Gov Sol",
        "page_url": "https://www.facebook.com/SolAragonesOfficial",
        "rss_url": "https://fetchrss.com/feed/1xR7AAGa58x81xR79j67y51W.rss",
    },
    {
        "name": "SPCC OSAS",
        "page_url": "https://www.facebook.com/SPCC.OSAS",
        "rss_url": "https://fetchrss.com/feed/1xR7JK2fJ09b1xR7LkFv45GU.rss",
    },
    {
        "name": "UDRRM",
        "page_url": "https://www.facebook.com/profile.php?id=61578211806333",
        "rss_url": "https://fetchrss.com/feed/1xR7EX92KEgF1xR7E582S8gc.rss",
    },

]

REQUEST_TIMEOUT_SECONDS = 20
MAX_POSTS_PER_SOURCE = 50
USER_AGENT = "Kaleo/1.0 (+announcement-ingestion)"


class _HTMLContentParser(HTMLParser):

    def __init__(self) -> None:
        super().__init__(convert_charrefs=True)
        self.text_parts: list[str] = []
        self.image_urls: list[str] = []

    def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        attrs_dict = dict(attrs)
        if tag.lower() == "img":
            src = attrs_dict.get("src") or attrs_dict.get("data-src")
            if src:
                self.image_urls.append(src.strip())

    def handle_data(self, data: str) -> None:
        cleaned = data.strip()
        if cleaned:
            self.text_parts.append(cleaned)


def _local_name(tag: str) -> str:

    return tag.rsplit("}", 1)[-1].split(":")[-1].lower()


def _child_text(element: ET.Element, names: set[str]) -> str:

    for child in element.iter():
        if _local_name(child.tag) in names:
            value = "".join(child.itertext()).strip()
            if value:
                return value
    return ""


def _extract_html(value: str) -> tuple[str, list[str]]:
    parser = _HTMLContentParser()
    try:
        parser.feed(value or "")
        parser.close()
    except Exception:
        pass

    text = re.sub(r"\s+", " ", " ".join(parser.text_parts)).strip()
    images = list(dict.fromkeys(parser.image_urls))
    return text, images


def _parse_timestamp(value: str) -> str | None:
    if not value:
        return None

    try:
        parsed = parsedate_to_datetime(value)
        if parsed.tzinfo is None:
            parsed = parsed.replace(tzinfo=timezone.utc)
        return parsed.astimezone(timezone.utc).isoformat()
    except (TypeError, ValueError, OverflowError):
        return value.strip()


def _item_image_urls(item: ET.Element, html_values: list[str]) -> list[str]:
    urls: list[str] = []

    # Common RSS media fields: media:content, media:thumbnail, enclosure.
    for element in item.iter():
        name = _local_name(element.tag)
        if name in {"content", "thumbnail", "enclosure"}:
            media_type = (element.attrib.get("type") or "").lower()
            url = (
                    element.attrib.get("url")
                    or element.attrib.get("href")
                    or element.attrib.get("src")
            )
            if url and (
                    name != "enclosure"
                    or media_type.startswith("image/")
                    or not media_type
            ):
                urls.append(url.strip())

    for html_value in html_values:
        _, found = _extract_html(html_value)
        urls.extend(found)

    # Keep only unique HTTP(S) URLs.
    result: list[str] = []
    seen: set[str] = set()
    for url in urls:
        if url.startswith(("https://", "http://")) and url not in seen:
            seen.add(url)
            result.append(url)
    return result


def _normalize_item(
        item: ET.Element,
        source: dict[str, str],
) -> dict[str, Any]:
    title = _child_text(item, {"title"})
    description = _child_text(item, {"description", "summary"})
    content_encoded = _child_text(item, {"encoded", "content"})
    link = _child_text(item, {"link"})
    guid = _child_text(item, {"guid", "id"})
    published = _child_text(
        item, {"pubdate", "published", "updated", "date", "created"}
    )

    html_values = [description, content_encoded]
    clean_parts = []
    for value in html_values:
        if value:
            clean_text, _ = _extract_html(value)
            if clean_text:
                clean_parts.append(clean_text)

    # Preserve a useful caption even when the feed has plain text rather than HTML.
    caption = " ".join(dict.fromkeys(clean_parts)).strip()
    if not caption:
        caption = description or content_encoded or title
        caption, _ = _extract_html(caption)

    return {
        "source": source.get("name", "Unknown Facebook Page"),
        "page_url": source.get("page_url", ""),
        "post_url": link or None,
        "post_id": guid or link or None,
        "title": title or None,
        "raw_caption": caption or "",
        "timestamp": _parse_timestamp(published),
        "image_urls": _item_image_urls(item, html_values),
    }


def _load_sources() -> list[dict[str, str]]:
    raw = os.getenv("FACEBOOK_FEEDS_JSON", "").strip()

    if raw:
        try:
            parsed = json.loads(raw)
        except json.JSONDecodeError as exc:
            raise ValueError(f"FACEBOOK_FEEDS_JSON is not valid JSON: {exc}") from exc
        if not isinstance(parsed, list):
            raise ValueError("FACEBOOK_FEEDS_JSON must be a JSON array.")
        sources = parsed
    else:
        sources = SOURCES

    valid_sources: list[dict[str, str]] = []
    for index, source in enumerate(sources):
        if not isinstance(source, dict):
            print(f"Skipping source #{index + 1}: expected an object.", file=sys.stderr)
            continue

        name = str(source.get("name", f"Facebook Page {index + 1}")).strip()
        rss_url = str(source.get("rss_url", "")).strip()
        page_url = str(source.get("page_url", "")).strip()

        if not rss_url or "PASTE_RSS_URL_HERE" in rss_url:
            print(f"Skipping {name}: no RSS URL configured.", file=sys.stderr)
            continue
        if not rss_url.startswith(("https://", "http://")):
            print(f"Skipping {name}: RSS URL must start with http:// or https://.", file=sys.stderr)
            continue

        valid_sources.append(
            {"name": name, "page_url": page_url, "rss_url": rss_url}
        )

    return valid_sources


def fetch_feed(source: dict[str, str]) -> list[dict[str, Any]]:

    request = urllib.request.Request(
        source["rss_url"],
        headers={
            "User-Agent": USER_AGENT,
            "Accept": "application/rss+xml, application/atom+xml, application/xml, text/xml, */*",
        },
        method="GET",
    )

    try:
        with urllib.request.urlopen(
                request, timeout=REQUEST_TIMEOUT_SECONDS
        ) as response:
            xml_data = response.read()
    except urllib.error.HTTPError as exc:
        raise RuntimeError(f"HTTP {exc.code}: {exc.reason}") from exc
    except urllib.error.URLError as exc:
        raise RuntimeError(f"Network error: {exc.reason}") from exc

    try:
        root = ET.fromstring(xml_data)
    except ET.ParseError as exc:
        raise RuntimeError(f"Feed returned invalid XML: {exc}") from exc

    # RSS uses <item>; Atom uses <entry>.
    items = [
        element
        for element in root.iter()
        if _local_name(element.tag) in {"item", "entry"}
    ]

    posts = [
        _normalize_item(item, source)
        for item in items[:MAX_POSTS_PER_SOURCE]
    ]
    return posts


def fetch_all_posts(
        sources: list[dict[str, str]] | None = None,
) -> dict[str, Any]:

    configured_sources = sources if sources is not None else _load_sources()
    announcements: list[dict[str, Any]] = []
    errors: list[dict[str, str]] = []
    seen_ids: set[str] = set()

    for source in configured_sources:
        name = source.get("name", "Unknown Facebook Page")
        try:
            posts = fetch_feed(source)
            for post in posts:
                unique_id = post.get("post_id")
                if unique_id:
                    dedupe_key = f"{name}:{unique_id}"
                    if dedupe_key in seen_ids:
                        continue
                    seen_ids.add(dedupe_key)
                announcements.append(post)

            print(f"[OK] {name}: fetched {len(posts)} post(s).", file=sys.stderr)

        except Exception as exc:
            errors.append({"source": name, "error": str(exc)})
            print(f"[ERROR] {name}: {exc}", file=sys.stderr)


    # Newest posts first when timestamps are present.
    announcements.sort(
        key=lambda post: post.get("timestamp") or "",
        reverse=True,
    )

    return {
        "announcements": announcements,
        "errors": errors,
    }


if __name__ == "__main__":
    try:
        result = fetch_all_posts()
        print(json.dumps(result, ensure_ascii=False, indent=2))
        if not result["announcements"] and result["errors"]:
            sys.exit(1)
    except ValueError as exc:
        print(f"Configuration error: {exc}", file=sys.stderr)
        sys.exit(2)
