package com.appoxee.internal.ui.push.base

import android.app.Notification
import android.app.PendingIntent
import android.graphics.Bitmap
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat

class NotificationBuilderImpl(private val builder: NotificationCompat.Builder) :
    NotificationBuilder {
    override fun setPriority(priority: Int) = apply { builder.priority = priority }
    override fun setContentTitle(title: CharSequence?) = apply { builder.setContentTitle(title) }
    override fun setContentText(text: CharSequence?) = apply { builder.setContentText(text) }
    override fun setLargeIcon(icon: Bitmap?) = apply { builder.setLargeIcon(icon) }
    override fun setAutoCancel(autoCancel: Boolean) = apply { builder.setAutoCancel(autoCancel) }
    override fun setStyle(style: NotificationCompat.Style?) = apply { builder.setStyle(style) }
    override fun setContentIntent(intent: PendingIntent?) =
        apply { builder.setContentIntent(intent) }

    override fun setDeleteIntent(intent: PendingIntent?) = apply { builder.setDeleteIntent(intent) }
    override fun setSmallIcon(icon: Int) = apply { builder.setSmallIcon(icon) }
    override fun build(): Notification = builder.build()
    override fun addAction(action: NotificationCompat.Action) = apply { builder.addAction(action) }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun setSmallIcon(icon: IconCompat?) = apply {
        icon?.let {
            builder.setSmallIcon(it)
        }
    }
}