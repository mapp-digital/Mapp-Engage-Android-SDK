package com.appoxee.internal.storage

import com.appoxee.internal.model.common.CustomAttributesCache
import com.appoxee.internal.model.request.RegisterDevice
import com.appoxee.internal.model.response.AppConfigPayload
import com.appoxee.internal.model.response.DevicePayload
import com.appoxee.shared.AppoxeeOptions
import java.util.concurrent.TimeUnit

internal class InMemoryStorageImpl(private val cacheValidity: Long = TimeUnit.MINUTES.toMillis(1)) :
    Storage {

    private var devicePayload: DevicePayload? = null
    private var registerDevice: RegisterDevice? = null
    private var initOptions: AppoxeeOptions? = null
    private var appConfigPayload: AppConfigPayload? = null
    private var clazz: Class<*>? = null
    private var timestamp: Long = 0

    private var customAttributes: CustomAttributesCache =
        CustomAttributesCache(attributes = emptyMap())

    override suspend fun clearRegistration() {
        devicePayload = null
        registerDevice = null
        initOptions = null
        appConfigPayload = null
    }

    override suspend fun saveDevicePayload(devicePayload: DevicePayload?) {
        this.devicePayload = devicePayload
    }

    override suspend fun getDevicePayload(): DevicePayload? {
        return devicePayload
    }

    override suspend fun saveRegistrationDevice(registerDevice: RegisterDevice?) {
        this.registerDevice = registerDevice
    }

    override suspend fun getRegistrationDevice(): RegisterDevice? {
        return registerDevice
    }

    override suspend fun saveInitOptions(options: AppoxeeOptions?) {
        this.initOptions = options
    }

    override suspend fun getInitOptions(): AppoxeeOptions? {
        return initOptions
    }

    override suspend fun saveAppConfig(appConfigPayload: AppConfigPayload?) {
        this.appConfigPayload = appConfigPayload
    }

    override suspend fun getAppConfig(): AppConfigPayload? {
        return appConfigPayload
    }

    override suspend fun setBroadcastClass(clazz: Class<*>) {
        this.clazz = clazz
    }

    override suspend fun getBroadcastClass(): Class<*>? {
        return clazz
    }

    override suspend fun isCacheValid(): Boolean {
        return System.currentTimeMillis() - timestamp > cacheValidity
    }

    override suspend fun updateCacheTimestamp() {
        timestamp = System.currentTimeMillis()
    }

    override suspend fun getTimestamp(): Long {
        return timestamp
    }

    override suspend fun setCustomAttributesCache(attributes: Map<String, Any?>) {
        this.customAttributes= CustomAttributesCache(attributes = attributes)
    }

    override suspend fun getCustomAttributesCache(): CustomAttributesCache {
        return customAttributes
    }
}