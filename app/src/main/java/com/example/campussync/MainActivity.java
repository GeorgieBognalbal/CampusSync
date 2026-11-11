package com.example.campussync;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.example.campussync.ai.CloudAIAdapter;
import com.example.campussync.ai.LocalNanoAIEngine;
import com.example.campussync.data.local.AnnouncementDatabase;
import com.example.campussync.data.local.AnnouncementEntity;
import com.example.campussync.data.model.ChatMessage;
import com.example.campussync.data.repository.AnnouncementRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * CampusSense Main Activity
 * Hack4Success 2026 - Student Around Services (LSPU-SPCC)
 * Integrated with LocalNanoAIEngine for Hybrid Edge-First Nano RAG with Cloud Fallback.
 */
public class MainActivity extends Activity {

    private static final int BLUE = Color.rgb(49, 87, 213);
    private static final int DARK = Color.rgb(24, 34, 56);
    private static final int MUTED = Color.rgb(122, 132, 153);
    private static final int PALE = Color.rgb(237, 241, 255);
    private static final int WHITE = Color.WHITE;
    private static final int GREEN = Color.rgb(36, 122, 87);
    private static final int PALE_GREEN = Color.rgb(234, 247, 240);

    private static final String[] QUICK_PROMPTS = {
            "May pasok ba bukas sa LSPU?",
            "Kailan deadline ng TDP scholarship?",
            "Midterm exam schedule",
            "Hack4Success intramurals",
            "Unknown question"
    };

    private LinearLayout contentContainer;
    private TextView navPosts, navChat, navSources;
    private ScrollView contentScroll;
    private View chatInputBar;
    private EditText chatInput;
    private TextView chatSend;
    private TextView statusText;

    private AnnouncementRepository announcementRepository;
    private final LocalNanoAIEngine nanoAIEngine = new LocalNanoAIEngine();
    private CloudAIAdapter cloudAdapter;
    private ConnectivityManager.NetworkCallback networkCallback;

    private LocalNanoAIEngine.Intent selectedCategoryFilter = null; // null means 'All'
    private String currentSearchQuery = "";

    private final List<ChatMessage> chatHistory = new ArrayList<>();
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private LinearLayout messageList;
    private Runnable thinkingTicker;
    private boolean botBusy = false;

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
        statusText = findViewById(R.id.statusText);

        cloudAdapter = new CloudAIAdapter(this);
        updateNetworkStatusUI();
        registerNetworkListener();

        AnnouncementDatabase database =
                AnnouncementDatabase.getInstance(getApplicationContext());

        announcementRepository =
                new AnnouncementRepository(database.announcementDao());

