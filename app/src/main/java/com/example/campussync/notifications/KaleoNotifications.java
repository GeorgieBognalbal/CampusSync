package com.example.campussync.notifications;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.example.campussync.MainActivity;
import com.example.campussync.R;
import com.example.campussync.data.local.AnnouncementEntity;

import java.util.List;

public class KaleoNotifications {

    public static final String KEY_URGENT_ENABLED = "kaleo_urgent_enabled";
    public static final String KEY_ALL_ENABLED = "kaleo_all_enabled";

    private static final String CHANNEL_URGENT = "kaleo_urgent_channel";
    private static final String CHANNEL_REGULAR = "kaleo_regular_channel";

    public static void createChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;

            NotificationChannel urgent = new NotificationChannel(
                    CHANNEL_URGENT,
                    "Urgent Campus Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            urgent.setDescription("Class suspensions, emergency weather warnings and urgent advisories");
            urgent.enableVibration(true);

            NotificationChannel regular = new NotificationChannel(
                    CHANNEL_REGULAR,
                    "Campus Announcements",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            regular.setDescription("Regular campus announcements, events, exams and scholarship notices");

            nm.createNotificationChannel(urgent);
            nm.createNotificationChannel(regular);
        }
    }

    public static boolean hasPermission(Context context) {
        if (Build.VERSION.SDK_INT >= 33) {
            return ContextCompat.checkSelfPermission(context, "android.permission.POST_NOTIFICATIONS")
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    public static void sendTest(Context context) {
        if (!hasPermission(context)) return;

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_URGENT)
                .setSmallIcon(R.drawable.ic_bell)
                .setContentTitle("Kaleo Campus Alert [Test]")
                .setContentText("Notifications are working properly! You'll receive real-time campus updates.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pi)
                .setAutoCancel(true);

        nm.notify(9999, builder.build());
    }

    public static void notifyNewPosts(Context context, List<AnnouncementEntity> newPosts) {
        if (newPosts == null || newPosts.isEmpty() || !hasPermission(context)) return;

        SharedPreferences prefs = context.getSharedPreferences("campussync", Context.MODE_PRIVATE);
        boolean urgentOn = prefs.getBoolean(KEY_URGENT_ENABLED, true);
        boolean allOn = prefs.getBoolean(KEY_ALL_ENABLED, true);

        if (!urgentOn && !allOn) return;

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        int notifId = 1000;
        for (AnnouncementEntity post : newPosts) {
            boolean isSuspension = isUrgentPost(post.message);
            if (isSuspension && !urgentOn) continue;
            if (!isSuspension && !allOn) continue;

            Intent intent = new Intent(context, MainActivity.class);
            PendingIntent pi = PendingIntent.getActivity(context, notifId, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            String channelId = isSuspension ? CHANNEL_URGENT : CHANNEL_REGULAR;
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(R.drawable.ic_bell)
                    .setContentTitle(isSuspension ? "⚠️ " + post.pageName : post.pageName)
                    .setContentText(post.message)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(post.message))
                    .setPriority(isSuspension ? NotificationCompat.PRIORITY_HIGH : NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pi)
                    .setAutoCancel(true);

            nm.notify(notifId++, builder.build());
        }
    }

    private static boolean isUrgentPost(String message) {
        if (message == null) return false;
        String m = message.toLowerCase();
        return m.contains("suspend") || m.contains("suspindido") || m.contains("walang pasok")
                || m.contains("typhoon") || m.contains("signal #") || m.contains("emergency");
    }
}
