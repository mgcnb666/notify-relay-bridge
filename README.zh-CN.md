# Notify Relay Bridge

通用 Android 通知转发套件：Android App 监听用户授权的系统通知，把匹配配置的通知字段发送到接收端服务器；接收端只记录内容、备注和接收时间，并追加写入 JSONL 文件。

当前 Android APK：`/root/work/notify-relay-bridge/Notify-Relay-Bridge-v1.2.0-debug.apk`。

v1.2.0 变更：

- App 默认英文界面。
- 新增持久化语言选择：`English` / `中文`。
- 测试推送内容和本地校验错误会跟随所选语言。

## 安装

### 1. 准备目录

```bash
cd /root/work/notify-relay-bridge
```

目录内容：

```text
app/      Android 原生 App 源码
server/   Python 接收端服务器源码
```

### 2. 启动接收端服务器

```bash
cd /root/work/notify-relay-bridge
python3 server/notify_receiver.py \
  --host 0.0.0.0 \
  --port 8788 \
  --output data/notifications.jsonl \
  --key-file key.env
```

首次启动会自动生成：

```text
key.env
```

其中包含：

```text
NOTIFY_RELAY_KEY=自动生成的随机key
```

### 3. 查看key

```bash
cd /root/work/notify-relay-bridge
python3 - <<'PY'
from pathlib import Path
for line in Path('key.env').read_text().splitlines():
    if line.startswith('NOTIFY_RELAY_KEY='):
        print(line.split('=', 1)[1])
PY
```

把输出填入 Android App 的：

```text
接收密钥
```

## 使用方法

### 1. 配置 App

打开 `Notify Relay Bridge` 后填写：

```text
服务器地址：http://服务器IP:8788/ingest
接收密钥：key.env 中的 NOTIFY_RELAY_KEY
允许监听的包名：com.whatsapp,com.whatsapp.w4b 或 *
关键词：* 或 keyword1,keyword2
```

说明：

```text
包名=*        监听所有 App 的通知
关键词=*      不按关键词过滤
关键词=a,b    只转发通知内容中包含 a 或 b 的通知
```

### 2. 开启通知访问权限

在 App 内点击：

```text
打开通知访问权限
```

然后在 Android 系统设置里允许：

```text
Notify Relay Bridge
```

### 3. 测试发送

在 App 中点击：

```text
测试发送
```

成功时 App 状态会显示：

```text
最近状态：success
HTTP：200
```

### 4. 命令行测试接收端

健康检查：

```bash
curl -sS http://127.0.0.1:8788/health
```

带密钥写入测试记录：

```bash
cd /root/work/notify-relay-bridge
KEY=$(python3 - <<'PY'
from pathlib import Path
for line in Path('key.env').read_text().splitlines():
    if line.startswith('NOTIFY_RELAY_KEY='):
        print(line.split('=', 1)[1])
PY
)

curl -sS -X POST http://127.0.0.1:8788/ingest \
  -H "X-Relay-Key: $KEY" \
  -H 'Content-Type: application/json' \
  -d '{"source":"manual_test","text":"hello","remark":"test"}'
```

查看写入文件概要：

```bash
wc -l /root/work/notify-relay-bridge/data/notifications.jsonl
python3 - <<'PY'
from pathlib import Path
import json
p = Path('/root/work/notify-relay-bridge/data/notifications.jsonl')
last = p.read_text(encoding='utf-8').splitlines()[-1]
obj = json.loads(last)
print('received_at=', obj.get('received_at'))
print('source=', obj.get('source'))
print('remark=', obj.get('remark'))
print('content_len=', len(obj.get('content') or ''))
PY
```

## 技术架构

### 1. 数据流

```text
Android 系统通知
    ↓
NotificationListenerService
    ↓
Notify Relay Bridge App 本地过滤
    ↓
HTTP POST /ingest
    ↓
notify_receiver.py 密钥校验
    ↓
data/notifications.jsonl
```

### 2. Android App

```text
名称：Notify Relay Bridge
包名：com.notifyrelay.bridge
入口：MainActivity
通知监听服务：NotificationForwarderService
配置存储：SharedPreferences
网络发送：HttpURLConnection
```

App 读取的通知字段：

```text
title
text
big_text
sub_text
summary_text
text_lines
category
channel_id
package
post_time
notification_key
```

App 请求头：

```text
X-Relay-Key: <接收密钥>
Authorization: Bearer <接收密钥>
Content-Type: application/json; charset=utf-8
```

### 3. 接收端服务器

```text
语言：Python 3 标准库
服务文件：server/notify_receiver.py
默认端口：8788
密钥文件：key.env
输出文件：data/notifications.jsonl
接口：/health, /ingest, /notify, /webhook, /latest
```

认证规则：

```text
/health 不需要密钥
/ingest /notify /webhook /latest 需要密钥
```

JSONL 每行结构：

```text
received_at      UTC 接收时间
received_ts      epoch 秒
path             请求路径
remark           备注
client.ip        客户端 IP
client.user_agent
content_type
source           来源字段
content          聚合文本内容
payload          原始 payload
```

### 4. 权限和边界

Android App 权限：

```text
INTERNET
ACCESS_NETWORK_STATE
BIND_NOTIFICATION_LISTENER_SERVICE  # 系统绑定权限，需用户手动开启通知访问
```

App 不使用：

```text
WebView
root
无障碍
截屏/OCR
短信权限
联系人权限
定位权限
相机/录音权限
悬浮窗权限
```
