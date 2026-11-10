
package com.example.campussync.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "announcements")
public class AnnouncementEntity {

    @NonNull
    @PrimaryKey
    public String facebookPostId;

    public String pageName;
    public String message;
    public String permalink;
    public long createdAt;

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
