package com.appoxee.internal.util

import android.app.Application
import android.content.pm.ApplicationInfo
import android.util.Log
import com.appoxee.sdk.BuildConfig
import com.appoxee.shared.AppoxeeOptions.LogLevel

internal class Logger private constructor(application: Application, logLevel: LogLevel) {
    private val includeSensitiveDetails = BuildConfig.DEBUG &&
        ((application.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0)
    private val isLoggingEnabled =
        logLevel == LogLevel.RELEASE ||
            ((application.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0)

    internal companion object {
        private const val REDACTED = "[details redacted]"
        @Volatile
        private lateinit var instance: Logger

        @JvmStatic
        internal fun init(application: Application, logLevel: LogLevel) {
            instance = Logger(application, logLevel)
        }


        @JvmStatic
        internal fun d(tag: String, message: String, releaseMessage: String = REDACTED) {
            print(tag, message, releaseMessage = releaseMessage) { s1, s2, _ ->
                Log.d(s1, s2)
            }
        }

        @JvmStatic
        internal fun w(tag: String, message: String, throwable: Throwable? = null, releaseMessage: String = REDACTED) {
            print(tag, message, throwable, releaseMessage) { s1, s2, t ->
                Log.w(s1, s2, t)
            }
        }

        @JvmStatic
        internal fun i(tag: String, message: String, throwable: Throwable? = null, releaseMessage: String = REDACTED) {
            print(tag, message, throwable, releaseMessage) { s1, s2, t ->
                Log.i(s1, s2, t)
            }
        }

        @JvmStatic
        internal fun e(tag: String, message: String, throwable: Throwable? = null, releaseMessage: String = REDACTED) {
            print(tag, message, throwable, releaseMessage) { s1, s2, t ->
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
            releaseMessage: String = REDACTED,
            call: (String, String, Throwable?) -> Unit
        ) {
            if (::instance.isInitialized && instance.isLoggingEnabled) {
                // Release diagnostics opt in to a safe summary. Never try to redact
                // arbitrary payloads or exception chains with a field-name denylist.
                val safeMessage = if (instance.includeSensitiveDetails) message else releaseMessage
                val safeThrowable = throwable.takeIf { instance.includeSensitiveDetails }
                val maxLength = 10000
                for (i in safeMessage.indices step maxLength) {
                    val msgLength =
                        if (safeMessage.length - i > maxLength) i + maxLength else safeMessage.length
                    if (i >= msgLength) break
                    val part = safeMessage.substring(i, msgLength)
                    call(tag, part, safeThrowable)
                }
            }
        }
    }
}
