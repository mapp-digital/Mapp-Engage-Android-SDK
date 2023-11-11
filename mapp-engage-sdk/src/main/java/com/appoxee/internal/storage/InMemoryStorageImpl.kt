package com.appoxee.internal.storage

import android.app.Application
import com.appoxee.internal.model.request.RegisterDevice
import com.appoxee.internal.model.response.DevicePayload

internal class InMemoryStorageImpl(private val application: Application) : Storage {

    private var devicePayload: DevicePayload? = null
    private var registerDevice: RegisterDevice? = null

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
}