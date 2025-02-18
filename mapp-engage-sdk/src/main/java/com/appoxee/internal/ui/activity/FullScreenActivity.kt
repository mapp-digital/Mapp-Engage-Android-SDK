package com.appoxee.internal.ui.activity

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.appoxee.internal.container.ActionContainer
import com.appoxee.internal.container.AppoxeeContainer
import com.appoxee.internal.container.PushContainer
import com.appoxee.internal.model.request.events.ClickType
import com.appoxee.internal.model.request.events.EventType
import com.appoxee.internal.ui.push.model.PushData
import com.appoxee.internal.util.CompatExt.getParcelableCompat
import com.appoxee.internal.util.Logger

class FullScreenActivity : AppCompatActivity() {

    private val TAG = FullScreenActivity::class.java.name

    companion object {
        @JvmStatic
        fun getIntent(context: Context): Intent {
            return Intent().apply {
                setPackage(context.packageName)
                setClass(context, FullScreenActivity::class.java)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            }
        }
    }

    private lateinit var appoxeeContainer: AppoxeeContainer
    private lateinit var actionContainer: ActionContainer
    private lateinit var pushContainer: PushContainer

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(android.R.style.Theme_Translucent_NoTitleBar_Fullscreen)
        super.onCreate(savedInstanceState)
        appoxeeContainer = AppoxeeContainer.getInstance(this)
        actionContainer = ActionContainer(this)
        pushContainer = PushContainer(this, appoxeeContainer)
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val pushData = intent.extras?.getParcelableCompat<PushData>("pushData")
        val notificationId = intent.getIntExtra("notificationId", 0)
        val eventType =
            intent.getIntExtra("eventType", 0).let { EventType.entries[it] }

        val clickType =
            intent.getStringExtra("clickType")?.let { ClickType.fromString(it) }
                ?: ClickType.LAUNCH_APP

        if (notificationId != 0) {
            pushContainer.pushManager.dismissNotification(notificationId)
        }

        val delegateIntent = pushContainer.pendingIntentProvider.createDelegateIntent(
            clickType = clickType,
            eventType = eventType,
            notificationId = notificationId,
            action = intent.action,
            pushData = pushData,
        )

        sendBroadcast(delegateIntent)

        when (clickType) {
            ClickType.OPEN_LANDING_PAGE -> {
                intent.handleIntentSafe("Error creating Open Landing Page Intent") {
                    actionContainer.actionHandler.openLandingPageExternal(it.toString())
                    finish()
                }
            }

            ClickType.OPEN_RICH_PUSH -> {
                pushData?.let {
                    actionContainer.actionHandler.showGif(it)
                    finish()
                }
            }

            ClickType.OPEN_DIALER -> {
                intent.handleIntentSafe("Error creating Open Dialer Intent") {
                    actionContainer.actionHandler.openDialer(it.toString())
                    finish()
                }
            }

            ClickType.OPEN_STORE -> {
                intent.handleIntentSafe("Error creating Open PlayStore Intent") {
                    actionContainer.actionHandler.openAppStore(it.toString())
                    finish()
                }
            }

            ClickType.OPEN_DEEP_LINK -> {
                intent.handleIntentSafe("Error creating Open DeepLink Intent") { uri ->
                    actionContainer.actionHandler.openDeepLink(
                        uri.toString(),
                        pushData?.id?.toString()
                    )
                    finish()
                }
            }

            ClickType.LAUNCH_APP -> {
                actionContainer.actionHandler.openLaunchActivity()
                finish()
            }

            ClickType.DISMISS -> {
                finish()
            }
        }
    }

    private inline fun Intent.handleIntentSafe(message: String, action: (Uri) -> Unit) {
        try {
            val data = this.data ?: this.extras?.getBundle("pushData")?.getString("actionUri")
                ?.let { Uri.parse(it) }
            data?.let {
                action.invoke(it)
            }
        } catch (e: Exception) {
            Logger.e(TAG, "${message}/n${e.message}", e)
        }
    }
}