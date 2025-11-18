package com.appoxee.internal.migration

import android.content.Context
import com.appoxee.internal.migration.data.OldRegistration
import com.appoxee.internal.util.Logger
import com.appoxee.shared.AppoxeeOptions
import org.json.JSONObject
import java.io.File
import androidx.core.content.edit

internal class MigrationHelperImpl(context: Context) : MigrationHelper {

    private val filesDir = context.filesDir
    private val fileName = "persistence_DeviceRealState.data"
    private val preferences = context.getSharedPreferences("appoxee", Context.MODE_PRIVATE)

    override suspend fun readTextFromFile(): String? {
        return File(getFilesDir(), getFileName()).let {
            if (it.exists()) it.readText(Charsets.UTF_8) else null
        }
    }

    override suspend fun getRegistrationOptions(): AppoxeeOptions? {
        val sdkKey = preferences.getString("sdk_key", "")
        val appId = preferences.getString("cep_app_id", "")
        val tenantId = preferences.getString("cep_tenant_id", "")
        val server = preferences.getString("server", null)
            ?.let { AppoxeeOptions.Server.get(it) }

        if (sdkKey != null && appId != null && tenantId != null && server != null) {
            return AppoxeeOptions(
                server = server,
                sdkKey = sdkKey,
                appId = appId,
                tenantId = tenantId
            )
        }
        return null
    }

    override suspend fun fetchRegistrationData(): OldRegistration? {
        return try {
            readTextFromFile()?.removePrefix("com.appoxee.internal.model.Device")?.let { rawData ->
                val json = if (rawData.isEmpty()) null else JSONObject(rawData)
                OldRegistration(
                    alias = json?.optString("alias"),
                    isRegistered = json?.optBoolean("isRegistered") ?: false,
                    pushEnabled = json?.optBoolean("pushEnabled") ?: false,
                    timestamp = json?.optLong("timestamp") ?: 0L,
                    pushToken = json?.optString("pushToken")
                )
            }
        } catch (e: Exception) {
            Logger.e(this.javaClass.name, e)
            null
        }
    }

    override fun isEqual(o1: AppoxeeOptions?, o2: AppoxeeOptions?): Boolean {
        if (o1 == null || o2 == null) return false
        return o1.server == o2.server &&
                o1.sdkKey == o2.sdkKey &&
                o1.appId == o2.appId &&
                o1.tenantId == o2.tenantId
    }

    override fun getFilesDir(): File {
        return filesDir
    }

    override fun getFileName(): String {
        return fileName
    }

    override fun deleteOldRegistration() {
        try {
            File(getFilesDir(), getFileName()).also {
                if (it.exists()) it.delete()
            }
            preferences.edit { clear() }
        } catch (e: Exception) {
            Logger.e(this.javaClass.name, e)
        }
    }
}