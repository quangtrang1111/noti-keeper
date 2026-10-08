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
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

/** Single screen, built in code (no layouts / AndroidX) to keep the APK tiny. */
public class MainActivity extends Activity implements View.OnClickListener,
        CompoundButton.OnCheckedChangeListener {

    private static final int BTN_CHECK = 1, BTN_BATTERY = 2, BTN_AUTOSTART = 3, BTN_ROM = 4,
            BTN_DONATE = 5;

    private static final String DONATE_URL = "https://ko-fi.com/quangtrang1111/?hidefeed=true&widget=true&embed=true";

    private TextView headline, status;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        int pad = dp(16);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);

        TextView title = text(root, "Noti Keeper", 22);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        text(root, Rom.ABOUT, 13);

        Switch enabled = new Switch(this);
        enabled.setText("Enabled");
        enabled.setTextSize(16);
        enabled.setChecked(KeepAlive.isEnabled(this));
        enabled.setPadding(0, dp(12), 0, dp(4));
        enabled.setOnCheckedChangeListener(this);
        root.addView(enabled);

        headline = text(root, "", 16);
        status = text(root, "", 13);
        status.setPadding(0, dp(8), 0, dp(8));

        button(root, BTN_CHECK, "Check now");
        button(root, BTN_BATTERY, "Battery: allow running on time");
        button(root, BTN_AUTOSTART, "Autostart settings");
        String romButton = Rom.setupButton(this);
        if (romButton != null) button(root, BTN_ROM, romButton);
        Button donate = button(root, BTN_DONATE, "Love this app? Buy me a coffee!");
        donate.setBackgroundTintList(ColorStateList.valueOf(0xFFFF5E5B));
        donate.setTextColor(Color.WHITE);

        ScrollView scroll = new ScrollView(this);
        scroll.setFitsSystemWindows(true);
        scroll.addView(root);
        setContentView(scroll);

        // Re-arm, e.g. after a force-stop.
        KeepAlive.arm(this);
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
        Rom.onOpen(this);
        refresh();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case BTN_CHECK:
                KeepAlive.ping(this, true);
                refresh();
                break;
            case BTN_BATTERY:
                if (!tryStart(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        pkgUri(getPackageName())))) {
                    open(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
                }
                break;
            case BTN_AUTOSTART:
                for (String screen : Rom.AUTOSTART_SCREENS) {
                    if (tryStart(new Intent().setComponent(ComponentName.unflattenFromString(screen)))) return;
                }
                open(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkgUri(getPackageName())));
                break;
            case BTN_ROM:
                open(Rom.setupIntent(this));
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

    private void refresh() {
        SharedPreferences p = KeepAlive.prefs(this);
        long next = p.getLong("nextWall", 0);
        long last = p.getLong("lastPing", 0);
        headline.setText(KeepAlive.isEnabled(this) && next > 0
                ? "Keeping your notifications on time. Next check: " + DateFormat.format("HH:mm", next)
                : "Paused");

        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        StringBuilder s = new StringBuilder("Last check: ")
                .append(last > 0 ? DateFormat.format("HH:mm:ss", last) : "never")
                .append("\nBattery optimization: ").append(pm.isIgnoringBatteryOptimizations(getPackageName())
                        ? "off (good)" : "ON (ROM may delay checks)")
                .append("\nGoogle Play Services: ");
        try {
            s.append(getPackageManager().getPackageInfo(KeepAlive.GMS, 0).versionName);
        } catch (Exception e) {
            s.append("NOT INSTALLED (this app cannot help)");
        }
        Rom.status(this, s);
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

    static Uri pkgUri(String pkg) {
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
