package com.appoxee.internal.push.style

import androidx.core.app.NotificationCompat

interface NotificationStyle {

    suspend fun getStyle(): NotificationCompat.Style
}