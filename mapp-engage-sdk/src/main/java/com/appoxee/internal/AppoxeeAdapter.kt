package com.appoxee.internal

import android.annotation.SuppressLint
import com.appoxee.internal.model.request.RegisterDeviceModel
import com.appoxee.internal.model.response.AppConfigPayload
import com.appoxee.internal.model.response.DevicePayload
import com.appoxee.internal.model.response.RegisterPayload
import com.appoxee.internal.model.response.ResponseData
import com.appoxee.internal.model.response.inapp.InappResponse
import com.appoxee.internal.model.response.inbox.InboxMessagesResponse
import com.appoxee.internal.network.EngageApi
import com.appoxee.internal.network.response.Response
import com.appoxee.internal.storage.Storage
import java.util.Objects

@SuppressLint("HardwareIds")
internal class AppoxeeAdapter(
    private val engageApi: EngageApi,
    private val storage: Storage
) {
    /**
     * Get device payload from server and save it to a local storage
     */
    private suspend fun refreshDevicePayload() {
        engageApi.getDevice().data?.payload?.let {
            storage.saveDevicePayload(it)
        }
    }

    internal suspend fun register(deviceModel: RegisterDeviceModel): RegisterPayload? {
        val response = engageApi.registerDevice(deviceModel)
        return if (response.isSuccess()) response.data?.payload else null
    }

    internal suspend fun setAlias(alias: String): String? {
        val device = storage.getDevicePayload()
        // new alias same as old alias
        if (Objects.equals(device?.alias, alias)) {
            return device?.dmcUserId
        }
        // alias has changed, update value to a server
        val response = engageApi.setAlias(alias)
        // get new device payload from server and save it
        refreshDevicePayload()
        // return result
        return response.data?.payload?.dmcUserId
    }

    internal suspend fun getAlias(): String {
        val response = engageApi.getDevice()
        return response.data?.payload?.alias ?: ""
    }

    internal suspend fun getDevice(): DevicePayload? {
        val result = engageApi.getDevice()
        return result.data?.payload
    }

    internal suspend fun optIn(pushToken: String): Boolean {
        val device = storage.getDevicePayload()
        if (Objects.equals(pushToken, device?.pushToken)) {
            return true
        }
        val response = engageApi.optIn(pushToken = pushToken)
        refreshDevicePayload()
        return response.isSuccess()
    }

    internal suspend fun optOut(pushToken: String): Boolean {
        val device = storage.getDevicePayload()
        if (Objects.equals(pushToken, device?.pushTokenBk)) {
            return true
        }
        val response = engageApi.optOut(pushTokenBk = pushToken)
        refreshDevicePayload()
        return response.isSuccess()
    }

    internal suspend fun getAppConfig(): Response<ResponseData<AppConfigPayload>> {
        return engageApi.getAppConfig()
    }

    internal suspend fun fetchInboxMessages(event: String): InboxMessagesResponse? {
        val response = engageApi.fetchInboxMessages(eventName = event)
        return response.data
    }

    internal suspend fun fetchInappMessages(event: String): InappResponse? {
        val response = engageApi.fetchInApp(eventName = event)
        return response.data
    }
}