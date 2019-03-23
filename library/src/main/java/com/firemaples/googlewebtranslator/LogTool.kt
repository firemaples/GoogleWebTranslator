package com.firemaples.googlewebtranslator

import android.util.Log

class LogTool {
    companion object {
        @JvmStatic
        fun d(tag: String, msg: String, force: Boolean = false) {
            if (!Tools.DEBUG && !force) {
                return
            }

            Log.d(tag, msg)
        }

        @JvmStatic
        fun i(tag: String, msg: String, force: Boolean = false) {
            if (!Tools.DEBUG && !force) {
                return
            }

            Log.i(tag, msg)
        }

        @JvmStatic
        fun w(tag: String, msg: String, force: Boolean = false) {
            if (!Tools.DEBUG && !force) {
                return
            }

            Log.w(tag, msg)
        }

        @JvmStatic
        fun e(tag: String, msg: String, force: Boolean = false) {
            if (!Tools.DEBUG && !force) {
                return
            }

            Log.e(tag, msg)
        }
    }
}
