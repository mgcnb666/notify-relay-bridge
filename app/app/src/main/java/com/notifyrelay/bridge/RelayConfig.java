package com.notifyrelay.bridge;

import android.content.Context;
import android.content.SharedPreferences;

final class RelayConfig {
    static final String PREFS = "notify_relay_config";
    static final String KEY_ENABLED = "enabled";
    static final String KEY_URL = "url";
    static final String KEY_RELAY_KEY = "relay_key";
    static final String KEY_PACKAGES = "packages";
    static final String KEY_KEYWORDS = "keywords";
    static final String KEY_SHOW_PUSH_CONTENT_LOCAL = "show_push_content_local";
    static final String KEY_LANGUAGE = "language";
    static final String KEY_LAST_STATUS = "last_status";
    static final String KEY_LAST_ERROR = "last_error";
    static final String KEY_LAST_TS = "last_ts";
    static final String KEY_LAST_HTTP = "last_http";
    static final String KEY_LAST_PUSH_CONTENT = "last_push_content";
    static final String KEY_LAST_NOTIFICATION_LEN = "last_notification_len";

    static final String DEFAULT_URL = "http://YOUR_SERVER_IP:8788/ingest";
    static final String DEFAULT_PACKAGES = "com.whatsapp,com.whatsapp.w4b";
    static final String DEFAULT_KEYWORDS = "*";
    static final String LANG_EN = "en";
    static final String LANG_ZH = "zh";
    static final String DEFAULT_LANGUAGE = "en";

    private RelayConfig() {}

    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static boolean enabled(Context c) {
        return prefs(c).getBoolean(KEY_ENABLED, true);
    }

    static String url(Context c) {
        return prefs(c).getString(KEY_URL, DEFAULT_URL);
    }

    static String relayKey(Context c) {
        return prefs(c).getString(KEY_RELAY_KEY, "");
    }

    static String packages(Context c) {
        return prefs(c).getString(KEY_PACKAGES, DEFAULT_PACKAGES);
    }

    static String keywords(Context c) {
        return prefs(c).getString(KEY_KEYWORDS, DEFAULT_KEYWORDS);
    }

    static String language(Context c) {
        String value = prefs(c).getString(KEY_LANGUAGE, DEFAULT_LANGUAGE);
        if (LANG_ZH.equals(value)) return LANG_ZH;
        return LANG_EN;
    }

    static boolean isEnglish(Context c) {
        return LANG_EN.equals(language(c));
    }

    static void setLanguage(Context c, String language) {
        String clean = LANG_ZH.equals(language) ? LANG_ZH : LANG_EN;
        prefs(c).edit().putString(KEY_LANGUAGE, clean).apply();
    }

    static void save(Context c, boolean enabled, String url, String relayKey, String packages, String keywords, boolean showPushContentLocal) {
        prefs(c).edit()
                .putBoolean(KEY_ENABLED, enabled)
                .putBoolean(KEY_SHOW_PUSH_CONTENT_LOCAL, showPushContentLocal)
                .putString(KEY_URL, url == null ? "" : url.trim())
                .putString(KEY_RELAY_KEY, relayKey == null ? "" : relayKey.trim())
                .putString(KEY_PACKAGES, packages == null ? DEFAULT_PACKAGES : packages.trim())
                .putString(KEY_KEYWORDS, keywords == null ? DEFAULT_KEYWORDS : keywords.trim())
                .apply();
    }

    static void recordResult(Context c, String status, int http, String error, String pushContent) {
        int notificationLen = pushContent == null ? 0 : pushContent.length();
        boolean keepPushContent = prefs(c).getBoolean(KEY_SHOW_PUSH_CONTENT_LOCAL, true);
        prefs(c).edit()
                .putString(KEY_LAST_STATUS, status == null ? "" : status)
                .putInt(KEY_LAST_HTTP, http)
                .putString(KEY_LAST_ERROR, error == null ? "" : error)
                .putLong(KEY_LAST_TS, System.currentTimeMillis())
                .putInt(KEY_LAST_NOTIFICATION_LEN, Math.max(0, notificationLen))
                .putString(KEY_LAST_PUSH_CONTENT, keepPushContent ? sanitizePushContent(pushContent) : "")
                .apply();
    }

    private static String sanitizePushContent(String content) {
        if (content == null) return "";
        String s = content.replace('\r', '\n').trim();
        while (s.contains("\n\n\n")) s = s.replace("\n\n\n", "\n\n");
        if (s.length() > 1200) s = s.substring(0, 1200) + "\n...";
        return s;
    }
}
