package com.appoxee.internal.util

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle

object CompatExt {
    val PENDING_INTENT_MUTABLE_UPDATE_FLAGS by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
    }

    val PENDING_INTENT_NO_CREATE_FLAGS by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
        } else {
            PendingIntent.FLAG_NO_CREATE
        }
    }

    val PENDING_INTENT_UPDATE_CURRENT_FLAGS by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
    }

    internal fun Activity.overrideTransition(type: Int, enter: Int, close: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            this.overrideActivityTransition(type, enter, close)
        } else {
            @Suppress("DEPRECATION")
            this.overridePendingTransition(enter, close)
        }
    }

    internal inline fun <reified T> Bundle.getParcelableCompat(key: String): T? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val clazz = Class.forName(T::class.java.name)
            this.getParcelable(key, clazz) as T?
        } else {
            @Suppress("DEPRECATION")
            this.getParcelable(key)
        }
    }

    internal inline fun <reified T> Intent.getParcelableExtraCompat(
        key: String,
    ): T? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val clazz = Class.forName(T::class.java.name)
            this.getParcelableExtra(key, clazz) as T?
        } else {
            @Suppress("DEPRECATION")
            this.getParcelableExtra(key)
        }
    }
}