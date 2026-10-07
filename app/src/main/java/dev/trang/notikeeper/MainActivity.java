package dev.trang.notikeeper;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

/** Single screen, built in code (no layouts / AndroidX) to keep the APK tiny. */
public class MainActivity extends Activity implements View.OnClickListener,
        CompoundButton.OnCheckedChangeListener, RadioGroup.OnCheckedChangeListener {

    /** OEM "autostart" screens ("package/class"), tried in order; falls back to App info. */
    private static final String[] AUTOSTART_SCREENS = {
            "com.miui.securitycenter/com.miui.permcenter.autostart.AutoStartManagementActivity",
            "com.coloros.safecenter/.permission.startup.StartupAppListActivity",
            "com.coloros.safecenter/.startupapp.StartupAppListActivity",
            "com.oplus.safecenter/.permission.startup.StartupAppListActivity",
            "com.oppo.safe/.permission.startup.StartupAppListActivity",
            "com.vivo.permissionmanager/.activity.BgStartUpManagerActivity",
            "com.iqoo.secure/.ui.phoneoptimize.BgStartUpManager",
    };

    // Outside the radio ids (5..30 = minutes).
    private static final int BTN_CHECK = 101, BTN_WRITE_SETTINGS = 102, BTN_BATTERY = 103,
            BTN_AUTOSTART = 104, BTN_GMS = 105, BTN_DONATE = 106;

    private static final String DONATE_URL = "https://ko-fi.com/quangtrang1111";

    private TextView status;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        int pad = dp(16);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);

        TextView title = text(root, "Noti Keeper", 22);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        text(root, "Helps notifications arrive on time without running in the background. "
                + "Every few minutes it checks Google Play Services' push connection; on Xiaomi "
                + "HyperOS China it also keeps Google Play Services from being frozen.", 13);

        Switch enabled = new Switch(this);
        enabled.setText("Enabled");
        enabled.setTextSize(16);
        enabled.setChecked(KeepAlive.isEnabled(this));
        enabled.setPadding(0, dp(12), 0, dp(4));
        enabled.setOnCheckedChangeListener(this);
        root.addView(enabled);

        text(root, "Check every:", 14);
        RadioGroup group = new RadioGroup(this);
        group.setOrientation(RadioGroup.HORIZONTAL);
        int current = KeepAlive.intervalMin(this);
        for (int m : KeepAlive.INTERVALS_MIN) {
            RadioButton rb = new RadioButton(this);
            rb.setId(m);
            rb.setText(m + "m");
            rb.setChecked(m == current);
            group.addView(rb);
        }
        group.setOnCheckedChangeListener(this);
        root.addView(group);

        status = text(root, "", 13);
        status.setPadding(0, dp(8), 0, dp(8));

        button(root, BTN_CHECK, "Check now");
        button(root, BTN_BATTERY, "Battery: don't optimize this app");
        button(root, BTN_AUTOSTART, "Autostart settings (Xiaomi / Oppo / vivo)");
        if (XiaomiGuard.check(this, false) != XiaomiGuard.NOT_APPLICABLE) {
            button(root, BTN_WRITE_SETTINGS, "Xiaomi: allow \"Modify system settings\"");
        }
        // Oppo / vivo: the user must set GMS battery to unrestricted by hand.
        button(root, BTN_GMS, "GPS: battery & autostart (Oppo / vivo)");
        Button donate = button(root, BTN_DONATE, "Love this app? Buy me a coffee!");
        donate.setBackgroundTintList(ColorStateList.valueOf(0xFFFF5E5B));
        donate.setTextColor(Color.WHITE);

        ScrollView scroll = new ScrollView(this);
        scroll.setFitsSystemWindows(true);
        scroll.addView(root);
        setContentView(scroll);

        // Re-arm the alarm, e.g. after a force-stop.
        KeepAlive.schedule(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        KeepAlive.uiVisible = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        KeepAlive.uiVisible = false;
        KeepAlive.exitSoon();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Also repairs right after the user grants "Modify system settings".
        if (XiaomiGuard.IS_XIAOMI) KeepAlive.ping(this, false);
        refresh();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case BTN_CHECK:
                KeepAlive.ping(this, true);
                refresh();
                break;
            case BTN_WRITE_SETTINGS:
                open(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, pkgUri(getPackageName())));
                break;
            case BTN_BATTERY:
                if (!tryStart(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        pkgUri(getPackageName())))) {
                    open(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
                }
                break;
            case BTN_AUTOSTART:
                for (String screen : AUTOSTART_SCREENS) {
                    if (tryStart(new Intent().setComponent(ComponentName.unflattenFromString(screen)))) return;
                }
                open(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkgUri(getPackageName())));
                break;
            case BTN_GMS:
                open(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkgUri(KeepAlive.GMS)));
                break;
            case BTN_DONATE:
                open(new Intent(Intent.ACTION_VIEW, Uri.parse(DONATE_URL)));
                break;
        }
    }

    /** "Enabled" switch. */
    @Override
    public void onCheckedChanged(CompoundButton b, boolean on) {
        KeepAlive.setEnabled(this, on);
        refresh();
    }

    /** Interval radio group; button ids are the minutes. */
    @Override
    public void onCheckedChanged(RadioGroup g, int id) {
        KeepAlive.setInterval(this, id);
        refresh();
    }

    private void refresh() {
        SharedPreferences p = KeepAlive.prefs(this);
        long next = p.getLong("nextWall", 0);
        long last = p.getLong("lastPing", 0);
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);

        StringBuilder s = new StringBuilder("Next check: ")
                .append(KeepAlive.isEnabled(this) && next > 0 ? DateFormat.format("HH:mm:ss", next) : "off")
                .append("\nLast check: ").append(last > 0 ? DateFormat.format("HH:mm:ss", last) : "never")
                .append("\nBattery optimization: ").append(pm.isIgnoringBatteryOptimizations(getPackageName())
                        ? "off (good)" : "ON (ROM may delay checks)")
                .append("\nGoogle Play Services: ");
        try {
            s.append(getPackageManager().getPackageInfo(KeepAlive.GMS, 0).versionName);
        } catch (Exception e) {
            s.append("NOT INSTALLED (this app cannot help)");
        }
        switch (XiaomiGuard.check(this, false)) {
            case XiaomiGuard.PROTECTED:
                s.append("\nHyperOS no-freeze list: GMS protected");
                break;
            case XiaomiGuard.MISSING:
                s.append("\nHyperOS no-freeze list: GMS missing (fixed on next check)");
                break;
            case XiaomiGuard.NEEDS_PERMISSION:
                s.append("\nHyperOS no-freeze list: GMS missing, allow \"Modify system settings\"");
                break;
        }
        status.setText(s);
    }

    private void open(Intent i) {
        if (!tryStart(i)) Toast.makeText(this, "Screen not available on this ROM", Toast.LENGTH_SHORT).show();
    }

    private boolean tryStart(Intent i) {
        try {
            startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static Uri pkgUri(String pkg) {
        return Uri.parse("package:" + pkg);
    }

    private TextView text(LinearLayout parent, String s, int sp) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        parent.addView(t);
        return t;
    }

    private Button button(LinearLayout parent, int id, String label) {
        Button b = new Button(this);
        b.setId(id);
        b.setText(label);
        b.setAllCaps(false);
        b.setOnClickListener(this);
        parent.addView(b);
        return b;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}
