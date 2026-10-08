package dev.trang.notikeeper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Alarm tick and boot/update hook. */
public class KeepAliveReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent intent) {
        String action = intent.getAction();
        if (KeepAlive.isEnabled(c)) {
            // ROM triggers don't survive a reboot, and PowerKeeper rebuilds the no-freeze list at boot.
            if (KeepAlive.ACTION_TICK.equals(action) || Intent.ACTION_BOOT_COMPLETED.equals(action)) {
                KeepAlive.ping(c, false);
            }
            KeepAlive.arm(c);
        }
        KeepAlive.exitSoon();
    }
}
