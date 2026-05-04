package com.notifyrelay.bridge;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

final class RelayPoster {
    interface Callback {
        void done(boolean ok, int httpCode, String error);
    }

    private RelayPoster() {}

    static void postNotification(Context context, String endpoint, String relayKey, String pkg, NotificationPayload notification, boolean test, Callback cb) {
        final Context app = context.getApplicationContext();
        new Thread(() -> {
            int http = -1;
            String err = "";
            boolean ok = false;
            String pushContent = notification == null ? "" : notification.combinedText();
            boolean english = RelayConfig.isEnglish(app);
            try {
                if (endpoint == null || endpoint.trim().isEmpty() || endpoint.contains("YOUR_SERVER_IP")) {
                    throw new IllegalArgumentException(english ? "Please enter the server /ingest URL first" : "请先填写服务器 /ingest 地址");
                }
                if (relayKey == null || relayKey.trim().isEmpty()) {
                    throw new IllegalArgumentException(english ? "Please enter the receiver key first" : "请先填写接收密钥");
                }
                if (notification == null) {
                    throw new IllegalArgumentException(english ? "Notification content is empty" : "通知内容为空");
                }

                JSONObject notif = new JSONObject();
                notif.put("title", notification.title);
                notif.put("text", notification.text);
                notif.put("big_text", notification.bigText);
                notif.put("sub_text", notification.subText);
                notif.put("summary_text", notification.summaryText);
                JSONArray lines = new JSONArray();
                for (String line : notification.textLines) lines.put(line);
                notif.put("text_lines", lines);
                notif.put("category", notification.category);
                notif.put("channel_id", notification.channelId);

                JSONObject body = new JSONObject();
                body.put("source", test ? "android_notification_test" : "android_notification");
                body.put("app", "Notify Relay Bridge");
                body.put("from", pkg == null ? "android" : pkg);
                body.put("package", pkg == null ? "" : pkg);
                body.put("notification", notif);
                body.put("post_time", notification.postTime / 1000L);
                body.put("ts", System.currentTimeMillis() / 1000L);
                body.put("notification_key", notification.key);
                body.put("text", pushContent);

                URL url = new URL(endpoint.trim());
                HttpURLConnection c = (HttpURLConnection) url.openConnection();
                c.setConnectTimeout(8000);
                c.setReadTimeout(10000);
                c.setRequestMethod("POST");
                c.setRequestProperty("Accept", "application/json");
                c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                c.setRequestProperty("User-Agent", "NotifyRelayBridge/1.0");
                c.setRequestProperty("X-Relay-Key", relayKey.trim());
                c.setRequestProperty("Authorization", "Bearer " + relayKey.trim());
                c.setDoOutput(true);
                byte[] raw = body.toString().getBytes(StandardCharsets.UTF_8);
                try (OutputStream os = c.getOutputStream()) {
                    os.write(raw);
                }
                http = c.getResponseCode();
                InputStream is = http >= 400 ? c.getErrorStream() : c.getInputStream();
                String resp = readSmall(is);
                ok = http >= 200 && http < 300;
                if (!ok) err = "HTTP " + http + " " + resp;
                c.disconnect();
            } catch (Exception e) {
                err = e.getClass().getSimpleName() + ": " + (e.getMessage() == null ? "" : e.getMessage());
            }
            RelayConfig.recordResult(app, ok ? "success" : "failed", http, err, pushContent);
            if (cb != null) cb.done(ok, http, err);
        }).start();
    }

    static void postTest(Context context, String endpoint, String relayKey, Callback cb) {
        boolean english = RelayConfig.isEnglish(context.getApplicationContext());
        NotificationPayload p = new NotificationPayload();
        p.title = "Notify Relay Bridge";
        p.text = english ? "Test notification forwarding content" : "测试通知转发内容";
        p.bigText = english
                ? "This is a test push payload for checking the app-to-receiver /ingest link."
                : "这是一条测试推送内容，用于验证 App 到接收端服务器的 /ingest 链路。";
        p.category = "test";
        p.channelId = "manual_test";
        p.key = "manual_test";
        p.postTime = System.currentTimeMillis();
        postNotification(context, endpoint, relayKey, "manual_test", p, true, cb);
    }

    private static String readSmall(InputStream is) throws Exception {
        if (is == null) return "";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[512];
        int n;
        int total = 0;
        while ((n = is.read(buf)) != -1 && total < 2048) {
            out.write(buf, 0, n);
            total += n;
        }
        return out.toString("UTF-8");
    }
}
