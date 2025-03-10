package com.appoxee.internal.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.VectorDrawable
import android.net.Uri
import android.os.Build
import android.util.DisplayMetrics
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import com.appoxee.internal.model.request.events.ClickType
import com.appoxee.internal.ui.push.model.PushData
import com.google.common.base.Charsets
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

object LibraryExtensions {
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

    @OptIn(ExperimentalEncodingApi::class)
    internal fun String?.encode(): String {
        if (this.isNullOrEmpty()) return ""
        return Base64.encode(this.toByteArray(Charsets.UTF_8), 0, this.length)
    }

    @OptIn(ExperimentalEncodingApi::class)
    internal fun String?.decode(): String {
        if (this.isNullOrEmpty()) return ""
        return Base64.decode(this, 0, this.length).decodeToString()
    }

    fun Context.getBitmap(iconResId: Int): Bitmap? {
        return try {
            if (iconResId == 0) return null

            val drawable = ContextCompat.getDrawable(this, iconResId)

            return if (drawable is BitmapDrawable) {
                drawable.bitmap
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && drawable is AdaptiveIconDrawable) {
                // Convert AdaptiveIconDrawable to Bitmap
                val bitmap = Bitmap.createBitmap(
                    drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888
                )
                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                bitmap
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


    fun Context.isValidSmallIcon(iconResId: Int): Boolean {
        val drawable = ContextCompat.getDrawable(this, iconResId) ?: return false

        return when (drawable) {
            is BitmapDrawable -> {
                // Check if the bitmap is monochrome (white with transparency)
                drawable.bitmap.isMonochrome()
            }

            is VectorDrawable -> {
                // Vector drawables are fine for small icons if they're monochrome
                true
            }

            else -> {
                false  // Not suitable if it's neither bitmap nor vector drawable
            }
        }
    }

    fun Bitmap?.isMonochrome(): Boolean {
        // Check the pixel colors of the bitmap to see if it's monochrome
        val width = this?.width ?: return false
        val height = this.height

        for (x in 0 until width) {
            for (y in 0 until height) {
                val pixel = this.getPixel(x, y)
                val alpha = Color.alpha(pixel)
                val red = Color.red(pixel)
                val green = Color.green(pixel)
                val blue = Color.blue(pixel)

                // Allow transparency (alpha > 0), but red, green, and blue should be the same (monochrome)
                if (alpha > 0 && (red != green || green != blue)) {
                    return false
                }
            }
        }
        return true
    }

}