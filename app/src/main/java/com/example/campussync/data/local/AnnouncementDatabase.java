package com.example.campussync.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {AnnouncementEntity.class}, version = 1, exportSchema = false)
public abstract class AnnouncementDatabase extends RoomDatabase {

    private static volatile AnnouncementDatabase INSTANCE;

    public abstract AnnouncementDao announcementDao();

    public static AnnouncementDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AnnouncementDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AnnouncementDatabase.class,
                            "campussync_database"
                    ).build();
                }
            }
        }
        return INSTANCE;
    }
}
