package com.appoxee.internal.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import com.appoxee.internal.push.model.PushData

object LibExt {
    internal fun Context.getAppTheme(): Int {
        val appInfo = this.packageManager.getApplicationInfo(
            this.packageName,
            PackageManager.GET_META_DATA
        )

        return appInfo.theme
    }

    internal fun Context.getLaunchingIntent(startNew: Boolean = false): Intent {
        val intent = this.packageManager.getLaunchIntentForPackage(this.packageName)
        val component = intent?.component
        return if (!startNew && intent != null) intent else Intent.makeRestartActivityTask(component)
    }

    internal fun Context.startMainActivity(bundle: Bundle?) {
        val launchingIntent = this.getLaunchingIntent().also { intent ->
            intent.putExtra("pushData", bundle?.getBundle("pushData"))
        }
        this.startActivity(launchingIntent)
    }

    internal fun Context.toDp(px:Int): Int {
        return (px / this.resources.displayMetrics.density).toInt()
    }

    internal fun Context.toPx(dp: Int): Int {
        return (dp * this.resources.displayMetrics.density).toInt()
    }
}