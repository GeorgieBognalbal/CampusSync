# Kaleo (CampusSync)

**Kaleo** is a smart, offline-first campus information platform that aggregates, cleans, categorizes, and serves campus announcements to students with AI-powered features. It consists of an Android application and a Python-based backend pipeline.

---

## Key Features

### Android Application (`/app`)
- **Smart Announcement Feed**: View, search, and filter campus updates by categories (*Suspension, Scholarship, Enrollment, Events, Exams, General*) and urgency levels (*High, Medium, Low*).
- **AI Campus Assistant**: Ask questions about campus policies, announcements, or student handbooks. Powered by local/cloud AI engines with context from the LSPU Student Handbook.
- **Calendar & Key Dates**: Automatically extracts and displays important deadlines and event dates from announcements.
- **Offline-First & Auto-Sync**: Uses Room DB (with FTS for fast search) and WorkManager (`AnnouncementSyncWorker`) for seamless background updates.
- **Push Notifications & Reminders**: Timely alerts for urgent announcements and upcoming deadlines.

### Backend Pipeline (`/Backend`)
- **RSS & Post Scraper** (`scraper.py`): Ingests announcements from campus RSS feeds and social media sources.
- **Data Ingestion** (`ingest.py` & `db.py`): Stores raw posts into SQLite without duplicates.
- **AI Processing Engine** (`ai_extractor.py`): Uses Google Gemini to format clean text, generate 1-sentence takeaways, extract structured dates, categorize, and tag urgency.
- **REST API Server** (`server.py`): Exposes `/api/announcements/sync` endpoint for delta syncing with mobile devices.

---

## Architecture Overview

```
 ┌───────────────────────────────────────────────────┐
 │                   Backend Server                  │
 │                                                   │
 │   [Scraper] ──► [Ingestion Pipeline] ──► [db.py]   │
 │                         │                         │
 │                         ▼                         │
 │                 [Gemini Extractor]                │
 │                         │                         │
 │                         ▼                         │
 │                 [Flask API Server]                │
 └─────────────────────────┬─────────────────────────┘
                           │ Sync API (HTTP / JSON)
                           ▼
 ┌───────────────────────────────────────────────────┐
 │                 Android App (Java)                │
 │                                                   │
 │  [WorkManager] ──► [AnnouncementRepository]       │
 │                             │                     │
 │                             ▼                     │
 │                     [Room SQLite DB]              │
 │                             │                     │
 │       ┌─────────────────────┴─────────────────┐   │
 │       ▼                                       ▼   │
 │  [Feed UI & Search]                 [AI Chat Assistant] │
 └───────────────────────────────────────────────────┘
```

---

## Getting Started

### 1. Prerequisites
- **Android Studio** (Ladybug / Jellyfish or newer recommended)
- **JDK 17**
- **Python 3.10+** (for Backend)
- **Android Device / Emulator** (API 24+)

---

### 2. Backend Setup (`/Backend`)

1. **Navigate to the Backend folder**:
   ```bash
   cd Backend
   ```

2. **Create and activate a Python virtual environment**:
   - **Linux / macOS**:
     ```bash
     python3 -m venv .venv
     source .venv/bin/activate
     ```
   - **Windows**:
     ```cmd
     python -m venv .venv
     .venv\Scripts\activate
     ```

3. **Install dependencies**:
   ```bash
   pip install -r requirements.txt
   ```

4. **Environment Variables**:
   Copy `.env.example` to `.env` or set environment variables:
   ```bash
   GEMINI_API_KEY=your-gemini-api-key
   CAMPUSSYNC_API_KEY=dev-key-change-me
   ```

5. **Initialize Database & Run Pipeline**:
   ```bash
   # Seed database with sample data (optional)
   python db.py seed

   # Fetch and process raw posts
   python ingest.py

   # Start the API server (runs on http://localhost:5000)
   python server.py
   ```

---

### 3. Android App Setup (`/app`)

1. **Configure Server Endpoint**:
   Create or edit `local.properties` in the project root directory to configure the backend server IP and API keys:
   ```properties
   campussync.apiBaseUrl=http://10.0.2.2:5000
   campussync.apiKey=dev-key-change-me
   campussync.geminiApiKey=your-gemini-api-key
   ```
   *(Note: Use `http://10.0.2.2:5000` when running on an Android Emulator. For physical USB devices, run `adb reverse tcp:5000 tcp:5000` and use `http://localhost:5000`).*

2. **Build and Run**:
   - Open the project in **Android Studio**.
   - Sync Gradle project.
   - Run the `app` configuration on your emulator or connected device.

---

## API Reference

### Announcements Sync Endpoint

`GET /api/announcements/sync`

**Headers**:
- `X-API-Key`: `<API_KEY>`

**Query Parameters**:
- `since` *(integer, optional)*: Epoch timestamp in milliseconds. Returns announcements updated since this timestamp. Default `0`.
- `limit` *(integer, optional)*: Max records per request (1-100). Default `50`.

**Response Format**:
```json
{
  "announcements": [
    {
      "id": "post-123",
      "page_name": "OSAS",
      "permalink": "https://...",
      "clean_text": "Cleaned announcement body text...",
      "key_takeaway": "Submit scholarship applications by Dec 5.",
      "important_dates": [
        { "label": "Deadline", "date": "2026-12-05" }
      ],
      "categories": ["Scholarship"],
      "urgency": "High",
      "created_at": 1731290000000,
      "updated_at": 1731300000000,
      "deleted": false
    }
  ],
  "next_since": 1731300000000,
  "has_more": false,
  "server_time": 1731300500000
}
```

---

## 📁 Repository Structure

```
CampusSync/
├── app/                        # Android Client Application
│   ├── build.gradle.kts
│   └── src/main/
│       ├── java/com/example/campussync/
│       │   ├── ai/             # AI Engine adapters & Handbook Knowledge Base
│       │   ├── calendar/       # Date extraction & calendar handling
│       │   ├── data/           # Room Database entities, DAOs, & Repositories
│       │   ├── notifications/  # System notifications & alerts
│       │   ├── sync/           # WorkManager background sync logic
│       │   └── MainActivity.java
│       └── res/                # Layouts, drawables, & strings
├── Backend/                    # Python Backend Services
│   ├── README.md               # Detailed Backend README
│   ├── SCHEMA.md               # Data schema definitions
│   ├── ai_extractor.py         # Gemini AI post enrichment logic
│   ├── db.py                   # SQLite storage helper
│   ├── ingest.py               # Ingestion pipeline runner
│   ├── scraper.py              # RSS/Web scraper
│   └── server.py               # Flask REST API server
├── build.gradle.kts            # Top-level Gradle configuration
├── settings.gradle.kts         # Gradle project settings
└── README.md                   # Project Documentation
```

---

