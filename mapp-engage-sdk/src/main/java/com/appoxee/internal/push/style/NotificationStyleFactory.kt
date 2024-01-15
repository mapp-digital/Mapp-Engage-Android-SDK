package com.appoxee.internal.push.style

import com.appoxee.internal.push.model.NotificationType
import com.appoxee.internal.push.model.PushData

@Suppress("MoveVariableDeclarationIntoWhen")
internal class NotificationStyleFactory() {
    fun buildNotificationStyle(pushData: PushData): NotificationStyle {
        val notificationType = NotificationType.fromString(pushData.type ?: "text")
        return when (notificationType) {
            NotificationType.IMAGE, NotificationType.GIF -> NotificationImageStyle(pushData)
            NotificationType.VIDEO -> NotificationVideoStyle(pushData)
            else -> NotificationTextStyle(pushData)
        }
    }
}