package com.appoxee.internal.storage

import android.app.Application
import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.appoxee.internal.model.common.CustomAttributesCache
import com.appoxee.internal.model.request.RegisterDevice
import com.appoxee.internal.model.response.AppConfigPayload
import com.appoxee.internal.model.response.DevicePayload
import com.appoxee.internal.util.DispatchersProvider
import com.appoxee.internal.util.Logger
import com.appoxee.shared.AppoxeeOptions
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.concurrent.TimeUnit

private val Application.dataStore: DataStore<Preferences> by preferencesDataStore(name = "EngageDataStore")

internal class PrefsStorageImpl(
    context: Context,
    private val dispatchersProvider: DispatchersProvider,
    private val dataValidityMs: Long = TimeUnit.DAYS.toMillis(1)
) : Storage {

    private val devicePayloadKey = stringPreferencesKey("devicePayload")
    private val registerDeviceKey = stringPreferencesKey("registerDevice")
    private val timestampKey = longPreferencesKey("timestamp")
    private val appoxeeOptionsKey = stringPreferencesKey("appoxeeOptions")
    private val appConfigKey = stringPreferencesKey("appConfig")
    private val broadcastKey = stringPreferencesKey("localBroadcast")

    private val tagsKey = stringSetPreferencesKey("tags")

    private val customAttributesKey = stringPreferencesKey("customAttributes")

    // DataStore is thread-safe and serialises writes internally via its own mutex;
    // adding a coroutine Mutex on top (especially inside edit{}) would create nested
    // lock acquisition and potential deadlocks. No extra locking is needed here.
    private val dataStore: DataStore<Preferences> by lazy { (context.applicationContext as Application).dataStore }

    override suspend fun getTimestamp(): Long {
        return withContext(dispatchersProvider.defaultDispatcher) {
            dataStore.data.first()[timestampKey] ?: 0
        }
    }

    override suspend fun addTags(tags: List<String>) {
        withContext(dispatchersProvider.defaultDispatcher) {
            dataStore.edit { prefs ->
                val existingTags = prefs[tagsKey].orEmpty().toMutableSet()
                existingTags.addAll(tags)
                prefs[tagsKey] = existingTags
            }
        }
    }

    override suspend fun removeTags(tags: List<String>) {
        withContext(dispatchersProvider.defaultDispatcher) {
            dataStore.edit { prefs ->
                val existingTags = prefs[tagsKey].orEmpty().toMutableSet()
                existingTags.removeAll(tags)
                prefs[tagsKey] = existingTags
            }
        }
    }

    override suspend fun getTags(): List<String> {
        return withContext(dispatchersProvider.defaultDispatcher) {
            dataStore.data.first()[tagsKey].orEmpty().toList()
        }
    }

    override suspend fun setCustomAttributesCache(attributes: Map<String, Any?>) {
        withContext(dispatchersProvider.defaultDispatcher) {
            dataStore.edit { prefs ->
                val customAttrCache = prefs[customAttributesKey]?.let {
                    CustomAttributesCache.fromJson(
                        JSONObject(it)
                    )
                } ?: CustomAttributesCache(attributes = emptyMap())

                val map = mutableMapOf<String, Any?>()

                if (customAttrCache.attributes.isNotEmpty()) {
                    map.putAll(customAttrCache.attributes)
                }
                if (attributes.isNotEmpty()) {
                    map.putAll(attributes)
                }

                val customAttributesCache = CustomAttributesCache(map)
                prefs[customAttributesKey] = customAttributesCache.toJson().toString()
            }
        }
    }

    override suspend fun getCustomAttributesCache(): CustomAttributesCache {
        return withContext(dispatchersProvider.defaultDispatcher) {
            dataStore.data.first()[customAttributesKey]?.let { json ->
                CustomAttributesCache.fromJson(JSONObject(json))
            } ?: CustomAttributesCache(attributes = emptyMap())
        }
    }

    override suspend fun removeCustomAttributes(attributes: Set<String>): Boolean {
        return withContext(dispatchersProvider.defaultDispatcher) {
            dataStore.edit { prefs ->
                val customAttrCache = prefs[customAttributesKey]?.let {
                    CustomAttributesCache.fromJson(
                        JSONObject(it)
                    )
                } ?: CustomAttributesCache(emptyMap())

                val data = customAttrCache.attributes.filterKeys { !attributes.contains(it) }

                prefs[customAttributesKey] = CustomAttributesCache(data).toJson().toString()
            }
            true
        }
    }

    override suspend fun updateCacheTimestamp() {
        withContext(dispatchersProvider.defaultDispatcher) {
            dataStore.edit { prefs ->
                prefs[timestampKey] = System.currentTimeMillis()
            }
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    override suspend fun isCacheValid(): Boolean {
        return withContext(dispatchersProvider.defaultDispatcher) {
            val now = System.currentTimeMillis()
            val lastSavedTimestamp = getTimestamp()
            now - lastSavedTimestamp < dataValidityMs
        }
    }


    override suspend fun clearRegistration() {
        withContext(dispatchersProvider.defaultDispatcher) {
            dataStore.edit { prefs ->
                prefs.remove(registerDeviceKey)
                prefs.remove(devicePayloadKey)
                prefs.remove(timestampKey)
                prefs.remove(appConfigKey)
                prefs.remove(appoxeeOptionsKey)
                prefs.remove(broadcastKey)
            }
        }
    }

    override suspend fun saveDevicePayload(devicePayload: DevicePayload?) {
        withContext(dispatchersProvider.defaultDispatcher) {
            dataStore.edit { prefs ->
                if (devicePayload == null) {
                    prefs.remove(devicePayloadKey)
                } else {
                    val json = devicePayload.toJSON()
                    prefs[devicePayloadKey] = json.toString()
                }
            }
        }
    }

    override suspend fun getDevicePayload(): DevicePayload? {
        return withContext(dispatchersProvider.defaultDispatcher) {
            val json = dataStore.data.first()[devicePayloadKey]
            try {
                json?.let {
                    DevicePayload.fromJSON(JSONObject(it))
                }
            } catch (e: Exception) {
                Logger.e(PrefsStorageImpl::class.java.name, "Failed to deserialize DevicePayload, clearing: ${e.message}", e)
                dataStore.edit { prefs ->
                    prefs.remove(devicePayloadKey)
                }
                null
            }
        }
    }

    override suspend fun saveRegistrationDevice(registerDevice: RegisterDevice?) {
        withContext(dispatchersProvider.defaultDispatcher) {
            dataStore.edit { prefs ->
                if (registerDevice == null) {
                    prefs.remove(registerDeviceKey)
                } else {
                    val json = registerDevice.asJson().getJSONObject("register")
                    prefs[registerDeviceKey] = json.toString()
                }
            }
        }
    }

    override suspend fun getRegistrationDevice(): RegisterDevice? {
        return withContext(dispatchersProvider.defaultDispatcher) {
            try {
                val json = dataStore.data.first()[registerDeviceKey]
                json?.let { RegisterDevice.fromJSON(JSONObject(it)) }
            } catch (e: Exception) {
                Logger.e(PrefsStorageImpl::class.java.name, "Failed to deserialize RegisterDevice, clearing: ${e.message}", e)
                dataStore.edit { prefs ->
                    prefs.remove(registerDeviceKey)
                }
                null
            }
        }
    }

    override suspend fun saveInitOptions(options: AppoxeeOptions?) {
        withContext(dispatchersProvider.defaultDispatcher) {
            options?.toJSON().let { json ->
                dataStore.edit { prefs ->
                    prefs[appoxeeOptionsKey] = json.toString()
                }
            }
        }
    }

    override suspend fun getInitOptions(): AppoxeeOptions? {
        return withContext(dispatchersProvider.defaultDispatcher) {
            try {
                val json = dataStore.data.first()[appoxeeOptionsKey]
                json?.let { AppoxeeOptions.fromJSON(JSONObject(it)) }
            } catch (e: Exception) {
                Logger.e(PrefsStorageImpl::class.java.name, "Failed to deserialize AppoxeeOptions, clearing: ${e.message}", e)
                dataStore.edit { prefs ->
                    prefs.remove(appoxeeOptionsKey)
                }
                null
            }
        }
    }

    override suspend fun saveAppConfig(appConfigPayload: AppConfigPayload?) {
        withContext(dispatchersProvider.defaultDispatcher) {
            appConfigPayload?.toJSON()?.let { appConfig ->
                dataStore.edit { prefs ->
                    prefs[appConfigKey] = appConfig.toString()
                }
            }
        }
    }

    override suspend fun getAppConfig(): AppConfigPayload? {
        return withContext(dispatchersProvider.defaultDispatcher) {
            try {
                val json = dataStore.data.first()[appConfigKey]
                json?.let { AppConfigPayload.fromJson(JSONObject(it)) }
            } catch (e: Exception) {
                Logger.e(PrefsStorageImpl::class.java.name, e.message ?: "", e)
                dataStore.edit { prefs ->
                    prefs.remove(appConfigKey)
                }
                null
            }
        }
    }

    override suspend fun setBroadcastClass(clazz: Class<*>) {
        withContext(dispatchersProvider.defaultDispatcher) {
            dataStore.edit { prefs ->
                prefs[broadcastKey] = clazz.name
            }
        }
    }

    override suspend fun getBroadcastClass(): Class<*>? {
        return withContext(dispatchersProvider.defaultDispatcher) {
            try {
                dataStore.data.first()[broadcastKey]?.let { className ->
                    val clazz = Class.forName(className)
                    // Validate the stored class is still a valid LocalPushBroadcast subtype
                    if (com.appoxee.shared.LocalPushBroadcast::class.java.isAssignableFrom(clazz)) {
                        clazz
                    } else {
                        Logger.e(PrefsStorageImpl::class.java.name, "Stored broadcast class $className is not a LocalPushBroadcast subtype")
                        null
                    }
                }
            } catch (e: Exception) {
                Logger.e(PrefsStorageImpl::class.java.name, e.message ?: "", e)
                null
            }
        }
    }
}
