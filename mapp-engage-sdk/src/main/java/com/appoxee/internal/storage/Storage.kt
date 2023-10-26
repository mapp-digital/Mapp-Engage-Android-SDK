package com.appoxee.internal.storage

import com.appoxee.internal.model.request.RegisterDeviceModel
import com.appoxee.internal.model.response.DevicePayload

internal interface Storage {
    suspend fun saveDevicePayload(devicePayload: DevicePayload?)

    suspend fun getDevicePayload(): DevicePayload?

    suspend fun saveRegistrationDevice(registerDeviceModel: RegisterDeviceModel?)

    suspend fun getRegistrationDevice(): RegisterDeviceModel?
}