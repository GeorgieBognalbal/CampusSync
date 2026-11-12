package com.example.campussync.calendar;

import com.example.campussync.data.local.AnnouncementEntity;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses and handles dates from campus announcements for schedule display and calendar integration.
 */
public class CampusDates {

    public static class DatedPost {
        public String postId;
        public String title;
        public String pageName;
        public String permalink;
        public String message;
        public boolean deadline;
        public int y, m, d;
        public int ey, em, ed;

        public long startMillis() {
            return localMillis(y, m, d);
        }

        public long endMillisExclusive() {
            if (ey > 0 && em >= 0 && ed > 0) {
                return localMillis(ey, em, ed) + 86400000L;
            }
            return startMillis() + 86400000L;
        }

        public boolean multiDay() {
            return ey > 0 && (ey != y || em != m || ed != d);
        }
    }

    public static long localMillis(int y, int m, int d) {
        Calendar c = Calendar.getInstance();
        c.set(y, m, d, 0, 0, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    public static long utcMillis(int y, int m, int d) {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        c.set(y, m, d, 0, 0, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private static final String[] MONTH_NAMES = {
            "jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec"
    };

    public static DatedPost extract(AnnouncementEntity a) {
        if (a == null || a.message == null || a.message.trim().isEmpty()) {
            return null;
        }

        Calendar now = Calendar.getInstance();
        int curYear = now.get(Calendar.YEAR);

        String text = a.message;
        String lower = text.toLowerCase(Locale.ROOT);

        Pattern p = Pattern.compile("(?i)(jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:tember)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)\\s+(\\d{1,2})(?:\\s*(?:to|-|–)\\s*(?:(\\d{1,2})|(?:(jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:tember)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)\\s+(\\d{1,2}))))?");
        Matcher m = p.matcher(text);

        if (!m.find()) {
            return null;
        }

        String mStr = m.group(1).substring(0, 3).toLowerCase(Locale.ROOT);
        int month = parseMonth(mStr);
        int day = Integer.parseInt(m.group(2));

        DatedPost post = new DatedPost();
        post.postId = a.facebookPostId;
        post.pageName = a.pageName;
        post.permalink = a.permalink;
        post.message = a.message;
        post.title = extractTitle(a.message);
        post.deadline = lower.contains("deadline") || lower.contains("until") || lower.contains("hanggang") || lower.contains("due");
        post.y = curYear;
        post.m = month;
        post.d = day;

        if (m.group(3) != null) {
            post.ey = curYear;
            post.em = month;
            post.ed = Integer.parseInt(m.group(3));
        } else if (m.group(4) != null && m.group(5) != null) {
            post.ey = curYear;
            post.em = parseMonth(m.group(4).substring(0, 3).toLowerCase(Locale.ROOT));
            post.ed = Integer.parseInt(m.group(5));
        } else {
            post.ey = post.y;
            post.em = post.m;
            post.ed = post.d;
        }

        return post;
    }

    private static int parseMonth(String shortName) {
        for (int i = 0; i < MONTH_NAMES.length; i++) {
            if (shortName.startsWith(MONTH_NAMES[i])) return i;
        }
        return 0;
    }

    private static String extractTitle(String message) {
        String[] lines = message.split("\\n+");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.length() > 3) {
                if (trimmed.length() > 60) return trimmed.substring(0, 57) + "...";
                return trimmed;
            }
        }
        return "Campus Announcement";
    }
}
