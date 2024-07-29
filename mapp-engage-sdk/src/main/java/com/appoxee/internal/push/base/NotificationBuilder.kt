package com.appoxee.internal.push.base

import android.app.Notification
import android.app.PendingIntent
import android.graphics.Bitmap
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat

interface NotificationBuilder {
    fun setPriority(priority: Int): NotificationBuilder
    fun setContentTitle(title: CharSequence?): NotificationBuilder
    fun setContentText(text: CharSequence?): NotificationBuilder
    fun setLargeIcon(icon: Bitmap?): NotificationBuilder
    fun setAutoCancel(autoCancel: Boolean): NotificationBuilder
    fun setStyle(style: NotificationCompat.Style?): NotificationBuilder
    fun setContentIntent(intent: PendingIntent?): NotificationBuilder
    fun setDeleteIntent(intent: PendingIntent?): NotificationBuilder
    fun setSmallIcon(icon: Int): NotificationBuilder
    fun build(): Notification
    fun addAction(action: NotificationCompat.Action): NotificationBuilder
    fun setSmallIcon(icon: IconCompat?): NotificationBuilder
}
