package com.appoxee.internal.network

import com.appoxee.internal.model.request.RegisterDevice
import com.appoxee.internal.model.request.events.TrackingKey
import com.appoxee.internal.model.response.AppConfigPayload
import com.appoxee.internal.model.response.DefaultResponse
import com.appoxee.internal.model.response.DevicePayload
import com.appoxee.internal.model.response.RegisterPayload
import com.appoxee.internal.model.response.ResponseData
import com.appoxee.internal.model.response.inapp.InappResponse
import com.appoxee.internal.model.response.inbox.InboxMessagesResponse
import com.appoxee.internal.network.response.Response

internal interface EngageApi {
    suspend fun registerDevice(
        register: RegisterDevice,
    ): Response<ResponseData<RegisterPayload>>

    suspend fun getDevice(): Response<ResponseData<DevicePayload>>
    suspend fun activate(timeSpent: Long): Response<ResponseData<DefaultResponse>>
    suspend fun setAlias(
        alias: String,
    ): Response<ResponseData<DefaultResponse>>

    suspend fun getAlias(): Response<ResponseData<DevicePayload>>

    suspend fun optIn(pushToken: String): Response<ResponseData<DefaultResponse>>
    suspend fun optOut(
        pushTokenBk: String,
    ): Response<ResponseData<DefaultResponse>>

    suspend fun getAppConfig(): Response<ResponseData<AppConfigPayload>>

    suspend fun fetchInboxMessages(eventName: String): Response<InboxMessagesResponse>

    suspend fun fetchInApp(eventName: String): Response<InappResponse>

    suspend fun addTags(tags: List<String>): Response<ResponseData<DefaultResponse>>

    suspend fun removeTags(tags: List<String>): Response<ResponseData<DefaultResponse>>

    suspend fun addCustomAttributes(attributes: Map<String, Any?>): Response<ResponseData<DefaultResponse>>
    suspend fun getCustomAttributes(attributes: List<String>): Response<ResponseData<Map<String, Any?>>>

    suspend fun inappEvent(
        originalEventId: String,
        templateId: Long,
        trackingKey: TrackingKey,
        trackingAttributes: Map<String, Any> = emptyMap()
    ): Response<ResponseData<Boolean>>
}