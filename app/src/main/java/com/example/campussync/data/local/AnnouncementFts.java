package com.example.campussync.data.local;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Fts4;
import androidx.room.FtsOptions;
import androidx.room.PrimaryKey;

/**
 * Full-Text Search index over announcements (owner: Jairelle).
 *
 * Room keeps this in sync with the announcements table automatically
 * (it creates triggers), so you never insert into it yourself.
 * Search it through AnnouncementDao.searchFts().
 */
@Fts4(contentEntity = AnnouncementEntity.class, tokenizer = FtsOptions.TOKENIZER_UNICODE61)
@Entity(tableName = "announcements_fts")
public class AnnouncementFts {

    @PrimaryKey
    @ColumnInfo(name = "rowid")
    public int rowid;

    public String pageName;
    public String message;
    public String keyTakeaway;
    public String categories;
}
