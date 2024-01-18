package com.appoxee.internal.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.appoxee.internal.model.request.events.ClickType
import com.appoxee.internal.push.model.PushData

object LibExt {
    internal fun Context.getAppTheme(): Int {
        val appInfo = this.packageManager.getApplicationInfo(
            this.packageName,
            PackageManager.GET_META_DATA
        )

        return appInfo.theme
    }

    internal fun Context.canHandleIntent(intent: Intent?): Boolean {
        return intent?.resolveActivity(this.packageManager) != null
    }

    internal fun Context.startIntentOrDefault(intent: Intent?): Boolean {
        if (this.canHandleIntent(intent)) {
            this.startActivity(intent)
            return true
        } else {
            intent?.data?.let {
                val link = it.getQueryParameter("link")
                if (link?.startsWith("http") == true) {
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
                    if (this.canHandleIntent(browserIntent)) {
                        startActivity(browserIntent)
                        return true
                    }
                }
            }

            this.getLaunchingIntent(false)?.let { launchingActivityIntent ->
                this.startActivity(launchingActivityIntent)
                return true
            }
        }
        return false
    }

    internal fun Context.getLaunchingIntent(startNew: Boolean = false): Intent? {
        val intent = this.packageManager.getLaunchIntentForPackage(this.packageName)
        val component = intent?.component
        return if (!startNew && intent != null) intent else Intent.makeRestartActivityTask(component)
    }

    internal fun Context.startMainActivity(pushData: PushData) {
        this.getLaunchingIntent()?.let { intent ->
            intent.setAction(ClickType.OPEN_RICH_PUSH.value)
            intent.putExtra("pushData", pushData)
            this.startActivity(intent)
        }
    }

    internal fun Context.toDp(px: Int): Int {
        return (px / this.resources.displayMetrics.density).toInt()
    }

    internal fun Context.toPx(dp: Int): Int {
        return (dp * this.resources.displayMetrics.density).toInt()
    }
}