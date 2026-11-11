
package com.example.campussync;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.campussync.data.local.AnnouncementDatabase;
import com.example.campussync.data.local.AnnouncementEntity;
import com.example.campussync.data.model.ChatMessage;
import com.example.campussync.data.repository.AnnouncementRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;



import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private static final int BLUE = Color.rgb(49, 87, 213);
    private static final int DARK = Color.rgb(24, 34, 56);
    private static final int MUTED = Color.rgb(122, 132, 153);
    private static final int PALE = Color.rgb(237, 241, 255);
    private static final int WHITE = Color.WHITE;

    private LinearLayout contentContainer;
    private TextView navPosts, navChat, navSources;
    private ScrollView contentScroll;
    private View chatInputBar;
    private EditText chatInput;
    private TextView chatSend;
    private AnnouncementRepository announcementRepository;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        contentContainer = findViewById(R.id.contentContainer);
        navPosts = findViewById(R.id.navPosts);
        navChat = findViewById(R.id.navChat);
        navSources = findViewById(R.id.navSources);
        contentScroll = findViewById(R.id.contentScroll);
        chatInputBar = findViewById(R.id.chatInputBar);
        chatInput = findViewById(R.id.chatInput);
        chatSend = findViewById(R.id.chatSend);

        AnnouncementDatabase database =
                AnnouncementDatabase.getInstance(getApplicationContext());

        announcementRepository =
                new AnnouncementRepository(database.announcementDao());

        seedDemoAnnouncementsIfEmpty();
        showAnnouncements();
    }

    private void seedDemoAnnouncementsIfEmpty() {
        executor.execute(() -> {
            List<AnnouncementEntity> existing =
                    announcementRepository.getAll();

            if (existing.size() < 5) {
                List<AnnouncementEntity> samples = Arrays.asList(
                        new AnnouncementEntity(
                                "demo-001",
                                "LSPU Official",
                                "Welcome to CampusSync! This is sample announcement data. Replace it with real posts after connecting your backend.",
                                "https://www.facebook.com/LSPUOfficial",
                                System.currentTimeMillis()
                        ),
                        new AnnouncementEntity(
                                "demo-002",
                                "SPCC OSAS",
                                "Sample reminder: Check the official university page for the latest student announcements.",
                                "https://www.facebook.com/SPCC.OSAS",
                                System.currentTimeMillis() - 3600000
                        ),
                        new AnnouncementEntity(
                                "demo-003",
                                "LSPU Official",
                                "Sample: Classes are suspended tomorrow in all levels due to the typhoon signal. Walang pasok — please stay safe and monitor this page for updates.",
                                "https://www.facebook.com/LSPUOfficial",
                                System.currentTimeMillis() - 7200000
                        ),
                        new AnnouncementEntity(
                                "demo-004",
                                "CCSOCTA",
                                "Sample: Midterm exam schedule — Nov 24 to Nov 28. Check your class adviser for room assignments. Bring your exam permit.",
                                "https://www.facebook.com/CCSOCTA",
                                System.currentTimeMillis() - 10800000
                        ),
                        new AnnouncementEntity(
                                "demo-005",
                                "SPCC OSAS",
                                "Sample: Scholarship requirements — certificate of grades, certificate of indigency, 2x2 ID photo, and a filled-out application form. Submit to OSAS before Dec 5.",
                                "https://www.facebook.com/SPCC.OSAS",
                                System.currentTimeMillis() - 14400000
                        )
                );

                announcementRepository.insertAll(samples);
            }

            runOnUiThread(this::showAnnouncements);
        });
    }

    public void onAnnouncementsClick(View view) {
        showAnnouncements();
    }

    public void onChatClick(View view) {
        showChat();
    }

    public void onSourcesClick(View view) {
        showSources();
    }

    private void setActiveTab(int tab) {
        chatInputBar.setVisibility(tab == 1 ? View.VISIBLE : View.GONE);
        if (tab != 1) stopThinkingAnimation();
        navPosts.setTextColor(tab == 0 ? BLUE : MUTED);
        navChat.setTextColor(tab == 1 ? BLUE : MUTED);
        navSources.setTextColor(tab == 2 ? BLUE : MUTED);

        navPosts.setTypeface(null,
                tab == 0 ? Typeface.BOLD : Typeface.NORMAL);

        navChat.setTypeface(null,
                tab == 1 ? Typeface.BOLD : Typeface.NORMAL);

        navSources.setTypeface(null,
                tab == 2 ? Typeface.BOLD : Typeface.NORMAL);
    }

    // =========================
    // ANNOUNCEMENTS
    // =========================

    private void showAnnouncements() {
        setActiveTab(0);
        contentContainer.removeAllViews();

        addText("Good day, LSPU! 👋",
                13, MUTED, false, 0, 2);

        addText("Campus updates,\none place.",
                29, DARK, true, 0, 12);

        LinearLayout summary = card();
        summary.setBackground(roundDrawable(BLUE, 20));
        summary.setPadding(dp(18), dp(17), dp(18), dp(17));

        summary.addView(text(
                "CAMPUS PULSE", 10, 0xFFDCE4FF, true
        ));

        TextView summaryTitle =
                text("Stay in sync with what’s happening.",
                        17, WHITE, true);

        LinearLayout.LayoutParams stp = params(-1, -2);
        stp.topMargin = dp(7);
        summary.addView(summaryTitle, stp);

        TextView summaryCaption =
                text("Announcements from your university pages, gathered for you.",
                        12, 0xFFE5EAFF, false);

        LinearLayout.LayoutParams scp = params(-1, -2);
        scp.topMargin = dp(5);
        summary.addView(summaryCaption, scp);

        addView(contentContainer, summary, 0, 15);

        addText("ANNOUNCEMENTS",
                11, MUTED, true, 0, 9);

        EditText search = new EditText(this);
        search.setSingleLine(true);
        search.setTextSize(14);
        search.setHint("Search announcements…");

        search.setCompoundDrawablesWithIntrinsicBounds(
                android.R.drawable.ic_menu_search, 0, 0, 0
        );

        search.setPadding(dp(14), 0, dp(14), 0);
        search.setBackground(
                roundDrawable(WHITE, 14, 0xFFE2E7F0)
        );

        contentContainer.addView(search, params(-1, 50));

        TextView feedLabel =
                text("Recent posts", 18, DARK, true);

        LinearLayout.LayoutParams flp = params(-1, -2);
        flp.topMargin = dp(19);
        flp.bottomMargin = dp(10);

        contentContainer.addView(feedLabel, flp);

        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);

        contentContainer.addView(list, params(-1, -2));

        search.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(
                    CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(
                    CharSequence s, int start, int before, int count) {
                loadAnnouncementsInto(list, s.toString().trim());
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
            }
        });

        loadAnnouncementsInto(list, "");
    }

    private void loadAnnouncementsInto(
            LinearLayout list, String query) {

        list.removeAllViews();

        executor.execute(() -> {
            List<AnnouncementEntity> announcements =
                    query.isEmpty()
                            ? announcementRepository.getAll()
                            : announcementRepository.search(query);

            runOnUiThread(() -> {
                if (isFinishing()) return;

                list.removeAllViews();

                if (announcements.isEmpty()) {
                    LinearLayout empty = card();

                    empty.setPadding(
                            dp(18), dp(22), dp(18), dp(22)
                    );

                    empty.addView(text(
                            "No matching announcements",
                            15, DARK, true
                    ));

                    TextView hint = text(
                            "Try another keyword or sync your sources when you’re online.",
                            12, MUTED, false
                    );

                    LinearLayout.LayoutParams hp = params(-1, -2);
                    hp.topMargin = dp(5);

                    empty.addView(hint, hp);
                    list.addView(empty);

                } else {
                    for (AnnouncementEntity item : announcements) {
                        addAnnouncementCard(list, item);
                    }
                }
            });
        });
    }

    private void addAnnouncementCard(
            LinearLayout parent, AnnouncementEntity item) {

        LinearLayout outer = card();
        outer.setPadding(dp(15), dp(15), dp(15), dp(14));

        LinearLayout top = row();

        TextView avatar = text(
                initials(item.pageName), 12, BLUE, true
        );

        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(roundDrawable(PALE, 12));

        top.addView(avatar, params(38, 38));

        LinearLayout sourceBlock = column();
        sourceBlock.setPadding(dp(10), 0, 0, 0);

        sourceBlock.addView(text(
                item.pageName, 14, DARK, true
        ));

        TextView time = text(
                DateFormat.format(
                        "MMM dd • hh:mm a",
                        item.createdAt
                ).toString(),
                11, MUTED, false
        );

        LinearLayout.LayoutParams tp = params(-2, -2);
        tp.topMargin = dp(3);

        sourceBlock.addView(time, tp);

        top.addView(sourceBlock,
                new LinearLayout.LayoutParams(0, -2, 1));

        TextView tag = text("POST", 9, BLUE, true);
        tag.setPadding(dp(9), dp(6), dp(9), dp(6));
        tag.setBackground(roundDrawable(PALE, 20));

        top.addView(tag);
        outer.addView(top);

        TextView message = text(
                item.message, 14, 0xFF46516A, false
        );

        message.setLineSpacing(dp(3), 1f);

        LinearLayout.LayoutParams mp = params(-1, -2);
        mp.topMargin = dp(15);
        mp.bottomMargin = dp(13);

        outer.addView(message, mp);

        View divider = new View(this);
        divider.setBackgroundColor(0xFFECEFF4);

        outer.addView(divider, params(-1, 1));

        LinearLayout footer = row();
        footer.setGravity(Gravity.CENTER_VERTICAL);

        TextView cached = text(
                "●  Saved on this device",
                10, 0xFF398260, true
        );

        footer.addView(cached,
                new LinearLayout.LayoutParams(0, -2, 1));

        TextView open = text(
                "Open post  ↗", 11, BLUE, true
        );

        footer.addView(open);
        open.setOnClickListener(v -> openUrl(item.permalink));

        LinearLayout.LayoutParams fp = params(-1, -2);
        fp.topMargin = dp(11);

        outer.addView(footer, fp);

        addView(parent, outer, 0, 12);
    }

    // =========================
    // CHAT
    // =========================

    private static final String[] SUGGESTIONS = {
            "May pasok ba?",
            "Midterm schedule",
            "Scholarship requirements"
    };

    /** Kept across tab switches so the conversation isn't lost. */
    private final List<ChatMessage> chatHistory = new ArrayList<>();
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    private LinearLayout messageList;
    private LinearLayout suggestionRow;
    private Runnable thinkingTicker;
    private boolean botBusy = false;

    private void showChat() {
        setActiveTab(1);
        contentContainer.removeAllViews();

        // Header
        LinearLayout header = row();
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView icon = text("✦", 18, WHITE, true);
        icon.setGravity(Gravity.CENTER);
        icon.setBackgroundResource(R.drawable.bot_avatar_background);
        header.addView(icon, params(40, 40));

        LinearLayout titleBlock = column();
        titleBlock.setPadding(dp(11), 0, 0, 0);
        titleBlock.addView(text("CampusSync Assistant", 15, DARK, true));
        TextView status = text("●  Answers from saved announcements",
                11, 0xFF398260, true);
        LinearLayout.LayoutParams sp = params(-2, -2);
        sp.topMargin = dp(3);
        titleBlock.addView(status, sp);
        header.addView(titleBlock);

        addView(contentContainer, header, 0, 16);

        // Messages
        messageList = column();
        contentContainer.addView(messageList, params(-1, -2));

        if (chatHistory.isEmpty()) {
            chatHistory.add(new ChatMessage(
                    "Hi! 👋 Ako si CampusSync Assistant. Itanong mo lang ang tungkol sa klase, exams, o scholarships — hahanapin ko sa mga official announcements.",
                    ChatMessage.Sender.BOT));
        }
        for (ChatMessage m : chatHistory) {
            messageList.addView(buildMessageView(m));
        }

        // Suggestion pills
        TextView suggestLabel = text("SUGGESTED", 10, MUTED, true);
        LinearLayout.LayoutParams slp = params(-1, -2);
        slp.topMargin = dp(10);
        slp.bottomMargin = dp(8);
        contentContainer.addView(suggestLabel, slp);

        HorizontalScrollView pillScroll = new HorizontalScrollView(this);
        pillScroll.setHorizontalScrollBarEnabled(false);
        suggestionRow = row();
        for (String s : SUGGESTIONS) {
            TextView pill = text(s, 13, BLUE, true);
            pill.setBackgroundResource(R.drawable.suggestion_pill);
            pill.setPadding(dp(14), dp(9), dp(14), dp(9));
            pill.setOnClickListener(v -> sendChatMessage(s));
            LinearLayout.LayoutParams pp = params(-2, -2);
            pp.rightMargin = dp(8);
            suggestionRow.addView(pill, pp);
        }
        pillScroll.addView(suggestionRow);
        contentContainer.addView(pillScroll, params(-1, -2));

        // Composer (pinned bar in activity_main.xml)
        chatSend.setOnClickListener(v -> {
            String q = chatInput.getText().toString().trim();
            if (!q.isEmpty()) {
                chatInput.setText("");
                sendChatMessage(q);
            }
        });
        chatInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                chatSend.performClick();
                return true;
            }
            return false;
        });

        scrollChatToBottom();
    }

    private void sendChatMessage(String question) {
        if (botBusy) return;
        botBusy = true;

        ChatMessage userMsg = new ChatMessage(question, ChatMessage.Sender.USER);
        chatHistory.add(userMsg);
        appendIfVisible(userMsg);

        ChatMessage thinking = ChatMessage.thinking();
        chatHistory.add(thinking);
        appendIfVisible(thinking);

        executor.execute(() -> {
            ChatMessage answer = buildAnswer(question);

            // Small delay so the "Thinking..." animation is visible
            uiHandler.postDelayed(() -> {
                if (isFinishing()) return;
                chatHistory.remove(thinking);
                chatHistory.add(answer);
                stopThinkingAnimation();
                if (messageList != null && messageList.isShown()) {
                    // Remove the thinking bubble (always the last child)
                    int last = messageList.getChildCount() - 1;
                    if (last >= 0) messageList.removeViewAt(last);
                    messageList.addView(buildMessageView(answer));
                    scrollChatToBottom();
                }
                botBusy = false;
            }, 1200);
        });
    }

    private void appendIfVisible(ChatMessage m) {
        if (messageList != null && messageList.isShown()) {
            messageList.addView(buildMessageView(m));
            scrollChatToBottom();
        }
    }

    /** Runs on the background executor: searches saved posts and forms a reply. */
    private ChatMessage buildAnswer(String question) {
        LinkedHashMap<String, AnnouncementEntity> found = new LinkedHashMap<>();
        for (String keyword : keywordsFor(question)) {
            for (AnnouncementEntity a : announcementRepository.search(keyword)) {
                found.put(a.facebookPostId, a);
            }
            if (found.size() >= 3) break;
        }

        if (found.isEmpty()) {
            return new ChatMessage(
                    "Pasensya, wala akong nakitang announcement tungkol diyan sa mga naka-save na posts. Subukan ang ibang keyword, o i-check ang official page para sa pinakabagong update.",
                    ChatMessage.Sender.BOT);
        }

        List<ChatMessage.Source> sources = new ArrayList<>();
        int i = 0;
        for (AnnouncementEntity a : found.values()) {
            if (i++ == 3) break;
            sources.add(new ChatMessage.Source(a.pageName, a.message, a.permalink));
        }

        AnnouncementEntity top = found.values().iterator().next();
        String reply = "Ito ang nakita ko mula sa " + top.pageName + ":\n\n"
                + shorten(top.message, 220)
                + (sources.size() > 1
                    ? "\n\nMay " + (sources.size() - 1) + " pang related na post sa ibaba."
                    : "");

        return new ChatMessage(reply, ChatMessage.Sender.BOT, sources);
    }

    /** Maps common Taglish questions to keywords the local search understands. */
    private List<String> keywordsFor(String question) {
        String q = question.toLowerCase(Locale.ROOT);
        List<String> keys = new ArrayList<>();

        if (q.contains("pasok") || q.contains("suspend") || q.contains("class")
                || q.contains("klase")) {
            keys.addAll(Arrays.asList("suspension", "suspended", "pasok", "classes"));
        }
        if (q.contains("midterm") || q.contains("exam")) {
            keys.addAll(Arrays.asList("midterm", "exam"));
        }
        if (q.contains("scholar")) {
            keys.addAll(Arrays.asList("scholarship", "requirements"));
        }
        if (q.contains("enroll")) {
            keys.add("enrollment");
        }

        // Fallback: any meaningful word from the question itself
        for (String word : q.replaceAll("[^a-z0-9ñ ]", " ").split("\\s+")) {
            if (word.length() > 3 && !keys.contains(word)) keys.add(word);
        }
        return keys;
    }

    // ---------- bubble views ----------

    private View buildMessageView(ChatMessage m) {
        LinearLayout wrapper = row();
        wrapper.setGravity(m.isUser() ? Gravity.END : Gravity.START);

        LinearLayout.LayoutParams wp = params(-1, -2);
        wp.bottomMargin = dp(10);
        wrapper.setLayoutParams(wp);

        int maxWidth = (int) (getResources().getDisplayMetrics().widthPixels * 0.78f);

        if (m.isUser()) {
            TextView bubble = text(m.getText(), 14, WHITE, false);
            bubble.setBackgroundResource(R.drawable.chat_bubble_user);
            bubble.setPadding(dp(14), dp(10), dp(14), dp(10));
            bubble.setMaxWidth(maxWidth);
            bubble.setLineSpacing(dp(2), 1f);
            wrapper.addView(bubble, params(-2, -2));
            return wrapper;
        }

        // Bot: small avatar + bubble column
        TextView avatar = text("✦", 12, WHITE, true);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackgroundResource(R.drawable.bot_avatar_background);
        LinearLayout.LayoutParams ap = params(28, 28);
        ap.rightMargin = dp(8);
        wrapper.addView(avatar, ap);

        LinearLayout bubble = column();
        bubble.setBackgroundResource(R.drawable.chat_bubble_bot);
        bubble.setPadding(dp(14), dp(10), dp(14), dp(12));

        if (m.isThinking()) {
            TextView dots = text("Thinking", 14, MUTED, false);
            dots.setTypeface(Typeface.DEFAULT, Typeface.ITALIC);
            bubble.addView(dots);
            startThinkingAnimation(dots);
        } else {
            TextView body = text(m.getText(), 14, 0xFF2E384F, false);
            body.setLineSpacing(dp(2), 1f);
            body.setMaxWidth(maxWidth - dp(64));
            bubble.addView(body);

            if (m.hasSources()) {
                TextView label = text("SOURCES", 9, MUTED, true);
                LinearLayout.LayoutParams lp = params(-2, -2);
                lp.topMargin = dp(12);
                lp.bottomMargin = dp(2);
                bubble.addView(label, lp);

                for (ChatMessage.Source s : m.getSources()) {
                    bubble.addView(buildSourceCard(s, maxWidth - dp(64)));
                }
            }
        }

        wrapper.addView(bubble, new LinearLayout.LayoutParams(
                -2, -2));
        bubble.setMinimumWidth(dp(90));
        return wrapper;
    }

    private View buildSourceCard(ChatMessage.Source s, int width) {
        LinearLayout card = row();
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setBackgroundResource(R.drawable.source_card_background);
        card.setPadding(dp(10), dp(9), dp(10), dp(9));

        TextView fb = text("f", 14, WHITE, true);
        fb.setGravity(Gravity.CENTER);
        fb.setBackground(roundDrawable(0xFF1877F2, 8));
        card.addView(fb, params(28, 28));

        LinearLayout info = column();
        info.setPadding(dp(9), 0, dp(6), 0);
        info.addView(text(s.getPageName(), 12, DARK, true));
        TextView snip = text(shorten(s.getSnippet(), 70), 11, MUTED, false);
        snip.setMaxLines(1);
        snip.setEllipsize(TextUtils.TruncateAt.END);
        info.addView(snip);
        card.addView(info, new LinearLayout.LayoutParams(0, -2, 1));

        card.addView(text("↗", 15, BLUE, true));
        card.setOnClickListener(v -> openUrl(s.getUrl()));

        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(width, -2);
        cp.topMargin = dp(6);
        card.setLayoutParams(cp);
        return card;
    }

    private void startThinkingAnimation(TextView target) {
        stopThinkingAnimation();
        final String[] frames = {"Thinking", "Thinking.", "Thinking..", "Thinking..."};
        thinkingTicker = new Runnable() {
            int frame = 0;
            @Override
            public void run() {
                target.setText(frames[frame++ % frames.length]);
                target.setAlpha(frame % 2 == 0 ? 1f : 0.7f);
                uiHandler.postDelayed(this, 350);
            }
        };
        uiHandler.post(thinkingTicker);
    }

    private void stopThinkingAnimation() {
        if (thinkingTicker != null) {
            uiHandler.removeCallbacks(thinkingTicker);
            thinkingTicker = null;
        }
    }

    private void scrollChatToBottom() {
        contentScroll.post(() -> contentScroll.fullScroll(View.FOCUS_DOWN));
    }

    private String shorten(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max).trim() + "…";
    }

    // =========================
    // SOURCES
    // =========================

    private void showSources() {
        setActiveTab(2);
        contentContainer.removeAllViews();

        addText("YOUR INFORMATION FEED",
                11, BLUE, true, 0, 8);

        addText("Trusted campus sources.",
                26, DARK, true, 0, 7);

        addText(
                "CampusSync collects public updates from the pages you follow. These are demo source cards for now.",
                13, MUTED, false, 0, 17
        );

        addSourceCard(
                "L",
                "LSPU Official",
                "University-wide announcements",
                "https://www.facebook.com/LSPUOfficial",
                0xFFEAF0FF
        );

        addSourceCard(
                "O",
                "SPCC OSAS",
                "Student affairs and services",
                "https://www.facebook.com/SPCC.OSAS",
                0xFFEAF7F0
        );

        addSourceCard(
                "C",
                "CCSOCTA",
                "College and organization updates",
                "https://www.facebook.com/CCSOCTA",
                0xFFFFF3E8
        );

        LinearLayout note = card();
        note.setPadding(dp(15), dp(15), dp(15), dp(15));
        note.setBackground(roundDrawable(0xFFEDF1FF, 16));

        note.addView(text(
                "HOW SYNC WORKS", 10, BLUE, true
        ));

        TextView detail = text(
                "Posts are downloaded by the backend and stored locally. Once cached, you can still browse and search them without an internet connection.",
                12, 0xFF526080, false
        );

        LinearLayout.LayoutParams noteParams =
                params(-1, -2);

        noteParams.topMargin = dp(6);
        note.addView(detail, noteParams);

        addView(contentContainer, note, 10, 10);
    }

    private void addSourceCard(
            String initial,
            String name,
            String description,
            String url,
            int tint) {

        LinearLayout source = card();
        source.setPadding(dp(15), dp(14), dp(15), dp(14));

        LinearLayout line = row();
        line.setGravity(Gravity.CENTER_VERTICAL);

        TextView avatar = text(initial, 17, BLUE, true);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(roundDrawable(tint, 13));

        line.addView(avatar, params(43, 43));

        LinearLayout details = column();
        details.setPadding(dp(12), 0, 0, 0);

        details.addView(text(name, 14, DARK, true));

        TextView desc = text(
                description, 11, MUTED, false
        );

        LinearLayout.LayoutParams dcp = params(-1, -2);
        dcp.topMargin = dp(4);

        details.addView(desc, dcp);

        line.addView(details,
                new LinearLayout.LayoutParams(0, -2, 1));

        TextView arrow = text("↗", 18, BLUE, true);
        line.addView(arrow);

        source.addView(line);
        source.setOnClickListener(v -> openUrl(url));

        addView(contentContainer, source, 0, 10);
    }

    // =========================
    // REUSABLE UI HELPERS
    // =========================

    private LinearLayout card() {
        LinearLayout layout = column();

        layout.setBackground(
                roundDrawable(WHITE, 18, 0xFFE7EAF2)
        );

        return layout;
    }

    private LinearLayout row() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        return layout;
    }

    private LinearLayout column() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private TextView text(
            String value, int size, int color, boolean bold) {

        TextView view = new TextView(this);

        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);

        view.setTypeface(
                Typeface.DEFAULT,
                bold ? Typeface.BOLD : Typeface.NORMAL
        );

        view.setIncludeFontPadding(true);

        return view;
    }

    private void addText(
            String value,
            int size,
            int color,
            boolean bold,
            int top,
            int bottom) {

        TextView view = text(value, size, color, bold);

        LinearLayout.LayoutParams p = params(-1, -2);
        p.topMargin = dp(top);
        p.bottomMargin = dp(bottom);

        contentContainer.addView(view, p);
    }

    private void addView(
            LinearLayout parent,
            View child,
            int top,
            int bottom) {

        LinearLayout.LayoutParams p = params(-1, -2);
        p.topMargin = dp(top);
        p.bottomMargin = dp(bottom);

        parent.addView(child, p);
    }

    private LinearLayout.LayoutParams params(
            int width, int height) {

        return new LinearLayout.LayoutParams(
                width < 0 ? width : dp(width),
                height < 0 ? height : dp(height)
        );
    }

    private int dp(int value) {
        return (int) (
                value * getResources()
                        .getDisplayMetrics().density + 0.5f
        );
    }

    private GradientDrawable roundDrawable(
            int color, int radius) {

        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));

        return drawable;
    }

    private GradientDrawable roundDrawable(
            int color, int radius, int stroke) {

        GradientDrawable drawable =
                roundDrawable(color, radius);

        drawable.setStroke(dp(1), stroke);

        return drawable;
    }

    private String initials(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "CS";
        }

        String[] parts = name.trim().split("\\s+");

        if (parts.length == 1) {
            return parts[0].substring(0, 1).toUpperCase();
        }

        return (
                parts[0].substring(0, 1) +
                        parts[1].substring(0, 1)
        ).toUpperCase();
    }

    private void openUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return;
        }

        try {
            startActivity(new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(url)
            ));
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopThinkingAnimation();
        uiHandler.removeCallbacksAndMessages(null);
        executor.shutdown();
    }
}
