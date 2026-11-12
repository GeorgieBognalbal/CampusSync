package com.example.campussync.ai;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Log;

import com.example.campussync.BuildConfig;
import com.example.campussync.data.local.AnnouncementEntity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

/**
 * Online answers: sends the question, the recent chat and the most relevant saved
 * announcements to Gemini, and asks for an answer grounded in those announcements.
 * Returns null when offline or when Gemini can't be reached, so the local engine answers.
 */
public class CloudAIAdapter implements LocalNanoAIEngine.CloudFallbackProvider {

    private static final String TAG = "CloudAIAdapter";

    private static final String GEMINI_BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/";

    private static final String[] CANDIDATE_MODELS = {
            "gemini-3.5-flash-lite",
            "gemini-3.6-flash"
    };

    private static final int MAX_CONTEXT_POSTS = 20;
    private static final int ALWAYS_INCLUDE_NEWEST = 8;
    private static final int MAX_POST_CHARS = 900;
    private static final int MAX_HISTORY_TURNS = 6;

    private static final TimeZone PH_TIME = TimeZone.getTimeZone("Asia/Manila");

    private final Context context;

    public CloudAIAdapter(Context context) {
        this.context = context != null ? context.getApplicationContext() : null;
    }

    public boolean isOnline() {
        if (context == null) return false;
        try {
            ConnectivityManager cm = (ConnectivityManager)
                    context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return false;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                android.net.Network network = cm.getActiveNetwork();
                if (network == null) return false;
                NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
                return capabilities != null && (
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                );
            } else {
                @SuppressWarnings("deprecation")
                NetworkInfo netInfo = cm.getActiveNetworkInfo();
                return netInfo != null && netInfo.isConnected();
            }
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean hasApiKey() {
        String key = BuildConfig.GEMINI_API_KEY;
        return key != null && !key.trim().isEmpty() && !key.equals("null");
    }

    @Override
    public LocalNanoAIEngine.NanoAIResult queryCloudFallback(String userQuery,
                                                            List<AnnouncementEntity> posts,
                                                            List<LocalNanoAIEngine.Turn> history) {
        if (!hasApiKey() || !isOnline()) {
            return null;
        }

        List<AnnouncementEntity> contextPosts = pickContextPosts(userQuery, posts);
        String apiKey = BuildConfig.GEMINI_API_KEY.trim();

        for (String modelName : CANDIDATE_MODELS) {
            try {
                JSONObject reply = callGemini(modelName, userQuery, contextPosts, history, apiKey);
                if (reply == null) continue;

                String answer = reply.optString("answer", "").trim();
                if (answer.isEmpty()) continue;

                List<AnnouncementEntity> used = new ArrayList<>();
                JSONArray ids = reply.optJSONArray("source_ids");
                if (ids != null) {
                    for (int i = 0; i < ids.length() && used.size() < 3; i++) {
                        int index = ids.optInt(i, 0) - 1;
                        if (index >= 0 && index < contextPosts.size()
                                && !used.contains(contextPosts.get(index))) {
                            used.add(contextPosts.get(index));
                        }
                    }
                }

                return new LocalNanoAIEngine.NanoAIResult(
                        answer,
                        LocalNanoAIEngine.Intent.GENERAL,
                        0.95f,
                        false,
                        used
                );
            } catch (Exception e) {
                Log.w(TAG, "Gemini call failed for " + modelName + ": " + e.getMessage());
            }
        }
        return null;
    }

    /** The newest posts plus the ones that share the most words with the question. */
    private List<AnnouncementEntity> pickContextPosts(String query, List<AnnouncementEntity> posts) {
        if (posts == null || posts.isEmpty()) return Collections.emptyList();

        List<AnnouncementEntity> byNewest = new ArrayList<>(posts);
        byNewest.sort((a, b) -> Long.compare(b.createdAt, a.createdAt));

        Set<String> words = new HashSet<>();
        for (String w : query.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+")) {
            if (w.length() > 2) words.add(w);
        }

        List<AnnouncementEntity> byMatch = new ArrayList<>(byNewest);
        List<Integer> scores = new ArrayList<>();
        for (AnnouncementEntity post : byMatch) scores.add(matchScore(post, words));
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < byMatch.size(); i++) order.add(i);
        order.sort((a, b) -> Integer.compare(scores.get(b), scores.get(a)));

        List<AnnouncementEntity> picked = new ArrayList<>();
        for (int i = 0; i < byNewest.size() && picked.size() < ALWAYS_INCLUDE_NEWEST; i++) {
            picked.add(byNewest.get(i));
        }
        for (int i : order) {
            if (picked.size() >= MAX_CONTEXT_POSTS || scores.get(i) == 0) break;
            AnnouncementEntity post = byMatch.get(i);
            if (!picked.contains(post)) picked.add(post);
        }
        picked.sort((a, b) -> Long.compare(b.createdAt, a.createdAt));
        return picked;
    }

