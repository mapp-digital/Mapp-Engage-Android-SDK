package com.appoxee.internal.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.util.DisplayMetrics
import androidx.core.graphics.toColorInt
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

    internal fun Activity.getDisplayMetrics(): DisplayMetrics {
        val displayMetrics = DisplayMetrics()
        displayMetrics.widthPixels = this.window.decorView.width
        displayMetrics.heightPixels = this.window.decorView.height
        return displayMetrics
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

    internal fun String?.toColor(): Int {
        if (this == null) return Color.TRANSPARENT
        try {
            if (this.startsWith("#")) {
                return this.toColorInt()
            } else if (this.startsWith("rgba")) {
                // format "rgba(r,g,b,a)" where r,g,b are integers (0-255), and "a" is float 0.0-1.0
                val values = this.replace("rgba(", "")
                    .replace(")", "")
                    .split(",")
                val r = values[0].toInt()
                val g = values[1].toInt()
                val b = values[2].toInt()
                val a = (values[3].toFloat() * 255).toInt()
                return Color.argb(a, r, g, b)
            } else {
                return Color.TRANSPARENT
            }
        } catch (e: Exception) {
            return Color.TRANSPARENT
        }
    }
}