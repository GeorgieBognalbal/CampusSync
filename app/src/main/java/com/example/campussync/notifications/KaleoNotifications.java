package com.example.campussync.notifications;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.campussync.MainActivity;
import com.example.campussync.R;
import com.example.campussync.data.local.AnnouncementEntity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Shows phone notifications for new announcements.
 *
 *  - Urgent posts (class suspensions, urgency = High) → "Urgent alerts" channel,
 *    high priority with sound, one notification per post.
 *  - Other new posts → "New announcements" channel, grouped into one summary.
 *
 * Users control this from the bell in the app header. Posts from sources they
 * turned off in Ask AI → Sources are never notified.
 */
public final class KaleoNotifications {

    public static final String CHANNEL_URGENT = "kaleo_urgent";
    public static final String CHANNEL_UPDATES = "kaleo_updates";

    private static final String GROUP_UPDATES = "kaleo_updates_group";
    private static final int ID_SUMMARY = 1000;

    // Same prefs file MainActivity uses, so settings and unfollowed sources are shared
    private static final String PREFS = "campussync_prefs";
    public static final String KEY_URGENT_ENABLED = "notify_urgent";
    public static final String KEY_ALL_ENABLED = "notify_all";
    private static final String KEY_UNFOLLOWED = "unfollowed_sources";

    private KaleoNotifications() { }

    /** Creates the channels. Safe to call many times (Android ignores duplicates). */
    public static void createChannels(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm == null) return;

        NotificationChannel urgent = new NotificationChannel(
                CHANNEL_URGENT, "Urgent alerts", NotificationManager.IMPORTANCE_HIGH);
        urgent.setDescription("Class suspensions and other high-urgency announcements");
        urgent.enableVibration(true);

        NotificationChannel updates = new NotificationChannel(
                CHANNEL_UPDATES, "New announcements", NotificationManager.IMPORTANCE_DEFAULT);
        updates.setDescription("Exams, events, scholarships and other new posts");

        NotificationChannel reminders = new NotificationChannel(
                KaleoReminders.CHANNEL_REMINDERS, "Exam & deadline reminders",
                NotificationManager.IMPORTANCE_DEFAULT);
        reminders.setDescription("The day before and the morning of dates on your Calendar");

