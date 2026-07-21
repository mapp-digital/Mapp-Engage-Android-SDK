package com.appoxee.internal.integration

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.appoxee.internal.util.Logger

internal fun interface IntelligenceEventSender {
    fun sendDmcUserId(dmcUserId: String)
}

internal class AndroidIntelligenceEventSender(
    context: Context,
) : IntelligenceEventSender {

    private val applicationContext = context.applicationContext

    override fun sendDmcUserId(dmcUserId: String) {
        if (dmcUserId.isEmpty()) return

        try {
            val intent = Intent(ACTION_NAME).apply {
                setPackage(applicationContext.packageName)
                putExtra(DMC_USER_ID_KEY, dmcUserId)
            }

            val receivers = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                applicationContext.packageManager.queryBroadcastReceivers(
                    intent,
                    PackageManager.ResolveInfoFlags.of(0),
                )
            } else {
                @Suppress("DEPRECATION")
                applicationContext.packageManager.queryBroadcastReceivers(intent, 0)
            }

            receivers.forEach { receiver ->
                val activityInfo = receiver.activityInfo ?: return@forEach
                val explicitIntent = Intent(intent).apply {
                    component = ComponentName(activityInfo.packageName, activityInfo.name)
                }
                applicationContext.sendBroadcast(explicitIntent)
                Logger.d(TAG,"Sent broadcast with intent: $intent")
            }
        } catch (exception: Exception) {
            Logger.w(TAG, "Failed to send Mapp Intelligence integration event", exception)
        }
    }

    private companion object {
        const val TAG = "IntelligenceEventSender"
        const val ACTION_NAME = "webtrekk.android.sdk.integration.MappIntelligenceListener"
        const val DMC_USER_ID_KEY = "dmcUserId"
    }
}
