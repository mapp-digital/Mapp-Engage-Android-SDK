package com.appoxee.internal.network

import com.appoxee.internal.model.response.DefaultResponse
import com.appoxee.internal.model.response.DeviceResponse
import com.appoxee.internal.model.response.RegisterResponse
import com.appoxee.internal.model.response.Response

internal interface EngageApi {
    suspend fun register(
        deviceModel: NetworkData,
    ): Response<RegisterResponse>

    suspend fun getDevice(deviceId: String): Response<DeviceResponse>
    suspend fun activate(deviceId: String): Response<DefaultResponse>
    suspend fun setAlias(
        deviceId: String,
        alias: String,
        pushToken: String?
    ): Response<DefaultResponse>

    suspend fun optIn(deviceId: String, pushToken: String): Response<DefaultResponse>
    suspend fun optOut(
        deviceId: String,
        pushTokenBk: String,
    ): Response<DefaultResponse>
}