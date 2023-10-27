package com.appoxee.internal

import android.annotation.SuppressLint
import com.appoxee.internal.model.request.RegisterDeviceModel
import com.appoxee.internal.model.response.AppConfigPayload
import com.appoxee.internal.model.response.DevicePayload
import com.appoxee.internal.model.response.RegisterPayload
import com.appoxee.internal.network.EngageApi

@SuppressLint("HardwareIds")
internal class AppoxeeAdapter(
    private val engageApi: EngageApi,
) {
    internal suspend fun register(deviceModel: RegisterDeviceModel): RegisterPayload? {
        return engageApi.registerDevice(deviceModel).payload
    }

    internal suspend fun setAlias(alias: String): String? {
        return engageApi.setAlias(alias).payload?.dmcUserId
    }

    internal suspend fun getAlias(): String {
        val response = engageApi.getDevice()
        return response.payload?.alias ?: ""
    }

    internal suspend fun getDevice(): DevicePayload? {
        val result = engageApi.getDevice()
        return result.payload
    }

    internal suspend fun optIn(pushToken: String): Boolean {
        return engageApi.optIn(pushToken = pushToken).payload ?: false
    }

    internal suspend fun optOut(pushToken: String): Boolean {
        return engageApi.optOut(pushTokenBk = pushToken).payload ?: false
    }

    internal suspend fun getAppConfig(): AppConfigPayload? {
        return engageApi.getAppConfig().payload
    }
}