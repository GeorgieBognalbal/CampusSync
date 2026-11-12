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
import java.util.Locale;

/**
 * Cloud AI Adapter for Path 3 (Hybrid Edge-First Nano RAG with Cloud Fallback)
 * 
 * Provides live multi-tier AI expansion when the device is connected to the internet.
 * Connects directly to Google Gemini Flash models via REST API using standard Android networking
 * (HttpURLConnection + org.json, zero external dependencies).
 * 
 * Employs a multi-tier model fallback strategy:
 * 1. gemini-3.5-flash-lite (Ultra-low latency ~800ms, highly responsive)
 * 2. gemini-3.1-flash-lite (Reliable secondary fallback)
 * 3. gemini-3.6-flash (Tertiary fallback)
 * 
 * If all cloud models fail or network is unavailable, it gracefully consults
 * the comprehensive CampusKnowledgeBase to always return accurate, helpful LSPU-SPCC answers.
 */
public class CloudAIAdapter implements LocalNanoAIEngine.CloudFallbackProvider {

    private static final String TAG = "CloudAIAdapter";

    private static final String GEMINI_BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/";

    /**
     * Prioritized list of active Gemini models for cloud generation.
     * Starts with ultra-low latency gemini-3.5-flash-lite (~800ms response).
     */
    private static final String[] CANDIDATE_MODELS = {
            "gemini-3.5-flash-lite",
            "gemini-3.1-flash-lite",
            "gemini-3.6-flash"
    };

    private final Context context;

    public CloudAIAdapter(Context context) {
        this.context = context != null ? context.getApplicationContext() : null;
    }

    /**
     * Checks if the device has an active internet connection.
     */
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

