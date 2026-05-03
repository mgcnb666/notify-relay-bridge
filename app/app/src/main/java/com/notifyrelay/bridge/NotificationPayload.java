package com.notifyrelay.bridge;

import java.util.ArrayList;
import java.util.List;

final class NotificationPayload {
    String title = "";
    String text = "";
    String bigText = "";
    String subText = "";
    String summaryText = "";
    String category = "";
    String channelId = "";
    String key = "";
    long postTime = 0L;
    final List<String> textLines = new ArrayList<>();

    String combinedText() {
        StringBuilder b = new StringBuilder();
        append(b, title);
        append(b, text);
        append(b, bigText);
        append(b, subText);
        append(b, summaryText);
        for (String line : textLines) append(b, line);
        return b.toString();
    }

    private static void append(StringBuilder b, String s) {
        if (s == null || s.trim().isEmpty()) return;
        if (b.length() > 0) b.append('\n');
        b.append(s);
    }
}
