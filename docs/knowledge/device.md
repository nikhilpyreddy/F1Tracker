# Device: install, debug, inspect

Target device: **Pixel 10 Pro** (Android 17), connected over **wireless debugging**.

```bash
ADB=~/Library/Android/sdk/platform-tools/adb
$ADB devices -l          # the phone shows up twice (IP:port and mDNS name). Pick ONE:
ANDROID_SERIAL=<ip:port> ./gradlew installDebug
$ADB -s <ip:port> shell am start -n com.nikhil.f1tracker/.MainActivity
```

- **Pin one connection:** always set `ANDROID_SERIAL`/`-s` to one connection. Otherwise
  `installDebug` sees two devices.
- **No `timeout` on macOS:** don't wrap commands in `timeout`; use the tool's own timeout instead.
- **Crashes:** `$ADB -s <serial> logcat -d -b crash | grep f1tracker`
- **Network:** OkHttp logs at BASIC: `logcat -d | grep okhttp`. HTTP/2 responses log as
  `<-- 200 url` with no "OK".
- **Database:** the phone has no `sqlite3`. Copy the database out and query it locally:
  ```bash
  for f in f1_tracker.db f1_tracker.db-wal f1_tracker.db-shm; do
    $ADB -s <serial> exec-out run-as com.nikhil.f1tracker cat databases/$f > /tmp/$f; done
  sqlite3 /tmp/f1_tracker.db ".tables"
  ```
- **Driving the UI:** `uiautomator dump` + `input tap` works (see the `android-device-tester` agent).
  Stop if the screen is off or locked, or if the owner is using the phone: automation fights them.
- **Screenshots:** `$ADB -s <serial> exec-out screencap -p > shot.png`. A black image means the
  screen is off.
