package com.masatolab.hole;

import android.content.Context;
import android.content.SharedPreferences;

public final class Prefs {
    private static final String FILE = "hole_settings";
    private static final String KEY_ALPHA = "alpha";
    private static final String KEY_SCALE = "scale";
    private static final String KEY_OFFSET_X = "offset_x";
    private static final String KEY_OFFSET_Y = "offset_y";
    private static final String KEY_RUNNING = "running";

    private Prefs() {}

    private static SharedPreferences p(Context c) {
        return c.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static float alpha(Context c) { return p(c).getFloat(KEY_ALPHA, 0.50f); }
    public static void alpha(Context c, float value) { p(c).edit().putFloat(KEY_ALPHA, value).apply(); }

    public static float scale(Context c) { return p(c).getFloat(KEY_SCALE, 1.00f); }
    public static void scale(Context c, float value) { p(c).edit().putFloat(KEY_SCALE, value).apply(); }

    public static float offsetX(Context c) { return p(c).getFloat(KEY_OFFSET_X, 0.00f); }
    public static void offsetX(Context c, float value) { p(c).edit().putFloat(KEY_OFFSET_X, value).apply(); }

    public static float offsetY(Context c) { return p(c).getFloat(KEY_OFFSET_Y, 0.00f); }
    public static void offsetY(Context c, float value) { p(c).edit().putFloat(KEY_OFFSET_Y, value).apply(); }

    public static boolean running(Context c) { return p(c).getBoolean(KEY_RUNNING, false); }
    public static void running(Context c, boolean value) { p(c).edit().putBoolean(KEY_RUNNING, value).apply(); }

    public static void resetCalibration(Context c) {
        p(c).edit()
                .putFloat(KEY_SCALE, 1.00f)
                .putFloat(KEY_OFFSET_X, 0.00f)
                .putFloat(KEY_OFFSET_Y, 0.00f)
                .apply();
    }
}
