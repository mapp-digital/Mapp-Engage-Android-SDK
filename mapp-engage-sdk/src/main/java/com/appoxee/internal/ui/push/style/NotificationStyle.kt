package com.appoxee.internal.ui.push.style

import androidx.core.app.NotificationCompat

internal fun interface NotificationStyle {
    suspend fun getStyle(): NotificationCompat.Style
}
