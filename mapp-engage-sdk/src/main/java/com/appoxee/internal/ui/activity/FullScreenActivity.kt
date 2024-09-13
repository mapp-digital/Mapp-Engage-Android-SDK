package com.appoxee.internal.ui.activity

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.FrameLayout.LayoutParams
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.appoxee.Appoxee
import com.appoxee.databinding.ActivityFullScreenBinding
import com.appoxee.internal.Actions
import com.appoxee.internal.AppoxeeImpl
import com.appoxee.internal.container.PushContainer
import com.appoxee.internal.container.StatsContainer
import com.appoxee.internal.model.request.events.ClickType
import com.appoxee.internal.model.request.events.EventType
import com.appoxee.internal.push.model.PushData
import com.appoxee.internal.stats.StatsClient
import com.appoxee.internal.ui.custom.MappWebView
import com.appoxee.internal.util.CompatExt.getParcelableCompat
import com.appoxee.internal.util.DispatchersImpl
import com.appoxee.internal.util.LibExt.startIntentOrDefault
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

    private lateinit var binding: ActivityFullScreenBinding
    private lateinit var statsClient: StatsClient
    private lateinit var pushContainer: PushContainer

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(android.R.style.Theme_Translucent_NoTitleBar_Fullscreen)
        super.onCreate(savedInstanceState)
        binding = ActivityFullScreenBinding.inflate(layoutInflater)
        pushContainer = PushContainer(this)
        statsClient = StatsContainer(this, DispatchersImpl()).statsClient
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(i: Intent?) {
        i?.let { intent ->
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
                        showWebView(it)
                    }
                }

                ClickType.OPEN_RICH_PUSH -> {
                    showGif(intent)
                }

                ClickType.OPEN_DIALER -> {
                    intent.handleIntentSafe("Error creating Open Dialer Intent") {
                        val dialerIntent = Intent(Intent.ACTION_DIAL, it)
                        startActivity(dialerIntent)
                        finish()
                    }
                }

                ClickType.OPEN_STORE -> {
                    intent.handleIntentSafe("Error creating Open PlayStore Intent") {
                        val dialerIntent = Intent(Intent.ACTION_VIEW, it)
                        startActivity(dialerIntent)
                        finish()
                    }
                }

                ClickType.OPEN_DEEP_LINK -> {
                    intent.handleIntentSafe("Error creating Open DeepLink Intent") { uri ->
                        val deepLinkIntent = createDeepLink(pushData, intent)
                        this@FullScreenActivity.startIntentOrDefault(deepLinkIntent)
                        finish()
                    }
                }

                ClickType.DISMISS -> {
                    finish()
                }

                else -> {}
            }
        }
    }

    private fun createDeepLink(pushData: PushData?, it: Intent): Intent {
        return Intent(Actions.MAPP_DEEP_LINK).apply {
            setPackage(this@FullScreenActivity.packageName)
            val uriBuilder = Uri.Builder()
                .scheme(Actions.MAPP_DEEP_LINK_SCHEME)
                .authority(Actions.MAPP_DEEP_LINK_AUTHORITY)
                .appendQueryParameter("link", it.data.toString())
            pushData?.id?.let { messageId ->
                uriBuilder.appendQueryParameter(
                    "messageId",
                    messageId.toString()
                )
            }

            data = uriBuilder.build()
        }
    }

    private fun showWebView(uri: Uri) {
        setContentView(binding.root)
        MappWebView.getInstance(this).apply {
            layoutParams =
                LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            binding.root.addView(this)
            this.loadUrl(uri.toString())
        }
    }

    private fun showGif(intent: Intent) {
        intent.extras?.getParcelableCompat<PushData>("pushData")?.let {
            (Appoxee.instance() as AppoxeeImpl?)?.activityLifecycleCallback?.handleRichPush(
                this@FullScreenActivity,
                it
            )
            finish()
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

    override fun onDestroy() {
        binding.root.removeAllViews()
        super.onDestroy()
    }
}