package com.mapp.engagesample.configuration

import com.appoxee.shared.AppoxeeOptions
import com.appoxee.shared.NotificationMode
import com.mapp.engagesample.BuildConfig

internal data class SdkConfiguration(
    val server: AppoxeeOptions.Server,
    val sdkKey: String,
    val appId: String,
    val tenantId: String,
    val notificationMode: NotificationMode,
    val logLevel: AppoxeeOptions.LogLevel,
) {
    fun hasRequiredValues(): Boolean = listOf(sdkKey, appId, tenantId).all {
        it.isNotBlank() && !it.equals("null", ignoreCase = true)
    }

    fun toOptions() = AppoxeeOptions(server, sdkKey, appId, tenantId).also {
        it.notificationMode = notificationMode
        it.logType = logLevel
    }

    companion object {
        fun buildDefaults() = SdkConfiguration(
            server = AppoxeeOptions.Server.L3,
            sdkKey = buildValue(BuildConfig.MAPP_SDK_KEY),
            appId = buildValue(BuildConfig.MAPP_APP_ID),
            tenantId = buildValue(BuildConfig.MAPP_TENANT_ID),
            notificationMode = NotificationMode.BACKGROUND_AND_FOREGROUND,
            logLevel = AppoxeeOptions.LogLevel.DEBUG,
        )

        private fun buildValue(value: String): String =
            value.trim().takeUnless { it.equals("null", ignoreCase = true) }.orEmpty()
    }
}
