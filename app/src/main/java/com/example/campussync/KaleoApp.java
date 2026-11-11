package com.example.campussync;

import android.app.Application;

import com.example.campussync.notifications.KaleoNotifications;
import com.example.campussync.notifications.KaleoReminders;
import com.example.campussync.sync.SyncScheduler;

/** Runs once when the app process starts: kicks off background syncing. */
public class KaleoApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        KaleoNotifications.createChannels(this);
        SyncScheduler.start(this);
        KaleoReminders.schedule(this);
    }
}
