package com.appoxee.internal.ui.push.style

import androidx.core.app.NotificationCompat

internal fun interface NotificationStyle {
    fun getStyle(): NotificationCompat.Style
}