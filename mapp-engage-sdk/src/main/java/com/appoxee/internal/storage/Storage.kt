package com.appoxee.internal.storage

import com.appoxee.internal.model.common.CustomAttributesCache
import com.appoxee.internal.model.request.RegisterDevice
import com.appoxee.internal.model.response.AppConfigPayload
import com.appoxee.internal.model.response.DevicePayload
import com.appoxee.shared.AppoxeeOptions

internal interface Storage {

    suspend fun clearRegistration()

    suspend fun saveDevicePayload(devicePayload: DevicePayload?)

    suspend fun getDevicePayload(): DevicePayload?

    suspend fun saveRegistrationDevice(registerDevice: RegisterDevice?)

    suspend fun getRegistrationDevice(): RegisterDevice?

    suspend fun saveInitOptions(options: AppoxeeOptions?)

    suspend fun getInitOptions(): AppoxeeOptions?

    suspend fun saveAppConfig(appConfigPayload: AppConfigPayload?)

    suspend fun getAppConfig(): AppConfigPayload?

    suspend fun setBroadcastClass(clazz: Class<*>)

    suspend fun getBroadcastClass(): Class<*>?

    suspend fun isCacheValid(): Boolean

    suspend fun updateCacheTimestamp()

    suspend fun getTimestamp(): Long

    suspend fun addTags(tags: List<String>)

    suspend fun removeTags(tags: List<String>)

    suspend fun getTags(): List<String>

    /**
     * Store to a local cache custom attributes
     */
    suspend fun setCustomAttributesCache(attributes: Map<String, Any?>)

    /**
     * Get custom attributes local cached value
     */
    suspend fun getCustomAttributesCache(): CustomAttributesCache
}