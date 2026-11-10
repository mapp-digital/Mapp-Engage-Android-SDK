package com.mapp.engagesample.configuration

import android.content.SharedPreferences
import com.appoxee.shared.AppoxeeOptions
import com.appoxee.shared.NotificationMode
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal interface SdkConfigurationStorage {
    fun load(): SdkConfiguration?
    suspend fun save(configuration: SdkConfiguration?)
}

/** Separate from SDK storage: only successfully applied overrides are used at app startup. */
internal class SdkConfigurationPreferences(
    private val preferences: SharedPreferences,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : SdkConfigurationStorage {
    override fun load(): SdkConfiguration? {
        return try {
            val sdkKey = preferences.getString("sdk_key", null) ?: return null
            val server = AppoxeeOptions.Server.valueOf(preferences.getString("server", "").orEmpty())
            SdkConfiguration(
                server = if (server == AppoxeeOptions.Server.STAGING) AppoxeeOptions.Server.TEST else server,
                sdkKey = sdkKey,
                appId = preferences.getString("app_id", "").orEmpty(),
                tenantId = preferences.getString("tenant_id", "").orEmpty(),
                notificationMode = NotificationMode.valueOf(preferences.getString("notification_mode", "").orEmpty()),
                logLevel = AppoxeeOptions.LogLevel.valueOf(preferences.getString("log_level", "").orEmpty()),
            ).takeIf { it.hasRequiredValues() }
        } catch (_: IllegalArgumentException) {
            null
        } catch (_: ClassCastException) {
            null
        }
    }

    override suspend fun save(configuration: SdkConfiguration?) = withContext(ioDispatcher) {
        val editor = preferences.edit().clear()
        if (configuration != null) {
            editor.putString("server", configuration.server.name)
                .putString("sdk_key", configuration.sdkKey)
                .putString("app_id", configuration.appId)
                .putString("tenant_id", configuration.tenantId)
                .putString("notification_mode", configuration.notificationMode.name)
                .putString("log_level", configuration.logLevel.name)
        }
        check(editor.commit()) { "Could not save settings on this device" }
    }
}