        nm.createNotificationChannel(urgent);
        nm.createNotificationChannel(updates);
        nm.createNotificationChannel(reminders);
    }

    public static boolean hasPermission(Context context) {
        if (Build.VERSION.SDK_INT < 33) return true;
        return context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean urgentEnabled(Context c) {
        return prefs(c).getBoolean(KEY_URGENT_ENABLED, true);
    }

    public static boolean allEnabled(Context c) {
        return prefs(c).getBoolean(KEY_ALL_ENABLED, true);
    }

    public static boolean isUrgent(AnnouncementEntity a) {
        if (a.urgency != null && a.urgency.equalsIgnoreCase("High")) return true;
        if (a.categories != null && a.categories.toLowerCase(Locale.ROOT).contains("|suspension|")) return true;
        String m = a.message == null ? "" : a.message.toLowerCase(Locale.ROOT);
        return m.contains("suspend") || m.contains("walang pasok") || m.contains("no classes");
    }

    /** Called by the sync worker with posts that were not on the phone before. */
    public static void notifyNewPosts(Context context, List<AnnouncementEntity> newPosts) {
        if (newPosts == null || newPosts.isEmpty() || !hasPermission(context)) return;
        createChannels(context);

        Set<String> unfollowed = prefs(context).getStringSet(KEY_UNFOLLOWED, new HashSet<>());
        List<AnnouncementEntity> urgent = new ArrayList<>();
        List<AnnouncementEntity> regular = new ArrayList<>();
        for (AnnouncementEntity a : newPosts) {
            if (unfollowed.contains(a.pageName)) continue;
            if (isUrgent(a)) urgent.add(a); else regular.add(a);
        }

        if (urgentEnabled(context)) {
            for (AnnouncementEntity a : urgent) showUrgent(context, a);
        } else {
            regular.addAll(0, urgent);   // still mention them in the normal summary
        }
        if (allEnabled(context) && !regular.isEmpty()) showUpdates(context, regular);
    }

    /** Sends sample notifications so the feature can be tried without the backend. */
    public static void sendTest(Context context) {
        AnnouncementEntity urgent = new AnnouncementEntity(
                "test-urgent-" + System.currentTimeMillis(), "LSPU Official",
                "Classes are suspended tomorrow in all levels due to the typhoon signal. Walang pasok — stay safe!",
                "https://www.facebook.com/LSPUOfficial", System.currentTimeMillis());
        AnnouncementEntity exam = new AnnouncementEntity(
                "test-exam-" + System.currentTimeMillis(), "CCSOCTA",
                "Midterm exam schedule — Nov 24 to Nov 28. Bring your exam permit.",
                "https://www.facebook.com/CCSOCTA", System.currentTimeMillis());
        AnnouncementEntity scholarship = new AnnouncementEntity(
                "test-scholar-" + System.currentTimeMillis(), "SPCC OSAS",
                "Scholarship requirements are now posted. Submit to OSAS before Dec 5.",
                "https://www.facebook.com/SPCC.OSAS", System.currentTimeMillis());

        List<AnnouncementEntity> posts = new ArrayList<>();
        posts.add(urgent);
        posts.add(exam);
        posts.add(scholarship);
        notifyNewPosts(context, posts);
    }

    // ---------- builders ----------

    private static void showUrgent(Context context, AnnouncementEntity a) {
        String body = summaryText(a);
        NotificationCompat.Builder b = new NotificationCompat.Builder(context, CHANNEL_URGENT)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(context.getColor(R.color.brand_primary))
                .setContentTitle("⚠ " + a.pageName)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setWhen(a.createdAt)
                .setShowWhen(true)
                .setAutoCancel(true)
                .setContentIntent(openAppIntent(context, a.facebookPostId.hashCode()));

        addOpenPostAction(context, b, a);
        post(context, a.facebookPostId.hashCode(), b);
    }

    private static void showUpdates(Context context, List<AnnouncementEntity> posts) {
        if (posts.size() == 1) {
            AnnouncementEntity a = posts.get(0);
            String body = summaryText(a);
            NotificationCompat.Builder b = new NotificationCompat.Builder(context, CHANNEL_UPDATES)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setColor(context.getColor(R.color.brand_primary))
                    .setContentTitle("New from " + a.pageName)
                    .setContentText(body)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                    .setWhen(a.createdAt)
                    .setShowWhen(true)
                    .setAutoCancel(true)
                    .setContentIntent(openAppIntent(context, ID_SUMMARY));
            addOpenPostAction(context, b, a);
            post(context, ID_SUMMARY, b);
            return;
        }

        NotificationCompat.InboxStyle inbox = new NotificationCompat.InboxStyle();
        int shown = 0;
        for (AnnouncementEntity a : posts) {
            if (shown++ == 5) break;
            inbox.addLine(a.pageName + ": " + shorten(summaryText(a), 60));
        }
        if (posts.size() > 5) inbox.setSummaryText("+" + (posts.size() - 5) + " more in Kaleo");

        String title = posts.size() + " new announcements";
        NotificationCompat.Builder b = new NotificationCompat.Builder(context, CHANNEL_UPDATES)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(context.getColor(R.color.brand_primary))
                .setContentTitle(title)
                .setContentText(posts.get(0).pageName + ": " + shorten(summaryText(posts.get(0)), 60))
                .setStyle(inbox.setBigContentTitle(title))
                .setNumber(posts.size())
                .setGroup(GROUP_UPDATES)
                .setAutoCancel(true)
                .setContentIntent(openAppIntent(context, ID_SUMMARY));
        post(context, ID_SUMMARY, b);
    }

    private static void addOpenPostAction(Context context, NotificationCompat.Builder b, AnnouncementEntity a) {
        if (a.permalink == null || a.permalink.isEmpty()) return;
        Intent view = new Intent(Intent.ACTION_VIEW, Uri.parse(a.permalink));
        PendingIntent pi = PendingIntent.getActivity(context,
                ("post" + a.facebookPostId).hashCode(), view,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        b.addAction(0, "Open post", pi);
    }

    private static PendingIntent openAppIntent(Context context, int requestCode) {
        Intent i = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(context, requestCode, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    @SuppressWarnings("MissingPermission")   // checked in hasPermission()
    private static void post(Context context, int id, NotificationCompat.Builder b) {
        if (!hasPermission(context)) return;
        try {
            NotificationManagerCompat.from(context).notify(id, b.build());
        } catch (SecurityException ignored) {
        }
    }

    /** Prefer the backend's one-line takeaway; fall back to the post text. */
    private static String summaryText(AnnouncementEntity a) {
        String t = a.keyTakeaway != null && !a.keyTakeaway.isEmpty() ? a.keyTakeaway : a.message;
        if (t == null) return "";
        return t.replaceFirst("(?i)^\\s*sample:\\s*", "").trim();
    }

    private static String shorten(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max).trim() + "…";
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
