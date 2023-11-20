package com.appoxee.internal.util

import android.app.Application
import android.content.pm.ApplicationInfo
import android.util.Log

internal class Logger private constructor(application: Application) {
    private val isDebuggable =
        ((application.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0)

    internal companion object {
        @Volatile
        private lateinit var instance: Logger

        fun init(application: Application) {
            instance = Logger(application)
        }

        fun d(tag: String, message: String) {
            print(tag, message, null) { s1, s2, t ->
                Log.d(s1, s2, t)
            }
        }

        fun w(tag: String, message: String, throwable: Throwable? = null) {
            print(tag, message, throwable) { s1, s2, t ->
                Log.w(s1, s2, t)
            }
        }

        fun i(tag: String, message: String, throwable: Throwable? = null) {
            print(tag, message, throwable) { s1, s2, t ->
                Log.i(s1, s2, t)
            }
        }

        fun e(tag: String, message: String, throwable: Throwable? = null) {
            print(tag, message, throwable) { s1, s2, t ->
                Log.e(s1, s2, t)
            }
        }

        private fun print(
            tag: String,
            message: String,
            throwable: Throwable? = null,
            call: (String, String, Throwable?) -> Unit
        ) {
            if (::instance.isInitialized && instance.isDebuggable) {
                val maxLength = 10000
                for (i in message.indices step maxLength) {
                    val msgLength =
                        if (message.length - i > maxLength) i + maxLength else message.length
                    if (i >= msgLength) break
                    val part = message.substring(i, msgLength)
                    call(tag, part, throwable)
                }
            }else{
                call(tag, message, throwable)
            }
        }
    }
}