    private static int matchScore(AnnouncementEntity post, Set<String> words) {
        if (words.isEmpty()) return 0;
        String text = (safe(post.message) + " " + safe(post.keyTakeaway) + " "
                + safe(post.categories) + " " + safe(post.pageName)).toLowerCase(Locale.ROOT);
        int score = 0;
        for (String w : words) {
            if (text.contains(w)) score++;
        }
        return score;
    }

    private JSONObject callGemini(String modelName, String userQuery,
                                  List<AnnouncementEntity> posts,
                                  List<LocalNanoAIEngine.Turn> history,
                                  String apiKey) throws Exception {
        URL url = new URL(GEMINI_BASE_URL + modelName + ":generateContent");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setRequestProperty("x-goog-api-key", apiKey);
        conn.setConnectTimeout(6000);
        conn.setReadTimeout(15000);
        conn.setDoOutput(true);

        JSONObject root = new JSONObject();
        root.put("systemInstruction", new JSONObject()
                .put("parts", new JSONArray().put(new JSONObject()
                        .put("text", buildInstructions(posts)))));

        JSONArray contents = new JSONArray();
        if (history != null) {
            int start = Math.max(0, history.size() - MAX_HISTORY_TURNS);
            for (int i = start; i < history.size(); i++) {
                LocalNanoAIEngine.Turn turn = history.get(i);
                if (turn.text == null || turn.text.trim().isEmpty()) continue;
                contents.put(new JSONObject()
                        .put("role", turn.fromUser ? "user" : "model")
                        .put("parts", new JSONArray().put(new JSONObject().put("text", turn.text.trim()))));
            }
        }
        contents.put(new JSONObject()
                .put("role", "user")
                .put("parts", new JSONArray().put(new JSONObject().put("text", userQuery.trim()))));
        root.put("contents", contents);

        JSONObject schema = new JSONObject()
                .put("type", "object")
                .put("properties", new JSONObject()
                        .put("answer", new JSONObject().put("type", "string"))
                        .put("source_ids", new JSONObject()
                                .put("type", "array")
                                .put("items", new JSONObject().put("type", "integer"))))
                .put("required", new JSONArray().put("answer").put("source_ids"));

        root.put("generationConfig", new JSONObject()
                .put("temperature", 0.3)
                .put("responseMimeType", "application/json")
                .put("responseJsonSchema", schema));

        byte[] requestBody = root.toString().getBytes(StandardCharsets.UTF_8);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(requestBody);
        }

        int statusCode = conn.getResponseCode();
        if (statusCode < 200 || statusCode >= 300) {
            Log.w(TAG, "Gemini " + modelName + " returned HTTP " + statusCode + ": "
                    + readStream(conn.getErrorStream()));
            return null;
        }

