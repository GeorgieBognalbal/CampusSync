package com.example.campussync.data.model;

public class ChatMessage {

    public enum Sender {
        USER,
        BOT
    }

    private final String text;
    private final Sender sender;
    private final long timestamp;

    public ChatMessage(String text, Sender sender) {
        this.text = text;
        this.sender = sender;
        this.timestamp = System.currentTimeMillis();
    }

    public String getText() {
        return text;
    }

    public Sender getSender() {
        return sender;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
