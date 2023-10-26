package com.appoxee.internal.network

import com.appoxee.internal.model.request.RegisterDeviceModel
import com.appoxee.internal.model.response.DefaultResponse
import com.appoxee.internal.model.response.DevicePayload
import com.appoxee.internal.model.response.RegisterPayload
import com.appoxee.internal.model.response.ResponseData

internal interface EngageApi {
    suspend fun registerDevice(
        register: RegisterDeviceModel,
    ): ResponseData<RegisterPayload>

    suspend fun getDevice(): ResponseData<DevicePayload>
    suspend fun activate(timeSpent: Long): ResponseData<DefaultResponse>
    suspend fun setAlias(
        alias: String,
    ): ResponseData<DefaultResponse>

    suspend fun getAlias(): ResponseData<DevicePayload>

    suspend fun optIn(pushToken: String): ResponseData<Boolean>
    suspend fun optOut(
        pushTokenBk: String,
    ): ResponseData<Boolean>
}