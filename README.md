# Notify Relay Bridge

A generic Android notification forwarding suite: the Android app listens to user-authorized system notifications, forwards matching notification fields to the receiver server, and the server records content, remarks, and receive timestamps into a JSONL file.

## Installation

### 1. Prepare the project directory

```bash
cd /root/work/notify-relay-bridge
```

Directory layout:

```text
app/      Native Android app source
server/   Python receiver server source
README.md English README
README.zh-CN.md Chinese README
```

### 2. Start the receiver server

```bash
cd /root/work/notify-relay-bridge
python3 server/notify_receiver.py \
  --host 0.0.0.0 \
  --port 8788 \
  --output data/notifications.jsonl \
  --key-file key.env
```

On first startup, the server automatically creates:

```text
key.env
```

It contains:

```text
NOTIFY_RELAY_KEY=auto-generated-random-key
```

The file permission is set to `600`.

### 3. Read the receiver key

```bash
cd /root/work/notify-relay-bridge
python3 - <<'PY'
from pathlib import Path
for line in Path('key.env').read_text().splitlines():
    if line.startswith('NOTIFY_RELAY_KEY='):
        print(line.split('=', 1)[1])
PY
```

Paste the output into the Android app field:

```text
Receiver Key
```

## Usage

### 1. Configure the app

Open `Notify Relay Bridge` and fill in:

```text
Server URL: http://SERVER_IP:8788/ingest
Receiver Key: NOTIFY_RELAY_KEY from key.env
Allowed Packages: com.whatsapp,com.whatsapp.w4b or *
Keywords: * or keyword1,keyword2
```

Rules:

```text
Allowed Packages = *   listen to notifications from all apps
Keywords = *           do not filter by keyword
Keywords = a,b         only forward notifications containing a or b
```

### 2. Enable notification access

In the app, tap:

```text
Open Notification Access Settings
```

Then allow this service in Android settings:

```text
Notify Relay Bridge
```

### 3. Send a test event

In the app, tap:

```text
Test Send
```

A successful status shows:

```text
Last Status: success
HTTP: 200
```

### 4. Test the receiver from the command line

Health check:

```bash
curl -sS http://127.0.0.1:8788/health
```

Write a test record with the key:

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

Inspect the output file summary:

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

## Technical Architecture

### 1. Data flow

```text
Android system notification
    ↓
NotificationListenerService
    ↓
Local filtering in Notify Relay Bridge
    ↓
HTTP POST /ingest
    ↓
Key validation in notify_receiver.py
    ↓
data/notifications.jsonl
```

### 2. Android app

```text
Name: Notify Relay Bridge
Package: com.notifyrelay.bridge
Entry activity: MainActivity
Notification listener service: NotificationForwarderService
Config storage: SharedPreferences
Network client: HttpURLConnection
```

Notification fields read by the app:

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

HTTP request headers:

```text
X-Relay-Key: <receiver-key>
Authorization: Bearer <receiver-key>
Content-Type: application/json; charset=utf-8
```

### 3. Receiver server

```text
Language: Python 3 standard library
Server file: server/notify_receiver.py
Default port: 8788
Key file: key.env
Output file: data/notifications.jsonl
Endpoints: /health, /ingest, /notify, /webhook, /latest
```

Authentication rules:

```text
/health does not require a key
/ingest /notify /webhook /latest require a key
```

JSONL record fields:

```text
received_at      UTC receive time
received_ts      epoch seconds
path             request path
remark           note/remark
client.ip        client IP
client.user_agent
content_type
source           source field
content          combined text content
payload          original payload
```

### 4. Permissions and boundaries

Android permissions:

```text
INTERNET
ACCESS_NETWORK_STATE
BIND_NOTIFICATION_LISTENER_SERVICE  # system-bound permission; user must enable notification access manually
```
