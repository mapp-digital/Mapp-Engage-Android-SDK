package com.appoxee.internal.storage

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.appoxee.internal.model.request.RegisterDeviceModel
import com.appoxee.internal.model.response.DevicePayload
import kotlinx.coroutines.flow.first
import org.json.JSONObject

internal class PrefsStorageImpl(private val application: Application) : Storage {

    val Application.dataStore: DataStore<Preferences> by preferencesDataStore(name = "EngageDataStore")

    private val devicePayloadKey = stringPreferencesKey("devicePayload")
    private val registerDeviceKey = stringPreferencesKey("registerDevice")

    override suspend fun saveDevicePayload(devicePayload: DevicePayload?) {
        application.dataStore.edit {
            val json = devicePayload?.toJSON()
            it[devicePayloadKey] = json.toString()
        }
    }

    override suspend fun getDevicePayload(): DevicePayload? {
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

    override suspend fun saveRegistrationDevice(registerDeviceModel: RegisterDeviceModel?) {
        application.dataStore.edit {
            val json = registerDeviceModel?.asJson()?.getJSONObject("register")
            it[registerDeviceKey] = json.toString()
        }
    }

    override suspend fun getRegistrationDevice(): RegisterDeviceModel? {
        return try {
            val json = application.dataStore.data.first()[registerDeviceKey]
            json?.let { RegisterDeviceModel.fromJSON(JSONObject(it)) }
        } catch (e: Exception) {
            application.dataStore.edit {
                it.remove(registerDeviceKey)
            }
            null
        }
    }
}