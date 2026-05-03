package com.notifyrelay.bridge;

import android.app.Notification;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

public class NotificationForwarderService extends NotificationListenerService {
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null) return;
        if (!RelayConfig.enabled(this)) return;

        String pkg = sbn.getPackageName();
        if (!NotificationFilter.packageAllowed(pkg, RelayConfig.packages(this))) return;

        Notification n = sbn.getNotification();
        if (n == null || n.extras == null) return;

        NotificationPayload payload = fromNotification(sbn, n);
        String all = payload.combinedText();
        if (!NotificationFilter.keywordAllowed(all, RelayConfig.keywords(this))) return;

        RelayPoster.postNotification(
                this,
                RelayConfig.url(this),
                RelayConfig.relayKey(this),
                pkg,
                payload,
                false,
                null
        );
    }

    private NotificationPayload fromNotification(StatusBarNotification sbn, Notification n) {
        Bundle e = n.extras;
        NotificationPayload p = new NotificationPayload();
        p.title = safe(e.getCharSequence(Notification.EXTRA_TITLE));
        p.text = safe(e.getCharSequence(Notification.EXTRA_TEXT));
        p.bigText = safe(e.getCharSequence(Notification.EXTRA_BIG_TEXT));
        p.subText = safe(e.getCharSequence(Notification.EXTRA_SUB_TEXT));
        p.summaryText = safe(e.getCharSequence(Notification.EXTRA_SUMMARY_TEXT));
        p.category = safe(n.category);
        if (android.os.Build.VERSION.SDK_INT >= 26) p.channelId = safe(n.getChannelId());
        p.key = safe(sbn.getKey());
        p.postTime = sbn.getPostTime();
        CharSequence[] lines = e.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
        if (lines != null) {
            for (CharSequence line : lines) {
                String s = safe(line);
                if (!s.trim().isEmpty()) p.textLines.add(s);
            }
        }
        return p;
    }

    private static String safe(CharSequence cs) {
        return cs == null ? "" : cs.toString();
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
