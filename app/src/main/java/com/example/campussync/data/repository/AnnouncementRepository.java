package com.example.campussync.data.repository;

import com.example.campussync.data.local.AnnouncementDao;
import com.example.campussync.data.local.AnnouncementEntity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The one place the screens get announcements from (owner: Jairelle).
 * All methods hit the database, so call them off the main thread (e.g. an ExecutorService).
 *
 * For Kahrd's LocalNanoAIEngine:
 *   searchAll("scholarship requirements")  -> posts matching ALL words (precise)
 *   searchAny("may pasok ba bukas")        -> posts matching ANY word (forgiving, for chat)
 *   getByCategory("Suspension")            -> Feed chips
 *   getUpcomingDeadlines("2026-11-11")     -> "kailan deadline?" questions
 */
public class AnnouncementRepository {

    private static final int SEARCH_LIMIT = 50;

    private final AnnouncementDao announcementDao;

    public AnnouncementRepository(AnnouncementDao announcementDao) {
        this.announcementDao = announcementDao;
    }

    public List<AnnouncementEntity> getAll() {
        return announcementDao.getAll();
    }

    /** Used by the Feed search box and the current chat code. Same as searchAll(). */
    public List<AnnouncementEntity> search(String text) {
        return searchAll(text);
    }

    /** Full-text search: every word must match (word prefixes count, "schol" finds "scholarship"). */
    public List<AnnouncementEntity> searchAll(String text) {
        return runSearch(text, " ");
    }

    /** Full-text search: any word may match. Better for chat questions. */
    public List<AnnouncementEntity> searchAny(String text) {
        return runSearch(text, " OR ");
    }

    private List<AnnouncementEntity> runSearch(String text, String joiner) {
        String ftsQuery = toFtsQuery(text, joiner);
        if (ftsQuery.isEmpty()) {
            return text == null || text.trim().isEmpty()
                    ? announcementDao.getAll()
                    : new ArrayList<>();
        }
        List<AnnouncementEntity> results = announcementDao.searchFts(ftsQuery, SEARCH_LIMIT);
        if (results.isEmpty()) {
            // FTS matches word starts only; fall back to a substring match for odd inputs.
            results = announcementDao.searchLike(text.trim());
        }
        return results;
    }

    /**
     * Turns what the user typed into a safe FTS query.
     * "May pasok ba?" -> "may* pasok* ba*"  (quotes, symbols and FTS operators are removed)
     */
    static String toFtsQuery(String text, String joiner) {
        if (text == null) return "";
        String cleaned = text.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", " ").trim();
        if (cleaned.isEmpty()) return "";

        StringBuilder query = new StringBuilder();
        for (String word : cleaned.split("\\s+")) {
            if (word.length() < 2 || word.equals("or") || word.equals("and")
                    || word.equals("not") || word.equals("near")) {
                continue;
            }
            if (query.length() > 0) query.append(joiner);
            query.append(word).append('*');
        }
        return query.toString();
    }

    public List<AnnouncementEntity> getByCategory(String category) {
        return announcementDao.getByCategory(category);
    }

    public List<AnnouncementEntity> getUrgent() {
        return announcementDao.getUrgent();
    }

    /** Posts with at least one important date on or after `todayIso` (YYYY-MM-DD). */
    public List<AnnouncementEntity> getUpcomingDeadlines(String todayIso) {
        List<AnnouncementEntity> upcoming = new ArrayList<>();
        for (AnnouncementEntity a : announcementDao.getWithDates()) {
            for (ImportantDate d : getImportantDates(a)) {
                if (d.date.compareTo(todayIso) >= 0) {
                    upcoming.add(a);
                    break;
                }
            }
        }
        return upcoming;
    }

    /** Parses the JSON in AnnouncementEntity.importantDates. */
    public static List<ImportantDate> getImportantDates(AnnouncementEntity a) {
        List<ImportantDate> dates = new ArrayList<>();
        if (a.importantDates == null || a.importantDates.isEmpty()) return dates;
        try {
            JSONArray arr = new JSONArray(a.importantDates);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                dates.add(new ImportantDate(o.optString("label", ""), o.optString("date", "")));
            }
        } catch (JSONException ignored) {
            // Bad data from the server: treat as no dates.
        }
        return dates;
    }

    /** Parses "|Suspension|Exams|" into ["Suspension", "Exams"]. */
    public static List<String> getCategories(AnnouncementEntity a) {
        List<String> list = new ArrayList<>();
        if (a.categories == null) return list;
        for (String c : a.categories.split("\\|")) {
            if (!c.isEmpty()) list.add(c);
        }
        return list;
    }

    public void insertAll(List<AnnouncementEntity> announcements) {
        announcementDao.insertAll(announcements);
    }

    public int count() {
        return announcementDao.count();
    }

    public static class ImportantDate {
        public final String label;
        public final String date;   // YYYY-MM-DD

        public ImportantDate(String label, String date) {
            this.label = label;
            this.date = date;
        }
    }
}
