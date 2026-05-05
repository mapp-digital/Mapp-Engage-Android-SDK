package com.appoxee.internal.provider

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.appoxee.sdk.R
import com.appoxee.internal.util.LibraryExtensions.getBitmap
import com.appoxee.internal.util.LibraryExtensions.isValidSmallIcon

internal class IconProviderImpl(private val context: Context) : IconProvider {
    private val customSmallIconColorName = "com.engage.mapp_notification_small_icon_color"
    private val customSmallIconName = "com.engage.mapp_notification_small_icon"
    private val customLargeIconName = "com.engage.mapp_notification_large_icon"

    override fun getLargeIcon(): Bitmap? {
        val iconId = context.packageManager.getApplicationInfo(
            context.packageName, PackageManager.GET_META_DATA
        ).icon
        return context.getBitmap(iconId)
    }

    override fun getSmallIcon(): Int {
        val pm = context.packageManager
        val appInfo = pm.getApplicationInfo(context.packageName, 0)
        val resId = appInfo.icon
        return if (resId != 0 && context.isValidSmallIcon(resId)) resId else R.drawable.me_ic_message
    }

    override fun getCustomSmallIcon(): Int {
        return context.packageManager.let {
            val metadata =
                it.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA).metaData
            val resId = metadata.getInt(customSmallIconName)
            if (resId != 0 && context.isValidSmallIcon(resId)) resId else getSmallIcon()
        }
    }

    override fun getCustomLargeIcon(): Bitmap? {
        return context.packageManager.let {
            val metadata =
                it.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA).metaData
            val resId = metadata.getInt(customLargeIconName)

            context.getBitmap(resId) /*?: getLargeIcon()*/
        }
    }

    @ColorInt
    override fun getCustomIconColor(): Int {
        return context.packageManager.let {
            val metadata =
                it.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA).metaData
            val colorId = metadata.getInt(customSmallIconColorName)
            if (colorId != 0) ContextCompat.getColor(context, colorId) else Color.DKGRAY
        }
    }
}