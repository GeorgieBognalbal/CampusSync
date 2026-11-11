package com.example.campussync.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.campussync.BuildConfig;
import com.example.campussync.data.local.AnnouncementDao;
import com.example.campussync.data.local.AnnouncementDatabase;
import com.example.campussync.data.local.AnnouncementEntity;
import com.example.campussync.sync.SyncState;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Downloads new/changed announcements from the backend and saves them in Room
 * (owner: Jairelle).
 *
 *   GET {API_BASE_URL}/api/announcements/sync?since=<cursor>&limit=100
 *   Header: X-API-Key: {API_KEY}
 *
 * Configure in local.properties (not committed):
 *   campussync.apiBaseUrl=http://localhost:5000
 *   campussync.apiKey=dev-key-change-me
 */
public class AnnouncementSyncWorker extends Worker {

    private static final String TAG = "AnnouncementSync";
    private static final int PAGE_SIZE = 100;
    private static final int MAX_PAGES = 50;          // safety stop: 5,000 posts per run
    private static final int TIMEOUT_MS = 15_000;

    public AnnouncementSyncWorker(
            @NonNull Context context,
            @NonNull WorkerParameters workerParams
    ) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d("CampusSync", "WORKER STARTED!");
        Context context = getApplicationContext();
        AnnouncementDatabase database = AnnouncementDatabase.getInstance(context);
        AnnouncementDao dao = database.announcementDao();

        long since = SyncState.getCursor(context);
        int saved = 0;
        int removed = 0;

        try {
            for (int page = 0; page < MAX_PAGES; page++) {
                JSONObject body = fetchPage(since);
                JSONArray items = body.getJSONArray("announcements");

                List<AnnouncementEntity> upserts = new ArrayList<>();
                List<String> deletions = new ArrayList<>();
                for (int i = 0; i < items.length(); i++) {
                    JSONObject item = items.getJSONObject(i);
                    if (item.optBoolean("deleted", false)) {
                        deletions.add(item.getString("id"));
                    } else {
                        upserts.add(toEntity(item));
                    }
                }

                // One transaction per page: either the whole page is saved or none of it.
                database.runInTransaction(() -> {
                    if (!upserts.isEmpty()) {
                        // First real post replaces the built-in demo posts.
                        dao.deleteDemoPosts();
                        dao.insertAll(upserts);
                    }
                    if (!deletions.isEmpty()) dao.deleteByIds(deletions);
                });

                Log.d("CampusSync", "Downloaded announcements: " + upserts.size());
                Log.d("CampusSync", "Deleted announcements: " + deletions.size());
                Log.d("CampusSync", "Total saved announcements: " + dao.count());

                saved += upserts.size();
                removed += deletions.size();

                // Only move the cursor after the page is safely saved.
                since = body.optLong("next_since", since);
                SyncState.setCursor(context, since);

                if (!body.optBoolean("has_more", false)) break;
            }

            SyncState.markSuccess(context);
            Log.i(TAG, "Sync done: " + saved + " saved, " + removed + " removed");
            return Result.success();

        } catch (HttpError e) {
            SyncState.markError(context, e.getMessage());
            Log.w(TAG, "Sync failed: " + e.getMessage());
            // Server problems (5xx) may be temporary; a wrong key or URL (4xx) won't fix itself.
            return e.code >= 500 ? Result.retry() : Result.failure();
        } catch (IOException e) {
            SyncState.markError(context, "Network: " + e.getMessage());
            Log.w(TAG, "Network error, will retry", e);
            return Result.retry();
        } catch (JSONException e) {
            SyncState.markError(context, "Bad response: " + e.getMessage());
            Log.e(TAG, "Server sent unexpected JSON", e);
            return Result.failure();
        }
    }

    private JSONObject fetchPage(long since) throws IOException, JSONException, HttpError {
        String base = BuildConfig.API_BASE_URL.replaceAll("/+$", "");
        URL url = new URL(base + "/api/announcements/sync?since=" + since + "&limit=" + PAGE_SIZE);

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        try {
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("X-API-Key", BuildConfig.API_KEY);

            int code = conn.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                throw new HttpError(code, readAll(conn.getErrorStream()));
            }
            return new JSONObject(readAll(conn.getInputStream()));
        } finally {
            conn.disconnect();
        }
    }

    /** Maps the server JSON (Backend/SCHEMA.md) to the Room entity. */
    static AnnouncementEntity toEntity(JSONObject o) throws JSONException {
        String cleanText = str(o, "clean_text");
        String rawCaption = str(o, "raw_caption");

        AnnouncementEntity a = new AnnouncementEntity(
                o.getString("id"),
                firstNonEmpty(str(o, "page_name"), "Unknown page"),
                firstNonEmpty(cleanText, rawCaption, ""),
                str(o, "permalink"),
                o.optLong("created_at", System.currentTimeMillis())
        );
        a.rawCaption = rawCaption;
        a.keyTakeaway = str(o, "key_takeaway");
        a.urgency = str(o, "urgency");
        a.updatedAt = o.optLong("updated_at", 0L);

        JSONArray cats = o.optJSONArray("categories");
        if (cats != null && cats.length() > 0) {
            StringBuilder sb = new StringBuilder("|");
            for (int i = 0; i < cats.length(); i++) sb.append(cats.getString(i)).append('|');
            a.categories = sb.toString();
        }

        JSONArray dates = o.optJSONArray("important_dates");
        a.importantDates = dates != null ? dates.toString() : "[]";

        JSONArray images = o.optJSONArray("image_urls");
        a.imageUrls = images != null ? images.toString() : "[]";
        return a;
    }

    /** org.json's optString turns JSON null into the text "null"; this returns real null. */
    private static String str(JSONObject o, String key) {
        if (!o.has(key) || o.isNull(key)) return null;
        String value = o.optString(key, null);
        return value == null || value.isEmpty() ? null : value;
    }

    private static String firstNonEmpty(String... values) {
        for (String v : values) if (v != null && !v.isEmpty()) return v;
        return "";
    }

    private static String readAll(InputStream in) throws IOException {
        if (in == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }

    private static class HttpError extends Exception {
        final int code;

        HttpError(int code, String body) {
            super("HTTP " + code + (body.isEmpty() ? "" : ": " + body));
            this.code = code;
        }
    }
}
