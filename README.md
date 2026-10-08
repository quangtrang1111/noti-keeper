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
- **Super light.** About 13 KB, with no libraries and no AndroidX. Each ROM gets its own build,
  so your phone only carries the code it needs.
- **0 MB RAM between checks.** There's no background service and no persistent notification.
  The app wakes up for a moment, does its check and exits itself.
- **Almost no battery.** On HyperOS China, Android itself wakes the app only when the freeze list
  changes. Elsewhere, the check interval adapts to charging, battery level and network.
- **Nothing to configure.** No interval to pick: the app shows when it will check next.
- **Private by design.** It has no internet permission, collects nothing and shows no ads.
- **Open source and transparent.** All the code is here, in a handful of small Java files you can
  read in a few minutes. Build the APK yourself and you know exactly what runs on your phone: no hidden
  trackers, no obfuscated SDKs and no surprises.

## Which APK do I need?

Each [release](../../releases) has two APKs. Pick the one for your phone:

| Your phone | APK | What it does |
|---|---|---|
| **Xiaomi HyperOS / MIUI China** (Xiaomi, Redmi, POCO) | `NotiKeeper-xiaomi-vX.Y.apk` | Keeps Google Play Services off the freeze list within ~1 s, plus a push check every 30 min |
| **Xiaomi HyperOS / MIUI Global** | `NotiKeeper-universal-vX.Y.apk` | Push checks and the autostart shortcut |
| **Oppo ColorOS / OnePlus / realme** | `NotiKeeper-universal-vX.Y.apk` | Push checks, autostart and Google Play Services battery shortcuts |
| **vivo OriginOS / Funtouch OS / iQOO** | `NotiKeeper-universal-vX.Y.apk` | Push checks, autostart and Google Play Services battery shortcuts |
| **Honor and other Android 7.0+ phones** | `NotiKeeper-universal-vX.Y.apk` | Push checks |

Not sure? Install the Xiaomi APK on a Xiaomi phone: if your ROM has no freeze list, the app tells
you to switch to the Universal one. Both builds share the same app ID, so uninstall one before
installing the other. Google Play Services must be installed.

## How it works

Google Play Services keeps one connection to `mtalk.google.com:5228` for all FCM pushes and
sends a heartbeat about every 28–30 minutes. Chinese ROMs and many carrier networks drop that
connection silently before then, so messages wait on Google's side until something reconnects.

An alarm regularly wakes the app, which sends these broadcasts to Google Play Services. GMS registers receivers for them without requiring a permission.

| Broadcast | Effect (verified on GMS 26.34) |
|---|---|
| `com.google.android.intent.action.GCM_RECONNECT` | Reconnects if disconnected; does nothing when connected |
| `com.google.android.intent.action.MCS_HEARTBEAT` | Sends a heartbeat now |
| `com.google.android.gms.gcm.ACTION_HEARTBEAT_NOW` | Same, newer action name |

A heartbeat over a dead connection fails, so GMS notices and reconnects right away.

### Universal build: automatic interval

Each heartbeat briefly wakes the mobile radio, so the app picks the interval at every check:

| Phone state | Next check in |
|---|---|
| Charging | 5 min |
| Battery saver on, or battery below 15% | 30 min |
| Mobile data | 10 min (carriers drop idle connections sooner) |
| Wi‑Fi | 15 min |

### Xiaomi build: the no-freeze list

HyperOS China freezes Google Play Services about 10 seconds after the screen turns off, which
kills the push connection. A frozen process can't be helped by pings. Apps listed in the hidden
setting `Settings.System.MILLET_NO_RESTRICT_APP` are never frozen, but PowerKeeper rebuilds that
list (at boot, on cloud config updates and on any battery setting change) and drops GMS.

Noti Keeper asks Android's JobScheduler to run it whenever that setting changes (a content
trigger), so it re-adds GMS within about a second without any process waiting in the background.
A 30-minute alarm is the backup: it re-checks the list and sends `GCM_RECONNECT`. Once GMS is
protected, its own heartbeat keeps the connection alive, so Noti Keeper skips the extra heartbeat
to save battery.

