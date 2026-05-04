package com.notifyrelay.bridge;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int LANG_EN_ID = 1001;
    private static final int LANG_ZH_ID = 1002;

    private EditText urlInput;
    private EditText keyInput;
    private EditText packagesInput;
    private EditText keywordsInput;
    private CheckBox enabledBox;
    private CheckBox showContentBox;
    private RadioGroup languageGroup;
    private TextView permissionStatus;
    private TextView lastStatus;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        buildUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    private void buildUi() {
        boolean en = isEnglish();
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(255, 251, 254));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(22), dp(18), dp(22));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));

        TextView title = text("Notify Relay Bridge", 26, Color.rgb(29, 27, 32), true);
        root.addView(title);
        TextView sub = text(t(
                "Generic Android notification forwarder: listens to user-authorized notifications and sends matching notification fields from allowed packages to your receiver server.",
                "通用 Android 通知转发器：监听用户授权的通知，把允许包名内、匹配关键词的通知字段发送到你的接收端服务器。接收端只记录内容和接收时间，不做内容解析。"),
                14, Color.rgb(73, 69, 79), false);
        sub.setPadding(0, dp(6), 0, dp(12));
        root.addView(sub);

        LinearLayout langCard = card();
        root.addView(langCard, lp(-1, -2, 0, 0, 0, dp(14)));
        langCard.addView(text("Language / 语言", 16, Color.rgb(29, 27, 32), true));
        languageGroup = new RadioGroup(this);
        languageGroup.setOrientation(RadioGroup.HORIZONTAL);
        languageGroup.setPadding(0, dp(8), 0, 0);
        RadioButton enBtn = radio("English", LANG_EN_ID);
        RadioButton zhBtn = radio("中文", LANG_ZH_ID);
        languageGroup.addView(enBtn, new RadioGroup.LayoutParams(0, dp(42), 1));
        languageGroup.addView(zhBtn, new RadioGroup.LayoutParams(0, dp(42), 1));
        languageGroup.check(en ? LANG_EN_ID : LANG_ZH_ID);
        languageGroup.setOnCheckedChangeListener((group, checkedId) -> {
            String lang = checkedId == LANG_ZH_ID ? RelayConfig.LANG_ZH : RelayConfig.LANG_EN;
            if (!lang.equals(RelayConfig.language(this))) {
                RelayConfig.setLanguage(this, lang);
                buildUi();
            }
        });
        langCard.addView(languageGroup);

        LinearLayout card = card();
        root.addView(card);

        permissionStatus = text(t("Notification access: checking", "通知访问权限：检查中"), 15, Color.rgb(73, 69, 79), true);
        card.addView(permissionStatus);
        Button permBtn = button(t("Open Notification Access Settings", "打开通知访问权限"), true);
        permBtn.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        card.addView(permBtn, lp(-1, dp(46), 0, dp(10), 0, 0));

        enabledBox = new CheckBox(this);
        enabledBox.setText(t("Enable auto forwarding", "启用自动转发"));
        enabledBox.setTextSize(15);
        enabledBox.setTextColor(Color.rgb(29, 27, 32));
        enabledBox.setChecked(RelayConfig.enabled(this));
        card.addView(enabledBox);

        showContentBox = new CheckBox(this);
        showContentBox.setText(t("Show latest pushed content in status (debug only)", "运行状态显示最近推送内容（仅用于调试）"));
        showContentBox.setTextSize(15);
        showContentBox.setTextColor(Color.rgb(29, 27, 32));
        showContentBox.setChecked(RelayConfig.prefs(this).getBoolean(RelayConfig.KEY_SHOW_PUSH_CONTENT_LOCAL, true));
        card.addView(showContentBox);

        urlInput = input(t("Server /ingest URL, e.g. http://SERVER_IP:8788/ingest", "服务器 /ingest 地址，例如 http://服务器IP:8788/ingest"), false);
        urlInput.setText(RelayConfig.url(this));
        card.addView(label(t("Server URL", "服务器地址")));
        card.addView(urlInput, lp(-1, dp(54), 0, 0, 0, dp(10)));

        keyInput = input(t("Paste NOTIFY_RELAY_KEY from server key.env", "填写服务器 key.env 里的 NOTIFY_RELAY_KEY"), true);
        keyInput.setText(RelayConfig.relayKey(this));
        card.addView(label(t("Receiver Key (required)", "接收密钥，必填")));
        card.addView(keyInput, lp(-1, dp(54), 0, 0, 0, dp(10)));

        packagesInput = input("com.whatsapp,com.whatsapp.w4b or *", false);
        packagesInput.setText(RelayConfig.packages(this));
        card.addView(label(t("Allowed package names, comma-separated; * = all apps", "允许监听的包名，逗号分隔，* 表示所有 App")));
        card.addView(packagesInput, lp(-1, dp(54), 0, 0, 0, dp(10)));

        keywordsInput = input(t("* or keyword1,keyword2", "* 或 keyword1,keyword2"), false);
        keywordsInput.setSingleLine(false);
        keywordsInput.setMinLines(2);
        keywordsInput.setGravity(Gravity.TOP | Gravity.START);
        keywordsInput.setText(RelayConfig.keywords(this));
        card.addView(label(t("Keywords, comma-separated; * = no keyword filter", "关键词，逗号分隔，* 表示不过滤关键词")));
        card.addView(keywordsInput, lp(-1, dp(86), 0, 0, 0, dp(12)));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        card.addView(row);
        Button save = button(t("Save", "保存配置"), true);
        save.setOnClickListener(v -> saveConfig(true));
        row.addView(save, new LinearLayout.LayoutParams(0, dp(46), 1));
        Button test = button(t("Test Send", "测试发送"), false);
        test.setOnClickListener(v -> sendTest());
        LinearLayout.LayoutParams testLp = new LinearLayout.LayoutParams(0, dp(46), 1);
        testLp.leftMargin = dp(10);
        row.addView(test, testLp);

        LinearLayout statusCard = card();
        LinearLayout.LayoutParams scLp = lp(-1, -2, 0, dp(14), 0, 0);
        root.addView(statusCard, scLp);
        statusCard.addView(text(t("Status", "运行状态"), 18, Color.rgb(29, 27, 32), true));
        lastStatus = text("", 14, Color.rgb(73, 69, 79), false);
        lastStatus.setPadding(0, dp(8), 0, 0);
        statusCard.addView(lastStatus);

        TextView note = text(t(
                "Tip: if Android hides notification content, this app may only receive text like “1 new message”. Setting packages or keywords to * forwards more notification content; use it only in trusted environments.",
                "提示：如果系统通知隐藏正文，App 只能收到类似“1 条新消息”的内容。把包名或关键词设为 * 会转发更多通知内容，请只在可信环境使用。"),
                13, Color.rgb(73, 69, 79), false);
        note.setPadding(0, dp(14), 0, 0);
        root.addView(note);

        setContentView(scroll);
        refreshStatus();
    }

    private void saveConfig(boolean toast) {
        RelayConfig.save(this,
                enabledBox.isChecked(),
                urlInput.getText().toString(),
                keyInput.getText().toString(),
                packagesInput.getText().toString(),
                keywordsInput.getText().toString(),
                showContentBox != null && showContentBox.isChecked());
        if (toast) Toast.makeText(this, t("Saved", "已保存"), Toast.LENGTH_SHORT).show();
        refreshStatus();
    }

    private void sendTest() {
        saveConfig(false);
        RelayPoster.postTest(this,
                RelayConfig.url(this),
                RelayConfig.relayKey(this),
                (ok, http, error) -> runOnUiThread(() -> {
                    Toast.makeText(this, ok ? t("Test sent successfully", "测试发送成功") : t("Test send failed", "测试发送失败"), Toast.LENGTH_SHORT).show();
                    refreshStatus();
                }));
    }

    private void refreshStatus() {
        boolean granted = isNotificationAccessEnabled();
        if (permissionStatus != null) {
            permissionStatus.setText(t("Notification access: ", "通知访问权限：") + (granted ? t("Enabled", "已开启") : t("Not enabled", "未开启")));
            permissionStatus.setTextColor(granted ? Color.rgb(20, 108, 67) : Color.rgb(179, 38, 30));
        }
        if (lastStatus != null) {
            SharedPreferences p = RelayConfig.prefs(this);
            String status = p.getString(RelayConfig.KEY_LAST_STATUS, "");
            if (status == null || status.isEmpty()) status = t("No forwarding records yet", "暂无转发记录");
            String err = p.getString(RelayConfig.KEY_LAST_ERROR, "");
            int http = p.getInt(RelayConfig.KEY_LAST_HTTP, -1);
            int notificationLen = p.getInt(RelayConfig.KEY_LAST_NOTIFICATION_LEN, 0);
            boolean showPushContent = p.getBoolean(RelayConfig.KEY_SHOW_PUSH_CONTENT_LOCAL, true);
            String pushContent = p.getString(RelayConfig.KEY_LAST_PUSH_CONTENT, "");
            long ts = p.getLong(RelayConfig.KEY_LAST_TS, 0L);
            String when = ts > 0 ? new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(ts)) : "-";
            StringBuilder sb = new StringBuilder();
            sb.append(t("Auto forwarding: ", "自动转发：")).append(RelayConfig.enabled(this) ? t("Enabled", "启用") : t("Paused", "暂停")).append('\n');
            sb.append(t("Last status: ", "最近状态：")).append(displayStatus(status)).append('\n');
            sb.append("HTTP: ").append(http).append('\n');
            sb.append(t("Pushed content: ", "推送内容：")).append(showPushContent && pushContent != null && !pushContent.isEmpty() ? pushContent : contentHiddenLabel(notificationLen)).append('\n');
            sb.append(t("Notification content length: ", "通知内容长度：")).append(notificationLen).append('\n');
            sb.append(t("Time: ", "时间：")).append(when);
            if (err != null && !err.isEmpty()) sb.append('\n').append(t("Error: ", "错误：")).append(err);
            lastStatus.setText(sb.toString());
        }
    }

    private String displayStatus(String status) {
        if (!isEnglish()) return status;
        if ("success".equalsIgnoreCase(status)) return "Success";
        if ("failed".equalsIgnoreCase(status)) return "Failed";
        return status;
    }

    private String contentHiddenLabel(int notificationLen) {
        if (notificationLen <= 0) return "-";
        return t("Hidden (", "已隐藏（") + notificationLen + t(" chars; enable the debug switch above to show)", "字，打开上方调试开关后显示）");
    }

    private boolean isNotificationAccessEnabled() {
        String flat = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        if (flat == null || flat.isEmpty()) return false;
        ComponentName cn = new ComponentName(this, NotificationForwarderService.class);
        return flat.toLowerCase(Locale.ROOT).contains(cn.flattenToString().toLowerCase(Locale.ROOT));
    }

    private LinearLayout card() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(16), dp(16), dp(16), dp(16));
        l.setBackgroundResource(R.drawable.card_bg);
        return l;
    }

    private TextView label(String s) {
        TextView v = text(s, 13, Color.rgb(73, 69, 79), true);
        v.setPadding(0, dp(6), 0, dp(4));
        return v;
    }

    private TextView text(String s, int sp, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextSize(sp);
        v.setTextColor(color);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        v.setLineSpacing(dp(2), 1.05f);
        return v;
    }

    private EditText input(String hint, boolean password) {
        EditText e = new EditText(this);
        e.setTextSize(14);
        e.setSingleLine(true);
        e.setHint(hint);
        e.setTextColor(Color.rgb(29, 27, 32));
        e.setHintTextColor(Color.rgb(121, 116, 126));
        e.setBackgroundResource(R.drawable.input_bg);
        e.setPadding(dp(14), 0, dp(14), 0);
        if (password) e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        else e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        return e;
    }

    private Button button(String s, boolean primary) {
        Button b = new Button(this);
        b.setText(s);
        b.setTextSize(14);
        b.setAllCaps(false);
        b.setTextColor(primary ? Color.WHITE : Color.rgb(29, 27, 32));
        b.setBackgroundResource(primary ? R.drawable.btn_primary : R.drawable.btn_tonal);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setPadding(dp(10), 0, dp(10), 0);
        if (android.os.Build.VERSION.SDK_INT >= 21) b.setStateListAnimator(null);
        return b;
    }

    private RadioButton radio(String s, int id) {
        RadioButton r = new RadioButton(this);
        r.setId(id);
        r.setText(s);
        r.setTextSize(15);
        r.setTextColor(Color.rgb(29, 27, 32));
        r.setGravity(Gravity.CENTER_VERTICAL);
        return r;
    }

    private LinearLayout.LayoutParams lp(int w, int h, int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.setMargins(l, t, r, b);
        return p;
    }

    private boolean isEnglish() {
        return RelayConfig.isEnglish(this);
    }

    private String t(String en, String zh) {
        return isEnglish() ? en : zh;
    }

    private int dp(int v) {
        return (int)(v * getResources().getDisplayMetrics().density + 0.5f);
    }
}
