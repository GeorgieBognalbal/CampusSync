
from flask import Flask, jsonify
from scraper import fetch_all_posts

app = Flask(__name__)

@app.route("/api/health", methods=["GET"])
def health_check():
    return jsonify({
        "status": "online",
        "application": "CampusSync",
        "message": "CampusSync backend is running!"
    }), 200


@app.route("/api/announcements/sync", methods=["GET"])
def sync_announcements():

    try:
        # Fetch announcements using your scraper
        result = fetch_all_posts()

        return jsonify({
            "success": True,
            "announcements": result.get("announcements", []),
            "errors": result.get("errors", [])
        }), 200

    except Exception as error:

        app.logger.exception("Announcement synchronization failed")

        return jsonify({
            "success": False,
            "message": "Failed to fetch announcements.",
            "error": str(error)
        }), 500


# START SERVER


import os

if __name__ == "__main__":
    port = int(os.environ.get("PORT", 5000))

    print("===================================")
    print("       CAMPUSSYNC BACKEND")
    print("===================================")
    print(f"Server running on port {port}")
    print("API: /api/announcements/sync")
    print("===================================")

    app.run(
        host="0.0.0.0",
        port=port,
        debug=False
    )

