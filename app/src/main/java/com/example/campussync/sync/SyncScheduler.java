package com.example.campussync.sync;

import android.content.Context;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.campussync.workers.AnnouncementSyncWorker;

import java.util.concurrent.TimeUnit;

/** Starts background syncing (owner: Jairelle). */
public final class SyncScheduler {

    public static final String PERIODIC_WORK = "announcement_sync_periodic";
    public static final String ONE_TIME_WORK = "announcement_sync_now";

    /** FetchRSS free feeds only refresh about once a day, so more often wastes battery. */
    private static final long SYNC_EVERY_HOURS = 6;

    private SyncScheduler() {}

    private static Constraints needsInternet() {
        return new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
    }

    /** Call once when the app starts: schedules the 6-hour sync and runs one now. */
    public static void start(Context context) {
        PeriodicWorkRequest periodic = new PeriodicWorkRequest.Builder(
                AnnouncementSyncWorker.class, SYNC_EVERY_HOURS, TimeUnit.HOURS)
                .setConstraints(needsInternet())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK, ExistingPeriodicWorkPolicy.KEEP, periodic);

        syncNow(context);
    }

    /** Sync right away (e.g. app opened, or a pull-to-refresh). */
    public static void syncNow(Context context) {
        OneTimeWorkRequest now = new OneTimeWorkRequest.Builder(AnnouncementSyncWorker.class)
                .setConstraints(needsInternet())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build();

        WorkManager.getInstance(context).enqueueUniqueWork(
                ONE_TIME_WORK, ExistingWorkPolicy.KEEP, now);
    }
}
