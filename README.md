# Noti Keeper

**Get your notifications on time on Chinese Android ROMs.**

On Xiaomi HyperOS/MIUI, Oppo ColorOS, vivo OriginOS and other Chinese ROMs, notifications from
Messenger, Gmail, WhatsApp, banking apps and many others often arrive late, in a burst when you
unlock the phone or toggle Wi‑Fi. Noti Keeper fixes that for every app at once by keeping Google's
shared push channel (FCM) alive.

[![Support me on Ko-fi](https://img.shields.io/badge/Ko--fi-Buy%20me%20a%20coffee-FF5E5B?logo=ko-fi&logoColor=white)](https://ko-fi.com/quangtrang1111/?hidefeed=true&widget=true&embed=true)

## Why Noti Keeper

- **Built for Chinese ROMs.** It includes a dedicated fix for HyperOS China's GMS freezer and
  shortcuts to the hidden battery and autostart screens on Xiaomi, Oppo and vivo.
- **Super light.** < 13 KB, with no libraries and no AndroidX.
- **0 MB RAM between checks.** There's no background service and no persistent notification.
  The app wakes up for a moment, does its check and exits itself.
- **Almost no battery.** It sets one alarm every few minutes and runs for a few milliseconds each
  time. On HyperOS China it even skips the radio wake-up once Google Play Services is protected.
- **Private by design.** It has no internet permission, collects nothing and shows no ads.
- **Open source and transparent.** All the code is here, in four small Java files you can read in
  a few minutes. Build the APK yourself and you know exactly what runs on your phone: no hidden
  trackers, no obfuscated SDKs and no surprises.

## Supported ROMs

| ROM | What Noti Keeper does |
|---|---|
| **Xiaomi HyperOS / MIUI China** (Xiaomi, Redmi, POCO) | Keeps Google Play Services off the freeze list automatically, plus push checks |
| **Xiaomi HyperOS / MIUI Global** | Push checks and the autostart shortcut |
| **Oppo ColorOS / OnePlus / realme** | Push checks, autostart shortcut and Google Play Services battery shortcut |
| **vivo OriginOS / Funtouch OS / iQOO** | Push checks, autostart shortcut and Google Play Services battery shortcut |
| **Other Android 7.0+ phones** | Push checks |

Google Play Services must be installed.

## How it works

Google Play Services keeps one connection to `mtalk.google.com:5228` for all FCM pushes and
sends a heartbeat about every 28–30 minutes. Chinese ROMs and many carrier networks drop that
connection silently before then, so messages wait on Google's side until something reconnects.

Every 5, 10, 15, 20 or 30 minutes (you choose), an alarm wakes the app, which sends these
broadcasts to Google Play Services. GMS registers receivers for them without requiring a permission.

| Broadcast | Effect (verified on GMS 26.34) |
|---|---|
| `com.google.android.intent.action.GCM_RECONNECT` | Reconnects if disconnected; does nothing when connected |
| `com.google.android.intent.action.MCS_HEARTBEAT` | Sends a heartbeat now |
| `com.google.android.gms.gcm.ACTION_HEARTBEAT_NOW` | Same, newer action name |

A heartbeat over a dead connection fails, so GMS notices and reconnects right away.

### Xiaomi HyperOS China: the no-freeze list

HyperOS China freezes Google Play Services about 10 seconds after the screen turns off, which
kills the push connection. A frozen process can't be helped by pings. Apps listed in the hidden
setting `Settings.System.MILLET_NO_RESTRICT_APP` are never frozen, but PowerKeeper rebuilds that
list (at boot, on cloud config updates and on any battery setting change) and drops GMS.

Noti Keeper re-adds GMS to that list on every check, at boot and whenever you open the app. Once
GMS is protected, its own heartbeat keeps the connection alive, so Noti Keeper skips the extra
heartbeat to save battery.

Oppo, vivo and other brands have no writable equivalent of this list. On those phones you set
Google Play Services' battery to Unrestricted by hand, and the app opens that screen for you.

## Install and set up

1. Download the APK from [Releases](../../releases), install it and open it once.
2. Tap **Battery: don't optimize this app**.
3. Tap **Autostart settings** and enable Noti Keeper. Also lock it in Recents if your ROM
   supports it, because a cleared or frozen app loses its alarms on Chinese ROMs.
4. **Xiaomi China ROM:** tap **Xiaomi: allow "Modify system settings"**. The status should then
   show "HyperOS no-freeze list: GMS protected".
5. **Oppo / vivo:** tap **GPS: battery & autostart** and set Google Play Services to
   Unrestricted / allow background activity / allow auto launch.

The status shows the last and next check. Dial `*#*#426#*#*` to see whether the push connection
stays up.

### "Built for an older version of Android"

The app targets Android 5.1 (`targetSdk 22`) on purpose: it's the only way Android allows writing
the HyperOS no-freeze list. Because of that, you may see a one-time warning. HyperOS installs it
normally. If your phone refuses to install it (some Android 14+ ROMs), use ADB:

```sh
adb install --bypass-low-target-sdk-block app-release.apk
```

## What it doesn't fix

If your ROM blocks the app that should show the notification (a bank or chat app, for example)
from being woken, GMS receives the message but can't deliver it (`*#*#426#*#*` shows a failed
broadcast). For those apps you still need to enable Autostart and set Battery to
"No restrictions" **for that app**.

## Check it with ADB

```sh
adb shell dumpsys alarm | grep -A3 dev.trang.notikeeper                       # alarm armed?
adb shell dumpsys activity service com.google.android.gms/.gcm.GcmService | grep -E "connected|Heartbeat|Client HB"
```

## Build

Requires JDK 17+ and Android SDK 35.

```sh
./gradlew assembleRelease     # app/build/outputs/apk/release/app-release.apk
```

Local builds are signed with your debug key. Official APKs on [Releases](../../releases) are built
and signed by GitHub Actions when a new `versionName` is merged into `main`.

## Support

If Noti Keeper saves your notifications, you can
[buy me a coffee on Ko-fi](https://ko-fi.com/quangtrang1111/?hidefeed=true&widget=true&embed=true). Thank you!

## License

[GPL-3.0](LICENSE) with an attribution term. You may use, modify and redistribute this code, but
any app or product built from it must:

- stay open source under the same license, and
- show **"Based on Noti Keeper by quangtrang1111 – https://github.com/quangtrang1111"** somewhere
  users can see it, such as the About screen or the store description.

See [LICENSE](LICENSE) for the full terms.
