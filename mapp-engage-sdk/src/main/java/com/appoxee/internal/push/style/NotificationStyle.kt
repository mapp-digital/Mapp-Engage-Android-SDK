package com.appoxee.internal.push.style

import androidx.core.app.NotificationCompat

interface NotificationStyle {
    fun getStyle(): NotificationCompat.Style
}