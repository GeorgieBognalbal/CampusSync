
import hmac
import os
import sys

from flask import Flask, jsonify, request

# Lets this file run from any folder (python Backend/server.py or python server.py).
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import db  # noqa: E402

API_KEY = os.environ.get("CAMPUSSYNC_API_KEY", "dev-key-change-me")
MAX_LIMIT = 100

app = Flask(__name__)
db.init_db()


@app.route("/api/health", methods=["GET"])
def health_check():
    return jsonify({
        "status": "online",
        "application": "Kaleo",
        "message": "Kaleo backend is running!",
        "server_time": db.now_ms(),
    }), 200


@app.route("/api/announcements/sync", methods=["GET"])
def sync_announcements():
    # Phones send the key in a header. Wrong or missing key -> 401.
    key = request.headers.get("X-API-Key", "")
    if not hmac.compare_digest(key, API_KEY):
        return jsonify({"success": False, "message": "Missing or wrong X-API-Key"}), 401

    try:
        since = int(request.args.get("since", 0))
        limit = int(request.args.get("limit", MAX_LIMIT))
    except ValueError:
        return jsonify({"success": False, "message": "since and limit must be numbers"}), 400
    if since < 0 or not 1 <= limit <= MAX_LIMIT:
        return jsonify({"success": False,
                        "message": f"since must be >= 0 and limit between 1 and {MAX_LIMIT}"}), 400

    try:
        # Reads from the database (filled by ingest.py), not live from Facebook,
        # so syncing is fast and works even if FetchRSS is down.
        rows, has_more = db.get_changed_since(since, limit)
        return jsonify({
            "success": True,
            "announcements": rows,
            "next_since": rows[-1]["updated_at"] if rows else since,
            "has_more": has_more,
            "server_time": db.now_ms(),
        }), 200

    except Exception as error:

        app.logger.exception("Announcement synchronization failed")

        return jsonify({
            "success": False,
            "message": "Failed to load announcements.",
            "error": str(error)
        }), 500


# START SERVER


if __name__ == "__main__":
    port = int(os.environ.get("PORT", 5000))

    if API_KEY == "dev-key-change-me":
        print("WARNING: default API key. Set CAMPUSSYNC_API_KEY before a real deploy.")

    print("===================================")
    print("         KALEO BACKEND")
    print("===================================")
    print(f"Server running on port {port}")
    print("API: /api/announcements/sync")
    print("===================================")

    app.run(
        host="0.0.0.0",
        port=port,
        debug=False
    )
