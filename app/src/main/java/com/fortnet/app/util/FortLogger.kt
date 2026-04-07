package com.fortnet.app.util

import android.util.Log

object FortLogger {
    private const val TAG = "FortNet_Log"

    fun d(message: String) {
        Log.d(TAG, "[DEBUG] $message")
    }

    fun i(message: String) {
        Log.i(TAG, "[INFO] $message")
    }

    fun e(message: String, throwable: Throwable? = null) {
        Log.e(TAG, "[ERROR] $message", throwable)
    }

    fun w(message: String) {
        Log.w(TAG, "[WARN] $message")
    }
}
