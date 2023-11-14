package com.appoxee.internal.network

import com.appoxee.internal.model.request.RegisterDevice
import com.appoxee.internal.model.request.events.ClickActionType
import com.appoxee.internal.model.request.events.PushEventType
import com.appoxee.internal.model.request.events.TrackingKey
import com.appoxee.internal.model.request.geo.GeoEvent
import com.appoxee.internal.model.response.AppConfigPayload
import com.appoxee.internal.model.response.DefaultResponse
import com.appoxee.internal.model.response.DevicePayload
import com.appoxee.internal.model.response.RegisterPayload
import com.appoxee.internal.model.response.ResponseData
import com.appoxee.internal.model.response.geo.RegionsResponse
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

    suspend fun pushEvent(
        messageId: Long,
        sendoutId: Long,
        clickActionType: ClickActionType,
        eventType: PushEventType
    ): Response<ResponseData<Boolean>>

    suspend fun getRegions(
        lat: Double,
        lng: Double,
        version: Int,
        pageSize: Int
    ): Response<ResponseData<RegionsResponse>>

    suspend fun regionEvent(
        geoEvent: GeoEvent,
        latitude: Double,
        longitude: Double,
        regionId: Long,
        version: Int
    ): Response<ResponseData<DefaultResponse>>
}