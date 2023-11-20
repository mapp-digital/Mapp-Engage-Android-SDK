@file:Suppress("PrivatePropertyName")

package com.appoxee.internal.push

import android.annotation.TargetApi
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.Builder
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmapOrNull
import com.appoxee.internal.push.model.PushData

internal class NotificationFactory(
    private val context: Context,
    private val notificationChannelId: String
) {
    fun createSimpleNotification(pushData: PushData): Notification {
        var builder = Builder(context, notificationChannelId)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentTitle(pushData.title)
            .setContentText(pushData.bigText)
            .setLargeIcon(getIcon(context))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(pushData.title)
                    .bigText(pushData.bigText)
            ).setAutoCancel(true)

        createPendingIntent(pushData)?.let {
            builder.setContentIntent(it)
        }

        builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            setSmallIconApi23(context, builder)
        } else {
            setSmallIcon(context, builder)
        }

        return builder.build()
    }

    private fun createPendingIntent(pushData: PushData): PendingIntent? {
        return context.packageManager.getLaunchIntentForPackage(context.packageName)?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            it.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            it.addCategory(Intent.CATEGORY_LAUNCHER)
            it.action = Intent.ACTION_MAIN

            it.setPackage(context.packageName)
            PendingIntent.getActivity(
                context,
                pushData.id.toInt(),
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }

    private fun setSmallIcon(context: Context, builder: Builder): Builder {
        val pm = context.packageManager
        val appInfo = pm.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
        builder.setSmallIcon(appInfo.icon)
        return builder
    }

    @TargetApi(value = Build.VERSION_CODES.M)
    private fun setSmallIconApi23(
        context: Context,
        builder: Builder
    ): Builder {
        val bitmap = context.packageManager.defaultActivityIcon.toBitmapOrNull()
        bitmap?.let { IconCompat.createWithBitmap(bitmap) }
            ?.let { builder.setSmallIcon(it) }
        return builder
    }

    private fun getIcon(context: Context): Bitmap? {
        val iconId = context.packageManager.getApplicationInfo(
            context.packageName,
            PackageManager.GET_META_DATA
        ).icon
        val drawable = ContextCompat.getDrawable(context, iconId)
        return drawable?.toBitmapOrNull()
    }
}