package com.firemaples.googlewebtranslator;

import android.util.Log;

public class LogTool {
    public static boolean _debug = true;

    public static void setDebug(boolean debug) {
        _debug = debug;
    }

    public static boolean isDebug() {
        return _debug;
    }

    public static void d(String tag, String msg) {
        if (!_debug) {
            return;
        }

        Log.d(tag, msg);
    }

    public static void i(String tag, String msg) {
        if (!_debug) {
            return;
        }

        Log.i(tag, msg);
    }

    public static void w(String tag, String msg) {
        if (!_debug) {
            return;
        }

        Log.w(tag, msg);
    }

    public static void e(String tag, String msg) {
        if (!_debug) {
            return;
        }

        Log.e(tag, msg);
    }
}
