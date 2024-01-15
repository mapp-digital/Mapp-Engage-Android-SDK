package com.appoxee.internal.ui.activity

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.FrameLayout.LayoutParams
import androidx.appcompat.app.AppCompatActivity
import com.appoxee.Appoxee
import com.appoxee.databinding.ActivityFullScreenBinding
import com.appoxee.internal.Actions
import com.appoxee.internal.AppoxeeImpl
import com.appoxee.internal.model.request.events.PushAction
import com.appoxee.internal.push.model.PushData
import com.appoxee.internal.ui.custom.MappWebView
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

    private lateinit var binding: ActivityFullScreenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(android.R.style.Theme_Translucent_NoTitleBar_Fullscreen)
        super.onCreate(savedInstanceState)
        binding = ActivityFullScreenBinding.inflate(layoutInflater)
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent?.let {
            val notificationId = it.getIntExtra("notificationId", 0)
            if (notificationId != 0) {
                Appoxee.instance().closeNotification(notificationId)
            }
            it.action?.let { a ->
                val pushData = it.extras?.getParcelableCompat<PushData>("pushData")
                val action = PushAction.fromString(a)
                when (action) {
                    PushAction.OPEN_LANDING_PAGE -> {
                        it.handleIntentSafe("Error creating Open PlayStore Intent") {
                            showWebView(it)
                        }
                    }

                    PushAction.OPEN_RICH_PUSH -> {
                        showGif(it)
                    }

                    PushAction.OPEN_DIALER -> {
                        it.handleIntentSafe("Error creating Open Dialer Intent") {
                            val dialerIntent = Intent(Intent.ACTION_DIAL, it)
                            startActivity(dialerIntent)
                        }
                    }

                    PushAction.OPEN_STORE -> {
                        it.handleIntentSafe("Error creating Open PlayStore Intent") {
                            val dialerIntent = Intent(Intent.ACTION_VIEW, it)
                            startActivity(dialerIntent)
                        }
                    }

                    PushAction.OPEN_DEEP_LINK -> {
                        it.handleIntentSafe("Error creating Open DeepLink Intent") { uri ->
                            handleDeepLink(pushData, intent)
                        }
                    }

                    else -> {}
                }
            }
        }
    }

    private fun handleDeepLink(pushData: PushData?, it: Intent) {
        val dialerIntent = Intent(Actions.MAPP_DEEP_LINK).apply {
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
        startActivity(dialerIntent)
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