Oppo, vivo and other brands have no writable equivalent of this list. On those phones you set
Google Play Services' battery to Unrestricted by hand, and the Universal app opens that screen for
you.

## Install and set up

Download [the right APK](#which-apk-do-i-need) from [Releases](../../releases), install it and
open it. If the Xiaomi APK fails with **"Couldn't install (-29)"**, see
[Installing on Android 14+](#installing-on-android-14).

Then tap the numbered buttons in order, once:

1. **Battery: allow running on time**: allow it.
2. **Autostart settings**: enable Noti Keeper. Also lock it in Recents if your ROM supports it,
   because a cleared or frozen app loses its alarms on Chinese ROMs.
3. Depends on your build:
   - **Xiaomi build: Allow "Modify system settings"**: turn it on. The status should then show
     "HyperOS no-freeze list: GMS protected".
   - **Universal build: Google Play Services: battery & autostart**: set it to Unrestricted /
     allow background activity / allow auto launch.

The app shows "Keeping your notifications on time. Next check: …" plus the last check. Dial `*#*#426#*#*` to see whether the push connection
stays up.

### Installing on Android 14+

This section is only for the **Xiaomi build**; the Universal build installs normally.

The Xiaomi build targets Android 5.1 (`targetSdk 22`) on purpose: it's the only way Android allows
a normal app to write the HyperOS no-freeze list. Android 14 and newer block installing apps that target
below Android 6.0, so depending on your ROM you may see **"Couldn't install (-29)"** or
"Installation package isn't compatible with system". Some ROMs (for example some HyperOS 3 builds)
allow it; others (for example some HyperOS 2 builds) don't.

This only blocks installing. Once installed with one of the methods below, the app works normally.
Install every **update** the same way; your settings are kept.

#### Option A: ADB (with a computer)

1. On the phone, enable **Developer options**, then turn on **USB debugging**.
   On Xiaomi, also turn on **Install via USB** (it may ask you to sign in to a Mi account).
2. Connect the phone and run:

   ```sh
   adb install --bypass-low-target-sdk-block NotiKeeper-xiaomi-v1.4.apk
   ```

#### Option B: Shizuku (no computer)

1. Install [Shizuku](https://shizuku.rikka.app/) and start it with **Wireless debugging**
   (Wi‑Fi required; the app guides you through pairing). On Xiaomi, also turn on
   **Install via USB** in Developer options.
2. Install the APK with either:
   - a Shizuku-based installer such as
     [Install With Options](https://github.com/zacharee/InstallWithOptions): grant it Shizuku
     access, pick the APK, enable the option to bypass the low target SDK block and install; or
   - Shizuku's `rish` shell in a terminal app such as Termux:

     ```sh
     pm install --bypass-low-target-sdk-block /sdcard/Download/NotiKeeper-xiaomi-v1.4.apk
     ```

Noti Keeper doesn't need Shizuku after installing, so it keeps working after a reboot even when
Shizuku is stopped.

## What it doesn't fix

If your ROM blocks the app that should show the notification (a bank or chat app, for example)
from being woken, GMS receives the message but can't deliver it (`*#*#426#*#*` shows a failed
broadcast). For those apps you still need to enable Autostart and set Battery to
"No restrictions" **for that app**.

## Check it with ADB

```sh
adb shell dumpsys alarm | grep -A3 dev.trang.notikeeper                       # alarm armed?
adb shell dumpsys jobscheduler | grep -A5 dev.trang.notikeeper                # Xiaomi build: trigger armed?
adb shell dumpsys activity service com.google.android.gms/.gcm.GcmService | grep -E "connected|Heartbeat|Client HB"
```

## Build

Requires JDK 17+ and Android SDK 35.

```sh
./gradlew assembleRelease     # app/build/outputs/apk/{xiaomi,universal}/release/app-*-release.apk
```

Shared code is in `app/src/main`. Each build's ROM logic is in `app/src/xiaomi` or
`app/src/universal`, behind a `Rom` class with the same members in both.

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