        JSONObject response = new JSONObject(readStream(conn.getInputStream()));
        JSONArray candidates = response.optJSONArray("candidates");
        if (candidates == null || candidates.length() == 0) return null;
        JSONObject content = candidates.getJSONObject(0).optJSONObject("content");
        if (content == null) return null;
        JSONArray parts = content.optJSONArray("parts");
        if (parts == null) return null;

        StringBuilder text = new StringBuilder();
        for (int p = 0; p < parts.length(); p++) {
            JSONObject part = parts.optJSONObject(p);
            if (part != null && !part.optBoolean("thought", false)) {
                text.append(part.optString("text", ""));
            }
        }
        if (text.length() == 0) return null;
        return new JSONObject(text.toString());
    }

    private String buildInstructions(List<AnnouncementEntity> posts) {
        SimpleDateFormat today = new SimpleDateFormat("EEEE, MMMM d, yyyy h:mm a", Locale.US);
        today.setTimeZone(PH_TIME);
        SimpleDateFormat posted = new SimpleDateFormat("MMM d, yyyy h:mm a", Locale.US);
        posted.setTimeZone(PH_TIME);

        StringBuilder sb = new StringBuilder();
        sb.append("You are Kaleo, the campus assistant in a student app for Laguna State Polytechnic ")
                .append("University - San Pablo City Campus (LSPU-SPCC) in the Philippines.\n")
                .append("Right now it is ").append(today.format(new Date())).append(" (Philippine time).\n\n")
                .append("How to answer:\n")
                .append("- Reply in the student's language: English, Filipino or Taglish.\n")
                .append("- Be short and friendly, like a helpful upperclassman: 1 to 4 sentences, ")
                .append("or a few bullet points when listing things. You may use **bold** for key facts.\n")
                .append("- For anything about LSPU (classes, suspensions, exams, scholarships, events, ")
                .append("rules, offices), use ONLY the announcements below. Mention the page and date ")
                .append("when it helps. Never invent dates, deadlines, requirements, offices or policies.\n")
                .append("- If the announcements don't cover the question, say you couldn't find it in ")
                .append("the announcements on the app, and suggest checking the official LSPU page or ")
                .append("the right office. For class suspensions, say no suspension has been posted ")
                .append("in the announcements you have, and to check LSPU's official page for the latest.\n")
                .append("- Small talk and general questions (greetings, time, study tips, general ")
                .append("knowledge) can be answered normally.\n")
                .append("- Don't say which AI model or company powers you. You are Kaleo.\n")
                .append("- source_ids: the numbers of the announcements you actually used, or an empty list.\n\n");

        if (posts.isEmpty()) {
            sb.append("There are no announcements saved on the app yet.\n");
            return sb.toString();
        }

        sb.append("Announcements saved on the app (newest first):\n");
        for (int i = 0; i < posts.size(); i++) {
            AnnouncementEntity p = posts.get(i);
            sb.append("\n[").append(i + 1).append("] ")
                    .append(safe(p.pageName).isEmpty() ? "Unknown page" : p.pageName)
                    .append(", posted ").append(posted.format(new Date(p.createdAt)));
            if (!safe(p.categories).isEmpty()) sb.append(" | categories: ").append(p.categories);
            if (!safe(p.urgency).isEmpty()) sb.append(" | urgency: ").append(p.urgency);
            sb.append("\n");
            if (!safe(p.keyTakeaway).isEmpty()) sb.append("Summary: ").append(p.keyTakeaway.trim()).append("\n");
            if (!safe(p.importantDates).isEmpty() && !p.importantDates.trim().equals("[]")) {
                sb.append("Dates: ").append(p.importantDates.trim()).append("\n");
            }
            String text = safe(p.message).trim();
            if (text.length() > MAX_POST_CHARS) text = text.substring(0, MAX_POST_CHARS) + "...";
            if (!text.isEmpty()) sb.append("Text: ").append(text).append("\n");
        }
        return sb.toString();
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String readStream(InputStream is) {
        if (is == null) return "";
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
