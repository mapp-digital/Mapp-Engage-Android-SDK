package com.appoxee.internal.provider

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.appoxee.internal.broadcast.MappInternalBroadcastReceiver
import com.appoxee.internal.model.request.events.ClickType
import com.appoxee.internal.model.request.events.EventType
import com.appoxee.internal.push.model.PushData
import com.appoxee.internal.push.model.PushUriType
import com.appoxee.internal.push.model.PushUriType.Companion.toPushAction
import com.appoxee.internal.ui.activity.FullScreenActivity
import com.appoxee.internal.util.CompatExt
import kotlin.random.Random

internal class PendingIntentProviderImpl(private val context: Context) : PendingIntentProvider {

    private val random = Random(10000)
    override fun createPendingIntent(pushData: PushData): PendingIntent? {
        val pushUriType = pushData.getContentUriType()
        val intent = if (pushUriType == null) {
            context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                addCategory(Intent.CATEGORY_LAUNCHER)
                setPackage(context.packageName)
                action = Intent.ACTION_MAIN
                putExtra("pushData", pushData)
                putExtra("eventType", EventType.CLICK.ordinal)
            }
        } else {
            FullScreenActivity.getIntent(context).apply {
                setAction(pushUriType.toPushAction().value)
                putExtra("pushData", pushData)
                setData(pushData.actionUri)
                putExtra("eventType", EventType.CLICK.ordinal)
            }
        }

        return intent?.let {
            PendingIntent.getActivity(
                context,
                random.nextInt(100, 10000),
                it,
                CompatExt.PENDING_INTENT_CANCEL_FLAGS
            )
        }
    }

    override fun createDismissPendingIntent(
        notificationId: Int,
        pushData: PushData?
    ): PendingIntent {
        val intent = Intent().apply {
            setPackage(context.packageName)
            putExtra("notificationId", notificationId)
            putExtra("eventType", EventType.DISMISS.ordinal)
            pushData?.let { putExtra("pushData", it) }
            setClass(context, MappInternalBroadcastReceiver::class.java)
            action = ClickType.DISMISS.value
        }
        return PendingIntent.getBroadcast(
            context,
            random.nextInt(100, 10000),
            intent,
            CompatExt.PENDING_INTENT_CANCEL_FLAGS
        )
    }

    override fun createCustomPendingIntent(
        uriType: PushUriType?,
        actionData: String?,
        pushData: PushData?,
        notificationId: Int,
        eventType: EventType
    ): PendingIntent {
        if (uriType == PushUriType.KEY_APP_DESTROY_PUSH) {
            return createDismissPendingIntent(notificationId, pushData)
        } else {
            val intent = FullScreenActivity.getIntent(context).apply {
                putExtra("notificationId", notificationId)
                putExtra("eventType", eventType.ordinal)
                action = uriType.toPushAction().value
                actionData?.let { data = Uri.parse(it) }
                pushData?.let { putExtra("pushData", it) }
            }
            return PendingIntent.getActivity(
                context,
                random.nextInt(100, 10000),
                intent,
                CompatExt.PENDING_INTENT_CANCEL_FLAGS
            )
        }
    }

    override fun createDelegateIntent(
        clickType: ClickType,
        eventType: EventType,
        notificationId: Int,
        pushData: PushData?,
    ): Intent {
        return Intent().apply {
            setPackage(context.packageName)
            putExtra("notificationId", notificationId)
            putExtra("eventType", eventType.ordinal)
            pushData?.let { putExtra("pushData", it) }
            setClass(context, MappInternalBroadcastReceiver::class.java)
            setAction(clickType.value)
//            if (eventType == EventType.BUTTON1) {
//                setAction(
//                    pushData?.buttonList?.getOrNull(0)?.fgActions?.firstOrNull()?.getUriType()
//                        .toPushAction().value
//                )
//            } else if (eventType == EventType.BUTTON2) {
//                setAction(
//                    pushData?.buttonList?.getOrNull(1)?.fgActions?.firstOrNull()?.getUriType()
//                        .toPushAction().value
//                )
//            } else if (eventType == EventType.BUTTON3) {
//                setAction(
//                    pushData?.buttonList?.getOrNull(2)?.fgActions?.firstOrNull()?.getUriType()
//                        .toPushAction().value
//                )
//            } else if (eventType == EventType.CLICK) {
//                setAction(pushData?.getContentUriType().toPushAction().value)
//            } else {
//                setAction(ClickType.DISMISS.value)
//            }
        }
    }
}