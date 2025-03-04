@file:Suppress("PrivatePropertyName")

package com.appoxee.internal.ui.push.base

import android.app.Notification
import android.app.Notification.FLAG_AUTO_CANCEL
import android.os.Build
import androidx.annotation.VisibleForTesting
import androidx.core.app.NotificationCompat
import com.appoxee.internal.Actions
import com.appoxee.internal.model.request.events.EventType
import com.appoxee.internal.provider.IconProvider
import com.appoxee.internal.provider.PendingIntentProvider
import com.appoxee.internal.ui.push.model.CategoriesFactory
import com.appoxee.internal.ui.push.model.NotificationType
import com.appoxee.internal.ui.push.model.PushData
import com.appoxee.internal.ui.push.model.PushUriType
import com.appoxee.internal.ui.push.style.NotificationStyleFactory
import com.appoxee.shared.LocalPushBroadcast

internal class NotificationFactory(
    private val notificationStyleFactory: NotificationStyleFactory,
    private val notificationBuilderFactory: NotificationBuilder,
    private val iconProvider: IconProvider,
    private val pendingIntentProvider: PendingIntentProvider,
) {
    fun createSimpleNotification(pushData: PushData, notificationId: Int): Notification {

        val notificationStyle = notificationStyleFactory.buildNotificationStyle(pushData).getStyle()

        val builder = notificationBuilderFactory
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setStyle(notificationStyle)
            .setContentTitle(pushData.title)
            .setContentText(pushData.bigText ?: "")

        iconProvider.getLargeIcon()?.let {
            builder.setLargeIcon(it)
        }

        pendingIntentProvider.createPendingIntent(
            pushData,
            notificationId,
            LocalPushBroadcast.PUSH_OPENED
        )?.let {
            builder.setContentIntent(it)
        }

        pendingIntentProvider.createDismissPendingIntent(notificationId, pushData).let {
            builder.setDeleteIntent(it)
        }

        setSmallIcon(builder)

        addButtons(builder, pushData, notificationId)

        val notification = builder.build()

        return notification.apply {
            flags = flags or FLAG_AUTO_CANCEL
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun setSmallIcon(builder: NotificationBuilder) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            builder.setSmallIcon(iconProvider.getSmallIconApi23())
        } else {
            builder.setSmallIcon(iconProvider.getSmallIcon())
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun addButtons(
        builder: NotificationBuilder,
        pushData: PushData,
        notificationId: Int
    ) {
        val language = pushData.language
        val notificationType = NotificationType.fromString(pushData.type)

        pushData.category?.let { category ->
            category.buttons.forEachIndexed { index, button ->
                val fgAction = pushData.buttonList
                    .flatMap { it?.fgActions ?: emptyList() }
                    .getOrNull(index)
                    ?.copy(isDestructive = button.isDestructive)

                val eventType = when (index) {
                    1 -> EventType.BUTTON2
                    2 -> EventType.BUTTON3
                    else -> EventType.BUTTON1
                }
                val pendingIntent = if (button.isDestructive) {
                    pendingIntentProvider.createDismissPendingIntent(notificationId, pushData)
                } else {
                    pendingIntentProvider.createCustomPendingIntent(
                        fgAction?.getUriType(),
                        fgAction?.getAction(),
                        LocalPushBroadcast.PUSH_BUTTON_CLICKED,
                        pushData,
                        notificationId,
                        eventType,
                    )
                }
                pendingIntent.let { pi ->
                    val title = button.getLocalizedTitle(language)
                    val action = NotificationCompat.Action(0, title, pi)
                    builder.addAction(action)
                }
            }
        }

        if (listOf(NotificationType.GIF, NotificationType.VIDEO).contains(notificationType)) {
            addSpecificButtons(
                builder = builder,
                pushData = pushData,
                isDestructive = false,
                buttonTitle = Actions.Button.PLAY,
                notificationId = notificationId,
                click = EventType.BUTTON2
            )
            addSpecificButtons(
                builder = builder,
                pushData = pushData,
                isDestructive = true,
                buttonTitle = Actions.Button.TURN_OFF,
                notificationId = notificationId,
                click = EventType.DISMISS
            )
        }
    }

    private fun addSpecificButtons(
        builder: NotificationBuilder,
        pushData: PushData,
        isDestructive: Boolean,
        buttonTitle: String,
        notificationId: Int,
        click: EventType
    ) {
        val language = pushData.language
        val uriType =
            if (isDestructive) PushUriType.KEY_APP_DESTROY_PUSH else PushUriType.KEY_PLAY
        val pendingIntent = if (Actions.Button.TURN_OFF.equals(buttonTitle, true)) {
            pendingIntentProvider.createDismissPendingIntent(notificationId, pushData)
        } else {
            pendingIntentProvider.createCustomPendingIntent(
                uriType,
                pushData.iosApxMedia,
                LocalPushBroadcast.PUSH_RICH,
                pushData,
                notificationId,
                click,
            )
        }

        val action = NotificationCompat.Action(
            0, buttonTitle, pendingIntent
        )

        builder.addAction(action)
    }
}