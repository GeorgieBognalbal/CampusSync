package com.example.campussync.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.campussync.sync.SyncState;

@Database(
        entities = {AnnouncementEntity.class, AnnouncementFts.class},
        version = 2,
        exportSchema = false
)
public abstract class AnnouncementDatabase extends RoomDatabase {

    private static volatile AnnouncementDatabase INSTANCE;

    public abstract AnnouncementDao announcementDao();

    public static AnnouncementDatabase getInstance(final Context context) {
        if (INSTANCE == null) {
            synchronized (AnnouncementDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AnnouncementDatabase.class,
                                    "campussync_database"
                            )
                            // The phone only keeps a copy of the server's data. If the table
                            // layout (version) changes without a migration, wipe and re-sync
                            // instead of crashing with "A migration from X to Y was required".
                            .fallbackToDestructiveMigration(true)
                            .addCallback(new Callback() {
                                @Override
                                public void onCreate(SupportSQLiteDatabase db) {
                                    // Empty database -> download everything on next sync.
                                    SyncState.reset(context.getApplicationContext());
                                }

                                @Override
                                public void onDestructiveMigration(SupportSQLiteDatabase db) {
                                    // Tables were wiped -> the old cursor would skip everything.
                                    SyncState.reset(context.getApplicationContext());
                                }
                            })
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
