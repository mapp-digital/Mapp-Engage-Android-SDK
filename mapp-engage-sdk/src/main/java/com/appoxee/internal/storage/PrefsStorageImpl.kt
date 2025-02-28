package com.appoxee.internal.storage

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.appoxee.internal.model.request.RegisterDevice
import com.appoxee.internal.model.response.AppConfigPayload
import com.appoxee.internal.model.response.DevicePayload
import com.appoxee.internal.util.DispatchersProvider
import com.appoxee.internal.util.Logger
import com.appoxee.shared.AppoxeeOptions
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Date

private val Application.dataStore: DataStore<Preferences> by preferencesDataStore(name = "EngageDataStore")

internal class PrefsStorageImpl(
    context: Context, private val dataValidityMs: Long, private val dispatchersProvider: DispatchersProvider
) : Storage {

    private val devicePayloadKey = stringPreferencesKey("devicePayload")
    private val registerDeviceKey = stringPreferencesKey("registerDevice")
    private val timestampKey = longPreferencesKey("timestamp")
    private val appoxeeOptionsKey = stringPreferencesKey("appoxeeOptions")
    private val appConfigKey = stringPreferencesKey("appConfig")
    private val broadcastKey = stringPreferencesKey("localBroadcast")

    private val dataStore: DataStore<Preferences> by lazy { (context.applicationContext as Application).dataStore }

    private val mutex = Mutex()

    private suspend fun getTimestamp(): Long {
        return withContext(dispatchersProvider.defaultDispatcher) {
            dataStore.data.first()[timestampKey] ?: 0
        }
    }

    private suspend fun isCacheValid(): Boolean {
        return withContext(dispatchersProvider.defaultDispatcher) {
            val now = Date().time
            val lastSavedTimestamp = getTimestamp()
            now - lastSavedTimestamp < dataValidityMs
        }
    }

    override suspend fun clearRegistration() {
        withContext(dispatchersProvider.defaultDispatcher) {
            dataStore.edit {
                mutex.withLock {
                    it.remove(registerDeviceKey)
                    it.remove(devicePayloadKey)
                    it.remove(timestampKey)
                    it.remove(appConfigKey)
                    it.remove(appoxeeOptionsKey)
                    it.remove(broadcastKey)
                }
            }
        }
    }

    override suspend fun saveDevicePayload(devicePayload: DevicePayload?) {
        withContext(dispatchersProvider.defaultDispatcher) {
            dataStore.edit {
                mutex.withLock {
                    val json = devicePayload?.toJSON()
                    it[devicePayloadKey] = json.toString()
                    it[timestampKey] = Date().time
                }
            }
        }
    }

    override suspend fun getDevicePayload(): DevicePayload? {
        return withContext(dispatchersProvider.defaultDispatcher) {
            if (!isCacheValid()) {
                dataStore.edit {
                    mutex.withLock {
                        it.remove(devicePayloadKey)
                    }
                }
            }

            val json = mutex.withLock {
                dataStore.data.first()[devicePayloadKey]
            }
            try {
                json?.let {
                    DevicePayload.fromJSON(JSONObject(it))
                }
            } catch (e: Exception) {
                dataStore.edit {
                    mutex.withLock {
                        it.remove(devicePayloadKey)
                    }
                }
                null
            }
        }
    }

    override suspend fun saveRegistrationDevice(registerDevice: RegisterDevice?) {
        withContext(dispatchersProvider.defaultDispatcher) {
            dataStore.edit {
                mutex.withLock {
                    val json = registerDevice?.asJson()?.getJSONObject("register")
                    it[registerDeviceKey] = json.toString()
                }
            }
        }
    }

    override suspend fun getRegistrationDevice(): RegisterDevice? {
        return withContext(dispatchersProvider.defaultDispatcher) {
            try {
                mutex.withLock {
                    val json = dataStore.data.first()[registerDeviceKey]
                    json?.let { RegisterDevice.fromJSON(JSONObject(it)) }
                }
            } catch (e: Exception) {
                dataStore.edit {
                    mutex.withLock {
                        it.remove(registerDeviceKey)
                    }
                }
                null
            }
        }
    }

    override suspend fun saveInitOptions(options: AppoxeeOptions?) {
        withContext(dispatchersProvider.defaultDispatcher) {
            options?.toJSON().let {
                dataStore.edit { prefs ->
                    mutex.withLock {
                        prefs[appoxeeOptionsKey] = it.toString()
                    }
                }
            }
        }
    }

    override suspend fun getInitOptions(): AppoxeeOptions? {
        return withContext(dispatchersProvider.defaultDispatcher) {
            try {
                mutex.withLock {
                    val json = dataStore.data.first()[appoxeeOptionsKey]
                    json?.let { AppoxeeOptions.fromJSON(JSONObject(it)) }
                }
            } catch (e: Exception) {
                dataStore.edit {
                    mutex.withLock {
                        it.remove(appoxeeOptionsKey)
                    }
                }
                null
            }
        }
    }

    override suspend fun saveAppConfig(appConfigPayload: AppConfigPayload?) {
        withContext(dispatchersProvider.defaultDispatcher) {
            appConfigPayload?.toJSON()?.let { appConfig ->
                dataStore.edit { prefs ->
                    mutex.withLock {
                        prefs[appConfigKey] = appConfig.toString()
                    }
                }
            }
        }
    }

    override suspend fun getAppConfig(): AppConfigPayload? {
        return withContext(dispatchersProvider.defaultDispatcher) {
            try {
                mutex.withLock {
                    val json = dataStore.data.first()[appConfigKey]
                    json?.let { AppConfigPayload.fromJson(JSONObject(it)) }
                }
            } catch (e: Exception) {
                Logger.e(PrefsStorageImpl::class.java.name, e.message ?: "", e)
                dataStore.edit {
                    mutex.withLock {
                        it.remove(appConfigKey)
                    }
                }
                null
            }
        }
    }

    override suspend fun setBroadcastClass(clazz: Class<*>) {
        withContext(dispatchersProvider.defaultDispatcher) {
            dataStore.edit { prefs ->
                mutex.withLock {
                    prefs[broadcastKey] = clazz.name
                }
            }
        }
    }

    override suspend fun getBroadcastClass(): Class<*>? {
        return withContext(dispatchersProvider.defaultDispatcher) {
            try {
                mutex.withLock {
                    dataStore.data.first()[broadcastKey]?.let {
                        Class.forName(it)
                    }
                }
            } catch (e: Exception) {
                Logger.e(PrefsStorageImpl::class.java.name, e.message ?: "", e)
                null
            }
        }
    }
}