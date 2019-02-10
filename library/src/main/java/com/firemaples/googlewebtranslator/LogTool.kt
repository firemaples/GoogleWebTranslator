package com.firemaples.googlewebtranslator

import android.util.Log

class LogTool {
    companion object {
        @JvmStatic
        fun d(tag: String, msg: String) {
            if (!DEBUG) {
                return
            }

            Log.d(tag, msg)
        }

        @JvmStatic
        fun i(tag: String, msg: String) {
            if (!DEBUG) {
                return
            }

            Log.i(tag, msg)
        }

        @JvmStatic
        fun w(tag: String, msg: String) {
            if (!DEBUG) {
                return
            }

            Log.w(tag, msg)
        }

        @JvmStatic
        fun e(tag: String, msg: String) {
            if (!DEBUG) {
                return
            }

            Log.e(tag, msg)
        }
    }
}
