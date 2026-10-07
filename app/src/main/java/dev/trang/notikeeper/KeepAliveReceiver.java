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
            // PowerKeeper rebuilds the no-freeze list at boot, so repair right away.
            if (KeepAlive.ACTION_TICK.equals(action) || Intent.ACTION_BOOT_COMPLETED.equals(action)) {
                KeepAlive.ping(c, false);
            }
            KeepAlive.schedule(c);
        }
        KeepAlive.exitSoon();
    }
}
