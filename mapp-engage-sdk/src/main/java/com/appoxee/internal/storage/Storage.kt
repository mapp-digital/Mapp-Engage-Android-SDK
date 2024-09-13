package com.appoxee.internal.storage

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
}