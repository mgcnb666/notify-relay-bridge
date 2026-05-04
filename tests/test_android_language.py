from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MAIN = ROOT / "app/app/src/main/java/com/notifyrelay/bridge/MainActivity.java"
CONFIG = ROOT / "app/app/src/main/java/com/notifyrelay/bridge/RelayConfig.java"
GRADLE = ROOT / "app/app/build.gradle"


def test_main_activity_has_persistent_language_selector_and_english_ui():
    src = MAIN.read_text(encoding="utf-8")
    assert "RadioGroup" in src
    assert "languageGroup" in src
    assert "RelayConfig.setLanguage" in src
    assert "Language / 语言" in src
    assert "Open Notification Access Settings" in src
    assert "Receiver Key" in src
    assert "Test Send" in src
    assert "通知访问权限" in src
    assert "测试发送" in src


def test_relay_config_persists_language_with_english_default():
    src = CONFIG.read_text(encoding="utf-8")
    assert "KEY_LANGUAGE" in src
    assert 'DEFAULT_LANGUAGE = "en"' in src
    assert "static String language" in src
    assert "static void setLanguage" in src


def test_relay_poster_has_language_aware_test_payload_and_errors():
    src = (ROOT / "app/app/src/main/java/com/notifyrelay/bridge/RelayPoster.java").read_text(encoding="utf-8")
    assert "RelayConfig.isEnglish" in src
    assert "Please enter the server /ingest URL first" in src
    assert "Please enter the receiver key first" in src
    assert "Test notification forwarding content" in src


def test_android_version_bumped_for_language_release():
    src = GRADLE.read_text(encoding="utf-8")
    assert "versionCode 3" in src
    assert "versionName '1.2.0'" in src
