package com.appoxee.internal

import android.annotation.SuppressLint
import com.appoxee.internal.model.request.RegisterDevice
import com.appoxee.internal.model.request.events.ClickActionType
import com.appoxee.internal.model.request.events.PushEventType
import com.appoxee.internal.model.request.events.TrackingKey
import com.appoxee.internal.model.response.AppConfigPayload
import com.appoxee.internal.model.response.DefaultResponse
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

    internal suspend fun register(deviceModel: RegisterDevice): RegisterPayload? {
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

    internal suspend fun addTags(tags: List<String>): Response<ResponseData<DefaultResponse>> {
        return engageApi.addTags(tags)
    }

    internal suspend fun removeTags(tags: List<String>): Response<ResponseData<DefaultResponse>> {
        return engageApi.removeTags(tags)
    }

    internal suspend fun addCustomAttributes(attributes: Map<String, Any?>): Response<ResponseData<DefaultResponse>> {
        return engageApi.addCustomAttributes(attributes)
    }

    internal suspend fun getCustomAttributes(attributes: List<String>): Response<ResponseData<Map<String, Any?>>> {
        return engageApi.getCustomAttributes(attributes)
    }

    internal suspend fun inappEvent(
        originalEventId: String,
        templateId: Long,
        trackingKey: TrackingKey,
        trackingAttributes: Map<String, Any> = emptyMap()
    ): Response<ResponseData<Boolean>> {
        return engageApi.inappEvent(
            originalEventId = originalEventId,
            templateId = templateId,
            trackingKey = trackingKey,
            trackingAttributes = trackingAttributes
        )
    }

    internal suspend fun pushEvent(
        messageId: Long,
        sendoutId: Long,
        clickActionType: ClickActionType,
        eventType: PushEventType
    ): Response<ResponseData<Boolean>> {
        return engageApi.pushEvent(messageId, sendoutId, clickActionType, eventType)
    }
}