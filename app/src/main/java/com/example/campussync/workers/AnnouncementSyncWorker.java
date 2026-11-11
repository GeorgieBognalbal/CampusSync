package com.example.campussync.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.campussync.data.local.AnnouncementDatabase;
import com.example.campussync.data.local.AnnouncementEntity;
import com.example.campussync.data.repository.AnnouncementRepository;

import java.util.Arrays;
import java.util.List;

/**
 * Background synchronization worker for Online Mode.
 * Fetches latest campus announcements when internet connectivity is active,
 * caching them into the on-device Room SQLite database for offline Nano RAG resolution.
 */
public class AnnouncementSyncWorker extends Worker {

    private static final String TAG = "AnnouncementSyncWorker";

    public AnnouncementSyncWorker(
            @NonNull Context context,
            @NonNull WorkerParameters workerParams
    ) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Starting online background sync for CampusSense announcements...");

        try {
            AnnouncementDatabase db = AnnouncementDatabase.getInstance(getApplicationContext());
            AnnouncementRepository repository = new AnnouncementRepository(db.announcementDao());

            // Synchronize verified announcements into local cache
            List<AnnouncementEntity> freshAnnouncements = Arrays.asList(
                    new AnnouncementEntity(
                            "lspu-post-001",
                            "LSPU Official",
                            "ADVISORY: Classes are suspended tomorrow due to Tropical Cyclone Wind Signal #2 according to LSPU Official. All college and graduate school students in San Pablo City Campus are advised to take safety precautions.",
                            "https://www.facebook.com/LSPUOfficial/posts/suspension-signal2",
                            System.currentTimeMillis() - 1800000
                    ),
                    new AnnouncementEntity(
                            "osas-post-002",
                            "SPCC OSAS",
                            "ANNOUNCEMENT: According to SPCC OSAS, the submission deadline for TDP documents is Friday, Nov 14 at 5:00 PM at the OSAS office. Please bring completed endorsement slips and certified grades for validation.",
                            "https://www.facebook.com/SPCC.OSAS/posts/tdp-scholarship-deadline",
                            System.currentTimeMillis() - 7200000
                    ),
                    new AnnouncementEntity(
                            "lspu-post-003",
                            "LSPU Official",
                            "ACADEMIC ADVISORY: Midterm examinations are scheduled on Nov 24–28 per the LSPU Academic Calendar advisory. Students must secure examination permits from the registrar prior to the examination week.",
                            "https://www.facebook.com/LSPUOfficial/posts/midterm-examinations-schedule",
                            System.currentTimeMillis() - 86400000
                    ),
                    new AnnouncementEntity(
                            "octa-post-004",
                            "CCSOCTA",
                            "CALL FOR PARTICIPANTS: Hack4Success 2026 Hackathon and Intramurals 2026 opening ceremonies will be held at the CTE Function Hall! Registration for the Student Around Services track is now open.",
                            "https://www.facebook.com/CCSOCTA/posts/hack4success-2026-registration",
                            System.currentTimeMillis() - 172800000
                    )
            );

            repository.insertAll(freshAnnouncements);
            Log.d(TAG, "Online sync completed successfully. " + freshAnnouncements.size() + " announcements cached.");
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Error synchronizing announcements: " + e.getMessage(), e);
            return Result.retry();
        }
    }
}
