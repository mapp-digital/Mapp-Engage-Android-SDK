package com.appoxee.internal.provider

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.WindowManager
import com.appoxee.BuildConfig
import com.appoxee.internal.model.request.RegisterDeviceModel
import java.util.Locale
import java.util.TimeZone

internal class DeviceProviderImpl(private val context: Context) : DeviceProvider {
    override fun generateRegistrationDevice(): RegisterDeviceModel {
        return RegisterDeviceModel(
            osName = getOSName(),
            appVersion = getAppVersion(),
            clientVersion = getClientVersion(),
            locale = getLocale(),
            timeZone = getTimeZone(),
            hardwareType = getHardwareType(),
            density = getDensity(),
            vendorID = getVendorId(),
            osNumber = getOSNumber(),
            resolution = getResolution()
        )
    }

    @SuppressLint("HardwareIds")
    override fun getUniqueDeviceId(): String {
        return Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        )
    }

    override fun getOSName(): String {
        return "Android"
    }

    override fun getOSNumber(): String {
        return Build.VERSION.SDK_INT.toString()
    }

    override fun getClientVersion(): String {
        return BuildConfig.VERSION_NAME
    }

    override fun getAppVersion(): String {
        return context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }

    override fun getLocale(): String {
        return Locale.getDefault().toString()
    }

    override fun getTimeZone(): String {
        return TimeZone.getDefault().id
    }

    override fun getHardwareType(): String {
        return Build.HARDWARE.toString()
    }

    override fun getDensity(): String {
        val displayMetrics = getDisplayMetrics()
        return displayMetrics.densityDpi.toString()
    }

    override fun getVendorId(): String {
        return Build.MANUFACTURER
    }

    override fun getResolution(): String {
        val metrics = getDisplayMetrics()
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        return String.format(Locale.US, "%dx%d", width, height)
    }

    private fun getDisplayMetrics(): DisplayMetrics {
        val displayMetrics = DisplayMetrics()
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics
    }
}