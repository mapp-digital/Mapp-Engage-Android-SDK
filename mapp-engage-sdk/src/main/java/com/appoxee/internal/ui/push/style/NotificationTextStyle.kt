package com.appoxee.internal.ui.push.style

import androidx.core.app.NotificationCompat
import com.appoxee.internal.ui.push.model.PushData

internal class NotificationTextStyle(private val pushData: PushData) : NotificationStyle {
    override fun getStyle(): NotificationCompat.Style {
        return NotificationCompat.BigTextStyle()
            .setBigContentTitle(pushData.title)
            .bigText(pushData.bigText)
    }
}