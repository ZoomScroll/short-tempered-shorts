package com.shorttempered.shorts;

import android.content.Context;
import android.content.SharedPreferences;

final class Prefs {
    static final String INTERVAL = "interval";
    static final String ONLY_SHORTS = "only_shorts";
    static final String YOUTUBE = "youtube";
    static final String INSTAGRAM = "instagram";
    static final String SHOW_OVERLAY = "show_overlay";

    static final int DEFAULT_INTERVAL = 15;
    static final int MIN_INTERVAL = 3;
    static final int MAX_INTERVAL = 90;

    private Prefs() {}

    static SharedPreferences get(Context c) {
        return c.getSharedPreferences("settings", Context.MODE_PRIVATE);
    }
}
