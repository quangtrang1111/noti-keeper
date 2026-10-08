package dev.trang.notikeeper;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

/**
 * HyperOS China freezes GMS after screen-off, killing the FCM socket. Apps in MILLET_NO_RESTRICT_APP
 * are never frozen, but PowerKeeper keeps dropping GMS from it, so we re-add it whenever it changes.
 * Needs WRITE_SETTINGS and targetSdk < 23. See https://github.com/dingwen07/hyperos-fcm-fix
 */
final class XiaomiGuard {
    static final int NOT_APPLICABLE = 0, PROTECTED = 1, MISSING = 2, REPAIRED = 3, NEEDS_PERMISSION = 4;

    /** Redmi and POCO devices also report "Xiaomi". */
    static final boolean IS_XIAOMI = "xiaomi".equalsIgnoreCase(Build.MANUFACTURER);
    private static final String KEY = "MILLET_NO_RESTRICT_APP";
    static final Uri URI = Settings.System.getUriFor(KEY);

    private XiaomiGuard() {}

    /** With {@code fix}, re-adds GMS if it's missing. */
    static int check(Context c, boolean fix) {
        if (!IS_XIAOMI) return NOT_APPLICABLE;
        try {
            ContentResolver r = c.getContentResolver();
            String value = Settings.System.getString(r, KEY);
            if (value == null) return NOT_APPLICABLE;
            value = value.trim();
            if (("," + value.replace(" ", "") + ",").contains("," + KeepAlive.GMS + ",")) return PROTECTED;
            if (!Settings.System.canWrite(c)) return NEEDS_PERMISSION;
            if (!fix) return MISSING;
            String fixed = value.isEmpty() ? KeepAlive.GMS : value + "," + KeepAlive.GMS;
            return Settings.System.putString(r, KEY, fixed) ? REPAIRED : MISSING;
        } catch (Exception e) {
            return NOT_APPLICABLE;
        }
    }

    /** GMS was probably frozen with a dead socket, so reconnect it right after a repair. */
    static void repair(Context c) {
        if (check(c, true) == REPAIRED) KeepAlive.ping(c, true);
    }
}
