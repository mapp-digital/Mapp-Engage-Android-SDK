package com.appoxee.internal.storage

import android.app.Application
import com.appoxee.internal.model.request.RegisterDeviceModel
import com.appoxee.internal.model.response.DevicePayload

internal class InMemoryStorageImpl(private val application: Application) : Storage {

    private var devicePayload: DevicePayload? = null
    private var registerDeviceModel: RegisterDeviceModel? = null

    override suspend fun saveDevicePayload(devicePayload: DevicePayload?) {
        this.devicePayload = devicePayload
    }

    override suspend fun getDevicePayload(): DevicePayload? {
        return devicePayload
    }

    override suspend fun saveRegistrationDevice(registerDeviceModel: RegisterDeviceModel?) {
        this.registerDeviceModel = registerDeviceModel
    }

    override suspend fun getRegistrationDevice(): RegisterDeviceModel? {
        return registerDeviceModel
    }
}