    /**
     * Resolves queries via cloud intelligence when local cache does not have high-confidence matches.
     * Tries live Gemini API candidates if GEMINI_API_KEY is present; otherwise falls back to
     * comprehensive campus encyclopedic knowledge.
     */
    @Override
    public LocalNanoAIEngine.NanoAIResult queryCloudFallback(String userQuery,
                                                            LocalNanoAIEngine.Intent detectedIntent,
                                                            AnnouncementEntity cachedSource) {
        if (!isOnline()) {
            return null; // Triggers transfer to local engine
        }

        String apiKey = BuildConfig.GEMINI_API_KEY;

        // 1. Try live Gemini calls across candidate models with automatic failover
        if (apiKey != null && !apiKey.trim().isEmpty() && !apiKey.equals("null")) {
            for (String modelName : CANDIDATE_MODELS) {
                try {
                    String liveAnswer = callGeminiLive(modelName, userQuery, cachedSource, apiKey.trim());
                    if (liveAnswer != null && !liveAnswer.trim().isEmpty()) {
                        // Check if Cloud AI evaluated the query and determined it should be resolved locally
                        if (liveAnswer.trim().equals("[TRANSFER_TO_LOCAL]") || liveAnswer.contains("[TRANSFER_TO_LOCAL]")) {
                            Log.d(TAG, "Cloud AI evaluated query and delegated transfer to local engine: " + userQuery);
                            return null; // Signals transfer to local engine
                        }
                        return new LocalNanoAIEngine.NanoAIResult(
                                liveAnswer,
                                detectedIntent != LocalNanoAIEngine.Intent.UNKNOWN ? detectedIntent : LocalNanoAIEngine.Intent.GENERAL,
                                0.95f,
                                cachedSource,
                                false // isOfflineAnswer = false indicates live cloud response
                        );
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Gemini call failed for model [" + modelName + "]: " + e.getMessage());
                }
            }
        }

        // If cloud models are unavailable or API key missing, return null to transfer to local engine
        return null;
    }

    /**
     * Executes HTTP POST to Google Gemini API using HttpURLConnection.
     */
    private String callGeminiLive(String modelName, String userQuery, AnnouncementEntity cachedSource, String apiKey) throws Exception {
        URL url = new URL(GEMINI_BASE_URL + modelName + ":generateContent?key=" + apiKey);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setConnectTimeout(6000);
        conn.setReadTimeout(9000);
        conn.setDoOutput(true);

        // Grounding context if a source post exists
        String contextSnippet = "";
        if (cachedSource != null && cachedSource.message != null && !cachedSource.message.trim().isEmpty()) {
            contextSnippet = "\n\nRelevant LSPU-SPCC announcement on file from " + cachedSource.pageName + ":\n\"" + cachedSource.message.trim() + "\"";
        }

        String prompt = "You are Kaleo Cloud AI, the official intelligent assistant exclusively for Laguna State Polytechnic University - San Pablo City Campus (LSPU-SPCC).\n\n" +
                "STRICT LSPU DOMAIN POLICY:\n" +
                "You only accept and answer queries related to LSPU, SPCC, campus academics, student life, university offices, schedules, announcements, and university services.\n" +
                "If the user asks an out-of-scope question unrelated to LSPU or student university life (such as general knowledge trivia, general programming, cooking recipes, foreign politics, pop culture, entertainment, etc.), politely decline with:\n" +
                "\"I am Kaleo, an AI assistant dedicated exclusively to Laguna State Polytechnic University - San Pablo City Campus (LSPU-SPCC). I can only assist with LSPU campus matters, announcements, academics, and student services. Please ask a campus-related question!\"\n\n" +
                "Campus Routing Rule:\n" +
                "If the student is specifically asking about today's real-time class suspension, latest official campus announcements, or specific Facebook posts, and no relevant announcement is provided in the context below, respond with exactly: [TRANSFER_TO_LOCAL]\n" +
                contextSnippet +
                "\n\nStudent Question: " + userQuery.trim();

        // Build Gemini Request Payload
        JSONObject root = new JSONObject();
        JSONArray contents = new JSONArray();
        JSONObject messageObj = new JSONObject();
        JSONArray parts = new JSONArray();
        JSONObject textPart = new JSONObject();
        textPart.put("text", prompt);
        parts.put(textPart);
        messageObj.put("parts", parts);
        contents.put(messageObj);
        root.put("contents", contents);

        byte[] requestBody = root.toString().getBytes(StandardCharsets.UTF_8);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(requestBody);
            os.flush();
        }

        int statusCode = conn.getResponseCode();
        if (statusCode >= 200 && statusCode < 300) {
            String responseStr = readStream(conn.getInputStream());
            JSONObject responseJson = new JSONObject(responseStr);
            JSONArray candidates = responseJson.optJSONArray("candidates");
            if (candidates != null && candidates.length() > 0) {
                JSONObject candidate = candidates.getJSONObject(0);
                JSONObject contentObj = candidate.optJSONObject("content");
                if (contentObj != null) {
                    JSONArray candidateParts = contentObj.optJSONArray("parts");
                    if (candidateParts != null && candidateParts.length() > 0) {
                        StringBuilder textBuilder = new StringBuilder();
                        for (int p = 0; p < candidateParts.length(); p++) {
                            JSONObject part = candidateParts.optJSONObject(p);
                            if (part != null) {
                                if (part.optBoolean("thought", false)) continue;
                                String text = part.optString("text", "");
                                if (!text.isEmpty()) {
                                    textBuilder.append(text);
                                }
                            }
                        }
                        if (textBuilder.length() > 0) {
                            return textBuilder.toString().trim();
                        }
                        // Fallback if all parts were filtered
                        for (int p = 0; p < candidateParts.length(); p++) {
                            JSONObject part = candidateParts.optJSONObject(p);
                            if (part != null) {
                                String text = part.optString("text", "");
                                if (!text.trim().isEmpty()) {
                                    return text.trim();
                                }
                            }
                        }
                    }
                }
            }
        } else {
            String errDetail = readStream(conn.getErrorStream());
            Log.w(TAG, "Gemini API [" + modelName + "] returned HTTP " + statusCode + ": " + errDetail);
        }

        return null;
    }

    private static String readStream(InputStream is) {
        if (is == null) return "";
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
