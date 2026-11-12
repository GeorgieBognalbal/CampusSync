
import hmac
import os
import sys

from flask import Flask, jsonify, request
from dotenv import load_dotenv

# CONFIGURATION

# Get the directory containing server.py
BASE_DIR = os.path.dirname(os.path.abspath(__file__))

# Allow imports regardless of working directory
sys.path.insert(0, BASE_DIR)

# Load the actual .env file from Backend/
load_dotenv(os.path.join(BASE_DIR, ".env"))

import db  # noqa: E402

# Read configuration from .env
API_KEY = os.environ.get("CAMPUSSYNC_API_KEY")
MAX_LIMIT = 100

# Fail early if the API key is missing
if not API_KEY:
    raise RuntimeError(
        "CAMPUSSYNC_API_KEY is missing! Check your Backend/.env file."
    )

# 5000 matches the app, README, ngrok and the laptop service. (5001 here meant
# "python server.py" started on a port the app never calls.)
PORT = int(os.environ.get("PORT", 5000))

app = Flask(__name__)

db.init_db()

# HEALTH CHECK

@app.route("/api/health", methods=["GET"])
def health_check():
    return jsonify({
        "status": "online",
        "application": "Kaleo",
        "message": "Kaleo backend is running!",
        "server_time": db.now_ms(),
    }), 200

# ANNOUNCEMENT SYNCHRONIZATION

@app.route("/api/announcements/sync", methods=["GET"])
def sync_announcements():

    # Authenticate Android application
    key = request.headers.get("X-API-Key", "")

    if not hmac.compare_digest(key, API_KEY):
        return jsonify({
            "success": False,
            "message": "Missing or wrong X-API-Key"
        }), 401

    try:
        since = int(request.args.get("since", 0))
        limit = int(request.args.get("limit", MAX_LIMIT))

    except ValueError:
        return jsonify({
            "success": False,
            "message": "since and limit must be numbers"
        }), 400

    if since < 0 or not 1 <= limit <= MAX_LIMIT:
        return jsonify({
            "success": False,
            "message": f"since must be >= 0 and limit between 1 and {MAX_LIMIT}"
        }), 400

    try:
        # Retrieve announcements from SQLite
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

    print("===================================")
    print("         KALEO BACKEND")
    print("===================================")
    print(f"Server running on port {PORT}")
    print("API: /api/announcements/sync")
    print("API key loaded successfully:", bool(API_KEY))
    print("===================================")

    app.run(
        host="0.0.0.0",
        port=PORT,
        debug=False
    )
