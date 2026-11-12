package com.example.campussync.calendar;

import android.text.format.DateUtils;

import com.example.campussync.data.local.AnnouncementEntity;

import java.util.Arrays;
import java.util.Locale;

/**
 * Finds dates inside announcement text ("Nov 24 to Nov 28", "before Dec 5",
 * "this Friday", "tomorrow"/"bukas"). Shared by the Calendar tab and the
 * background exam/deadline reminders, so both always agree on the dates.
 */
public final class CampusDates {

    private CampusDates() { }

    /** A dated item pulled out of an announcement. Dates are local, all-day. */
    public static class DatedPost {
        public String postId, title, pageName, permalink, message;
        public boolean deadline;
        public int y, m, d;        // start date (m is 0-based like java.util.Calendar)
        public int ey, em, ed;     // end date (same as start for single-day items)

        public long startMillis() { return localMillis(y, m, d); }
        public long endMillisExclusive() { return localMillis(ey, em, ed) + DateUtils.DAY_IN_MILLIS; }
        public boolean multiDay() { return y != ey || m != em || d != ed; }
    }

    public static long localMillis(int y, int m, int d) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.clear();
        c.set(y, m, d, 0, 0, 0);
        return c.getTimeInMillis();
    }

    public static long utcMillis(int y, int m, int d) {
        java.util.Calendar c = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));
        c.clear();
        c.set(y, m, d, 0, 0, 0);
        return c.getTimeInMillis();
    }

    private static final String MONTHS = "jan|feb|mar|apr|may|jun|jul|aug|sept|sep|oct|nov|dec";
    private static final java.util.regex.Pattern MONTH_DAY = java.util.regex.Pattern.compile(
            "\\b(" + MONTHS + ")[a-z]*\\.?\\s+(\\d{1,2})(?:st|nd|rd|th)?"
                    + "(?:\\s*(?:to|until|-|–|—)\\s*(?:(" + MONTHS + ")[a-z]*\\.?\\s+)?(\\d{1,2})(?:st|nd|rd|th)?)?",
            java.util.regex.Pattern.CASE_INSENSITIVE);
    private static final java.util.regex.Pattern WEEKDAY = java.util.regex.Pattern.compile(
            "\\b(sunday|monday|tuesday|wednesday|thursday|friday|saturday)\\b",
            java.util.regex.Pattern.CASE_INSENSITIVE);
    private static final java.util.regex.Pattern TOMORROW = java.util.regex.Pattern.compile(
            "\\b(tomorrow|bukas)\\b", java.util.regex.Pattern.CASE_INSENSITIVE);

    static int monthIndex(String s) {
        String k = s.toLowerCase(Locale.ROOT);
        if (k.startsWith("sep")) return 8;
        String[] names = {"jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec"};
        for (int i = 0; i < names.length; i++) if (k.startsWith(names[i])) return i;
        return -1;
    }

    /** Finds a date in the post ("Nov 24 to Nov 28", "before Dec 5", "this Friday", "tomorrow"). */
    public static DatedPost extract(AnnouncementEntity a) {
        if (a.message == null) return null;
        String msg = a.message;

        java.util.Calendar posted = java.util.Calendar.getInstance();
        posted.setTimeInMillis(a.createdAt);

        DatedPost item = new DatedPost();
        java.util.regex.Matcher md = MONTH_DAY.matcher(msg);

        if (md.find()) {
            int m = monthIndex(md.group(1));
            int d = Integer.parseInt(md.group(2));
            if (m < 0 || d < 1 || d > 31) return null;
            int y = posted.get(java.util.Calendar.YEAR);
            // A date far before the post date means next year (e.g. posted in Dec about "Jan 5")
            if (localMillis(y, m, d) < a.createdAt - 60L * DateUtils.DAY_IN_MILLIS) y++;
            item.y = y; item.m = m; item.d = d;

            if (md.group(4) != null) {
                int em = md.group(3) != null ? monthIndex(md.group(3)) : m;
                int ed = Integer.parseInt(md.group(4));
                int ey = y;
                if (em < m || (em == m && ed < d)) {
                    if (md.group(3) == null) em = (m + 1) % 12;   // "Nov 28 - 2" → Dec 2
                    if (em < m) ey = y + 1;
                }
                item.ey = ey; item.em = em; item.ed = ed;
            } else {
                item.ey = y; item.em = m; item.ed = d;
            }
        } else {
            java.util.Calendar c = (java.util.Calendar) posted.clone();
            java.util.regex.Matcher wd = WEEKDAY.matcher(msg);
            if (TOMORROW.matcher(msg).find()) {
                c.add(java.util.Calendar.DAY_OF_MONTH, 1);
            } else if (wd.find()) {
                String[] days = {"sunday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday"};
                int target = Arrays.asList(days).indexOf(wd.group(1).toLowerCase(Locale.ROOT)) + 1;
                int diff = (target - c.get(java.util.Calendar.DAY_OF_WEEK) + 7) % 7;
                c.add(java.util.Calendar.DAY_OF_MONTH, diff == 0 ? 7 : diff);
            } else {
                return null;
            }
            item.y = item.ey = c.get(java.util.Calendar.YEAR);
            item.m = item.em = c.get(java.util.Calendar.MONTH);
            item.d = item.ed = c.get(java.util.Calendar.DAY_OF_MONTH);
        }

        String lower = msg.toLowerCase(Locale.ROOT);
        item.postId = a.facebookPostId;
        item.pageName = a.pageName;
        item.permalink = a.permalink;
        item.message = msg;
        item.deadline = lower.contains("before") || lower.contains("deadline")
                || lower.contains("until") || lower.contains("submit");
        item.title = titleFor(msg, item);
        return item;
    }

    static String titleFor(String msg, DatedPost item) {
        String t = msg.replaceFirst("(?i)^\\s*sample:\\s*", "");
        // Cut at the first sentence or dash
        for (String sep : new String[]{" — ", " - ", ". ", "! ", ": "}) {
            int i = t.indexOf(sep);
            if (i > 8) t = t.substring(0, i);
        }
        t = shorten(t.trim(), 60);
        if (item.deadline && !t.toLowerCase(Locale.ROOT).contains("deadline")) t = "Deadline: " + t;
        return t;
    }

    static String shorten(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max).trim() + "…";
    }

    /** Today's date at local midnight. */
    public static long todayMillis() {
        java.util.Calendar c = java.util.Calendar.getInstance();
        return localMillis(c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH),
                c.get(java.util.Calendar.DAY_OF_MONTH));
    }
}
