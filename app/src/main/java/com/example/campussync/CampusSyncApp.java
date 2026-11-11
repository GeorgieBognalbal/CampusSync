package com.example.campussync;

import android.app.Application;

import com.example.campussync.sync.SyncScheduler;

/** Runs once when the app process starts: kicks off background syncing. */
public class CampusSyncApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        SyncScheduler.start(this);
    }
}
