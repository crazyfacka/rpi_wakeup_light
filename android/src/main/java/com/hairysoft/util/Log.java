package com.hairysoft.util;

/**
 * Class to override Android default logging class.
 * This way I could disable the logging in the final APK without having to remove them throughout the application.
 */
public class Log {

    private static final boolean l = false;

    private Log() { }

    public static void d(String tag, String message) {
        if(!l) return;
        android.util.Log.d(tag, message);
    }

    public static void e(String tag, String message) {
        if(!l) return;
        android.util.Log.e(tag, message);
    }

    public static void e(String tag, String message, Throwable tr) {
        if(!l) return;
        android.util.Log.e(tag, message, tr);
    }

}
