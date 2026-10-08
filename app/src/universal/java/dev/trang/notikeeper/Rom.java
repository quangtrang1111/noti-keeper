package dev.trang.notikeeper;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.BatteryManager;
import android.os.PowerManager;
import android.provider.Settings;

/** Build for every other ROM (Oppo, vivo, Honor, Xiaomi Global…). Same members as the Xiaomi build's Rom. */
final class Rom {
    static final String ABOUT = "Keeps notifications on time by checking Google Play Services' push "
            + "connection. How often it checks adapts to charging, battery and network.";

    static final String[] AUTOSTART_SCREENS = {
            "com.miui.securitycenter/com.miui.permcenter.autostart.AutoStartManagementActivity",
            "com.coloros.safecenter/.permission.startup.StartupAppListActivity",
            "com.coloros.safecenter/.startupapp.StartupAppListActivity",
            "com.oplus.safecenter/.permission.startup.StartupAppListActivity",
            "com.oppo.safe/.permission.startup.StartupAppListActivity",
            "com.vivo.permissionmanager/.activity.BgStartUpManagerActivity",
            "com.iqoo.secure/.ui.phoneoptimize.BgStartUpManager",
            "com.hihonor.systemmanager/.startupmgr.ui.StartupNormalAppListActivity",
    };

    private Rom() {}

    static void arm(Context c, boolean on) {}

    /** Re-evaluated at every tick, so plugging in or switching network applies from the next check. */
    static int intervalMin(Context c) {
        BatteryManager bm = c.getSystemService(BatteryManager.class);
        if (bm.isCharging()) return 5;
        if (c.getSystemService(PowerManager.class).isPowerSaveMode()
                || bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) < 15) return 30;
        // Carriers drop idle connections sooner than home routers.
        ConnectivityManager cm = c.getSystemService(ConnectivityManager.class);
        NetworkCapabilities nc = cm.getNetworkCapabilities(cm.getActiveNetwork());
        return nc != null && nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ? 10 : 15;
    }

    /** Nothing keeps GMS unfrozen here, so always heartbeat. */
    static boolean needsHeartbeat(Context c) {
        return true;
    }

    static void onOpen(Context c) {}

    /** No writable no-freeze list: the user sets GMS battery to unrestricted by hand. */
    static String setupButton(Context c) {
        return "Google Play Services: battery & autostart";
    }

    static Intent setupIntent(Context c) {
        return new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, MainActivity.pkgUri(KeepAlive.GMS));
    }

    static void status(Context c, StringBuilder s) {}
}
