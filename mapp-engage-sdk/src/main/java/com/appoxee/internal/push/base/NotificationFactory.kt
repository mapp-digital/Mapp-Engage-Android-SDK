@file:Suppress("PrivatePropertyName")

package com.appoxee.internal.push.base

import android.annotation.TargetApi
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.Builder
import androidx.core.app.NotificationCompat.FLAG_AUTO_CANCEL
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmapOrNull
import com.appoxee.internal.Actions
import com.appoxee.internal.broadcast.MappInternalBroadcastReceiver
import com.appoxee.internal.model.request.events.PushAction
import com.appoxee.internal.model.response.Category
import com.appoxee.internal.push.model.CategoriesFactory
import com.appoxee.internal.push.model.CategoryType
import com.appoxee.internal.push.model.NotificationType
import com.appoxee.internal.push.model.PushData
import com.appoxee.internal.push.model.PushUriType
import com.appoxee.internal.push.model.PushUriType.Companion.toPushAction
import com.appoxee.internal.push.style.NotificationStyleFactory
import com.appoxee.internal.ui.activity.FullScreenActivity
import com.appoxee.internal.util.CompatExt
import java.util.Objects

internal class NotificationFactory(
    private val context: Context,
    private val categoriesFactory: CategoriesFactory,
    private val notificationStyleFactory: NotificationStyleFactory,
    private val notificationChannelId: String,
) {
    suspend fun createSimpleNotification(pushData: PushData, notificationId: Int): Notification {
        val notificationStyle = notificationStyleFactory.buildNotificationStyle(pushData).getStyle()
        var builder =
            Builder(context, notificationChannelId)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentTitle(pushData.title)
                .setContentText(pushData.bigText)
                .setLargeIcon(getIcon(context))
                .setAutoCancel(true)
                .setStyle(notificationStyle)

        createPendingIntent(pushData)?.let {
            builder.setContentIntent(it)
        }

        createDismissPendingIntent(notificationId, pushData).let {
            builder.setDeleteIntent(it)
        }

        builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            setSmallIconApi23(context, builder)
        } else {
            setSmallIcon(context, builder)
        }

        addButtons(builder, pushData, notificationId)

        return builder.build().apply {
            flags = flags or FLAG_AUTO_CANCEL
        }
    }

    private suspend fun addButtons(builder: Builder, pushData: PushData, notificationId: Int) {
        val categories = categoriesFactory.getCategories()
        val category = categories.firstOrNull { Objects.equals(pushData.category, it.name?.value) }
        val language = pushData.language
        val notificationType = NotificationType.fromString(pushData.type)

        pushData.buttonList.flatMap { it?.fgActions ?: emptyList() }
            .forEachIndexed { index, fgAction ->
                val pendingIntent = if (fgAction.isDestroyAction()) {
                    createDismissPendingIntent(notificationId, pushData)
                } else {
                    createCustomPendingIntent(
                        fgAction.getUriType(),
                        fgAction.getAction(),
                        null,
                        notificationId,
                        notificationId
                    )
                }
                pendingIntent.let { pi ->
                    val title = category?.buttons?.get(index)?.getLocalizedTitle(language)
                    val action = NotificationCompat.Action(0, title, pi)
                    builder.addAction(action)
                }
            }

        if (listOf(NotificationType.GIF, NotificationType.VIDEO).contains(notificationType)) {
            categories.firstOrNull { CategoryType.APX_SPECIFIC_ANDROID == it.name }
                ?.let { specificCategory ->
                    addSpecificButtons(
                        builder, pushData, specificCategory, Actions.Button.PLAY, notificationId
                    )
                    addSpecificButtons(
                        builder, pushData, specificCategory, Actions.Button.TURN_OFF, notificationId
                    )
                }
        }
    }

    private fun addSpecificButtons(
        builder: Builder,
        pushData: PushData,
        specificCategory: Category,
        buttonTitle: String,
        notificationId: Int,
    ) {
        val language = pushData.language
        specificCategory.buttons.firstOrNull { buttonTitle.equals(it.title, true) }?.let {
            val uriType =
                if (it.isDestructive) PushUriType.KEY_APP_DESTROY_PUSH else PushUriType.KEY_PLAY
            val pendingIntent = if (Actions.Button.TURN_OFF.equals(buttonTitle, true)) {
                createDismissPendingIntent(notificationId, pushData)
            } else {
                createCustomPendingIntent(
                    uriType, pushData.iosApxMedia, pushData, notificationId, notificationId
                )
            }

            val action = NotificationCompat.Action(
                0, it.getLocalizedTitle(language), pendingIntent
            )
            builder.addAction(action)
        }
    }

    private fun createPendingIntent(pushData: PushData): PendingIntent? {
        val pushUriType = pushData.getContentUriType()
        val intent = if (pushUriType == null) {
            context.packageManager.getLaunchIntentForPackage(context.packageName)?.let {
                it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                it.addCategory(Intent.CATEGORY_LAUNCHER)
                it.setPackage(context.packageName)
                it.action = Intent.ACTION_MAIN
                it.putExtra("pushData", pushData)
            }
        } else {
            FullScreenActivity.getIntent(context).let {
                it.setAction(pushUriType.toPushAction().value)
                it.putExtra("pushData", pushData)
                it.setData(pushData.actionUri)
            }
        }

        return intent?.let {
            PendingIntent.getActivity(
                context,
                pushData.id.toInt(),
                it,
                CompatExt.PENDING_INTENT_CANCEL_FLAGS
            )
        }
    }


    private fun createCustomPendingIntent(
        uriType: PushUriType,
        actionData: String?,
        pushData: PushData?,
        requestCode: Int,
        notificationId: Int,
    ): PendingIntent {
        if (uriType == PushUriType.KEY_APP_DESTROY_PUSH) {
            return createDismissPendingIntent(notificationId, pushData)
        } else {
            val intent = FullScreenActivity.getIntent(context).apply {
                putExtra("notificationId", notificationId)
                action = uriType.toPushAction().value
                actionData?.let {
                    data = Uri.parse(it)
                }
                pushData?.let { this.putExtra("pushData", it) }
            }
            return PendingIntent.getActivity(
                context,
                requestCode,
                intent,
                CompatExt.PENDING_INTENT_CANCEL_FLAGS
            )
        }
    }

    private fun createDismissPendingIntent(
        notificationId: Int,
        pushData: PushData?
    ): PendingIntent {
        val intent = Intent().apply {
            setPackage(context.packageName)
            putExtra("notificationId", notificationId)
            pushData?.let { putExtra("pushData", it) }
            setClass(context, MappInternalBroadcastReceiver::class.java)
            action = PushAction.DISMISS.value
        }
        return PendingIntent.getBroadcast(
            context,
            notificationId,
            intent,
            CompatExt.PENDING_INTENT_CANCEL_FLAGS
        )
    }

    private fun setSmallIcon(context: Context, builder: Builder): Builder {
        val pm = context.packageManager
        val appInfo = pm.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
        builder.setSmallIcon(appInfo.icon)
        return builder
    }

    @TargetApi(value = Build.VERSION_CODES.M)
    private fun setSmallIconApi23(
        context: Context, builder: Builder
    ): Builder {
        val bitmap = context.packageManager.defaultActivityIcon.toBitmapOrNull()
        bitmap?.let { IconCompat.createWithBitmap(bitmap) }?.let { builder.setSmallIcon(it) }
        return builder
    }

    private fun getIcon(context: Context): Bitmap? {
        val iconId = context.packageManager.getApplicationInfo(
            context.packageName, PackageManager.GET_META_DATA
        ).icon
        val drawable = ContextCompat.getDrawable(context, iconId)
        return drawable?.toBitmapOrNull()
    }
}