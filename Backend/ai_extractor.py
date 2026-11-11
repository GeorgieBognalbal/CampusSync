"""
CampusSync AI extractor (owner: Benedict).

Job: take posts the scraper saved, run them through Gemini (text + images),
and save the AI fields back with save_announcement().

Starter code only: replace the TODO with the real implementation.
Ask the model to reply in JSON with exactly these keys (see SCHEMA.md):
    clean_text, key_takeaway, important_dates, categories, urgency
"""

import db


def process_pending():
    for post in db.get_unprocessed(limit=20):
        # post has: id, page_name, raw_caption, image_urls, created_at ...
        #
        # TODO (Benedict): call Gemini with post["raw_caption"] and post["image_urls"],
        # parse its JSON reply, then:
        #
        # db.save_announcement({
        #     "id":              post["id"],
        #     "clean_text":      "...",
        #     "key_takeaway":    "...",
        #     "important_dates": [{"label": "Deadline", "date": "2026-12-05"}],
        #     "categories":      ["Scholarship"],   # from the allowed list
        #     "urgency":         "Medium",          # High / Medium / Low
        # })
        #
        # save_announcement() raises ValueError if a category, urgency or date
        # format is wrong, so bad AI output is caught before it reaches phones.
        raise NotImplementedError


if __name__ == "__main__":
    db.init_db()
    process_pending()
