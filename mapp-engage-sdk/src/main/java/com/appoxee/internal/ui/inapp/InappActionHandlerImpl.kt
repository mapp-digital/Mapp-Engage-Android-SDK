package com.appoxee.internal.ui.inapp

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.content.res.AppCompatResources
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.core.graphics.drawable.toBitmap
import com.appoxee.R
import com.appoxee.internal.Actions
import com.appoxee.internal.model.response.inapp.ActionData
import com.appoxee.internal.model.response.inapp.InappActionType
import com.appoxee.internal.model.response.inapp.InappButton
import com.appoxee.internal.util.Logger

class InappActionHandlerImpl(private val context: Context) : InappActionHandler {
    private val TAG = this::class.java.name
    override fun handleAction(actionData: ActionData) {
        when (actionData.actionType) {
            InappActionType.DEEPLINK -> {
                handleDeeplink(actionData)
            }

            InappActionType.APP_STORE -> {
                handleAppStore(actionData)
            }

            InappActionType.LANDING_PAGE -> {
                if (actionData.openInApp) {
                    handleLandingPageInApp(actionData)
                } else {
                    handleLandingPageExternal(actionData)
                }
            }

            InappActionType.DIALER -> {
                handleDialer(actionData)
            }

            else -> {}
        }
    }

    override fun handleAction(button: InappButton) {
        handleAction(button.actionData)
    }

    override fun handleDeeplink(actionData: ActionData) {
        val message = "Deeplink: ${actionData.link}"
        Logger.d(TAG, message)
        actionData.link?.let { url ->
            //val uri = Uri.parse("${Actions.DEEP_LINK_URI}$url&messageId=${actionData.messageId}")
            val uriBuilder = Uri.Builder()
                .scheme(Actions.MAPP_DEEP_LINK_SCHEME)
                .authority(Actions.MAPP_DEEP_LINK_AUTHORITY)
                .appendQueryParameter("link", url)
                .appendQueryParameter("messageId", actionData.messageId.toString())

            val deeplinkIntent = Intent(Actions.MAPP_DEEP_LINK_ACTION, uriBuilder.build())
                .setPackage(context.packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            try {
                if (deeplinkIntent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(deeplinkIntent)
                } else {
                    Logger.e(
                        TAG,
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
                Logger.e(TAG, "Can't open deeplink: ${e.message}")
            }
        }
    }

    override fun handleAppStore(actionData: ActionData) {
        val message = "AppStore: ${actionData.link}"
        Logger.d(TAG, message)
        actionData.link?.let { applicationId ->
            val playStoreUri = Uri.parse("market://details?id=${actionData.link}")
            val webUri =
                Uri.parse("https://play.google.com/store/apps/details?id=${actionData.link}")

            val playStoreIntent = Intent(Intent.ACTION_VIEW, playStoreUri).apply {
                // Make sure the Play Store app is explicitly opened
                setPackage("com.android.vending")
            }

            try {
                startActivity(context, playStoreIntent, null)
            } catch (e: ActivityNotFoundException) {
                // Play Store app is not installed, fallback to the web browser
                val webIntent = Intent(Intent.ACTION_VIEW, webUri)
                startActivity(context, webIntent, null)
            }
        }
    }

    override fun handleLandingPageInApp(actionData: ActionData) {
        val message = "Landing Page In App: ${actionData.link}"
        Logger.d(TAG, message)
        actionData.link?.let { url ->
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

            val customTabsIntent = builder.build()
            customTabsIntent.launchUrl(context, Uri.parse(url))
        }
    }

    override fun handleLandingPageExternal(actionData: ActionData) {
        val message = "Landing Page External: ${actionData.link}"
        Logger.d(TAG, message)
        actionData.link?.let { url ->
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(browserIntent)
        }
    }

    override fun handleDialer(actionData: ActionData) {
        val message = "Dialer: ${actionData.link}"
        Logger.d(TAG, message)
        actionData.link?.let { telephoneNumber ->
            if (telephoneNumber.startsWith("tel")) {
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse(telephoneNumber)
                }
                context.startActivity(intent)
            }
        }

    }
}