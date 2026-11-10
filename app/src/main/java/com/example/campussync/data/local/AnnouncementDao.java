package com.example.campussync.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface AnnouncementDao {

    @Query("SELECT * FROM announcements ORDER BY createdAt DESC")
    List<AnnouncementEntity> getAll();

    @Query("SELECT * FROM announcements WHERE message LIKE '%' || :keyword || '%' OR pageName LIKE '%' || :keyword || '%' ORDER BY createdAt DESC")
    List<AnnouncementEntity> search(String keyword);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<AnnouncementEntity> announcements);

    @Query("DELETE FROM announcements")
    void deleteAll();

    @Query("SELECT COUNT(*) FROM announcements")
    int count();
}
