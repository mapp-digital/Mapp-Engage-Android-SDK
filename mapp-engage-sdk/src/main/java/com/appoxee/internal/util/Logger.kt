package com.appoxee.internal.util

import android.app.Application
import android.content.pm.ApplicationInfo
import android.util.Log
import com.appoxee.shared.AppoxeeOptions.LogLevel

internal class Logger private constructor(application: Application, logLevel: LogLevel) {
    private val isLoggingEnabled =
        logLevel == LogLevel.RELEASE ||
            ((application.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0)

    internal companion object {
        @Volatile
        private lateinit var instance: Logger

        @JvmStatic
        internal fun init(application: Application, logLevel: LogLevel) {
            instance = Logger(application, logLevel)
        }


        @JvmStatic
        internal fun d(tag: String, message: String) {
            print(tag, message) { s1, s2, _ ->
                Log.d(s1, s2)
            }
        }

        @JvmStatic
        internal fun w(tag: String, message: String, throwable: Throwable? = null) {
            print(tag, message, throwable) { s1, s2, t ->
                Log.w(s1, s2, t)
            }
        }

        @JvmStatic
        internal fun i(tag: String, message: String, throwable: Throwable? = null) {
            print(tag, message, throwable) { s1, s2, t ->
                Log.i(s1, s2, t)
            }
        }

        @JvmStatic
        internal fun e(tag: String, message: String, throwable: Throwable? = null) {
            print(tag, message, throwable) { s1, s2, t ->
                Log.e(s1, s2, t)
            }
        }

        @JvmStatic
        internal fun e(tag: String, throwable: Throwable? = null) {
            print(tag, throwable?.message ?: "", throwable) { s1, s2, t ->
                Log.e(s1, s2, t)
            }
        }

        private fun print(
            tag: String,
            message: String,
            throwable: Throwable? = null,
            call: (String, String, Throwable?) -> Unit
        ) {
            if (::instance.isInitialized && instance.isLoggingEnabled) {
                val maxLength = 10000
                for (i in message.indices step maxLength) {
                    val msgLength =
                        if (message.length - i > maxLength) i + maxLength else message.length
                    if (i >= msgLength) break
                    val part = message.substring(i, msgLength)
                    call(tag, part, throwable)
                }
            }
        }
    }
}
