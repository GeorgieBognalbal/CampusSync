package com.example.campussync.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * One announcement stored on the phone (owner: Jairelle).
 *
 * The first 5 fields are what the Feed and Chat screens already use.
 * The rest come from the backend sync (see Backend/SCHEMA.md).
 */
@Entity(
        tableName = "announcements",
        indices = {@Index("createdAt"), @Index("urgency")}
)
public class AnnouncementEntity {

    @NonNull
    @PrimaryKey
    public String facebookPostId;

    public String pageName;

    /** Text to show: the AI's clean text if available, otherwise the raw caption. */
    public String message;

    public String permalink;
    public long createdAt;

    // ---- from the sync API ----

    /** Original caption from Facebook (before AI cleanup). */
    public String rawCaption;

    /** One-sentence summary from the AI. Null if the AI hasn't processed it yet. */
    public String keyTakeaway;

    /** Categories wrapped in pipes, e.g. "|Suspension|Exams|". Use getByCategory() to query. */
    public String categories;

    /** "High", "Medium", "Low", or null. */
    public String urgency;

    /** JSON array, e.g. [{"label":"Deadline","date":"2026-12-05"}]. */
    public String importantDates;

    /** JSON array of image URLs. */
    public String imageUrls;

    /** Server's updated_at (epoch ms). */
    public long updatedAt;

    public AnnouncementEntity(
            @NonNull String facebookPostId,
            String pageName,
            String message,
            String permalink,
            long createdAt
    ) {
        this.facebookPostId = facebookPostId;
        this.pageName = pageName;
        this.message = message;
        this.permalink = permalink;
        this.createdAt = createdAt;
    }
}
