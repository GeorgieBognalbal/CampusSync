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

    /**
     * Full-text search. `ftsQuery` must already be in FTS syntax, e.g. "exam* midterm*".
     * Use AnnouncementRepository.search() instead of calling this directly; it builds
     * the query safely from what the user typed.
     */
    @Query("SELECT a.* FROM announcements a " +
            "JOIN announcements_fts ON a.rowid = announcements_fts.rowid " +
            "WHERE announcements_fts MATCH :ftsQuery " +
            "ORDER BY a.createdAt DESC LIMIT :limit")
    List<AnnouncementEntity> searchFts(String ftsQuery, int limit);

    /** Old substring search, kept as a fallback when FTS finds nothing. */
    @Query("SELECT * FROM announcements WHERE message LIKE '%' || :keyword || '%' " +
            "OR pageName LIKE '%' || :keyword || '%' ORDER BY createdAt DESC")
    List<AnnouncementEntity> searchLike(String keyword);

    /** For the Feed category chips, e.g. getByCategory("Suspension"). */
    @Query("SELECT * FROM announcements WHERE categories LIKE '%|' || :category || '|%' " +
            "ORDER BY createdAt DESC")
    List<AnnouncementEntity> getByCategory(String category);

    @Query("SELECT * FROM announcements WHERE urgency = 'High' ORDER BY createdAt DESC")
    List<AnnouncementEntity> getUrgent();

    @Query("SELECT * FROM announcements WHERE importantDates IS NOT NULL " +
            "AND importantDates != '[]' ORDER BY createdAt DESC")
    List<AnnouncementEntity> getWithDates();

    /** Which of these ids are already saved (used to find genuinely new posts). */
    @Query("SELECT facebookPostId FROM announcements WHERE facebookPostId IN (:ids)")
    List<String> getExistingIds(List<String> ids);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<AnnouncementEntity> announcements);

    @Query("DELETE FROM announcements WHERE facebookPostId IN (:ids)")
    void deleteByIds(List<String> ids);

    /** Removes the built-in demo posts (ids starting with "demo-"). */
    @Query("DELETE FROM announcements WHERE facebookPostId LIKE 'demo-%'")
    int deleteDemoPosts();

    /** Real cached records; used to recover safely from a stale server cursor. */
    @Query("SELECT COUNT(*) FROM announcements WHERE facebookPostId NOT LIKE 'demo-%'")
    int countRealAnnouncements();

    /** Insert fallback data only while the database is empty, under one transaction. */
    @androidx.room.Transaction
    default int insertDemoIfEmpty(List<AnnouncementEntity> demos) {
        if (count() != 0) return 0;
        insertAll(demos);
        return demos.size();
    }

    @Query("DELETE FROM announcements")
    void deleteAll();

    @Query("SELECT COUNT(*) FROM announcements")
    int count();
}
