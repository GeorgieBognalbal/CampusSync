package com.example.campussync.util;

import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;

import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight Markdown-to-Spannable formatter for chat responses and announcements.
 * Supports bold (**...**), italic (*...*), bold-italic (***...***), inline code (`...`),
 * Markdown headings (# ...), and asterisk bullet items (* ...).
 */
public final class MarkdownFormatter {

    public enum Style {
        BOLD,
        ITALIC,
        BOLD_ITALIC,
        MONOSPACE
    }

    public static final class SpanRange {
        public final int start;
        public final int end;
        public final Style style;

        public SpanRange(int start, int end, Style style) {
            this.start = start;
            this.end = end;
            this.style = style;
        }
    }

    public static final class ParsedText {
        public final String text;
        public final List<SpanRange> spans;

        public ParsedText(String text, List<SpanRange> spans) {
            this.text = text;
            this.spans = spans;
        }
    }

    private MarkdownFormatter() {
    }

    public static CharSequence format(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        ParsedText parsed = parse(input);
        SpannableStringBuilder ssb = new SpannableStringBuilder(parsed.text);
        for (SpanRange range : parsed.spans) {
            if (range.start >= range.end || range.end > ssb.length()) {
                continue;
            }
            Object span = toAndroidSpan(range.style);
            if (span != null) {
                ssb.setSpan(span, range.start, range.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        return ssb;
    }

    private static Object toAndroidSpan(Style style) {
        switch (style) {
            case BOLD:
                return new StyleSpan(Typeface.BOLD);
            case ITALIC:
                return new StyleSpan(Typeface.ITALIC);
            case BOLD_ITALIC:
                return new StyleSpan(Typeface.BOLD_ITALIC);
            case MONOSPACE:
                return new TypefaceSpan("monospace");
            default:
                return null;
        }
    }

    public static ParsedText parse(String input) {
        if (input == null || input.isEmpty()) {
            return new ParsedText("", new ArrayList<>());
        }

        StringBuilder out = new StringBuilder(input.length());
        List<SpanRange> spans = new ArrayList<>();

        String[] lines = input.split("\n", -1);
        for (int lineIdx = 0; lineIdx < lines.length; lineIdx++) {
            if (lineIdx > 0) {
                out.append('\n');
            }
            String line = lines[lineIdx];
            parseLine(line, out, spans);
        }

        return new ParsedText(out.toString(), spans);
    }

    private static void parseLine(String line, StringBuilder out, List<SpanRange> spans) {
        if (line.isEmpty()) {
            return;
        }

        // Convert Markdown headings (# Heading) into bold lines.
        int headingLevel = countLeadingHashes(line);
        if (headingLevel > 0 && headingLevel < line.length() && line.charAt(headingLevel) == ' ') {
            String headingContent = line.substring(headingLevel + 1).trim();
            int start = out.length();
            parseInline(headingContent, out, spans);
            int end = out.length();
            if (end > start) {
                spans.add(new SpanRange(start, end, Style.BOLD));
            }
            return;
        }

        // Convert Markdown bullet prefixes ("* item" or "  * item") to "  • item"
        // so leading bullet asterisks are not confused with italic markers.
        int indent = 0;
        while (indent < line.length() && line.charAt(indent) == ' ') {
            indent++;
        }
        if (indent + 1 < line.length()
                && line.charAt(indent) == '*'
                && line.charAt(indent + 1) == ' ') {
            out.append(line, 0, indent).append("• ");
            parseInline(line.substring(indent + 2), out, spans);
            return;
        }

        parseInline(line, out, spans);
    }

    private static int countLeadingHashes(String line) {
        int count = 0;
        while (count < line.length() && count < 6 && line.charAt(count) == '#') {
            count++;
        }
        return count;
    }

    private static void parseInline(String text, StringBuilder out, List<SpanRange> spans) {
        int i = 0;
        int len = text.length();

        while (i < len) {
            if (text.startsWith("****", i)) {
                while (i < len && text.charAt(i) == '*') {
                    out.append('*');
                    i++;
                }
                continue;
            }

            if (text.startsWith("***", i)) {
                if (i + 3 < len && !Character.isWhitespace(text.charAt(i + 3))) {
                    int end = findClosingTripleStar(text, i + 3);
                    if (end != -1) {
                        int start = out.length();
                        parseInline(text.substring(i + 3, end), out, spans);
                        if (out.length() > start) {
                            spans.add(new SpanRange(start, out.length(), Style.BOLD_ITALIC));
                        }
                        i = end + 3;
                        continue;
                    }
                }
            }

            if (text.startsWith("**", i)) {
                if (i + 2 < len && !Character.isWhitespace(text.charAt(i + 2))) {
                    int end = findClosingDoubleStar(text, i + 2);
                    if (end != -1) {
                        int start = out.length();
                        parseInline(text.substring(i + 2, end), out, spans);
                        if (out.length() > start) {
                            spans.add(new SpanRange(start, out.length(), Style.BOLD));
                        }
                        i = end + 2;
                        continue;
                    }
                }
                out.append("**");
                i += 2;
                continue;
            }

            char c = text.charAt(i);
            if (c == '`') {
                int end = text.indexOf('`', i + 1);
                if (end > i + 1) {
                    int start = out.length();
                    out.append(text, i + 1, end);
                    spans.add(new SpanRange(start, out.length(), Style.MONOSPACE));
                    i = end + 1;
                    continue;
                }
            }

            if (c == '*' && i + 1 < len && !Character.isWhitespace(text.charAt(i + 1))) {
                int end = findClosingSingleStar(text, i + 1);
                if (end != -1) {
                    int start = out.length();
                    parseInline(text.substring(i + 1, end), out, spans);
                    if (out.length() > start) {
                        spans.add(new SpanRange(start, out.length(), Style.ITALIC));
                    }
                    i = end + 1;
                    continue;
                }
            }

            out.append(c);
            i++;
        }
    }

    private static int findClosingTripleStar(String text, int fromIndex) {
        int idx = fromIndex;
        while (idx < text.length()) {
            int found = text.indexOf("***", idx);
            if (found == -1) {
                return -1;
            }
            if (found > fromIndex
                    && !Character.isWhitespace(text.charAt(found - 1))
                    && !text.substring(fromIndex, found).trim().isEmpty()) {
                return found;
            }
            idx = found + 1;
        }
        return -1;
    }

    private static int findClosingDoubleStar(String text, int fromIndex) {
        int idx = fromIndex;
        while (idx < text.length()) {
            int found = text.indexOf("**", idx);
            if (found == -1) {
                return -1;
            }
            // If closing "**" is part of "***" and there is an unpaired '*' inside,
            // shift to the last two '*' so the inner "*italic*" can close first.
            boolean shifted = false;
            if (found + 2 < text.length()
                    && text.charAt(found + 2) == '*'
                    && hasUnpairedSingleStar(text, fromIndex, found)) {
                found++;
                shifted = true;
            }
            if (found > fromIndex
                    && !Character.isWhitespace(text.charAt(found - 1))
                    && (shifted || text.charAt(found - 1) != '*')
                    && !text.substring(fromIndex, found).trim().isEmpty()) {
                return found;
            }
            idx = found + 2;
        }
        return -1;
    }

    private static boolean hasUnpairedSingleStar(String text, int start, int end) {
        int singleCount = 0;
        for (int i = start; i < end; i++) {
            if (text.charAt(i) != '*') {
                continue;
            }
            if (i + 1 < end && text.charAt(i + 1) == '*') {
                i++;
                continue;
            }
            singleCount++;
        }
        return (singleCount % 2) == 1;
    }

    private static int findClosingSingleStar(String text, int fromIndex) {
        int len = text.length();
        boolean inDouble = false;
        boolean justClosedDouble = false;
        for (int idx = fromIndex; idx < len; idx++) {
            if (text.charAt(idx) != '*') {
                justClosedDouble = false;
                continue;
            }
            if (idx + 1 < len && text.charAt(idx + 1) == '*') {
                boolean wasInDouble = inDouble;
                inDouble = !inDouble;
                justClosedDouble = wasInDouble;
                idx++;
                continue;
            }
            if (!inDouble && (!Character.isWhitespace(text.charAt(idx - 1))
                    && (text.charAt(idx - 1) != '*' || justClosedDouble))) {
                return idx;
            }
            justClosedDouble = false;
        }
        return -1;
    }
}
