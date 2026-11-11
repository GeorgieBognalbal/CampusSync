package com.example.campussync.sync;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Remembers where the last sync stopped and when it last succeeded (owner: Jairelle).
 *
 * Kahrd: use getLastSyncTime() / hasEverSynced() for the "Offline Mode / Synced" badge.
 */
public final class SyncState {

    private static final String PREFS = "campussync_sync";
    private static final String KEY_CURSOR = "since_cursor";
    private static final String KEY_LAST_SYNC = "last_sync_time";
    private static final String KEY_LAST_ERROR = "last_error";
    private static final String KEY_HAS_REAL_DATA = "has_real_data";

    private SyncState() {}

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** The server's next_since from the last page saved. 0 = download everything. */
    public static long getCursor(Context context) {
        return prefs(context).getLong(KEY_CURSOR, 0L);
    }

    public static void setCursor(Context context, long cursor) {
        prefs(context).edit().putLong(KEY_CURSOR, cursor).apply();
    }

    /** Epoch ms of the last successful sync, or 0 if the app has never synced. */
    public static long getLastSyncTime(Context context) {
        return prefs(context).getLong(KEY_LAST_SYNC, 0L);
    }

    public static boolean hasEverSynced(Context context) {
        return getLastSyncTime(context) > 0;
    }

    /** True after at least one real server announcement has been committed locally. */
    public static boolean hasRealData(Context context) {
        return prefs(context).getBoolean(KEY_HAS_REAL_DATA, false);
    }

    public static void markRealData(Context context) {
        prefs(context).edit().putBoolean(KEY_HAS_REAL_DATA, true).apply();
    }

    public static void markSuccess(Context context) {
        prefs(context).edit()
                .putLong(KEY_LAST_SYNC, System.currentTimeMillis())
                .remove(KEY_LAST_ERROR)
                .apply();
    }

    /** Short reason the last sync failed, or null. Handy for debugging. */
    public static String getLastError(Context context) {
        return prefs(context).getString(KEY_LAST_ERROR, null);
    }

    public static void markError(Context context, String error) {
        prefs(context).edit().putString(KEY_LAST_ERROR, error).apply();
    }

    /** Reset sync metadata when Room creates a new, empty local database. */
    public static void reset(Context context) {
        prefs(context).edit()
                .remove(KEY_CURSOR)
                .remove(KEY_LAST_SYNC)
                .remove(KEY_LAST_ERROR)
                .remove(KEY_HAS_REAL_DATA)
                .apply();
    }
}
