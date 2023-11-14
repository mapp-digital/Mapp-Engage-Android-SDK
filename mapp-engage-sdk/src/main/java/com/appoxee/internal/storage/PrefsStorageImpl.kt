package com.appoxee.internal.storage

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.appoxee.internal.model.request.RegisterDevice
import com.appoxee.internal.model.response.DevicePayload
import com.appoxee.shared.AppoxeeOptions
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import java.util.Date

internal class PrefsStorageImpl(
    private val application: Application,
    private val dataValidityMs: Long
) : Storage {

    private val Application.dataStore: DataStore<Preferences> by preferencesDataStore(name = "EngageDataStore")

    private val devicePayloadKey = stringPreferencesKey("devicePayload")
    private val registerDeviceKey = stringPreferencesKey("registerDevice")
    private val timestampKey = longPreferencesKey("timestamp")
    private val appoxeeOptionsKey = stringPreferencesKey("appoxeeOptions")

    private suspend fun getTimestamp(): Long {
        return application.dataStore.data.first()[timestampKey] ?: 0
    }

    private suspend fun isCacheValid(): Boolean {
        val now = Date().time
        val lastSavedTimestamp = getTimestamp()
        return now - lastSavedTimestamp < dataValidityMs
    }

    override suspend fun saveDevicePayload(devicePayload: DevicePayload?) {
        application.dataStore.edit {
            val json = devicePayload?.toJSON()
            it[devicePayloadKey] = json.toString()
            it[timestampKey] = Date().time
        }
    }

    override suspend fun getDevicePayload(): DevicePayload? {
        if (!isCacheValid()) {
            application.dataStore.edit {
                it.remove(devicePayloadKey)
            }
            return null
        }

        val json = application.dataStore.data.first()[devicePayloadKey]
        return try {
            json?.let {
                DevicePayload.fromJSON(JSONObject(it))
            }
        } catch (e: Exception) {
            application.dataStore.edit {
                it.remove(devicePayloadKey)
            }
            null
        }
    }

    override suspend fun saveRegistrationDevice(registerDevice: RegisterDevice?) {
        application.dataStore.edit {
            val json = registerDevice?.asJson()?.getJSONObject("register")
            it[registerDeviceKey] = json.toString()
        }
    }

    override suspend fun getRegistrationDevice(): RegisterDevice? {
        return try {
            val json = application.dataStore.data.first()[registerDeviceKey]
            json?.let { RegisterDevice.fromJSON(JSONObject(it)) }
        } catch (e: Exception) {
            application.dataStore.edit {
                it.remove(registerDeviceKey)
            }
            null
        }
    }

    override suspend fun saveInitOptions(options: AppoxeeOptions?) {
        options?.toJSON().let {
            application.dataStore.edit { prefs ->
                prefs[appoxeeOptionsKey] = it.toString()
            }
        }
    }

    override suspend fun getInitOptions(): AppoxeeOptions? {
        return try {
            val json = application.dataStore.data.first()[appoxeeOptionsKey]
            json?.let { AppoxeeOptions.fromJSON(JSONObject(it)) }
        } catch (e: Exception) {
            application.dataStore.edit { it.remove(appoxeeOptionsKey) }
            null
        }
    }
}