package com.appoxee.internal.provider

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.appoxee.internal.broadcast.MappInternalBroadcastReceiver
import com.appoxee.internal.model.request.events.ClickType
import com.appoxee.internal.model.request.events.EventType
import com.appoxee.internal.ui.activity.FullScreenActivity
import com.appoxee.internal.ui.push.model.PushData
import com.appoxee.internal.ui.push.model.PushUriType
import com.appoxee.internal.ui.push.model.PushUriType.Companion.toPushAction
import com.appoxee.internal.util.CompatExt
import com.appoxee.shared.LocalPushBroadcast
import androidx.core.net.toUri

internal class PendingIntentProviderImpl(private val context: Context) : PendingIntentProvider {
    override fun createPendingIntent(
        pushData: PushData,
        notificationId: Int,
        action: String?
    ): PendingIntent? {
        val pushUriType = pushData.getContentUriType()
        val intent = FullScreenActivity.getIntent(context).apply {
            this.action = action
            putExtra("clickType", pushUriType.toPushAction().value)
            putExtra("pushData", pushData)
            putExtra("eventType", EventType.CLICK.ordinal)
            putExtra("buttonPosition", -1)
            setData(pushData.actionUri)
        }

        return PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            CompatExt.PENDING_INTENT_UPDATE_CURRENT_FLAGS
        )
    }

    override fun createDismissPendingIntent(
        notificationId: Int,
        pushData: PushData?
    ): PendingIntent {
        val intent = Intent(LocalPushBroadcast.PUSH_DISMISSED).apply {
            setPackage(context.packageName)
            putExtra("notificationId", notificationId)
            putExtra("eventType", EventType.DISMISS.ordinal)
            pushData?.let { putExtra("pushData", it) }
            putExtra("buttonPosition", -1)
            setClass(context, MappInternalBroadcastReceiver::class.java)
        }
        return PendingIntent.getBroadcast(
            context,
            notificationId,
            intent,
            CompatExt.PENDING_INTENT_UPDATE_CURRENT_FLAGS
        )
    }

    override fun createCustomPendingIntent(
        uriType: PushUriType?,
        actionData: String?,
        action: String?,
        pushData: PushData?,
        notificationId: Int,
        eventType: EventType,
    ): PendingIntent {
        if (uriType == PushUriType.KEY_APP_DESTROY_PUSH) {
            return createDismissPendingIntent(notificationId, pushData)
        } else {
            val intent = FullScreenActivity.getIntent(context).apply {
                setAction(action)
                putExtra("notificationId", notificationId)
                putExtra("eventType", eventType.ordinal)
                putExtra("clickType", uriType.toPushAction().value)
                actionData?.let { data = it.toUri() }
                pushData?.let { putExtra("pushData", it) }
            }
            return PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                CompatExt.PENDING_INTENT_UPDATE_CURRENT_FLAGS
            )
        }
    }

    override fun createDelegateIntent(
        clickType: ClickType,
        eventType: EventType,
        notificationId: Int,
        action: String?,
        pushData: PushData?,
    ): Intent {
        return Intent().apply {
            setPackage(context.packageName)
            putExtra("notificationId", notificationId)
            putExtra("eventType", eventType.ordinal)
            putExtra("clickType", clickType.value)
            pushData?.let { putExtra("pushData", it) }
            setClass(context, MappInternalBroadcastReceiver::class.java)
            setAction(action)
        }
    }
}