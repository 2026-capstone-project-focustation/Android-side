package net.focustation.myapplication.util

import android.util.Log
import net.focustation.myapplication.BuildConfig

/**
 * 디버그 빌드에서만 로그를 출력하는 경량 래퍼.
 */
object DebugLog {
    private const val TAG = "NaverMap"

    fun d(message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, message)
        }
    }

    fun w(message: String) {
        if (BuildConfig.DEBUG) {
            Log.w(TAG, message)
        }
    }

    fun e(message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            Log.e(TAG, message, throwable)
        }
    }
}

