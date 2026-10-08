package dev.trang.notikeeper;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.SystemClock;

/**
 * Every tick we poke GMS's push client: GCM_RECONNECT reconnects a dead socket, and a heartbeat
 * detects a silently dropped one. GMS doesn't protect these broadcasts with a permission.
 * ROM-specific behavior lives in each flavor's {@link Rom}.
 */
final class KeepAlive {
    static final String ACTION_TICK = "dev.trang.notikeeper.TICK";
    static final String GMS = "com.google.android.gms";

    private static final String GCM_RECONNECT = "com.google.android.intent.action.GCM_RECONNECT";
    private static final String MCS_HEARTBEAT = "com.google.android.intent.action.MCS_HEARTBEAT";
    private static final String HEARTBEAT_NOW = "com.google.android.gms.gcm.ACTION_HEARTBEAT_NOW";

    /** True while MainActivity is on screen; the process must not exit then. */
    static volatile boolean uiVisible;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final Runnable EXIT = () -> {
        if (!uiVisible) Process.killProcess(Process.myPid());
    };

    private KeepAlive() {}

    /** Exit after prefs are flushed instead of staying cached in RAM; the next alarm restarts us. */
    static void exitSoon() {
        MAIN.removeCallbacks(EXIT);
        MAIN.postDelayed(EXIT, 2000);
    }

    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("state", Context.MODE_PRIVATE);
    }

    static boolean isEnabled(Context c) {
        return prefs(c).getBoolean("enabled", true);
    }

    static void setEnabled(Context c, boolean on) {
        prefs(c).edit().putBoolean("enabled", on).apply();
        arm(c);
    }

    /** Arms (or disarms) the alarm and any ROM trigger. Safe to call repeatedly. */
    static void arm(Context c) {
        boolean on = isEnabled(c);
        Rom.arm(c, on);
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = PendingIntent.getBroadcast(c, 0,
                new Intent(c, KeepAliveReceiver.class).setAction(ACTION_TICK),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (!on) {
            am.cancel(pi);
            prefs(c).edit().remove("nextWall").apply();
            return;
        }
        long delay = Rom.intervalMin(c) * 60_000L;
        am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + delay, pi);
        prefs(c).edit().putLong("nextWall", System.currentTimeMillis() + delay).apply();
    }

    static void ping(Context c, boolean forceHeartbeat) {
        boolean heartbeat = Rom.needsHeartbeat(c) || forceHeartbeat;
        send(c, GCM_RECONNECT);
        if (heartbeat) {
            send(c, MCS_HEARTBEAT);
            send(c, HEARTBEAT_NOW);
        }
        prefs(c).edit().putLong("lastPing", System.currentTimeMillis()).apply();
    }

    private static void send(Context c, String action) {
        c.sendBroadcast(new Intent(action).setPackage(GMS));
    }
}
