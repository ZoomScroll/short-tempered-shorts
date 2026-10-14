package com.shorttempered.shorts;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayDeque;

/**
 * Watches YouTube and Instagram. While a Shorts / Reels feed is on screen it counts
 * down and then performs a swipe-up gesture to move to the next video.
 * A small floating panel shows the countdown and lets you pause or skip.
 */
public class AutoScrollService extends AccessibilityService {

    static final String YOUTUBE_PKG = "com.google.android.youtube";
    static final String INSTAGRAM_PKG = "com.instagram.android";

    // View-id fragments that only appear on the Shorts / Reels players.
    private static final String[] YOUTUBE_MARKERS = {":id/reel_"};
    private static final String[] INSTAGRAM_MARKERS = {":id/clips_"};
    private static final int MAX_NODES_SCANNED = 800;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private SharedPreferences prefs;
    private WindowManager windowManager;

    private boolean ticking = false;
    private boolean paused = false;
    private int elapsed = 0;

    private LinearLayout overlay;
    private TextView timerView;
    private TextView pauseView;
    private WindowManager.LayoutParams overlayParams;
    private boolean overlayShown = false;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            onTick();
            if (ticking) handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        prefs = Prefs.get(this);
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        startTicking();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null) return;
        if (isTargetEnabled(event.getPackageName().toString())) startTicking();
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public void onDestroy() {
        ticking = false;
        handler.removeCallbacks(tick);
        hideOverlay();
        super.onDestroy();
    }

    private void startTicking() {
        if (ticking || prefs == null) return;
        ticking = true;
        handler.removeCallbacks(tick);
        handler.postDelayed(tick, 1000);
    }

    private boolean isTargetEnabled(String pkg) {
        if (prefs == null) return false;
        if (YOUTUBE_PKG.equals(pkg)) return prefs.getBoolean(Prefs.YOUTUBE, true);
        if (INSTAGRAM_PKG.equals(pkg)) return prefs.getBoolean(Prefs.INSTAGRAM, true);
        return false;
    }

    private void onTick() {
        AccessibilityNodeInfo root = null;
        try {
            root = getRootInActiveWindow();
        } catch (Exception ignored) {
        }
        String pkg = (root != null && root.getPackageName() != null)
                ? root.getPackageName().toString() : null;

        // Left the app (or turned it off in settings): go quiet until it's opened again.
        if (pkg == null || !isTargetEnabled(pkg)) {
            hideOverlay();
            elapsed = 0;
            ticking = false;
            return;
        }

        boolean onFeed = !prefs.getBoolean(Prefs.ONLY_SHORTS, true) || isShortsScreen(root, pkg);
        if (!onFeed) {
            hideOverlay();
            elapsed = 0;
            return;
        }

        int interval = clampInterval(prefs.getInt(Prefs.INTERVAL, Prefs.DEFAULT_INTERVAL));
        if (prefs.getBoolean(Prefs.SHOW_OVERLAY, true)) showOverlay(); else hideOverlay();

        if (!paused) {
            elapsed++;
            if (elapsed >= interval) swipeNext();
        }
        updateOverlay(interval);
    }

    private static int clampInterval(int v) {
        return Math.max(Prefs.MIN_INTERVAL, Math.min(Prefs.MAX_INTERVAL, v));
    }

    private boolean isShortsScreen(AccessibilityNodeInfo root, String pkg) {
        String[] markers = YOUTUBE_PKG.equals(pkg) ? YOUTUBE_MARKERS : INSTAGRAM_MARKERS;
        ArrayDeque<AccessibilityNodeInfo> queue = new ArrayDeque<>();
        queue.add(root);
        int seen = 0;
        while (!queue.isEmpty() && seen < MAX_NODES_SCANNED) {
            AccessibilityNodeInfo node = queue.poll();
            seen++;
            String id = node.getViewIdResourceName();
            if (id != null) {
                for (String m : markers) {
                    if (id.contains(m)) return true;
                }
            }
            int count = node.getChildCount();
            for (int i = 0; i < count; i++) {
                AccessibilityNodeInfo child = node.getChild(i);
                if (child != null) queue.add(child);
            }
        }
        return false;
    }

    private void swipeNext() {
        elapsed = 0;
        DisplayMetrics dm = getResources().getDisplayMetrics();
        float x = dm.widthPixels * 0.5f;
        Path path = new Path();
        path.moveTo(x, dm.heightPixels * 0.70f);
        path.lineTo(x, dm.heightPixels * 0.25f);
        GestureDescription.StrokeDescription stroke =
                new GestureDescription.StrokeDescription(path, 0, 220);
        GestureDescription gesture = new GestureDescription.Builder().addStroke(stroke).build();
        dispatchGesture(gesture, null, null);
    }

    // ---------- floating panel ----------

    private int dp(float v) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    private TextView makeLabel(String text, float sizeSp) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(Color.WHITE);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(6), dp(6), dp(6), dp(6));
        t.setMinWidth(dp(40));
        return t;
    }

    private void buildOverlay() {
        overlay = new LinearLayout(this);
        overlay.setOrientation(LinearLayout.VERTICAL);
        overlay.setGravity(Gravity.CENTER_HORIZONTAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xB3000000);
        bg.setCornerRadius(dp(22));
        overlay.setBackground(bg);
        overlay.setPadding(dp(4), dp(6), dp(4), dp(6));

        timerView = makeLabel("", 13);
        pauseView = makeLabel("⏸", 22);
        TextView skipView = makeLabel("⏭", 22);

        overlay.addView(timerView);
        overlay.addView(pauseView);
        overlay.addView(skipView);

        pauseView.setOnClickListener(v -> {
            paused = !paused;
            updateOverlay(clampInterval(prefs.getInt(Prefs.INTERVAL, Prefs.DEFAULT_INTERVAL)));
        });
        skipView.setOnClickListener(v -> swipeNext());

        // Drag the panel around by its timer.
        timerView.setOnTouchListener(new View.OnTouchListener() {
            float startX, startY;
            int origX, origY;

            @Override
            public boolean onTouch(View v, MotionEvent e) {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = e.getRawX();
                        startY = e.getRawY();
                        origX = overlayParams.x;
                        origY = overlayParams.y;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        overlayParams.x = origX + (int) (e.getRawX() - startX);
                        overlayParams.y = origY + (int) (e.getRawY() - startY);
                        if (overlayShown) {
                            try {
                                windowManager.updateViewLayout(overlay, overlayParams);
                            } catch (Exception ignored) {
                            }
                        }
                        return true;
                    default:
                        return true;
                }
            }
        });

        overlayParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        overlayParams.gravity = Gravity.TOP | Gravity.START;
        overlayParams.x = dp(6);
        overlayParams.y = (int) (getResources().getDisplayMetrics().heightPixels * 0.35f);
    }

    private void showOverlay() {
        if (overlayShown || windowManager == null) return;
        if (overlay == null) buildOverlay();
        try {
            windowManager.addView(overlay, overlayParams);
            overlayShown = true;
        } catch (Exception ignored) {
        }
    }

    private void hideOverlay() {
        if (!overlayShown || windowManager == null) return;
        try {
            windowManager.removeView(overlay);
        } catch (Exception ignored) {
        }
        overlayShown = false;
    }

    private void updateOverlay(int interval) {
        if (overlay == null) return;
        timerView.setText(paused ? "off" : (Math.max(0, interval - elapsed) + "s"));
        pauseView.setText(paused ? "▶" : "⏸");
    }
}
