package dev.trang.notikeeper;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

/** Xiaomi HyperOS / MIUI China build. Same members as the universal build's Rom. */
final class Rom {
    static final String ABOUT = "Keeps notifications on time on Xiaomi HyperOS / MIUI China. It stops "
            + "the system from freezing Google Play Services and checks its push connection.";

    static final String[] AUTOSTART_SCREENS = {
            "com.miui.securitycenter/com.miui.permcenter.autostart.AutoStartManagementActivity",
    };

    /** Shown below the setup buttons; none here. */
    static final String SETUP_NOTE = null;

    private Rom() {}

    static void arm(Context c, boolean on) {
        GuardJob.arm(c, on);
    }

    /** GuardJob repairs within ~1 s, so the alarm is only a backup. */
    static int intervalMin(Context c) {
        return 30;
    }

    /** Also repairs the list. An unfrozen GMS keeps its connection alive, so skip the radio wake-up. */
    static boolean needsHeartbeat(Context c) {
        return XiaomiGuard.check(c, true) != XiaomiGuard.PROTECTED;
    }

    /** Repairs right after the user grants "Modify system settings". */
    static void onOpen(Context c) {
        XiaomiGuard.repair(c);
    }

    static String setupButton(Context c) {
        return XiaomiGuard.check(c, false) != XiaomiGuard.NOT_APPLICABLE
                ? "3. Allow \"Modify system settings\"" : null;
    }

    static Intent setupIntent(Context c) {
        return new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, MainActivity.pkgUri(c.getPackageName()));
    }

    static void status(Context c, StringBuilder s) {
        switch (XiaomiGuard.check(c, false)) {
            case XiaomiGuard.PROTECTED:
                s.append("\nHyperOS no-freeze list: GMS protected");
                break;
            case XiaomiGuard.MISSING:
                s.append("\nHyperOS no-freeze list: GMS missing (fixing)");
                break;
            case XiaomiGuard.NEEDS_PERMISSION:
                s.append("\nHyperOS no-freeze list: GMS missing, allow \"Modify system settings\"");
                break;
            default:
                s.append("\nNo HyperOS China freeze list on this phone: install the Universal build");
        }
    }
}
