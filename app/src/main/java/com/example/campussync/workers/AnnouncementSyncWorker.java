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
import com.example.campussync.notifications.KaleoNotifications;
import com.example.campussync.notifications.KaleoReminders;
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
import java.util.HashSet;
import java.util.List;

/**
 * Downloads new/changed announcements from the backend and saves them in Room
 * (owner: Jairelle).
 *
 *   GET {API_BASE_URL}/api/announcements/sync?since=<cursor>&limit=100
 *   Header: X-API-Key: {API_KEY}
 *
 * Configure in local.properties (not committed):
 *   campussync.apiBaseUrl=http://<computer-wifi-ip>:5000
 *   campussync.apiKey=<backend-api-key>
 */
public class AnnouncementSyncWorker extends Worker {

    private static final String TAG = "CampusSync";
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
        Context context = getApplicationContext();
        AnnouncementDatabase database = AnnouncementDatabase.getInstance(context);
        AnnouncementDao dao = database.announcementDao();

        // Room opens the database lazily. Touch it BEFORE reading the cursor: if this
        // open creates a fresh database (or wipes an old one), its callback resets the
        // cursor, and we must read the reset value, not the stale one that would skip
        // every post the phone no longer has.
        dao.count();

        long since = SyncState.getCursor(context);
        // First sync ever downloads the whole backlog; don't notify for all of it.
        boolean firstSync = since == 0;
        List<AnnouncementEntity> newPosts = new ArrayList<>();
        int saved = 0;
        int removed = 0;
        boolean reachedEnd = false;

        try {
            for (int page = 0; page < MAX_PAGES; page++) {
                // A pull-to-refresh replaces this run; stop between pages so two syncs
                // don't write the cursor at the same time.
                if (isStopped()) {
                    Log.i(TAG, "Sync stopped (replaced by a newer request) at cursor=" + since);
                    // Pages saved so far are final; the newer run will see these posts
                    // as already stored, so announce them now or they'd never notify.
                    KaleoNotifications.notifyNewPosts(context, newPosts);
                    return Result.success();
                }
                JSONObject body = fetchPage(since);
                JSONArray items = body.getJSONArray("announcements");

                // If the server database was recreated, its updated_at values may all be
                // older than our persisted cursor. Recover only when we have no real cache.
                if (page == 0 && items.length() == 0 && since > 0
                        && dao.countRealAnnouncements() == 0) {
                    Log.w(TAG, "Cursor " + since + " returned no rows and no real cache exists; retrying from since=0");
                    since = 0;
                    body = fetchPage(since);
                    items = body.getJSONArray("announcements");
                }
                Log.i(TAG, "Page " + (page + 1) + ": received " + items.length()
                        + " announcements; cursor=" + since
                        + ", has_more=" + body.optBoolean("has_more", false));

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

                // Posts not on the phone yet -> candidates for a notification
                if (!upserts.isEmpty() && !firstSync) {
                    List<String> ids = new ArrayList<>();
                    for (AnnouncementEntity a : upserts) ids.add(a.facebookPostId);
                    HashSet<String> existing = new HashSet<>(dao.getExistingIds(ids));
                    for (AnnouncementEntity a : upserts) {
                        if (!existing.contains(a.facebookPostId)) newPosts.add(a);
                    }
                }

                // One transaction per page: either the whole page is saved or none of it.
                final int[] demoCount = {0};
                database.runInTransaction(() -> {
                    if (!upserts.isEmpty()) {
                        // Remove fallback rows in the same transaction as the first real upsert.
                        demoCount[0] = dao.deleteDemoPosts();
                        dao.insertAll(upserts);
                    }
                    if (!deletions.isEmpty()) dao.deleteByIds(deletions);
                });
                if (!upserts.isEmpty()) SyncState.markRealData(context);

                saved += upserts.size();
                removed += deletions.size();

                // Only move the cursor after the page is safely saved.
                long nextSince = body.optLong("next_since", since);
                boolean hasMore = body.optBoolean("has_more", false);
                if (hasMore && nextSince <= since) {
                    throw new JSONException("Pagination did not advance cursor (since="
                            + since + ", next_since=" + nextSince + ")");
                }
                since = nextSince;
                SyncState.setCursor(context, since);
                Log.i(TAG, "Saved cursor=" + since + "; has_more=" + hasMore);

                if (!hasMore) {
                    reachedEnd = true;
                    break;
                }
            }

            if (!reachedEnd) {
                String error = "Stopped at safety limit before pagination completed";
                SyncState.markError(context, error);
                Log.w(TAG, error + "; cursor=" + since + "; scheduling retry");
                return Result.retry();
            }

            SyncState.markSuccess(context);
            KaleoNotifications.notifyNewPosts(context, newPosts);
            KaleoReminders.check(context);
            Log.i(TAG, "Sync succeeded: " + saved + " saved/updated, " + removed
                    + " deleted; final cursor=" + since);
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
        Log.i(TAG, "Contacting " + base + "/api/announcements/sync (since=" + since + ")");

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        try {
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("X-API-Key", BuildConfig.API_KEY);

            int code = conn.getResponseCode();
            Log.i(TAG, "Sync API HTTP " + code);
            if (code != HttpURLConnection.HTTP_OK) {
                throw new HttpError(code, readAll(conn.getErrorStream()));
            }
            JSONObject response = new JSONObject(readAll(conn.getInputStream()));
            if (!response.optBoolean("success", false)) {
                throw new JSONException("Sync API returned success=false");
            }
            return response;
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
