package com.example.campussync.notifications;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.text.format.DateUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.campussync.MainActivity;
import com.example.campussync.R;
import com.example.campussync.calendar.CampusDates;
import com.example.campussync.data.local.AnnouncementDatabase;
import com.example.campussync.data.local.AnnouncementEntity;

import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Exam / event / deadline reminders, using the same dates the Calendar tab shows.
 *
 *  - The day before:  "Tomorrow: Midterm exam schedule"
 *  - The day itself:  "Today: Midterm exam schedule"
 *
 * Runs from its own job every 12 hours (no internet needed, the posts are saved on
 * the phone) and after every successful sync. Each reminder is sent once.
 */
public final class KaleoReminders {

    public static final String CHANNEL_REMINDERS = "kaleo_reminders";
    public static final String KEY_ENABLED = "notify_reminders";

    private static final String WORK_NAME = "kaleo_reminders";
    private static final String PREFS = "campussync_prefs";          // shared with MainActivity
    private static final String KEY_UNFOLLOWED = "unfollowed_sources";
    private static final String KEY_SENT = "reminders_sent";
    private static final String TAG = "KaleoReminders";

    /** Don't buzz phones in the middle of the night. */
    private static final int EARLIEST_HOUR = 6;
    private static final int LATEST_HOUR = 22;

    private KaleoReminders() { }

    /** Called once at app start. */
    public static void schedule(Context context) {
        PeriodicWorkRequest req = new PeriodicWorkRequest.Builder(
                ReminderWorker.class, 12, TimeUnit.HOURS).build();
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, req);
    }

    /** Background job: no network constraint, reminders work offline. */
    public static class ReminderWorker extends Worker {
        public ReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
            super(context, params);
        }

        @NonNull
        @Override
        public Result doWork() {
            check(getApplicationContext());
            return Result.success();
        }
    }

    /** Sends any due reminders. Call from a background thread (reads the database). */
    public static void check(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!prefs.getBoolean(KEY_ENABLED, true)) return;
        if (!KaleoNotifications.hasPermission(context)) return;

        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < EARLIEST_HOUR || hour >= LATEST_HOUR) return;

        KaleoNotifications.createChannels(context);

        List<AnnouncementEntity> posts;
        try {
            posts = AnnouncementDatabase.getInstance(context).announcementDao().getAll();
        } catch (Exception e) {
            Log.w(TAG, "Couldn't read posts for reminders", e);
            return;
        }

        Set<String> unfollowed = prefs.getStringSet(KEY_UNFOLLOWED, new HashSet<>());
        Set<String> sent = new HashSet<>(prefs.getStringSet(KEY_SENT, new HashSet<>()));
        long today = CampusDates.todayMillis();
        long tomorrow = today + DateUtils.DAY_IN_MILLIS;

        // Keys for dates other than today/tomorrow can never fire again: drop them.
        String todayTag = dateTag(today), tomorrowTag = dateTag(tomorrow);
        boolean changed = sent.removeIf(k -> !k.contains("|" + todayTag + "|")
                && !k.contains("|" + tomorrowTag + "|"));

        for (AnnouncementEntity a : posts) {
            if (a.pageName != null && unfollowed.contains(a.pageName)) continue;
            CampusDates.DatedPost p = CampusDates.extract(a);
            if (p == null) continue;

            long start = p.startMillis();
            String when;
            if (start == tomorrow) when = "Tomorrow";
            else if (start == today) when = "Today";
            else continue;

            // One reminder per post per day ("…|2026-11-23")
            String key = p.postId + "|" + dateTag(start) + "|" + when;
            if (sent.contains(key)) continue;

            show(context, p, when, key.hashCode());
            sent.add(key);
            changed = true;
        }

        if (changed) prefs.edit().putStringSet(KEY_SENT, sent).apply();
    }

    private static String dateTag(long localMidnight) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(localMidnight);
        return c.get(Calendar.YEAR) + "-" + (c.get(Calendar.MONTH) + 1) + "-" + c.get(Calendar.DAY_OF_MONTH);
    }

    private static void show(Context context, CampusDates.DatedPost p, String when, int id) {
        String title = (p.deadline ? (when.equals("Today") ? "Due today: " : "Due tomorrow: ")
                : when + ": ") + p.title.replaceFirst("^Deadline: ", "");
        String text = dateLabel(p) + "  •  " + p.pageName;

        Intent open = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent openApp = PendingIntent.getActivity(context, id, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder b = new NotificationCompat.Builder(context, CHANNEL_REMINDERS)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(context.getColor(R.color.brand_primary))
                .setContentTitle("📅 " + title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setContentIntent(openApp);

        if (p.permalink != null && !p.permalink.isEmpty()) {
            PendingIntent post = PendingIntent.getActivity(context, id + 1,
                    new Intent(Intent.ACTION_VIEW, Uri.parse(p.permalink)),
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            b.addAction(0, "Open post", post);
        }

        try {
            NotificationManagerCompat.from(context).notify(id, b.build());
        } catch (SecurityException ignored) {
        }
    }

    private static String dateLabel(CampusDates.DatedPost p) {
        String[] mon = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        String start = mon[p.m] + " " + p.d;
        if (!p.multiDay()) return start;
        return start + " – " + (p.em == p.m ? "" : mon[p.em] + " ") + p.ed;
    }
}
