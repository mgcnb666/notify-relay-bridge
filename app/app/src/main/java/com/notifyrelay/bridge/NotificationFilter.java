package com.notifyrelay.bridge;

import java.util.Locale;

final class NotificationFilter {
    private NotificationFilter() {}

    static boolean packageAllowed(String pkg, String allowedCsv) {
        if (pkg == null || pkg.isEmpty()) return false;
        String csv = allowedCsv == null ? "" : allowedCsv.trim();
        if (csv.isEmpty() || "*".equals(csv)) return true;
        String[] parts = csv.split(",");
        for (String p : parts) {
            String allow = p.trim();
            if (allow.isEmpty()) continue;
            if ("*".equals(allow)) return true;
            if (pkg.equals(allow)) return true;
        }
        return false;
    }

    static boolean keywordAllowed(String text, String keywordsCsv) {
        String csv = keywordsCsv == null ? "" : keywordsCsv.trim();
        if (csv.isEmpty() || "*".equals(csv)) return true;
        String body = text == null ? "" : text.toLowerCase(Locale.ROOT);
        String[] parts = csv.split(",");
        for (String p : parts) {
            String kw = p.trim().toLowerCase(Locale.ROOT);
            if (kw.isEmpty()) continue;
            if ("*".equals(kw)) return true;
            if (body.contains(kw)) return true;
        }
        return false;
    }
}
