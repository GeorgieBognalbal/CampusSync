package com.example.campussync;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.Gravity;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.example.campussync.ai.CloudAIAdapter;
import com.example.campussync.ai.LocalNanoAIEngine;
import com.example.campussync.data.local.AnnouncementDatabase;
import com.example.campussync.calendar.CampusDates;
import com.example.campussync.data.local.AnnouncementEntity;
import com.example.campussync.data.model.ChatMessage;
import com.example.campussync.notifications.KaleoNotifications;
import com.example.campussync.notifications.KaleoReminders;
import com.example.campussync.data.repository.AnnouncementRepository;
import com.example.campussync.sync.SyncScheduler;
import com.example.campussync.sync.SyncState;
import com.example.campussync.util.MarkdownFormatter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {

    // Loaded from res/values/colors.xml (or values-night/ in dark mode)
    private int BLUE, DARK, BODY, MUTED, PALE, CARD, BORDER, SUCCESS, SKELETON;
    private static final int WHITE = 0xFFFFFFFF;

    private static final String PREFS = "campussync_prefs";
    private static final String KEY_UNFOLLOWED = "unfollowed_sources";
    private static final String KEY_LAST_SYNC = "last_sync";

    private LinearLayout contentContainer;
    private ScrollView contentScroll;
    private SwipeRefreshLayout swipeRefresh;
    private TextView statusText;
    private View chatInputBar;
    private EditText chatInput;
    private TextView chatSend;

    private ImageView[] navIcons;
    private TextView[] navLabels;
    private int currentTab = 0;

    private AnnouncementRepository announcementRepository;
    private final LocalNanoAIEngine nanoAIEngine = new LocalNanoAIEngine();
    private CloudAIAdapter cloudAdapter;
    private SharedPreferences prefs;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    /**
     * Chat answers get their own thread: a cloud AI call can take up to ~20 s on a
     * slow connection, and on the shared executor that blocked the Feed, Calendar
     * and search from loading until the answer arrived.
     */
    private final ExecutorService chatExecutor =
            Executors.newSingleThreadExecutor();

    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    /** Infinite animations (skeletons, thinking dots) we must stop when views go away. */
    private final List<ValueAnimator> skeletonAnimators = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        applySystemBarInsets();

        loadColors();
        cloudAdapter = new CloudAIAdapter(this);

        contentContainer = findViewById(R.id.contentContainer);
        contentScroll = findViewById(R.id.contentScroll);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        statusText = findViewById(R.id.statusText);
        findViewById(R.id.notifButton).setOnClickListener(v -> showNotificationSettings());
        chatInputBar = findViewById(R.id.chatInputBar);
        chatInput = findViewById(R.id.chatInput);
        chatSend = findViewById(R.id.chatSend);

        navIcons = new ImageView[]{
                findViewById(R.id.navPostsIcon),
                findViewById(R.id.navChatIcon),
                findViewById(R.id.navCalendarIcon)
        };
        navLabels = new TextView[]{
                findViewById(R.id.navPostsLabel),
                findViewById(R.id.navChatLabel),
                findViewById(R.id.navCalendarLabel)
        };

        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        AnnouncementDatabase database =
                AnnouncementDatabase.getInstance(getApplicationContext());

        announcementRepository =
                new AnnouncementRepository(database.announcementDao());

        // Pull-to-refresh
        swipeRefresh.setColorSchemeColors(BLUE);
        swipeRefresh.setProgressBackgroundColorSchemeColor(CARD);
        swipeRefresh.setOnRefreshListener(this::refreshAnnouncements);

        // Tap the status chip to refresh too
        statusText.setOnClickListener(v -> {
            if (isOnline && SyncState.getLastError(this) != null) {
                showSyncProblem();
                return;
            }
            swipeRefresh.setRefreshing(true);
            refreshAnnouncements();
        });

        startNetworkWatch();
        updateStatus();
        seedDemoAnnouncementsIfEmpty();
        maybeAskNotificationPermission();
        showAnnouncements();
        watchSync();
    }

    /**
     * Android 15+ draws the app behind the status bar, navigation bar and
     * keyboard. Pad the root view so the header clears the status bar and the
     * chat box always sits right above the keyboard.
     */
    private void applySystemBarInsets() {
        // Before Android 15 the system already keeps us clear of the bars and keyboard
        if (android.os.Build.VERSION.SDK_INT < 35) return;

        View root = ((android.view.ViewGroup) findViewById(android.R.id.content)).getChildAt(0);
        final int baseLeft = root.getPaddingLeft();
        final int baseTop = root.getPaddingTop();
        final int baseRight = root.getPaddingRight();
        final int baseBottom = root.getPaddingBottom();

        root.setOnApplyWindowInsetsListener((v, insets) -> {
            android.graphics.Insets bars = insets.getInsets(
                    android.view.WindowInsets.Type.systemBars()
                            | android.view.WindowInsets.Type.displayCutout());
            android.graphics.Insets ime = insets.getInsets(
                    android.view.WindowInsets.Type.ime());
            int top = bars.top;
            int left = bars.left;
            int right = bars.right;
            int bottom = Math.max(bars.bottom, ime.bottom);
            boolean keyboardOpen = ime.bottom > 0;

            v.setPadding(baseLeft + left, baseTop + top,
                    baseRight + right, baseBottom + bottom);

            // Keep the latest message visible when the keyboard opens
            if (keyboardOpen && currentTab == 1) scrollChatToBottom();

            return insets;
        });
        root.requestApplyInsets();
    }

    private void loadColors() {
        BLUE = getColor(R.color.brand_primary);
        DARK = getColor(R.color.text_primary);
        BODY = getColor(R.color.text_body);
        MUTED = getColor(R.color.text_secondary);
        PALE = getColor(R.color.soft_blue);
        CARD = getColor(R.color.card_background);
        BORDER = getColor(R.color.card_border);
        SUCCESS = getColor(R.color.status_text);
        SKELETON = getColor(R.color.skeleton);
    }

    private void seedDemoAnnouncementsIfEmpty() {
        // Real posts have been synced from the backend: don't bring the demo posts back.
        if (SyncState.hasEverSynced(this) || SyncState.hasRealData(this)) return;

        executor.execute(() -> {
            List<AnnouncementEntity> existing =
                    announcementRepository.getAll();

            if (existing.isEmpty()
                    && !SyncState.hasEverSynced(this)
                    && !SyncState.hasRealData(this)) {
                List<AnnouncementEntity> samples = Arrays.asList(
                        new AnnouncementEntity(
                                "demo-001",
                                "LSPU Official",
                                "Welcome to Kaleo! This is sample announcement data. Replace it with real posts after connecting your backend.",
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
                        ),
                        new AnnouncementEntity(
                                "demo-006",
                                "CCSOCTA",
                                "Sample: Join the CCS Tech Week event this Friday at the AVR! Seminar on mobile app development plus a coding contest.",
                                "https://www.facebook.com/CCSOCTA",
                                System.currentTimeMillis() - 18000000
                        )
                );

                int inserted = announcementRepository.insertDemoIfEmpty(samples);
                android.util.Log.i("CampusSync", "Fallback demo announcements inserted: " + inserted);
            }

            runOnUiThread(() -> {
                if (currentTab == 0) showAnnouncements();
            });
        });
    }

    public void onAnnouncementsClick(View view) {
        showAnnouncements();
    }

    public void onChatClick(View view) {
        showChat();
    }

    public void onCalendarClick(View view) {
        showCalendar();
    }

    private void setActiveTab(int tab) {
        currentTab = tab;
        stopSkeletons();

        for (int i = 0; i < navIcons.length; i++) {
            boolean active = i == tab;
            navIcons[i].setImageTintList(
                    ColorStateList.valueOf(active ? BLUE : MUTED));
            navIcons[i].setBackgroundResource(
                    active ? R.drawable.nav_pill : 0);
            navLabels[i].setTextColor(active ? BLUE : MUTED);
            navLabels[i].setTypeface(null,
                    active ? Typeface.BOLD : Typeface.NORMAL);
        }

        chatInputBar.setVisibility(tab == 1 ? View.VISIBLE : View.GONE);
        if (tab != 1) stopThinkingAnimation();
        if (tab != 2) stopCountdown();

        // Pull-to-refresh only makes sense on the feed
        swipeRefresh.setEnabled(tab == 0);
        updateStatus();
    }

    // =========================
    // SYNC / REFRESH
    // =========================

    private void refreshAnnouncements() {
        if (!isOnline) {
            // Still queue it: it runs by itself as soon as a connection is back
            // (a laptop server on Wi-Fi without internet also counts as "offline" here).
            toast("You're offline — Kaleo will sync when you're connected");
        }
        // User asked: replace any sync stuck waiting in retry/backoff and run now.
        SyncScheduler.syncNow(getApplicationContext(), true);

        // No internet: the worker waits for a connection, so don't spin forever.
        uiHandler.postDelayed(() -> {
            if (isFinishing()) return;
            swipeRefresh.setRefreshing(false);
            updateStatus();
        }, 10_000);
    }

    private LiveData<List<WorkInfo>> syncWork;
    private Observer<List<WorkInfo>> syncObserver;
    private String lastHandledSync = "";

    /**
     * Watches the sync for the whole life of the screen (registered once).
     *
     * The old version added a new observer on every refresh and only reacted to
     * "finished" states. That missed two cases:
     *  - LiveData first replays the PREVIOUS run's result, so the observer fired on
     *    old news and removed itself before the new sync finished -> feed not reloaded.
     *  - A failed sync goes back to ENQUEUED (retry), never "finished" -> the spinner
     *    and status never showed the failure.
     */
    private void watchSync() {
        if (syncObserver != null) return;
        syncWork = WorkManager.getInstance(getApplicationContext())
                .getWorkInfosForUniqueWorkLiveData(SyncScheduler.ONE_TIME_WORK);
        syncObserver = infos -> {
            if (infos == null || infos.isEmpty() || isFinishing()) return;
            WorkInfo info = infos.get(0);
            WorkInfo.State state = info.getState();
            boolean retrying = state == WorkInfo.State.ENQUEUED && info.getRunAttemptCount() > 0;
            if (!state.isFinished() && !retrying) return;   // still waiting / running

            String key = info.getId() + ":" + state + ":" + info.getRunAttemptCount();
            if (key.equals(lastHandledSync)) return;
            lastHandledSync = key;

            swipeRefresh.setRefreshing(false);
            updateStatus();
            if (currentTab == 0) showAnnouncements();
            if (calendarSyncEnabled()) syncAllToPhoneCalendar(false);
            else if (currentTab == 2) showCalendar();
        };
        syncWork.observeForever(syncObserver);
    }

    private void updateStatus() {
        long last = SyncState.getLastSyncTime(this);
        String error = SyncState.getLastError(this);
        boolean failed = isOnline && error != null;
        if (!isOnline) {
            statusText.setText("Offline");
        } else if (failed) {
            statusText.setText("Sync failed · tap");
        } else if (last == 0) {
            statusText.setText("Not synced yet");
        } else {
            String when = System.currentTimeMillis() - last < DateUtils.MINUTE_IN_MILLIS
                    ? "just now"
                    : relativeTime(last);
            statusText.setText("Updated " + when);
        }

        android.graphics.drawable.Drawable icon = getDrawable(R.drawable.ic_refresh);
        if (icon != null) {
            icon = icon.mutate();
            icon.setTint(failed ? getColor(R.color.cat_suspension) : SUCCESS);
            icon.setBounds(0, 0, dp(12), dp(12));
            statusText.setCompoundDrawables(icon, null, null, null);
        }
        statusText.setTextColor(failed ? getColor(R.color.cat_suspension) : SUCCESS);
        statusText.setBackgroundTintList(failed
                ? ColorStateList.valueOf(getColor(R.color.cat_suspension_bg)) : null);
    }

    /** Explains why the last sync failed, in plain words, with a retry button. */
    private void showSyncProblem() {
        String error = SyncState.getLastError(this);
        String e = error == null ? "" : error;
        String hint;
        if (e.contains("HTTP 401")) {
            hint = "The app's API key doesn't match the server. Set campussync.apiKey in "
                    + "local.properties to the server's CAMPUSSYNC_API_KEY, then rebuild.";
        } else if (e.startsWith("Network")) {
            hint = "Couldn't reach the Kaleo server at " + BuildConfig.API_BASE_URL + ".\n\n"
                    + "• Is server.py running on the laptop?\n"
                    + "• Are the phone and laptop on the same Wi-Fi?\n"
                    + "• Did the laptop's IP change? Update campussync.apiBaseUrl in local.properties.\n"
                    + "• On the emulator, use http://10.0.2.2:5000";
        } else if (e.contains("HTTP 5")) {
            hint = "The server had an error. Check the terminal running server.py.";
        } else {
            hint = "Something went wrong while syncing.";
        }

        new android.app.AlertDialog.Builder(this)
                .setTitle("Couldn't sync")
                .setMessage(hint + "\n\nDetails: " + e)
                .setPositiveButton("Try again", (d, w) -> {
                    swipeRefresh.setRefreshing(true);
                    refreshAnnouncements();
                })
                .setNegativeButton("Close", null)
                .show();
    }

    // =========================
    // CATEGORIES
    // =========================

    /** Announcement type, detected from keywords in the post. */
    private enum Category {
        ALL("All", "", 0, 0),
        SUSPENSION("Classes", "⚠ Suspension", R.color.cat_suspension, R.color.cat_suspension_bg,
                "suspend", "walang pasok", "no classes", "typhoon", "pasok"),
        EXAM("Exams", "Exam", R.color.cat_exam, R.color.cat_exam_bg,
                "midterm", "exam", "finals", "quiz"),
        SCHOLARSHIP("Scholarships", "Scholarship", R.color.cat_scholarship, R.color.cat_scholarship_bg,
                "scholar", "grant", "financial aid", "allowance"),
        EVENT("Events", "Event", R.color.cat_event, R.color.cat_event_bg,
                "event", "seminar", "contest", "fair", "celebration", "intrams", "week"),
        GENERAL("General", "Update", R.color.cat_general, R.color.cat_general_bg);

        final String chipLabel;
        final String tagLabel;
        final int fgRes;
        final int bgRes;
        final String[] keywords;

        Category(String chipLabel, String tagLabel, int fgRes, int bgRes, String... keywords) {
            this.chipLabel = chipLabel;
            this.tagLabel = tagLabel;
            this.fgRes = fgRes;
            this.bgRes = bgRes;
            this.keywords = keywords;
        }

        static Category of(String message) {
            String m = message == null ? "" : message.toLowerCase(Locale.ROOT);
            for (Category c : new Category[]{SUSPENSION, EXAM, SCHOLARSHIP, EVENT}) {
                for (String k : c.keywords) {
                    if (m.contains(k)) return c;
                }
            }
            return GENERAL;
        }
    }

    private static final Category[] FILTERS = {
            Category.ALL, Category.SUSPENSION, Category.EXAM,
            Category.SCHOLARSHIP, Category.EVENT
    };

    private Category currentFilter = Category.ALL;
    private String currentQuery = "";

    // =========================
    // ANNOUNCEMENTS
    // =========================

    private void showAnnouncements() {
        setActiveTab(0);
        contentContainer.removeAllViews();

        addText("Good day, LSPU! 👋",
                13, MUTED, false, 0, 4);

        addText("Campus updates,\none place.",
                28, DARK, true, 0, 16);

        // Campus Pulse: shows the newest post
        LinearLayout summary = card();
        summary.setBackground(roundDrawable(BLUE, 20));
        summary.setPadding(dp(20), dp(16), dp(20), dp(16));

        summary.addView(text(
                "CAMPUS PULSE  •  LATEST", 10, 0xFFDCE4FF, true
        ));

        TextView summaryTitle =
                text("Stay in sync with what’s happening.",
                        16, WHITE, true);
        summaryTitle.setMaxLines(3);
        summaryTitle.setEllipsize(TextUtils.TruncateAt.END);

        LinearLayout.LayoutParams stp = params(-1, -2);
        stp.topMargin = dp(8);
        summary.addView(summaryTitle, stp);

        TextView summaryCaption =
                text("Announcements from your university pages, gathered for you.",
                        12, 0xFFE5EAFF, false);

        LinearLayout.LayoutParams scp = params(-1, -2);
        scp.topMargin = dp(8);
        summary.addView(summaryCaption, scp);

        addView(contentContainer, summary, 0, 16);
        loadPulseInto(summary, summaryTitle, summaryCaption);

        // Search
        EditText search = new EditText(this);
        search.setSingleLine(true);
        search.setTextSize(14);
        search.setTextColor(DARK);
        search.setHintTextColor(MUTED);
        search.setHint("Search announcements…");
        search.setText(currentQuery);

        search.setCompoundDrawablesWithIntrinsicBounds(
                android.R.drawable.ic_menu_search, 0, 0, 0
        );

        search.setPadding(dp(12), 0, dp(16), 0);
        search.setBackgroundResource(R.drawable.search_background);

        contentContainer.addView(search, params(-1, 48));

        // Filter chips
        HorizontalScrollView chipScroll = new HorizontalScrollView(this);
        chipScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout chipRow = row();
        chipScroll.addView(chipRow);

        LinearLayout.LayoutParams csp = params(-1, -2);
        csp.topMargin = dp(12);
        contentContainer.addView(chipScroll, csp);

        TextView feedLabel =
                text("Recent posts", 18, DARK, true);

        LinearLayout.LayoutParams flp = params(-1, -2);
        flp.topMargin = dp(20);
        flp.bottomMargin = dp(12);

        contentContainer.addView(feedLabel, flp);

        LinearLayout list = column();
        contentContainer.addView(list, params(-1, -2));

        Runnable renderChips = new Runnable() {
            @Override
            public void run() {
                chipRow.removeAllViews();
                for (Category c : FILTERS) {
                    boolean active = c == currentFilter;
                    TextView chip = text(c.chipLabel, 13,
                            active ? WHITE : BLUE, true);
                    chip.setPadding(dp(16), dp(8), dp(16), dp(8));
                    if (active) {
                        chip.setBackground(roundDrawable(BLUE, 20));
                    } else {
                        chip.setBackgroundResource(R.drawable.suggestion_pill);
                    }
                    Runnable self = this;
                    chip.setOnClickListener(v -> {
                        currentFilter = c;
                        self.run();
                        loadAnnouncementsInto(list);
                    });
                    LinearLayout.LayoutParams cp = params(-2, -2);
                    cp.rightMargin = dp(8);
                    chipRow.addView(chip, cp);
                }
            }
        };
        renderChips.run();

        search.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(
                    CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(
                    CharSequence s, int start, int before, int count) {
                currentQuery = s.toString().trim();
                loadAnnouncementsInto(list);
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
            }
        });

        loadAnnouncementsInto(list);
    }

    private void loadPulseInto(LinearLayout card, TextView title, TextView caption) {
        executor.execute(() -> {
            List<AnnouncementEntity> all = visibleAnnouncements(announcementRepository.getAll());
            if (all.isEmpty()) return;
            AnnouncementEntity latest = all.get(0);

            runOnUiThread(() -> {
                if (isFinishing() || currentTab != 0) return;
                Category c = Category.of(latest.message);
                String prefix = c == Category.SUSPENSION ? "⚠ " : "";
                title.setText(new SpannableStringBuilder(prefix)
                        .append(shorten(MarkdownFormatter.format(latest.message), 110)));
                caption.setText(latest.pageName + "  •  "
                        + relativeTime(latest.createdAt) + "   Tap to open ↗");
                card.setOnClickListener(v -> openUrl(latest.permalink));
            });
        });
    }

    /** Removes posts from sources the user unfollowed. */
    private List<AnnouncementEntity> visibleAnnouncements(List<AnnouncementEntity> input) {
        Set<String> unfollowed = prefs.getStringSet(KEY_UNFOLLOWED, new HashSet<>());
        List<AnnouncementEntity> out = new ArrayList<>();
        for (AnnouncementEntity a : input) {
            if (!unfollowed.contains(a.pageName)) out.add(a);
        }
        return out;
    }

    private void loadAnnouncementsInto(LinearLayout list) {
        final String query = currentQuery;
        final Category filter = currentFilter;

        showSkeletons(list);

        executor.execute(() -> {
            List<AnnouncementEntity> raw =
                    query.isEmpty()
                            ? announcementRepository.getAll()
                            : announcementRepository.search(query);

            List<AnnouncementEntity> announcements = new ArrayList<>();
            for (AnnouncementEntity a : visibleAnnouncements(raw)) {
                if (filter == Category.ALL || Category.of(a.message) == filter) {
                    announcements.add(a);
                }
            }

            // Keep skeletons on screen briefly so loading doesn't flicker
            uiHandler.postDelayed(() -> {
                if (isFinishing() || currentTab != 0) return;

                stopSkeletons();
                list.removeAllViews();

                if (announcements.isEmpty()) {
                    list.addView(emptyState(
                            "No matching announcements",
                            "Try another keyword or filter, or pull down to refresh."));
                } else {
                    int i = 0;
                    for (AnnouncementEntity item : announcements) {
                        View card = addAnnouncementCard(list, item);
                        animateIn(card, i++ * 40L);
                    }
                }
            }, 250);
        });
    }

    private View addAnnouncementCard(
            LinearLayout parent, AnnouncementEntity item) {

        Category category = Category.of(item.message);

        LinearLayout outer = card();
        outer.setPadding(dp(16), dp(16), dp(16), dp(12));

        LinearLayout top = row();
        top.setGravity(Gravity.CENTER_VERTICAL);

        TextView avatar = text(
                initials(item.pageName), 12, BLUE, true
        );

        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(roundDrawable(PALE, 12));

        top.addView(avatar, params(40, 40));

        LinearLayout sourceBlock = column();
        sourceBlock.setPadding(dp(12), 0, 0, 0);

        sourceBlock.addView(text(
                item.pageName, 14, DARK, true
        ));

        TextView time = text(
                relativeTime(item.createdAt),
                11, MUTED, false
        );

        LinearLayout.LayoutParams tp = params(-2, -2);
        tp.topMargin = dp(4);

        sourceBlock.addView(time, tp);

        top.addView(sourceBlock,
                new LinearLayout.LayoutParams(0, -2, 1));

        TextView tag = text(category.tagLabel.toUpperCase(Locale.ROOT),
                9, getColor(category.fgRes), true);
        tag.setLetterSpacing(0.06f);
        tag.setPadding(dp(8), dp(4), dp(8), dp(4));
        tag.setBackground(roundDrawable(getColor(category.bgRes), 20));

        top.addView(tag);
        outer.addView(top);

        TextView message = text(
                MarkdownFormatter.format(item.message), 14, BODY, false
        );

        message.setLineSpacing(dp(4), 1f);

        LinearLayout.LayoutParams mp = params(-1, -2);
        mp.topMargin = dp(12);
        mp.bottomMargin = dp(12);

        outer.addView(message, mp);

        View divider = new View(this);
        divider.setBackgroundColor(getColor(R.color.divider));

        outer.addView(divider, params(-1, 1));

        LinearLayout footer = row();
        footer.setGravity(Gravity.CENTER_VERTICAL);

        TextView cached = text(
                "●  Saved on this device",
                10, SUCCESS, true
        );

        footer.addView(cached,
                new LinearLayout.LayoutParams(0, -2, 1));

        TextView open = text(
                "Open post  ↗", 12, BLUE, true
        );
        open.setPadding(dp(8), dp(8), 0, dp(4));

        footer.addView(open);
        open.setOnClickListener(v -> openUrl(item.permalink));

        LinearLayout.LayoutParams fp = params(-1, -2);
        fp.topMargin = dp(4);

        outer.addView(footer, fp);

        addView(parent, outer, 0, 12);
        return outer;
    }

    // ---------- loading skeletons ----------

    private void showSkeletons(LinearLayout list) {
        stopSkeletons();
        list.removeAllViews();
        for (int i = 0; i < 3; i++) {
            LinearLayout sk = card();
            sk.setPadding(dp(16), dp(16), dp(16), dp(16));

            LinearLayout top = row();
            top.setGravity(Gravity.CENTER_VERTICAL);
            top.addView(skeletonBlock(40, 40, 12));
            LinearLayout lines = column();
            lines.setPadding(dp(12), 0, 0, 0);
            lines.addView(skeletonBlock(120, 12, 6));
            View l2 = skeletonBlock(70, 10, 5);
            ((LinearLayout.LayoutParams) l2.getLayoutParams()).topMargin = dp(8);
            lines.addView(l2);
            top.addView(lines);
            sk.addView(top);

            for (int w : new int[]{-1, -1, 180}) {
                View line = skeletonBlock(w, 12, 6);
                ((LinearLayout.LayoutParams) line.getLayoutParams()).topMargin = dp(12);
                sk.addView(line);
            }

            ObjectAnimator pulse = ObjectAnimator.ofFloat(sk, View.ALPHA, 1f, 0.45f);
            pulse.setDuration(700);
            pulse.setRepeatMode(ValueAnimator.REVERSE);
            pulse.setRepeatCount(ValueAnimator.INFINITE);
            pulse.start();
            skeletonAnimators.add(pulse);

            addView(list, sk, 0, 12);
        }
    }

    private View skeletonBlock(int width, int height, int radius) {
        View v = new View(this);
        v.setBackground(roundDrawable(SKELETON, radius));
        v.setLayoutParams(params(width, height));
        return v;
    }

    private void stopSkeletons() {
        for (ValueAnimator a : skeletonAnimators) a.cancel();
        skeletonAnimators.clear();
    }

    // ---------- empty state ----------

    private View emptyState(String title, String hint) {
        LinearLayout empty = card();
        empty.setGravity(Gravity.CENTER_HORIZONTAL);
        empty.setPadding(dp(24), dp(32), dp(24), dp(32));

        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.ic_empty);
        icon.setImageTintList(ColorStateList.valueOf(BLUE));
        icon.setBackground(roundDrawable(PALE, 32));
        icon.setPadding(dp(16), dp(16), dp(16), dp(16));
        empty.addView(icon, params(64, 64));

        TextView t = text(title, 15, DARK, true);
        t.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tp = params(-2, -2);
        tp.topMargin = dp(16);
        empty.addView(t, tp);

        TextView h = text(hint, 12, MUTED, false);
        h.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams hp = params(-2, -2);
        hp.topMargin = dp(4);
        empty.addView(h, hp);

        return empty;
    }

    // =========================
    // CHAT
    // =========================

    private static final String[] SUGGESTIONS = {
            "May pasok ba bukas sa LSPU?",
            "Ano ang official grading system?",
            "Paano maayos ang INC grade?",
            "TDP scholarship requirements",
            "Dress code rules & wash days",
            "Programs offered in CCS",
            "Emergency hotlines San Pablo",
            "Saan ang OSAS at Registrar?"
    };

    /** Kept across tab switches so the conversation isn't lost. */
    private final List<ChatMessage> chatHistory = new ArrayList<>();

    private LinearLayout messageList;
    private View suggestionBlock;
    private final List<ObjectAnimator> thinkingAnimators = new ArrayList<>();
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
        titleBlock.setPadding(dp(12), 0, 0, 0);
        titleBlock.addView(text("Kaleo Assistant", 15, DARK, true));
        TextView status = text(isOnline
                        ? "●  Answers from saved announcements"
                        : "●  Offline — answering from saved posts",
                11, isOnline ? SUCCESS : getColor(R.color.cat_exam), true);
        LinearLayout.LayoutParams sp = params(-2, -2);
        sp.topMargin = dp(4);
        titleBlock.addView(status, sp);
        header.addView(titleBlock, new LinearLayout.LayoutParams(0, -2, 1));

        // Sources button: shows which pages the assistant reads from
        TextView sourcesButton = text("Sources ▾", 12, BLUE, true);
        sourcesButton.setBackgroundResource(R.drawable.suggestion_pill);
        sourcesButton.setPadding(dp(12), dp(8), dp(12), dp(8));
        header.addView(sourcesButton);

        addView(contentContainer, header, 0, 12);

        LinearLayout sourcesPanel = buildSourcesPanel();
        sourcesPanel.setVisibility(View.GONE);
        addView(contentContainer, sourcesPanel, 0, 16);

        sourcesButton.setOnClickListener(v -> {
            boolean open = sourcesPanel.getVisibility() == View.VISIBLE;
            sourcesButton.setText(open ? "Sources ▾" : "Sources ▴");
            if (open) {
                sourcesPanel.setVisibility(View.GONE);
            } else {
                sourcesPanel.setVisibility(View.VISIBLE);
                animateIn(sourcesPanel, 0);
            }
        });

        // Messages
        messageList = column();
        contentContainer.addView(messageList, params(-1, -2));

        if (chatHistory.isEmpty()) {
            chatHistory.add(new ChatMessage(
                    "Hi! 👋 Ako si Kaleo. Itanong mo lang ang tungkol sa klase, exams, o scholarships — hahanapin ko sa mga official announcements.",
                    ChatMessage.Sender.BOT));
        }
        boolean userHasAsked = false;
        for (ChatMessage m : chatHistory) {
            messageList.addView(buildMessageView(m));
            if (m.isUser()) userHasAsked = true;
        }

        // Suggestion pills: only until the first question is asked
        suggestionBlock = null;
        if (!userHasAsked) {
            LinearLayout block = column();

            block.addView(text("TRY ASKING", 10, MUTED, true));

            HorizontalScrollView pillScroll = new HorizontalScrollView(this);
            pillScroll.setHorizontalScrollBarEnabled(false);
            LinearLayout pills = row();
            for (String s : SUGGESTIONS) {
                TextView pill = text(s, 13, BLUE, true);
                pill.setBackgroundResource(R.drawable.suggestion_pill);
                pill.setPadding(dp(16), dp(8), dp(16), dp(8));
                pill.setOnClickListener(v -> sendChatMessage(s));
                LinearLayout.LayoutParams pp = params(-2, -2);
                pp.rightMargin = dp(8);
                pills.addView(pill, pp);
            }
            pillScroll.addView(pills);
            LinearLayout.LayoutParams psp = params(-1, -2);
            psp.topMargin = dp(8);
            block.addView(pillScroll, psp);

            LinearLayout.LayoutParams bp = params(-1, -2);
            bp.topMargin = dp(8);
            contentContainer.addView(block, bp);
            suggestionBlock = block;
        }

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

        hideSuggestions();

        ChatMessage userMsg = new ChatMessage(question, ChatMessage.Sender.USER);
        chatHistory.add(userMsg);
        appendIfVisible(userMsg);

        ChatMessage thinking = ChatMessage.thinking();
        chatHistory.add(thinking);
        appendIfVisible(thinking);

        chatExecutor.execute(() -> {
            ChatMessage built;
            try {
                built = buildAnswer(question);
            } catch (Exception e) {
                // Without this, one crash left botBusy = true and the chat ignored
                // every message after it until the app was restarted.
                android.util.Log.e("Kaleo", "Chat answer failed", e);
                built = new ChatMessage(
                        "Pasensya, may error sa pagsagot. Pakisubukan ulit.",
                        ChatMessage.Sender.BOT);
            }
            final ChatMessage answer = built;

            // Small delay so the typing animation is visible
            uiHandler.postDelayed(() -> {
                if (isFinishing()) return;
                chatHistory.remove(thinking);
                chatHistory.add(answer);
                stopThinkingAnimation();
                if (messageList != null && messageList.isShown()) {
                    // Remove the thinking bubble (always the last child)
                    int last = messageList.getChildCount() - 1;
                    if (last >= 0) messageList.removeViewAt(last);
                    View v = buildMessageView(answer);
                    messageList.addView(v);
                    animateIn(v, 0);
                    scrollChatToBottom();
                }
                botBusy = false;
            }, 1200);
        });
    }

    private void hideSuggestions() {
        if (suggestionBlock == null) return;
        View block = suggestionBlock;
        suggestionBlock = null;
        block.animate().alpha(0f).setDuration(150)
                .withEndAction(() -> block.setVisibility(View.GONE));
    }

    private void appendIfVisible(ChatMessage m) {
        if (messageList != null && messageList.isShown()) {
            View v = buildMessageView(m);
            messageList.addView(v);
            animateIn(v, 0);
            scrollChatToBottom();
        }
    }

    /** Runs on the background executor: evaluates query using LocalNanoAIEngine Edge-First RAG with Cloud Fallback. */
    private ChatMessage buildAnswer(String question) {
        List<AnnouncementEntity> allPosts = visibleAnnouncements(announcementRepository.getAll());
        long start = System.nanoTime();
        LocalNanoAIEngine.NanoAIResult result =
                nanoAIEngine.processHybridQuery(question, allPosts, cloudAdapter);
        long latencyMs = (System.nanoTime() - start) / 1000000;

        List<ChatMessage.Source> sources = new ArrayList<>();
        if (result.primarySource != null) {
            sources.add(new ChatMessage.Source(
                    result.primarySource.pageName,
                    result.primarySource.message,
                    result.primarySource.permalink
            ));
        }

        String tag = result.isOfflineAnswer
                ? "⚡ [" + latencyMs + "ms • Local Nano AI]\n"
                : "☁️ [" + latencyMs + "ms • Cloud AI]\n";

        return new ChatMessage(tag + result.answer, ChatMessage.Sender.BOT, sources);
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
        wp.bottomMargin = dp(12);
        wrapper.setLayoutParams(wp);

        int maxWidth = (int) (getResources().getDisplayMetrics().widthPixels * 0.78f);

        if (m.isUser()) {
            TextView bubble = text(m.getText(), 14, WHITE, false);
            bubble.setBackgroundResource(R.drawable.chat_bubble_user);
            bubble.setPadding(dp(16), dp(12), dp(16), dp(12));
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
        bubble.setPadding(dp(16), dp(12), dp(16), dp(12));

        if (m.isThinking()) {
            bubble.addView(typingDots());
        } else {
            TextView body = text(MarkdownFormatter.format(m.getText()), 14, BODY, false);
            body.setLineSpacing(dp(2), 1f);
            body.setMaxWidth(maxWidth - dp(64));
            bubble.addView(body);

            if (m.hasSources()) {
                TextView label = text("SOURCES", 9, MUTED, true);
                LinearLayout.LayoutParams lp = params(-2, -2);
                lp.topMargin = dp(12);
                bubble.addView(label, lp);

                for (ChatMessage.Source s : m.getSources()) {
                    bubble.addView(buildSourceCard(s, maxWidth - dp(64)));
                }
            }
        }

        wrapper.addView(bubble, new LinearLayout.LayoutParams(-2, -2));
        return wrapper;
    }

    /** Three dots that bounce one after another, like a typing indicator. */
    private View typingDots() {
        stopThinkingAnimation();
        // Fixed-height row with the dots resting near the bottom, so there is
        // room above them to bounce without being clipped by the bubble.
        LinearLayout dots = row();
        dots.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
        dots.setPadding(dp(4), 0, dp(4), dp(4));
        dots.setClipChildren(false);
        dots.setClipToPadding(false);
        dots.setMinimumWidth(dp(44));

        for (int i = 0; i < 3; i++) {
            View dot = new View(this);
            dot.setBackground(roundDrawable(MUTED, 4));
            LinearLayout.LayoutParams p = params(8, 8);
            if (i > 0) p.leftMargin = dp(6);
            dots.addView(dot, p);

            // Bounce up 5dp and brighten, staggered so it reads as a wave
            ObjectAnimator bounce = ObjectAnimator.ofPropertyValuesHolder(dot,
                    android.animation.PropertyValuesHolder.ofFloat(
                            View.TRANSLATION_Y, 0f, -dp(5)),
                    android.animation.PropertyValuesHolder.ofFloat(
                            View.ALPHA, 0.4f, 1f));
            bounce.setDuration(350);
            bounce.setRepeatMode(ValueAnimator.REVERSE);
            bounce.setRepeatCount(ValueAnimator.INFINITE);
            bounce.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
            bounce.setStartDelay(i * 130L);
            dot.setAlpha(0.4f);
            bounce.start();
            thinkingAnimators.add(bounce);
        }

        // 20dp tall: 8dp dot + 4dp bottom padding + 8dp headroom for the bounce
        dots.setLayoutParams(new LinearLayout.LayoutParams(-2, dp(20)));
        return dots;
    }

    private void stopThinkingAnimation() {
        for (ObjectAnimator a : thinkingAnimators) a.cancel();
        thinkingAnimators.clear();
    }

    private View buildSourceCard(ChatMessage.Source s, int width) {
        LinearLayout card = row();
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setBackgroundResource(R.drawable.source_card_background);
        card.setPadding(dp(12), dp(8), dp(12), dp(8));

        TextView fb = text("f", 14, WHITE, true);
        fb.setGravity(Gravity.CENTER);
        fb.setBackground(roundDrawable(0xFF1877F2, 8));
        card.addView(fb, params(28, 28));

        LinearLayout info = column();
        info.setPadding(dp(8), 0, dp(8), 0);
        info.addView(text(s.getPageName(), 12, DARK, true));
        TextView snip = text(shorten(MarkdownFormatter.format(s.getSnippet()), 70), 11, MUTED, false);
        snip.setMaxLines(1);
        snip.setEllipsize(TextUtils.TruncateAt.END);
        info.addView(snip);
        card.addView(info, new LinearLayout.LayoutParams(0, -2, 1));

        card.addView(text("↗", 15, BLUE, true));
        card.setOnClickListener(v -> openUrl(s.getUrl()));

        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(width, -2);
        cp.topMargin = dp(8);
        card.setLayoutParams(cp);
        return card;
    }

    private void scrollChatToBottom() {
        contentScroll.post(() -> contentScroll.fullScroll(View.FOCUS_DOWN));
    }

    private CharSequence shorten(CharSequence s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        int end = max;
        while (end > 0 && Character.isWhitespace(s.charAt(end - 1))) {
            end--;
        }
        return new SpannableStringBuilder(s, 0, end).append("…");
    }

    // =========================
    // SOURCES (shown inside the AI chat)
    // =========================

    private static final String[][] SOURCES = {
            {"L", "LSPU Official", "University-wide announcements", "https://www.facebook.com/LSPUOfficial"},
            {"O", "SPCC OSAS", "Student affairs and services", "https://www.facebook.com/SPCC.OSAS"},
            {"C", "CCSOCTA", "College and organization updates", "https://www.facebook.com/CCSOCTA"}
    };
    private static final int[] SOURCE_TINTS = {
            R.color.soft_blue, R.color.soft_green, R.color.soft_orange
    };

    /** Collapsible panel under the chat header listing the pages the assistant reads. */
    private LinearLayout buildSourcesPanel() {
        LinearLayout panel = card();
        panel.setPadding(dp(16), dp(16), dp(16), dp(16));

        panel.addView(text("SOURCES THE ASSISTANT USES", 10, BLUE, true));

        Set<String> unfollowed = prefs.getStringSet(KEY_UNFOLLOWED, new HashSet<>());
        int following = SOURCES.length - countUnfollowed(unfollowed);

        TextView countText = text(followingText(following), 12, MUTED, false);
        LinearLayout.LayoutParams ctp = params(-1, -2);
        ctp.topMargin = dp(4);
        ctp.bottomMargin = dp(12);
        panel.addView(countText, ctp);

        for (int i = 0; i < SOURCES.length; i++) {
            addSourceCard(panel, SOURCES[i][0], SOURCES[i][1], SOURCES[i][2],
                    SOURCES[i][3], getColor(SOURCE_TINTS[i]), countText);
        }

        TextView note = text(
                "Posts are synced from these pages and saved on this device, so answers still work offline.",
                11, MUTED, false);
        LinearLayout.LayoutParams np = params(-1, -2);
        np.topMargin = dp(4);
        panel.addView(note, np);

        return panel;
    }

    private String followingText(int following) {
        return "Following " + following + " of " + SOURCES.length
                + ". Turn a source off to hide its posts from the feed, calendar and answers.";
    }

    private int countUnfollowed(Set<String> unfollowed) {
        int n = 0;
        for (String[] s : SOURCES) if (unfollowed.contains(s[1])) n++;
        return n;
    }

    private void addSourceCard(
            LinearLayout parent,
            String initial,
            String name,
            String description,
            String url,
            int tint,
            TextView countText) {

        LinearLayout source = column();
        source.setBackgroundResource(R.drawable.source_card_background);
        source.setPadding(dp(12), dp(12), dp(8), dp(12));

        LinearLayout line = row();
        line.setGravity(Gravity.CENTER_VERTICAL);

        TextView avatar = text(initial, 15, BLUE, true);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(roundDrawable(tint, 12));

        line.addView(avatar, params(40, 40));

        LinearLayout details = column();
        details.setPadding(dp(12), 0, dp(8), 0);

        details.addView(text(name, 14, DARK, true));

        TextView desc = text(
                description, 11, MUTED, false
        );

        LinearLayout.LayoutParams dcp = params(-1, -2);
        dcp.topMargin = dp(4);
        details.addView(desc, dcp);

        TextView openLink = text("View page ↗", 11, BLUE, true);
        LinearLayout.LayoutParams olp = params(-2, -2);
        olp.topMargin = dp(4);
        details.addView(openLink, olp);
        openLink.setOnClickListener(v -> openUrl(url));

        line.addView(details,
                new LinearLayout.LayoutParams(0, -2, 1));

        // Follow toggle
        Switch follow = new Switch(this);
        Set<String> unfollowed = prefs.getStringSet(KEY_UNFOLLOWED, new HashSet<>());
        follow.setChecked(!unfollowed.contains(name));
        follow.setContentDescription("Follow " + name);
        follow.setOnCheckedChangeListener((b, checked) -> {
            Set<String> set = new HashSet<>(
                    prefs.getStringSet(KEY_UNFOLLOWED, new HashSet<>()));
            if (checked) set.remove(name); else set.add(name);
            prefs.edit().putStringSet(KEY_UNFOLLOWED, set).apply();
            countText.setText(followingText(SOURCES.length - countUnfollowed(set)));
            source.animate().alpha(checked ? 1f : 0.6f).setDuration(150);
        });
        if (!follow.isChecked()) source.setAlpha(0.6f);
        line.addView(follow);

        source.addView(line);

        addView(parent, source, 0, 8);
    }

    // =========================
    // CALENDAR
    // =========================

    private static final String KEY_CAL_SYNC = "calendar_sync_enabled";
    private static final String KEY_CAL_EVENT_PREFIX = "cal_event_";
    private static final int REQ_CALENDAR = 42;

    /** A dated item from an announcement, plus the category the UI colors it by. */
    private static class CalendarItem extends CampusDates.DatedPost {
        Category category;
    }

    private static long localMillis(int y, int m, int d) {
        return CampusDates.localMillis(y, m, d);
    }

    private static long utcMillis(int y, int m, int d) {
        return CampusDates.utcMillis(y, m, d);
    }

    /** Date parsing lives in CampusDates (shared with the reminders). */
    private CalendarItem extractCalendarItem(AnnouncementEntity a) {
        CampusDates.DatedPost p = CampusDates.extract(a);
        if (p == null) return null;
        CalendarItem item = new CalendarItem();
        item.postId = p.postId;
        item.title = p.title;
        item.pageName = p.pageName;
        item.permalink = p.permalink;
        item.message = p.message;
        item.deadline = p.deadline;
        item.y = p.y; item.m = p.m; item.d = p.d;
        item.ey = p.ey; item.em = p.em; item.ed = p.ed;
        item.category = Category.of(a.message);
        return item;
    }

    /** Upcoming dated items from followed sources, soonest first. Runs on the executor. */
    private List<CalendarItem> loadUpcomingItems() {
        List<CalendarItem> items = new ArrayList<>();
        long today = localMillis(
                java.util.Calendar.getInstance().get(java.util.Calendar.YEAR),
                java.util.Calendar.getInstance().get(java.util.Calendar.MONTH),
                java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH));
        for (AnnouncementEntity a : visibleAnnouncements(announcementRepository.getAll())) {
            CalendarItem item = extractCalendarItem(a);
            if (item != null && item.endMillisExclusive() > today) items.add(item);
        }
        items.sort((x, y) -> Long.compare(x.startMillis(), y.startMillis()));
        return items;
    }

    private Runnable countdownTicker;

    private void showCalendar() {
        setActiveTab(2);
        contentContainer.removeAllViews();

        addText("YOUR SCHEDULE", 11, BLUE, true, 0, 8);
        addText("Upcoming dates.", 26, DARK, true, 0, 4);
        addText("Exams, events and deadlines found in your campus announcements.",
                13, MUTED, false, 0, 16);

        LinearLayout countdownHolder = column();
        contentContainer.addView(countdownHolder, params(-1, -2));

        // Phone calendar sync toggle
        LinearLayout syncRow = card();
        syncRow.setOrientation(LinearLayout.HORIZONTAL);
        syncRow.setGravity(Gravity.CENTER_VERTICAL);
        syncRow.setPadding(dp(16), dp(12), dp(12), dp(12));

        TextView calIcon = text("📅", 18, DARK, false);
        calIcon.setGravity(Gravity.CENTER);
        calIcon.setBackground(roundDrawable(PALE, 12));
        syncRow.addView(calIcon, params(40, 40));

        LinearLayout syncText = column();
        syncText.setPadding(dp(12), 0, dp(8), 0);
        syncText.addView(text("Sync to phone calendar", 14, DARK, true));
        TextView syncSub = text(calendarSyncEnabled()
                        ? "On • new dates are added automatically"
                        : "Adds these dates to Google Calendar",
                11, MUTED, false);
        LinearLayout.LayoutParams ssp = params(-2, -2);
        ssp.topMargin = dp(4);
        syncText.addView(syncSub, ssp);
        syncRow.addView(syncText, new LinearLayout.LayoutParams(0, -2, 1));

        Switch syncSwitch = new Switch(this);
        syncSwitch.setChecked(calendarSyncEnabled());
        syncSwitch.setContentDescription("Sync to phone calendar");
        syncSwitch.setOnCheckedChangeListener((b, checked) -> {
            if (checked && !hasCalendarPermission()) {
                b.setChecked(false);
                requestPermissions(new String[]{
                        android.Manifest.permission.READ_CALENDAR,
                        android.Manifest.permission.WRITE_CALENDAR}, REQ_CALENDAR);
                return;
            }
            prefs.edit().putBoolean(KEY_CAL_SYNC, checked).apply();
            if (checked) syncAllToPhoneCalendar(true);
            else showCalendar();
        });
        syncRow.addView(syncSwitch);
        addView(contentContainer, syncRow, 0, 20);

        TextView listLabel = text("Coming up", 18, DARK, true);
        LinearLayout.LayoutParams llp = params(-1, -2);
        llp.bottomMargin = dp(12);
        contentContainer.addView(listLabel, llp);

        LinearLayout list = column();
        contentContainer.addView(list, params(-1, -2));
        showSkeletons(list);

        executor.execute(() -> {
            List<CalendarItem> items = loadUpcomingItems();
            Set<String> synced = syncedPostIds(items);

            uiHandler.postDelayed(() -> {
                if (isFinishing() || currentTab != 2) return;
                stopSkeletons();
                list.removeAllViews();

                renderCountdown(countdownHolder, items);

                if (items.isEmpty()) {
                    list.addView(emptyState("No upcoming dates yet",
                            "When an announcement mentions an exam, event or deadline, it shows up here."));
                    return;
                }
                int i = 0;
                for (CalendarItem item : items) {
                    View row = buildCalendarRow(item, synced.contains(item.postId));
                    addView(list, row, 0, 12);
                    animateIn(row, i++ * 40L);
                }
            }, 200);
        });
    }

    // ---------- exam countdown ----------

    private void renderCountdown(LinearLayout holder, List<CalendarItem> items) {
        stopCountdown();
        holder.removeAllViews();

        CalendarItem exam = null;
        for (CalendarItem it : items) {
            if (it.category == Category.EXAM) { exam = it; break; }
        }

        LinearLayout card = column();
        GradientDrawable bg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF4A6FF0, 0xFF1E3790});
        bg.setCornerRadius(dp(20));
        card.setBackground(bg);
        card.setPadding(dp(20), dp(16), dp(20), dp(20));

        card.addView(text("EXAM COUNTDOWN", 10, 0xFFDCE4FF, true));

        if (exam == null) {
            TextView none = text("No upcoming exams found 🎉", 18, WHITE, true);
            LinearLayout.LayoutParams p = params(-1, -2);
            p.topMargin = dp(8);
            card.addView(none, p);
            card.addView(text("We'll count down as soon as an exam schedule is posted.",
                    12, 0xFFE5EAFF, false));
            addView(holder, card, 0, 16);
            return;
        }

        TextView title = text(exam.title, 16, WHITE, true);
        title.setMaxLines(2);
        title.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams tp = params(-1, -2);
        tp.topMargin = dp(8);
        card.addView(title, tp);

        TextView when = text(dateLabel(exam) + "  •  " + exam.pageName, 12, 0xFFE5EAFF, false);
        LinearLayout.LayoutParams wp = params(-1, -2);
        wp.topMargin = dp(4);
        card.addView(when, wp);

        LinearLayout boxes = row();
        LinearLayout.LayoutParams bp = params(-1, -2);
        bp.topMargin = dp(16);
        card.addView(boxes, bp);

        TextView[] values = new TextView[3];
        String[] units = {"DAYS", "HOURS", "MINS"};
        for (int i = 0; i < 3; i++) {
            LinearLayout box = column();
            box.setGravity(Gravity.CENTER_HORIZONTAL);
            box.setBackground(roundDrawable(0x26FFFFFF, 14));
            box.setPadding(0, dp(12), 0, dp(12));
            values[i] = text("0", 26, WHITE, true);
            values[i].setGravity(Gravity.CENTER);
            box.addView(values[i]);
            TextView unit = text(units[i], 10, 0xFFDCE4FF, true);
            unit.setGravity(Gravity.CENTER);
            box.addView(unit);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1);
            if (i > 0) lp.leftMargin = dp(8);
            boxes.addView(box, lp);
        }

        TextView ongoing = text("", 13, WHITE, true);
        LinearLayout.LayoutParams op = params(-1, -2);
        op.topMargin = dp(12);
        card.addView(ongoing, op);

        final CalendarItem target = exam;
        card.setOnClickListener(v -> openUrl(target.permalink));

        countdownTicker = new Runnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                long left = target.startMillis() - now;
                if (left <= 0) {
                    boxes.setVisibility(View.GONE);
                    ongoing.setText("Exams are ongoing — good luck! 🍀");
                    return;
                }
                values[0].setText(String.valueOf(left / DateUtils.DAY_IN_MILLIS));
                values[1].setText(String.valueOf((left / DateUtils.HOUR_IN_MILLIS) % 24));
                values[2].setText(String.valueOf((left / DateUtils.MINUTE_IN_MILLIS) % 60));
                ongoing.setText(left < 3 * DateUtils.DAY_IN_MILLIS
                        ? "Almost there — time for a final review! 📚" : "");
                ongoing.setVisibility(ongoing.getText().length() == 0 ? View.GONE : View.VISIBLE);
                uiHandler.postDelayed(this, 30_000);
            }
        };
        countdownTicker.run();

        addView(holder, card, 0, 16);
        animateIn(card, 0);
    }

    private void stopCountdown() {
        if (countdownTicker != null) {
            uiHandler.removeCallbacks(countdownTicker);
            countdownTicker = null;
        }
    }

    // ---------- calendar rows ----------

    private View buildCalendarRow(CalendarItem item, boolean inPhoneCalendar) {
        LinearLayout card = card();
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(12), dp(12), dp(12), dp(12));

        int fg = getColor(item.category.fgRes);
        int bgc = getColor(item.category.bgRes);

        // Date block
        LinearLayout date = column();
        date.setGravity(Gravity.CENTER);
        date.setBackground(roundDrawable(bgc, 12));
        String[] mon = {"JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"};
        TextView monthText = text(mon[item.m], 10, fg, true);
        monthText.setGravity(Gravity.CENTER);
        date.addView(monthText);
        TextView dayText = text(String.valueOf(item.d), 20, fg, true);
        dayText.setGravity(Gravity.CENTER);
        dayText.setIncludeFontPadding(false);
        date.addView(dayText);
        card.addView(date, params(56, 56));

        LinearLayout info = column();
        info.setPadding(dp(12), 0, dp(8), 0);

        TextView title = text(item.title, 14, DARK, true);
        title.setMaxLines(2);
        title.setEllipsize(TextUtils.TruncateAt.END);
        info.addView(title);

        TextView sub = text(dateLabel(item) + "  •  " + untilLabel(item), 11, MUTED, false);
        LinearLayout.LayoutParams sp = params(-2, -2);
        sp.topMargin = dp(4);
        info.addView(sub, sp);

        TextView tag = text(item.category.tagLabel.toUpperCase(Locale.ROOT) + "  •  " + item.pageName,
                9, fg, true);
        tag.setLetterSpacing(0.04f);
        LinearLayout.LayoutParams tgp = params(-2, -2);
        tgp.topMargin = dp(4);
        info.addView(tag, tgp);

        card.addView(info, new LinearLayout.LayoutParams(0, -2, 1));

        // Add to calendar
        TextView add = text(inPhoneCalendar ? "✓" : "＋", 16,
                inPhoneCalendar ? SUCCESS : BLUE, true);
        add.setGravity(Gravity.CENTER);
        add.setBackground(roundDrawable(inPhoneCalendar ? getColor(R.color.soft_green) : PALE, 20));
        add.setContentDescription(inPhoneCalendar ? "In your calendar" : "Add to calendar");
        add.setOnClickListener(v -> {
            if (hasCalendarPermission()) {
                executor.execute(() -> {
                    boolean ok = upsertPhoneEvent(item);
                    runOnUiThread(() -> {
                        toast(ok ? "Added to your calendar" : "Couldn't find a calendar on this phone");
                        if (ok) {
                            add.setText("✓");
                            add.setTextColor(SUCCESS);
                            add.setBackground(roundDrawable(getColor(R.color.soft_green), 20));
                        }
                    });
                });
            } else {
                openCalendarInsert(item);   // works without permission
            }
        });
        card.addView(add, params(40, 40));

        card.setOnClickListener(v -> openUrl(item.permalink));
        return card;
    }

    private String dateLabel(CalendarItem item) {
        String[] mon = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        String start = mon[item.m] + " " + item.d;
        if (!item.multiDay()) return start;
        return start + " – " + (item.em == item.m ? "" : mon[item.em] + " ") + item.ed;
    }

    private String untilLabel(CalendarItem item) {
        long today = localMillis(
                java.util.Calendar.getInstance().get(java.util.Calendar.YEAR),
                java.util.Calendar.getInstance().get(java.util.Calendar.MONTH),
                java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH));
        long days = Math.round((item.startMillis() - today) / (double) DateUtils.DAY_IN_MILLIS);
        if (days < 0) return "Ongoing";
        if (days == 0) return "Today";
        if (days == 1) return "Tomorrow";
        return "in " + days + " days";
    }

    // ---------- phone calendar sync ----------

    private boolean calendarSyncEnabled() {
        return prefs.getBoolean(KEY_CAL_SYNC, false) && hasCalendarPermission();
    }

    private boolean hasCalendarPermission() {
        return checkSelfPermission(android.Manifest.permission.WRITE_CALENDAR)
                == android.content.pm.PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(android.Manifest.permission.READ_CALENDAR)
                == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == REQ_NOTIFICATIONS) {
            if (KaleoNotifications.hasPermission(this)) {
                toast("Notifications on — you'll be alerted about new announcements");
            }
            return;
        }
        if (requestCode != REQ_CALENDAR) return;
        if (hasCalendarPermission()) {
            prefs.edit().putBoolean(KEY_CAL_SYNC, true).apply();
            syncAllToPhoneCalendar(true);
        } else {
            toast("Calendar permission is needed to sync. You can still add dates one by one.");
            if (currentTab == 2) showCalendar();
        }
    }

    /** Adds/updates every upcoming item in the phone calendar. Safe to call repeatedly. */
    private void syncAllToPhoneCalendar(boolean showResult) {
        if (!calendarSyncEnabled()) return;
        executor.execute(() -> {
            int added = 0;
            for (CalendarItem item : loadUpcomingItems()) {
                boolean isNew = !prefs.contains(KEY_CAL_EVENT_PREFIX + item.postId);
                if (upsertPhoneEvent(item) && isNew) added++;
            }
            final int count = added;
            runOnUiThread(() -> {
                if (showResult) {
                    toast(count == 0 ? "Calendar is up to date"
                            : "Added " + count + (count == 1 ? " date" : " dates") + " to your calendar");
                }
                if (currentTab == 2) showCalendar();
            });
        });
    }

    /** Inserts the item as an all-day event, or updates the one we inserted before. */
    private boolean upsertPhoneEvent(CalendarItem item) {
        try {
            long calendarId = findWritableCalendarId();
            if (calendarId < 0) return false;

            android.content.ContentValues v = new android.content.ContentValues();
            v.put(android.provider.CalendarContract.Events.CALENDAR_ID, calendarId);
            v.put(android.provider.CalendarContract.Events.TITLE, item.title);
            v.put(android.provider.CalendarContract.Events.DESCRIPTION,
                    item.message + "\n\nSource: " + item.pageName
                            + (item.permalink != null ? "\n" + item.permalink : "")
                            + "\n\nAdded by Kaleo");
            v.put(android.provider.CalendarContract.Events.ALL_DAY, 1);
            v.put(android.provider.CalendarContract.Events.EVENT_TIMEZONE, "UTC");
            v.put(android.provider.CalendarContract.Events.DTSTART, utcMillis(item.y, item.m, item.d));
            v.put(android.provider.CalendarContract.Events.DTEND,
                    utcMillis(item.ey, item.em, item.ed) + DateUtils.DAY_IN_MILLIS);

            String key = KEY_CAL_EVENT_PREFIX + item.postId;
            long existing = prefs.getLong(key, -1);
            if (existing >= 0 && phoneEventExists(existing)) {
                getContentResolver().update(
                        android.content.ContentUris.withAppendedId(
                                android.provider.CalendarContract.Events.CONTENT_URI, existing),
                        v, null, null);
                return true;
            }

            Uri uri = getContentResolver().insert(
                    android.provider.CalendarContract.Events.CONTENT_URI, v);
            if (uri == null) return false;
            prefs.edit().putLong(key, android.content.ContentUris.parseId(uri)).apply();
            return true;
        } catch (SecurityException | IllegalArgumentException e) {
            return false;
        }
    }

    private boolean phoneEventExists(long eventId) {
        try (android.database.Cursor c = getContentResolver().query(
                android.provider.CalendarContract.Events.CONTENT_URI,
                new String[]{android.provider.CalendarContract.Events._ID},
                android.provider.CalendarContract.Events._ID + "=? AND "
                        + android.provider.CalendarContract.Events.DELETED + "=0",
                new String[]{String.valueOf(eventId)}, null)) {
            return c != null && c.moveToFirst();
        } catch (SecurityException e) {
            return false;
        }
    }

    /** The phone's primary calendar (usually the Google account), or any writable one. */
    private long findWritableCalendarId() {
        String[] proj = {
                android.provider.CalendarContract.Calendars._ID,
                android.provider.CalendarContract.Calendars.IS_PRIMARY
        };
        String sel = android.provider.CalendarContract.Calendars.VISIBLE + "=1 AND "
                + android.provider.CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL + ">="
                + android.provider.CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR;
        long fallback = -1;
        try (android.database.Cursor c = getContentResolver().query(
                android.provider.CalendarContract.Calendars.CONTENT_URI, proj, sel, null, null)) {
            if (c == null) return -1;
            while (c.moveToNext()) {
                long id = c.getLong(0);
                if (c.getInt(1) == 1) return id;
                if (fallback < 0) fallback = id;
            }
        } catch (SecurityException e) {
            return -1;
        }
        return fallback;
    }

    /** Which of these items we've already put in the phone calendar. Runs on the executor. */
    private Set<String> syncedPostIds(List<CalendarItem> items) {
        Set<String> out = new HashSet<>();
        if (!hasCalendarPermission()) return out;
        for (CalendarItem it : items) {
            long id = prefs.getLong(KEY_CAL_EVENT_PREFIX + it.postId, -1);
            if (id >= 0 && phoneEventExists(id)) out.add(it.postId);
        }
        return out;
    }

    /** Opens the calendar app with the event pre-filled. Needs no permission. */
    private void openCalendarInsert(CalendarItem item) {
        try {
            Intent i = new Intent(Intent.ACTION_INSERT)
                    .setData(android.provider.CalendarContract.Events.CONTENT_URI)
                    .putExtra(android.provider.CalendarContract.Events.TITLE, item.title)
                    .putExtra(android.provider.CalendarContract.Events.DESCRIPTION,
                            item.message + "\n\nSource: " + item.pageName
                                    + (item.permalink != null ? "\n" + item.permalink : ""))
                    .putExtra(android.provider.CalendarContract.EXTRA_EVENT_ALL_DAY, true)
                    .putExtra(android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME, item.startMillis())
                    .putExtra(android.provider.CalendarContract.EXTRA_EVENT_END_TIME,
                            item.endMillisExclusive() - 1);
            startActivity(i);
        } catch (Exception e) {
            toast("No calendar app found on this phone");
        }
    }

    private void toast(String msg) {
        android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show();
    }

    // =========================
    // OFFLINE INDICATOR
    // =========================

    private TextView offlineBanner;
    private boolean isOnline = true;
    private android.net.ConnectivityManager connectivity;
    private android.net.ConnectivityManager.NetworkCallback networkCallback;

    private void startNetworkWatch() {
        offlineBanner = findViewById(R.id.offlineBanner);
        connectivity = getSystemService(android.net.ConnectivityManager.class);
        if (connectivity == null) return;

        isOnline = checkOnline();
        if (!isOnline) showOfflineBanner();

        networkCallback = new android.net.ConnectivityManager.NetworkCallback() {
            @Override
            public void onCapabilitiesChanged(android.net.Network network,
                                              android.net.NetworkCapabilities caps) {
                boolean ok = caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        && caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED);
                runOnUiThread(() -> setOnline(ok));
            }

            @Override
            public void onLost(android.net.Network network) {
                runOnUiThread(() -> setOnline(checkOnline()));
            }
        };
        try {
            connectivity.registerDefaultNetworkCallback(networkCallback);
        } catch (SecurityException e) {
            networkCallback = null;
        }
    }

    private boolean checkOnline() {
        if (connectivity == null) return true;
        android.net.Network n = connectivity.getActiveNetwork();
        if (n == null) return false;
        android.net.NetworkCapabilities caps = connectivity.getNetworkCapabilities(n);
        return caps != null
                && caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }

    private void setOnline(boolean online) {
        if (isFinishing() || online == isOnline) return;
        isOnline = online;
        updateStatus();

        if (!online) {
            showOfflineBanner();
            return;
        }

        // Back online: say so briefly, then sync automatically
        offlineBanner.setText("✓  Back online — syncing new posts…");
        offlineBanner.setTextColor(getColor(R.color.status_text));
        offlineBanner.setBackgroundColor(getColor(R.color.soft_green));
        SyncScheduler.syncNow(getApplicationContext());
        watchSync();
        uiHandler.postDelayed(() -> {
            if (isOnline && offlineBanner != null) {
                offlineBanner.animate().alpha(0f).setDuration(200)
                        .withEndAction(() -> offlineBanner.setVisibility(View.GONE));
            }
        }, 2500);
    }

    private void showOfflineBanner() {
        if (offlineBanner == null) return;
        offlineBanner.setText("⚡  You're offline — showing posts saved on this device");
        offlineBanner.setTextColor(getColor(R.color.cat_exam));
        offlineBanner.setBackgroundColor(getColor(R.color.soft_orange));
        offlineBanner.animate().cancel();
        offlineBanner.setAlpha(0f);
        offlineBanner.setVisibility(View.VISIBLE);
        offlineBanner.animate().alpha(1f).setDuration(200);
    }

    // =========================
    // NOTIFICATIONS
    // =========================

    private static final int REQ_NOTIFICATIONS = 43;
    private static final String KEY_NOTIF_ASKED = "notif_permission_asked";

    /** Asks once (Android 13+) right after the first launch. */
    private void maybeAskNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT < 33) return;
        if (KaleoNotifications.hasPermission(this)) return;
        if (prefs.getBoolean(KEY_NOTIF_ASKED, false)) return;
        prefs.edit().putBoolean(KEY_NOTIF_ASKED, true).apply();
        requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                REQ_NOTIFICATIONS);
    }

    /** Bell in the header: notification settings + a test button. */
    private void showNotificationSettings() {
        LinearLayout box = column();
        box.setPadding(dp(20), dp(20), dp(20), dp(8));

        box.addView(text("Notifications", 18, DARK, true));
        TextView sub = text("Get alerted when new campus announcements are synced.",
                12, MUTED, false);
        LinearLayout.LayoutParams sp = params(-1, -2);
        sp.topMargin = dp(4);
        sp.bottomMargin = dp(16);
        box.addView(sub, sp);

        boolean allowed = KaleoNotifications.hasPermission(this);

        if (!allowed) {
            TextView blocked = text(
                    "⚠  Notifications are turned off for Kaleo. Tap here to allow them.",
                    12, getColor(R.color.cat_exam), true);
            blocked.setBackground(roundDrawable(getColor(R.color.soft_orange), 12));
            blocked.setPadding(dp(12), dp(12), dp(12), dp(12));
            blocked.setOnClickListener(v -> requestNotificationsOrOpenSettings());
            LinearLayout.LayoutParams bp = params(-1, -2);
            bp.bottomMargin = dp(16);
            box.addView(blocked, bp);
        }

        box.addView(notificationSwitchRow(
                "Urgent alerts",
                "Class suspensions and high-urgency posts, with sound",
                KaleoNotifications.KEY_URGENT_ENABLED));
        box.addView(notificationSwitchRow(
                "All new announcements",
                "Exams, events, scholarships and other updates",
                KaleoNotifications.KEY_ALL_ENABLED));
        box.addView(notificationSwitchRow(
                "Exam & deadline reminders",
                "The day before (and morning of) dates on your Calendar tab",
                KaleoReminders.KEY_ENABLED));

        TextView note = text(
                "Sources you turned off in Ask AI → Sources won't send notifications.",
                11, MUTED, false);
        LinearLayout.LayoutParams np = params(-1, -2);
        np.topMargin = dp(8);
        box.addView(note, np);

        TextView test = text("Send a test notification", 13, BLUE, true);
        test.setGravity(Gravity.CENTER);
        test.setBackgroundResource(R.drawable.suggestion_pill);
        test.setPadding(dp(16), dp(12), dp(16), dp(12));
        test.setOnClickListener(v -> {
            if (!KaleoNotifications.hasPermission(this)) {
                requestNotificationsOrOpenSettings();
                return;
            }
            KaleoNotifications.sendTest(getApplicationContext());
            toast("Test sent — check your notification shade");
        });
        LinearLayout.LayoutParams tp = params(-1, -2);
        tp.topMargin = dp(16);
        box.addView(test, tp);

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setView(box)
                .setPositiveButton("Done", null)
                .create();
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(roundDrawable(CARD, 20));
        }
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setTextColor(BLUE);
    }

    private View notificationSwitchRow(String title, String desc, String key) {
        LinearLayout row = row();
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(8), 0, dp(8));

        LinearLayout texts = column();
        texts.addView(text(title, 14, DARK, true));
        TextView d = text(desc, 11, MUTED, false);
        LinearLayout.LayoutParams dp4 = params(-1, -2);
        dp4.topMargin = dp(4);
        texts.addView(d, dp4);
        row.addView(texts, new LinearLayout.LayoutParams(0, -2, 1));

        Switch sw = new Switch(this);
        sw.setChecked(prefs.getBoolean(key, true));
        sw.setContentDescription(title);
        sw.setOnCheckedChangeListener((b, checked) -> {
            prefs.edit().putBoolean(key, checked).apply();
            if (checked && !KaleoNotifications.hasPermission(this)) {
                requestNotificationsOrOpenSettings();
            }
        });
        row.addView(sw);
        return row;
    }

    /** Shows the system prompt if Android still allows it, otherwise opens app settings. */
    private void requestNotificationsOrOpenSettings() {
        if (android.os.Build.VERSION.SDK_INT >= 33
                && (!prefs.getBoolean(KEY_NOTIF_ASKED, false)
                || shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS))) {
            prefs.edit().putBoolean(KEY_NOTIF_ASKED, true).apply();
            requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                    REQ_NOTIFICATIONS);
            return;
        }
        try {
            startActivity(new Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, getPackageName()));
        } catch (Exception e) {
            toast("Open Settings → Apps → Kaleo → Notifications");
        }
    }

    // =========================
    // REUSABLE UI HELPERS
    // =========================

    /** Fade + slide up, used for new cards and chat bubbles. */
    private void animateIn(View v, long delay) {
        v.setAlpha(0f);
        v.setTranslationY(dp(12));
        v.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(delay)
                .setDuration(200)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private String relativeTime(long millis) {
        return DateUtils.getRelativeTimeSpanString(
                millis,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE
        ).toString();
    }

    private LinearLayout card() {
        LinearLayout layout = column();

        layout.setBackground(
                roundDrawable(CARD, 16, BORDER)
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
            CharSequence value, int size, int color, boolean bold) {

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
        if (syncWork != null && syncObserver != null) syncWork.removeObserver(syncObserver);
        stopThinkingAnimation();
        stopCountdown();
        if (connectivity != null && networkCallback != null) {
            try { connectivity.unregisterNetworkCallback(networkCallback); } catch (Exception ignored) { }
        }
        stopSkeletons();
        uiHandler.removeCallbacksAndMessages(null);
        executor.shutdown();
        chatExecutor.shutdown();
    }
}
