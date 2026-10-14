package com.shorttempered.shorts;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.TypedValue;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

public class MainActivity extends Activity {

    private SharedPreferences prefs;
    private TextView status;
    private Button enableButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = Prefs.get(this);

        ScrollView scroll = new ScrollView(this);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(20), dp(20), dp(20), dp(32));
        scroll.addView(col);

        TextView title = text("Short Tempered Shorts", 26);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        col.addView(title);
        col.addView(text("Scrolls YouTube Shorts and Instagram Reels for you.", 15));

        status = text("", 16);
        status.setTypeface(Typeface.DEFAULT_BOLD);
        status.setPadding(0, dp(20), 0, dp(8));
        col.addView(status);

        enableButton = new Button(this);
        enableButton.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        col.addView(enableButton);

        col.addView(heading("Seconds per video"));
        TextView intervalLabel = text("", 16);
        col.addView(intervalLabel);
        SeekBar seek = new SeekBar(this);
        seek.setMax(Prefs.MAX_INTERVAL - Prefs.MIN_INTERVAL);
        int current = prefs.getInt(Prefs.INTERVAL, Prefs.DEFAULT_INTERVAL);
        seek.setProgress(current - Prefs.MIN_INTERVAL);
        intervalLabel.setText(current + " seconds, then next video");
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                int value = progress + Prefs.MIN_INTERVAL;
                intervalLabel.setText(value + " seconds, then next video");
                prefs.edit().putInt(Prefs.INTERVAL, value).apply();
            }

            @Override
            public void onStartTrackingTouch(SeekBar s) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar s) {
            }
        });
        col.addView(seek);

        col.addView(heading("Where it works"));
        addSwitch(col, "YouTube Shorts", Prefs.YOUTUBE, true);
        addSwitch(col, "Instagram Reels", Prefs.INSTAGRAM, true);

        col.addView(heading("Options"));
        addSwitch(col, "Only scroll on the Shorts / Reels screen", Prefs.ONLY_SHORTS, true);
        col.addView(small("If it stops working after a YouTube or Instagram update, turn this off. "
                + "It will then scroll anywhere in those apps, so use the pause button."));
        addSwitch(col, "Show floating pause / skip buttons", Prefs.SHOW_OVERLAY, true);
        col.addView(small("Drag the panel by its timer to move it."));

        col.addView(heading("Can't switch it on?"));
        col.addView(small("On Android 13 and newer, apps installed from a file are blocked from "
                + "accessibility until you allow it: tap the button below, then the ⋮ menu "
                + "(top right) → \"Allow restricted settings\". Then come back and turn it on."));
        Button appInfo = new Button(this);
        appInfo.setText("Open app info");
        appInfo.setOnClickListener(v -> startActivity(new Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", getPackageName(), null))));
        col.addView(appInfo);

        setContentView(scroll);
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean on = isServiceEnabled();
        status.setText(on ? "✅ Auto-scroll is ON" : "⚪ Auto-scroll is OFF");
        enableButton.setText(on ? "Accessibility settings" : "Turn on auto-scroll");
    }

    private boolean isServiceEnabled() {
        String enabled = Settings.Secure.getString(
                getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabled == null) return false;
        ComponentName cn = new ComponentName(this, AutoScrollService.class);
        return enabled.contains(cn.flattenToString()) || enabled.contains(cn.flattenToShortString());
    }

    private void addSwitch(LinearLayout col, String label, String key, boolean def) {
        Switch sw = new Switch(this);
        sw.setText(label);
        sw.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        sw.setChecked(prefs.getBoolean(key, def));
        sw.setPadding(0, dp(10), 0, dp(10));
        sw.setOnCheckedChangeListener((b, checked) -> prefs.edit().putBoolean(key, checked).apply());
        col.addView(sw);
    }

    private TextView text(String s, float sp) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        return t;
    }

    private TextView heading(String s) {
        TextView t = text(s, 18);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(0, dp(24), 0, dp(4));
        return t;
    }

    private TextView small(String s) {
        TextView t = text(s, 13);
        t.setTextColor(Color.GRAY);
        return t;
    }

    private int dp(float v) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }
}
