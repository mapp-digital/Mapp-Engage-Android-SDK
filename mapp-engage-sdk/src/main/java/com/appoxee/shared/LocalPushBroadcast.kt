package com.appoxee.shared

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.appoxee.internal.util.CompatExt.getParcelableExtraCompat

abstract class LocalPushBroadcast : BroadcastReceiver() {
    companion object Action {
        const val PUSH_RECEIVED = "com.appoxee.PUSH_RECEIVED"
        const val PUSH_OPENED = "com.appoxee.PUSH_OPENED"
        const val PUSH_SILENT = "com.appoxee.PUSH_SILENT"
        const val PUSH_DISMISSED = "com.appoxee.PUSH_DISMISSED"
        const val PUSH_BUTTON_CLICKED = "com.appoxee.BUTTON_CLICKED"
        const val PUSH_RICH = "com.appoxee.RICH_PUSH"

        val actionsForReporting =
            listOf(PUSH_OPENED, PUSH_SILENT, PUSH_DISMISSED, PUSH_BUTTON_CLICKED)

        val allActions = actionsForReporting
            .toMutableList()
            .apply {
                addAll(listOf(PUSH_RECEIVED, PUSH_RICH))
            }.toList()
    }

    abstract fun onReceived(push: MappPush)
    abstract fun onOpened(push: MappPush)
    abstract fun onSilent(push: MappPush)
    abstract fun onDismissed(push: MappPush)
    abstract fun onButtonClick(push: MappPush)
    abstract fun onRichPush(push: MappPush)

    override fun onReceive(c: Context?, i: Intent?) {
        i?.let { intent ->
            intent.action?.let { action ->
                val push = intent.getParcelableExtraCompat<MappPush>("mappPush") ?: return
                when (action) {
                    PUSH_RECEIVED -> {
                        onReceived(push)
                    }

                    PUSH_OPENED -> {
                        onOpened(push)
                    }

                    PUSH_SILENT -> {
                        onSilent(push)
                    }

                    PUSH_DISMISSED -> {
                        onDismissed(push)
                    }

                    PUSH_BUTTON_CLICKED -> {
                        onButtonClick(push)
                    }

                    PUSH_RICH -> {
                        onRichPush(push)
                    }
                }
            }
        }
    }
}
