package com.appoxee.internal.ui.action

import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.content.res.AppCompatResources
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.appoxee.sdk.R
import com.appoxee.internal.Actions
import com.appoxee.internal.container.AppoxeeContainer
import com.appoxee.internal.ui.push.model.PushData
import com.appoxee.internal.util.Logger

/**
 * Default implementation of [ActionHandler]
 * Main handler to execute all actions from notification's or in-app's click events
 */
internal class MessageActionHandler(private val context: Context) : ActionHandler {
    private val TAG = MessageActionHandler::class.java.simpleName

    private val appoxeeContainer by lazy { AppoxeeContainer.getInstance(context) }
    override fun openAppStore(url: String) {
        val applicationId = Uri.parse(url).getQueryParameter("id")
        val message = "AppStore: $applicationId"
        Logger.d(TAG, message)
        val playStoreUri = Uri.parse("market://details?id=${applicationId}")
        val webUri =
            Uri.parse("https://play.google.com/store/apps/details?id=${applicationId}")

        val playStoreIntent = Intent(Intent.ACTION_VIEW, playStoreUri).apply {
            setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        }
        try {
            context.startActivity(playStoreIntent, null)
        } catch (e: ActivityNotFoundException) {
            // Play Store app is not installed, fallback to the web browser
            val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            }
            context.startActivity(webIntent, null)
        }
    }

    override fun openDeepLink(url: String, messageId: String?) {
        val message = "Deeplink: $url"
        Logger.d(ContentValues.TAG, message)
        //val uri = Uri.parse("${Actions.DEEP_LINK_URI}$url&messageId=${actionData.messageId}")
        val uriBuilder = Uri.Builder()
            .scheme(Actions.MAPP_DEEP_LINK_SCHEME)
            .authority(Actions.MAPP_DEEP_LINK_AUTHORITY)
            .appendQueryParameter("link", url)

        messageId?.let {
            uriBuilder.appendQueryParameter("messageId", it)
        }

        val deeplinkIntent = Intent(Actions.MAPP_DEEP_LINK_ACTION, uriBuilder.build()).apply {
            setPackage(context.packageName)
            setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        }

        try {
            if (deeplinkIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(deeplinkIntent)
            } else {
                Logger.e(
                    ContentValues.TAG,
                    """Activity not found to handle deeplink. Please create activity, or update existing one, with intent-filter as follows: \n"
        <activity
            android:name=".DeepLinkActivity"
            android:exported="true">
            <intent-filter>
                <data
                    android:host="deeplink"
                    android:scheme="apx" />
                <action android:name="com.appoxee.VIEW_DEEPLINK" />

                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />
                <category android:name="{applicationId}" />
            </intent-filter>
        </activity>
                            """
                )
            }
        } catch (e: ActivityNotFoundException) {
            Logger.e(ContentValues.TAG, "Can't open deeplink: ${e.message}")
        }
    }

    override fun openDialer(phoneNumber: String) {
        val message = "Dialer: $phoneNumber"
        Logger.d(TAG, message)
        if (phoneNumber.startsWith("tel")) {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                val number = phoneNumber
                    .replace(" ", "")
                    .replace("tel:", "tel:+")
                    .trim()
                data = Uri.parse(number)
            }
            context.startActivity(intent)
        }
    }

    override fun openLandingPageExternal(url: String) {
        val message = "Landing Page External: $url"
        Logger.d(TAG, message)
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        }
        context.startActivity(browserIntent)
    }

    override fun openLandingPageInternal(url: String) {
        val message = "Landing Page In App: $url"
        Logger.d(TAG, message)
        val toolbarColor = ContextCompat.getColor(context, android.R.color.holo_orange_light)
        val backIcon =
//BitmapFactory.decodeResource(context.resources,R.drawable.me_ic_arrow_back)
            AppCompatResources.getDrawable(context, R.drawable.me_ic_arrow_back)?.toBitmap()
        val customTabColorSchemeParams = CustomTabColorSchemeParams.Builder()
            .setToolbarColor(toolbarColor)
            .build()

        val builder = CustomTabsIntent.Builder()
            .setCloseButtonPosition(CustomTabsIntent.CLOSE_BUTTON_POSITION_DEFAULT)
            .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
            .setUrlBarHidingEnabled(true)
            .setDefaultColorSchemeParams(customTabColorSchemeParams)

        backIcon?.let {
            builder.setCloseButtonIcon(it)
        }

        val customTabsIntent = builder.build().apply {
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        }

        customTabsIntent.launchUrl(context, Uri.parse(url))
    }

    override fun openLaunchActivity() {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val component = intent?.component
        val launchIntent = intent ?: Intent.makeRestartActivityTask(component)
        context.startActivity(launchIntent)
    }

    override fun showGif(pushData: PushData) {
        appoxeeContainer.activityLifecycleHandler.handleRichPush(
            context,
            pushData
        )
    }

    override fun customAction(uri: Uri) {

    }
}