        seedDemoAnnouncementsIfEmpty();
        showAnnouncements();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateNetworkStatusUI();
    }

    private void updateNetworkStatusUI() {
        runOnUiThread(() -> {
            if (statusText == null) return;
            boolean online = cloudAdapter != null && cloudAdapter.isOnline();
            if (online) {
                statusText.setText("● NANO AI READY (ONLINE)");
                statusText.setTextColor(GREEN);
            } else {
                statusText.setText("● NANO AI READY (OFFLINE)");
                statusText.setTextColor(GREEN);
            }
        });
    }

    private void registerNetworkListener() {
        try {
            ConnectivityManager cm = (ConnectivityManager)
                    getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                networkCallback = new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(android.net.Network network) {
                        updateNetworkStatusUI();
                    }

                    @Override
                    public void onLost(android.net.Network network) {
                        updateNetworkStatusUI();
                    }
                };
                cm.registerDefaultNetworkCallback(networkCallback);
            }
        } catch (Exception ignored) {}
    }

    private void seedDemoAnnouncementsIfEmpty() {
        executor.execute(() -> {
            List<AnnouncementEntity> existing = announcementRepository.getAll();

            boolean needsReseed = existing.isEmpty();
            if (!needsReseed && existing.size() <= 2) {
                for (AnnouncementEntity item : existing) {
                    if ("demo-001".equals(item.facebookPostId) || "demo-002".equals(item.facebookPostId)) {
                        needsReseed = true;
                        break;
                    }
                }
            }

            if (needsReseed) {
                announcementRepository.deleteAll();
                List<AnnouncementEntity> samples = Arrays.asList(
                        new AnnouncementEntity(
                                "lspu-post-001",
                                "LSPU Official",
                                "ADVISORY: Classes are suspended tomorrow due to Tropical Cyclone Wind Signal #2 according to LSPU Official. All college and graduate school students in San Pablo City Campus are advised to take safety precautions.",
                                "https://www.facebook.com/LSPUOfficial/posts/suspension-signal2",
                                System.currentTimeMillis() - 1800000
                        ),
                        new AnnouncementEntity(
                                "osas-post-002",
                                "SPCC OSAS",
                                "ANNOUNCEMENT: According to SPCC OSAS, the submission deadline for TDP documents is Friday, Nov 14 at 5:00 PM at the OSAS office. Please bring completed endorsement slips and certified grades for validation.",
                                "https://www.facebook.com/SPCC.OSAS/posts/tdp-scholarship-deadline",
                                System.currentTimeMillis() - 7200000
                        ),
                        new AnnouncementEntity(
                                "lspu-post-003",
                                "LSPU Official",
                                "ACADEMIC ADVISORY: Midterm examinations are scheduled on Nov 24–28 per the LSPU Academic Calendar advisory. Students must secure examination permits from the registrar prior to the examination week.",
                                "https://www.facebook.com/LSPUOfficial/posts/midterm-examinations-schedule",
                                System.currentTimeMillis() - 86400000
                        ),
                        new AnnouncementEntity(
                                "octa-post-004",
                                "CCSOCTA",
                                "CALL FOR PARTICIPANTS: Hack4Success 2026 Hackathon and Intramurals 2026 opening ceremonies will be held at the CTE Function Hall! Registration for the Student Around Services track is now open.",
                                "https://www.facebook.com/CCSOCTA/posts/hack4success-2026-registration",
                                System.currentTimeMillis() - 172800000
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
        if (chatInputBar != null) {
            chatInputBar.setVisibility(tab == 1 ? View.VISIBLE : View.GONE);
        }
        if (tab != 1) stopThinkingAnimation();

        navPosts.setTextColor(tab == 0 ? BLUE : MUTED);
        navChat.setTextColor(tab == 1 ? BLUE : MUTED);
        navSources.setTextColor(tab == 2 ? BLUE : MUTED);

        navPosts.setTypeface(null, tab == 0 ? Typeface.BOLD : Typeface.NORMAL);
        navChat.setTypeface(null, tab == 1 ? Typeface.BOLD : Typeface.NORMAL);
        navSources.setTypeface(null, tab == 2 ? Typeface.BOLD : Typeface.NORMAL);
    }

    // =========================================================================
    // 1. ANNOUNCEMENTS FEED (With Interactive Category Filter Pills)
    // =========================================================================

    private void showAnnouncements() {
        setActiveTab(0);
        contentContainer.removeAllViews();

        addText("Good day, LSPU! 👋", 13, MUTED, false, 0, 2);
        addText("Campus updates,\none place.", 29, DARK, true, 0, 12);

        // Header Pulse Banner
        LinearLayout summary = card();
        summary.setBackground(roundDrawable(BLUE, 20));
        summary.setPadding(dp(18), dp(17), dp(18), dp(17));

        summary.addView(text("CAMPUS PULSE • LOCAL AI CACHE", 10, 0xFFDCE4FF, true));

        TextView summaryTitle = text("Stay in sync with what’s happening.", 17, WHITE, true);
        LinearLayout.LayoutParams stp = params(-1, -2);
        stp.topMargin = dp(7);
        summary.addView(summaryTitle, stp);

        TextView summaryCaption = text("Offline intelligence ready. Browse verified announcements or ask Nano AI directly.", 12, 0xFFE5EAFF, false);
        LinearLayout.LayoutParams scp = params(-1, -2);
        scp.topMargin = dp(5);
        summary.addView(summaryCaption, scp);

        addView(contentContainer, summary, 0, 15);

        addText("SEARCH & FILTER", 11, MUTED, true, 0, 9);

        // Search Input
        EditText search = new EditText(this);
        search.setSingleLine(true);
        search.setTextSize(14);
        search.setHint("Search announcements…");
        search.setCompoundDrawablesWithIntrinsicBounds(
                android.R.drawable.ic_menu_search, 0, 0, 0
        );
        search.setPadding(dp(14), 0, dp(14), 0);
        search.setBackground(roundDrawable(WHITE, 14, 0xFFE2E7F0));
        contentContainer.addView(search, params(-1, 50));

        // Interactive Category Filter Pills
        final LocalNanoAIEngine.Intent[] filterIntents = {
                null,
                LocalNanoAIEngine.Intent.SUSPENSION,
                LocalNanoAIEngine.Intent.SCHOLARSHIP,
                LocalNanoAIEngine.Intent.ACADEMICS,
                LocalNanoAIEngine.Intent.EVENTS
        };
        final String[] filterNames = {
                "All",
                "🌧️ Suspensions",
                "🎓 Scholarships",
                "📚 Academics",
                "📢 Events"
        };

        HorizontalScrollView pillScrollView = new HorizontalScrollView(this);
        pillScrollView.setHorizontalScrollBarEnabled(false);
        LinearLayout pillRow = new LinearLayout(this);
        pillRow.setOrientation(LinearLayout.HORIZONTAL);
        pillRow.setPadding(0, dp(10), 0, dp(6));

        final List<TextView> pillViews = new ArrayList<>();
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);

        for (int i = 0; i < filterIntents.length; i++) {
            final LocalNanoAIEngine.Intent intentFilter = filterIntents[i];
            final String label = filterNames[i];

            TextView pill = text(label, 12, DARK, false);
            pill.setPadding(dp(13), dp(7), dp(13), dp(7));

            boolean isSelected = (selectedCategoryFilter == intentFilter);
            updatePillStyle(pill, isSelected);

            pill.setOnClickListener(v -> {
                selectedCategoryFilter = intentFilter;
                for (int j = 0; j < pillViews.size(); j++) {
                    updatePillStyle(pillViews.get(j), filterIntents[j] == selectedCategoryFilter);
                }
                loadAnnouncementsInto(list, currentSearchQuery);
            });

            pillViews.add(pill);
            LinearLayout.LayoutParams pp = params(-2, -2);
            pp.rightMargin = dp(8);
            pillRow.addView(pill, pp);
        }

        pillScrollView.addView(pillRow, new FrameLayout.LayoutParams(-2, -2));
        contentContainer.addView(pillScrollView, params(-1, -2));

        // Section Title
        TextView feedLabel = text("Recent posts", 18, DARK, true);
        LinearLayout.LayoutParams flp = params(-1, -2);
        flp.topMargin = dp(14);
        flp.bottomMargin = dp(10);
        contentContainer.addView(feedLabel, flp);

        contentContainer.addView(list, params(-1, -2));

        search.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().trim();
                loadAnnouncementsInto(list, currentSearchQuery);
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        loadAnnouncementsInto(list, "");
    }

    private void updatePillStyle(TextView pill, boolean isSelected) {
        if (isSelected) {
            pill.setBackground(roundDrawable(BLUE, 16));
            pill.setTextColor(WHITE);
            pill.setTypeface(null, Typeface.BOLD);
        } else {
            pill.setBackground(roundDrawable(WHITE, 16, 0xFFD8DEEB));
            pill.setTextColor(DARK);
            pill.setTypeface(null, Typeface.NORMAL);
        }
    }

    private void loadAnnouncementsInto(LinearLayout list, String query) {
        list.removeAllViews();

        executor.execute(() -> {
            List<AnnouncementEntity> announcements =
                    query.isEmpty()
                            ? announcementRepository.getAll()
                            : announcementRepository.search(query);

            if (selectedCategoryFilter != null) {
                List<AnnouncementEntity> filtered = new ArrayList<>();
                for (AnnouncementEntity item : announcements) {
                    if (matchesCategory(item, selectedCategoryFilter)) {
                        filtered.add(item);
                    }
                }
                announcements = filtered;
            }

            final List<AnnouncementEntity> displayList = announcements;

            runOnUiThread(() -> {
                if (isFinishing()) return;

                list.removeAllViews();

                if (displayList.isEmpty()) {
                    LinearLayout empty = card();
                    empty.setPadding(dp(18), dp(22), dp(18), dp(22));

                    empty.addView(text("No matching announcements", 15, DARK, true));

                    TextView hint = text(
                            selectedCategoryFilter != null
                                    ? "No announcements found under this filter. Tap 'All' or sync for newer posts."
                                    : "Try another keyword or sync your sources when you’re online.",
                            12, MUTED, false
                    );

                    LinearLayout.LayoutParams hp = params(-1, -2);
                    hp.topMargin = dp(5);
                    empty.addView(hint, hp);
                    list.addView(empty);

                } else {
                    for (AnnouncementEntity item : displayList) {
                        addAnnouncementCard(list, item);
                    }
                }
            });
        });
    }

    private boolean matchesCategory(AnnouncementEntity item, LocalNanoAIEngine.Intent category) {
        if (category == null) return true;
        List<String> keywords = nanoAIEngine.getDictionaryForIntent(category);
        if (keywords == null) return true;
        String content = (item.message + " " + item.pageName).toLowerCase(Locale.ROOT);
        for (String kw : keywords) {
            if (content.contains(kw)) {
                return true;
            }
        }
        return false;
    }

    private void addAnnouncementCard(LinearLayout parent, AnnouncementEntity item) {
        LinearLayout outer = card();
        outer.setPadding(dp(15), dp(15), dp(15), dp(14));

        LinearLayout top = row();

        TextView avatar = text(initials(item.pageName), 12, BLUE, true);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(roundDrawable(PALE, 12));
        top.addView(avatar, params(38, 38));

        LinearLayout sourceBlock = column();
        sourceBlock.setPadding(dp(10), 0, 0, 0);

        sourceBlock.addView(text(item.pageName, 14, DARK, true));

        TextView time = text(
                DateFormat.format("MMM dd • hh:mm a", item.createdAt).toString(),
                11, MUTED, false
        );

        LinearLayout.LayoutParams tp = params(-2, -2);
        tp.topMargin = dp(3);
        sourceBlock.addView(time, tp);

        top.addView(sourceBlock, new LinearLayout.LayoutParams(0, -2, 1));

        TextView tag = text("POST", 9, BLUE, true);
        tag.setPadding(dp(9), dp(6), dp(9), dp(6));
        tag.setBackground(roundDrawable(PALE, 20));
        top.addView(tag);

        outer.addView(top);

        TextView message = text(item.message, 14, 0xFF46516A, false);
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

        TextView cached = text("●  Saved on this device", 10, GREEN, true);
        footer.addView(cached, new LinearLayout.LayoutParams(0, -2, 1));

        TextView open = text("Open post  ↗", 11, BLUE, true);
        footer.addView(open);
        open.setOnClickListener(v -> openUrl(item.permalink));

        LinearLayout.LayoutParams fp = params(-1, -2);
        fp.topMargin = dp(11);
        outer.addView(footer, fp);

        addView(parent, outer, 0, 12);
    }

    // =========================================================================
    // 2. CONVERSATIONAL CHAT UI (Connected to LocalNanoAIEngine & CloudAIAdapter)
    // =========================================================================

    private void showChat() {
        setActiveTab(1);
        contentContainer.removeAllViews();

        // Header Card with Bot Avatar and Live Status Badge
        LinearLayout header = row();
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView icon = text("✦", 18, WHITE, true);
        icon.setGravity(Gravity.CENTER);
        icon.setBackgroundResource(R.drawable.bot_avatar_background);
        header.addView(icon, params(40, 40));

        LinearLayout titleBlock = column();
        titleBlock.setPadding(dp(11), 0, 0, 0);
        titleBlock.addView(text("CampusSense Assistant", 15, DARK, true));

        boolean online = cloudAdapter != null && cloudAdapter.isOnline();
        TextView status = text(
                online ? "● NANO AI READY (ONLINE)" : "● NANO AI READY (OFFLINE)",
                11, GREEN, true
        );
        LinearLayout.LayoutParams sp = params(-2, -2);
        sp.topMargin = dp(3);
        titleBlock.addView(status, sp);
        header.addView(titleBlock);

        addView(contentContainer, header, 0, 16);

        // Chat Message List
        messageList = column();
        contentContainer.addView(messageList, params(-1, -2));

        if (chatHistory.isEmpty()) {
            chatHistory.add(new ChatMessage(
                    "Hi! 👋 Ako si CampusSense Assistant. Sub-15ms on-device intelligence ang sumasagot sa klase, scholarships, at exam schedules—kahit naka-Airplane Mode!",
                    ChatMessage.Sender.BOT
            ));
        }

        for (ChatMessage m : chatHistory) {
            messageList.addView(buildMessageView(m));
        }

        // Quick Demonstration Prompt Chips
        TextView suggestLabel = text("DEMO QUICK PROMPTS (GOLDEN TEST CASES)", 10, MUTED, true);
        LinearLayout.LayoutParams slp = params(-1, -2);
        slp.topMargin = dp(14);
        slp.bottomMargin = dp(8);
        contentContainer.addView(suggestLabel, slp);

        HorizontalScrollView pillScroll = new HorizontalScrollView(this);
        pillScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout chipRow = row();

        for (String promptText : QUICK_PROMPTS) {
            TextView pill = text(promptText, 12, DARK, false);
            pill.setBackgroundResource(R.drawable.suggestion_pill);
            pill.setPadding(dp(13), dp(8), dp(13), dp(8));
            pill.setOnClickListener(v -> sendChatMessage(promptText));

            LinearLayout.LayoutParams pp = params(-2, -2);
            pp.rightMargin = dp(8);
            chipRow.addView(pill, pp);
        }

        pillScroll.addView(chipRow);
        contentContainer.addView(pillScroll, params(-1, -2));

        // Bottom Chat Composer Actions
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
            List<AnnouncementEntity> cachedPosts = announcementRepository.getAll();
            long start = System.nanoTime();
            LocalNanoAIEngine.NanoAIResult result =
                    nanoAIEngine.processHybridQuery(question, cachedPosts, cloudAdapter);
            long latencyMs = (System.nanoTime() - start) / 1000000;

            List<ChatMessage.Source> sources = new ArrayList<>();
            if (result.primarySource != null) {
                sources.add(new ChatMessage.Source(
                        result.primarySource.pageName,
                        result.primarySource.message,
                        result.primarySource.permalink
                ));
            }

            // Frame conversational reply bubble
            String modePrefix = result.isOfflineAnswer
                    ? "⚡ [" + latencyMs + "ms • Local Nano AI]\n"
                    : "☁️ [" + latencyMs + "ms • Cloud Fallback]\n";

            String replyText = modePrefix + result.answer;
            ChatMessage botAnswer = new ChatMessage(replyText, ChatMessage.Sender.BOT, sources);

            // Small delay for smooth thinking transition
            uiHandler.postDelayed(() -> {
                if (isFinishing()) return;
                chatHistory.remove(thinking);
                chatHistory.add(botAnswer);
                stopThinkingAnimation();

                if (messageList != null && messageList.isShown()) {
                    int last = messageList.getChildCount() - 1;
                    if (last >= 0) messageList.removeViewAt(last);
                    messageList.addView(buildMessageView(botAnswer));
                    scrollChatToBottom();
                }
                botBusy = false;
            }, 300);
        });
    }

    private void appendIfVisible(ChatMessage m) {
        if (messageList != null && messageList.isShown()) {
            messageList.addView(buildMessageView(m));
            scrollChatToBottom();
        }
    }

    private View buildMessageView(ChatMessage m) {
        if (m.isThinking()) {
            return buildThinkingView();
        }
        return m.isUser() ? buildUserBubble(m) : buildBotBubble(m);
    }

    private View buildUserBubble(ChatMessage m) {
        LinearLayout row = row();
        row.setGravity(Gravity.END);
        row.setPadding(0, dp(4), 0, dp(4));

        TextView bubble = text(m.getText(), 14, WHITE, false);
        bubble.setBackgroundResource(R.drawable.chat_bubble_user);
        bubble.setPadding(dp(14), dp(10), dp(14), dp(10));

        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-2, -2);
        p.gravity = Gravity.END;
        row.addView(bubble, p);
        return row;
    }

    private View buildBotBubble(ChatMessage m) {
        LinearLayout outer = column();
        outer.setPadding(0, dp(6), 0, dp(6));

        LinearLayout line = row();
        line.setGravity(Gravity.START | Gravity.TOP);

        TextView icon = text("✦", 12, WHITE, true);
        icon.setGravity(Gravity.CENTER);
        icon.setBackgroundResource(R.drawable.bot_avatar_background);
        line.addView(icon, params(26, 26));

        LinearLayout content = column();
        content.setPadding(dp(8), 0, 0, 0);

        TextView bubble = text(m.getText(), 14, DARK, false);
        bubble.setBackgroundResource(R.drawable.chat_bubble_bot);
        bubble.setPadding(dp(13), dp(10), dp(13), dp(10));
        bubble.setLineSpacing(dp(2), 1.15f);
        content.addView(bubble, params(-2, -2));

        if (m.hasSources()) {
            for (ChatMessage.Source s : m.getSources()) {
                content.addView(buildSourceCard(s, -1));
            }
        }

        line.addView(content, new LinearLayout.LayoutParams(0, -2, 1));
        outer.addView(line);
        return outer;
    }

    private View buildThinkingView() {
        LinearLayout wrapper = row();
        wrapper.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        wrapper.setPadding(0, dp(6), 0, dp(6));

        TextView icon = text("✦", 12, WHITE, true);
        icon.setGravity(Gravity.CENTER);
        icon.setBackgroundResource(R.drawable.bot_avatar_background);
        wrapper.addView(icon, params(26, 26));

        TextView bubble = text("Thinking...", 13, MUTED, false);
        bubble.setBackgroundResource(R.drawable.chat_bubble_bot);
        bubble.setPadding(dp(13), dp(9), dp(13), dp(9));
        startThinkingAnimation(bubble);

        LinearLayout.LayoutParams p = params(-2, -2);
        p.leftMargin = dp(8);
        wrapper.addView(bubble, p);
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
        if (contentScroll != null) {
            contentScroll.post(() -> contentScroll.fullScroll(View.FOCUS_DOWN));
        }
    }

    private String shorten(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max).trim() + "…";
    }

    // =========================================================================
    // 3. SOURCES TAB
    // =========================================================================

    private void showSources() {
        setActiveTab(2);
        contentContainer.removeAllViews();

        addText("YOUR INFORMATION FEED", 11, BLUE, true, 0, 8);
        addText("Trusted campus sources.", 26, DARK, true, 0, 7);
        addText(
                "CampusSense collects public updates from official university pages and saves them into a local Room SQLite database for offline RAG synthesis.",
                13, MUTED, false, 0, 17
        );

        addSourceCard("L", "LSPU Official", "University-wide announcements & advisories", "https://www.facebook.com/LSPUOfficial", 0xFFEAF0FF);
        addSourceCard("O", "SPCC OSAS", "Student affairs, scholarships & validation", "https://www.facebook.com/SPCC.OSAS", 0xFFEAF7F0);
        addSourceCard("C", "CCSOCTA", "College and organization events & activities", "https://www.facebook.com/CCSOCTA", 0xFFFFF3E8);

        LinearLayout note = card();
        note.setPadding(dp(15), dp(15), dp(15), dp(15));
        note.setBackground(roundDrawable(0xFFEDF1FF, 16));

        note.addView(text("HOW NANO RAG WORKS", 10, BLUE, true));

        TextView detail = text(
                "1. Intent Tokenizer parses queries in English or Taglish.\n" +
                "2. Context Ranker scores local posts with (IntentWeight×0.4) + (KeywordHits×0.4) + (Recency×0.2).\n" +
                "3. Answer Synthesizer compiles factual answers citing official pages in <15ms without server reliance.",
                12, 0xFF526080, false
        );

        LinearLayout.LayoutParams noteParams = params(-1, -2);
        noteParams.topMargin = dp(6);
        note.addView(detail, noteParams);

        addView(contentContainer, note, 10, 10);
    }

    private void addSourceCard(String initial, String name, String description, String url, int tint) {
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

        TextView desc = text(description, 11, MUTED, false);
        LinearLayout.LayoutParams dcp = params(-1, -2);
        dcp.topMargin = dp(4);
        details.addView(desc, dcp);

        line.addView(details, new LinearLayout.LayoutParams(0, -2, 1));

        TextView arrow = text("↗", 18, BLUE, true);
        line.addView(arrow);

        source.addView(line);
        source.setOnClickListener(v -> openUrl(url));

        addView(contentContainer, source, 0, 10);
    }

    // =========================================================================
    // REUSABLE UI HELPERS
    // =========================================================================

    private LinearLayout card() {
        LinearLayout layout = column();
        layout.setBackground(roundDrawable(WHITE, 18, 0xFFE7EAF2));
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

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
        view.setIncludeFontPadding(true);
        return view;
    }

    private void addText(String value, int size, int color, boolean bold, int top, int bottom) {
        TextView view = text(value, size, color, bold);
        LinearLayout.LayoutParams p = params(-1, -2);
        p.topMargin = dp(top);
        p.bottomMargin = dp(bottom);
        contentContainer.addView(view, p);
    }

    private void addView(LinearLayout parent, View child, int top, int bottom) {
        LinearLayout.LayoutParams p = params(-1, -2);
        p.topMargin = dp(top);
        p.bottomMargin = dp(bottom);
        parent.addView(child, p);
    }

    private LinearLayout.LayoutParams params(int width, int height) {
        return new LinearLayout.LayoutParams(
                width < 0 ? width : dp(width),
                height < 0 ? height : dp(height)
        );
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable roundDrawable(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }

    private GradientDrawable roundDrawable(int color, int radius, int stroke) {
        GradientDrawable drawable = roundDrawable(color, radius);
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
        return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
    }

    private void openUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return;
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception ignored) {}
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopThinkingAnimation();
        uiHandler.removeCallbacksAndMessages(null);

        if (networkCallback != null) {
            try {
                ConnectivityManager cm = (ConnectivityManager)
                        getSystemService(Context.CONNECTIVITY_SERVICE);
                if (cm != null) {
                    cm.unregisterNetworkCallback(networkCallback);
                }
            } catch (Exception ignored) {}
        }
        executor.shutdown();
    }
}
