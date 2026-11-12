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
            return new LocalNanoAIEngine.NanoAIResult(
                    "Device is currently offline. Connect to Wi-Fi or mobile data for live Cloud AI expansion, or check your local cached announcements.",
                    detectedIntent, 0.20f, null, true
            );
        }

        String apiKey = BuildConfig.GEMINI_API_KEY;

        // 1. Try live Gemini calls across candidate models with automatic failover
        if (apiKey != null && !apiKey.trim().isEmpty() && !apiKey.equals("null")) {
            for (String modelName : CANDIDATE_MODELS) {
                try {
                    String liveAnswer = callGeminiLive(modelName, userQuery, cachedSource, apiKey.trim());
                    if (liveAnswer != null && !liveAnswer.trim().isEmpty()) {
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

        // 2. Graceful fallback: Consult CampusKnowledgeBase and synthesize structured guidance
        String expandedAnswer = buildExpandedCloudResponse(userQuery, detectedIntent, apiKey);

        return new LocalNanoAIEngine.NanoAIResult(
                expandedAnswer,
                detectedIntent != LocalNanoAIEngine.Intent.UNKNOWN ? detectedIntent : LocalNanoAIEngine.Intent.GENERAL,
                0.90f,
                cachedSource,
                true // isOfflineAnswer = true since this is served locally from knowledge base
        );
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

        String prompt = "You are Kaleo Cloud AI, an intelligent, friendly student assistant for Laguna State Polytechnic University - San Pablo City Campus (LSPU-SPCC). " +
                "Answer the student's question helpfully, accurately, and concisely in English or Taglish. " +
                "Be polite and informative. If asking about campus facilities, offices, programs, or rules, refer accurately to LSPU San Pablo City Campus." +
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

    private String buildExpandedCloudResponse(String userQuery, LocalNanoAIEngine.Intent detectedIntent, String apiKey) {
        String q = userQuery.toLowerCase(Locale.ROOT).trim();

        // 1. Try comprehensive CampusKnowledgeBase first
        CampusKnowledgeBase.KnowledgeEntry kbMatch = CampusKnowledgeBase.findBestMatch(q, detectedIntent);
        if (kbMatch != null && kbMatch.content != null && !kbMatch.content.trim().isEmpty()) {
            return kbMatch.content;
        }

        // 2. Specific intent-based campus guidance
        String body;
        if (detectedIntent == LocalNanoAIEngine.Intent.SUSPENSION) {
            body = "✅ **Class Suspension Inquiries**\n\n" +
                    "Classes remain regular unless an official announcement is issued by the LSPU Administration or the San Pablo City LGU.\n\n" +
                    "• Check the official **LSPU - San Pablo City Campus Facebook Page** for verified weather or emergency suspension bulletins.\n" +
                    "• Pull down the Feed tab to sync the latest announcements.";
        } else if (detectedIntent == LocalNanoAIEngine.Intent.SCHOLARSHIP) {
            body = "🎓 **Scholarship & Financial Assistance**\n\n" +
                    "LSPU-SPCC students can apply for government and institutional financial grants:\n\n" +
                    "• **Tulong Dunong Program (TDP-TES)** & **CHED Tertiary Education Subsidy (TES)**\n" +
                    "• **Iskolar ng Laguna** & San Pablo City Educational Assistance\n" +
                    "• **UniFAST Free Higher Education** (RA 10931)\n\n" +
                    "For application periods and document submission, visit the **Office of Student Affairs and Services (OSAS)** in the Administration Building.";
        } else if (q.contains("course") || q.contains("program") || q.contains("degree") || q.contains("curriculum") || q.contains("major")) {
            body = "Laguna State Polytechnic University (LSPU) San Pablo City Campus offers recognized undergraduate programs:\n\n" +
                    "• **College of Computer Studies (CCS)**: BS Computer Science (BSCS), BS Information Technology (BSIT)\n" +
                    "• **College of Teacher Education (CTE)**: BSED (English, Math, Science, Filipino, Social Studies), BEED\n" +
                    "• **College of Business Management & Accountancy (CBMA)**: BSA, BSBA (Financial / Marketing), BS Entrep\n" +
                    "• **College of Engineering (COE)**: BS Civil, Mechanical, Electrical, Computer Engineering\n" +
                    "• **College of Industrial Technology (CIT)**: BIT (Automotive, Electrical, Electronics, Mechanical)\n" +
                    "• **College of Arts & Sciences (CAS)**: BA Communication, BS Mathematics\n" +
                    "• **College of Criminal Justice Education (CCJE)**: BS Criminology\n\n" +
                    "For admission requirements and entrance examination schedules, visit the SPCC Registrar's Office.";
        } else if (q.contains("portal") || q.contains("grades") || q.contains("account") || q.contains("password") || q.contains("cor")) {
            body = "You can access your official LSPU student portal at **https://lspu.edu.ph/portal** to view:\n\n" +
                    "• Verified midterm and final grades\n" +
                    "• Certificate of Registration (COR) and enrolled units\n" +
                    "• Semester billing assessments and clearances\n\n" +
                    "If you forgot your student password or portal login, visit the campus ICT/MIS unit in the Administration Building.";
        } else if (q.contains("president") || q.contains("campus director") || q.contains("dean") || q.contains("officials")) {
            body = "Laguna State Polytechnic University is guided by University President Dr. Mario R. Briones and the SPCC Campus Administration. College deans and department chairpersons hold office at their respective academic buildings.";
        } else if (q.contains("library") || q.contains("book") || q.contains("thesis") || q.contains("study")) {
            body = "The LSPU-SPCC Campus Library is open Monday to Friday, 8:00 AM to 5:00 PM. Students can borrow references, access academic journals, or utilize quiet study spaces by presenting a validated student ID.";
        } else {
            body = "I couldn't reach the live AI service right now to answer **\"" + userQuery.trim() + "\"**.\n\n" +
                    "Here are helpful LSPU-SPCC resources:\n" +
                    "• **Campus Administration**: The Registrar, OSAS, and Accounting are open Mon–Fri, 8:00 AM – 5:00 PM at the Administration Building.\n" +
                    "• **Online Portal**: Check grades and enrolled subjects at **https://lspu.edu.ph/portal**.\n" +
                    "• **Feed Sync**: Pull down the Feed tab to get the latest official university announcements.";
        }

        // Help notice if GEMINI_API_KEY is not yet added
        if (apiKey == null || apiKey.trim().isEmpty() || apiKey.equals("null")) {
            return body + "\n\n💡 *Tip: To enable live Gemini Flash generative reasoning for open queries, add `campussync.geminiApiKey=YOUR_KEY` in local.properties.*";
        }

        return body;
    }
}
