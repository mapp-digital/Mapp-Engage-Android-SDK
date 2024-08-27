package com.appoxee.internal.storage

import com.appoxee.internal.model.request.RegisterDevice
import com.appoxee.internal.model.response.AppConfigPayload
import com.appoxee.internal.model.response.DevicePayload
import com.appoxee.shared.AppoxeeOptions

internal class InMemoryStorageImpl() : Storage {

    private var devicePayload: DevicePayload? = null
    private var registerDevice: RegisterDevice? = null
    private var initOptions: AppoxeeOptions? = null
    private var appConfigPayload: AppConfigPayload? = null
    private var clazz: Class<*>? = null

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
        this.clazz=clazz
    }

    override suspend fun getBroadcastClass(): Class<*>? {
        return clazz
    }
}