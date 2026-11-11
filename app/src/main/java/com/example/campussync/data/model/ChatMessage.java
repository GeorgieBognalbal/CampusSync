package com.example.campussync.data.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChatMessage {

    public enum Sender {
        USER,
        BOT
    }

    /** TEXT = normal bubble, THINKING = temporary "Thinking..." placeholder. */
    public enum Type {
        TEXT,
        THINKING
    }

    /** A cited announcement shown as a card under a bot answer. */
    public static class Source {
        private final String pageName;
        private final String snippet;
        private final String url;

        public Source(String pageName, String snippet, String url) {
            this.pageName = pageName;
            this.snippet = snippet;
            this.url = url;
        }

        public String getPageName() { return pageName; }
        public String getSnippet() { return snippet; }
        public String getUrl() { return url; }
    }

    private final String text;
    private final Sender sender;
    private final Type type;
    private final long timestamp;
    private final List<Source> sources;

    public ChatMessage(String text, Sender sender) {
        this(text, sender, Type.TEXT, null);
    }

    public ChatMessage(String text, Sender sender, List<Source> sources) {
        this(text, sender, Type.TEXT, sources);
    }

    private ChatMessage(String text, Sender sender, Type type, List<Source> sources) {
        this.text = text;
        this.sender = sender;
        this.type = type;
        this.timestamp = System.currentTimeMillis();
        this.sources = sources == null
                ? Collections.emptyList()
                : new ArrayList<>(sources);
    }

    public static ChatMessage thinking() {
        return new ChatMessage("", Sender.BOT, Type.THINKING, null);
    }

    public String getText() { return text; }
    public Sender getSender() { return sender; }
    public Type getType() { return type; }
    public long getTimestamp() { return timestamp; }
    public List<Source> getSources() { return sources; }

    public boolean isUser() { return sender == Sender.USER; }
    public boolean isThinking() { return type == Type.THINKING; }
    public boolean hasSources() { return !sources.isEmpty(); }
}
