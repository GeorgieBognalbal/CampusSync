
package com.example.campussync.data.local;

import android.content.Context;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OfflineAnnouncementRepository {

    private final AnnouncementDao dao;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    public OfflineAnnouncementRepository(Context context) {
        AnnouncementDatabase database =
                AnnouncementDatabase.getInstance(context);

        dao = database.announcementDao();
    }

    // Retrieve all downloaded announcements
    public void getAll(Callback<List<AnnouncementEntity>> callback) {
        executor.execute(() -> {
            List<AnnouncementEntity> announcements = dao.getAll();

            if (callback != null) {
                callback.onResult(announcements);
            }
        });
    }

    // Search downloaded announcements
    public void search(
            String keyword,
            Callback<List<AnnouncementEntity>> callback
    ) {
        executor.execute(() -> {
            List<AnnouncementEntity> results =
                    dao.searchLike(keyword);

            if (callback != null) {
                callback.onResult(results);
            }
        });
    }

    public interface Callback<T> {
        void onResult(T result);
    }
}
