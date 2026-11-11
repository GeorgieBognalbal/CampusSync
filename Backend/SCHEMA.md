# Kaleo Announcement Schema

Every part of the pipeline uses these exact field names and formats.
Owner: Jairelle. Talk to her before changing anything here.

## Who writes what

| Field | Type | Written by | Example |
|---|---|---|---|
| `id` | string, unique, never changes | Geor (via ingest.py) | RSS `<guid>` or Facebook post ID |
| `page_name` | string | Geor (via ingest.py) | `"SPCC OSAS"` |
| `permalink` | string (URL) | Geor (via ingest.py) | `"https://www.facebook.com/..."` |
| `raw_caption` | string, plain text (HTML stripped) | Geor (via ingest.py) | original post text |
| `image_urls` | list of strings | Geor (via ingest.py) | `["https://.../poster.jpg"]` |
| `created_at` | integer, epoch **milliseconds** | Geor (via ingest.py) | `1731290000000` |
| `clean_text` | string | Benedict | cleaned full text (incl. text read from images) |
| `key_takeaway` | string, 1 sentence | Benedict | `"Submit requirements to OSAS before Dec 5."` |
| `important_dates` | list of `{label, date}`, date is `YYYY-MM-DD` | Benedict | `[{"label": "Deadline", "date": "2026-12-05"}]` |
| `categories` | list, from allowed list below | Benedict | `["Scholarship"]` |
| `urgency` | `"High"`, `"Medium"` or `"Low"` | Benedict | `"Medium"` |
| `updated_at` | integer, epoch ms | **set automatically** | changes on every save |
| `deleted` | boolean | **set automatically** | `false` |

**Allowed categories:** `Suspension`, `Scholarship`, `Enrollment`, `Events`, `Exams`, `General`
(no `#`; `"#Suspension"` is accepted and cleaned up automatically).

## How to save (ingest.py & Benedict)

```python
import db
db.save_announcement({"id": "...", "page_name": "...", ...})
```

- Only the fields you pass are changed, so ingest.py saves the raw post first and Benedict adds the AI fields later. If nothing changed, the post is left alone (phones do not re-download it).
- Saving the same `id` again updates the post. It never duplicates.
- Wrong category, urgency or date format raises `ValueError`.
- Benedict gets posts that still need AI with `db.get_unprocessed()`.

## Sync API (for the Android app)

```
GET /api/announcements/sync?since=<epoch ms>&limit=<1-100>
Header: X-API-Key: <key>
```

Response:
```json
{
  "announcements": [ { ...all fields above... } ],
  "next_since": 1731300500000,
  "has_more": false,
  "server_time": 1731300600000
}
```

- The phone stores `next_since` and sends it as `since` next time. `since=0` means everything.
- If `has_more` is true, call again right away with the new `since`.
- Rows with `deleted: true` should be removed from the phone.
