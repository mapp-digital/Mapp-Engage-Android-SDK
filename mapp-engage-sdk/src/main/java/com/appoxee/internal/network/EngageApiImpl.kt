package com.appoxee.internal.network

import com.appoxee.internal.model.request.Activation
import com.appoxee.internal.model.request.AttributesGet
import com.appoxee.internal.model.request.AttributesSet
import com.appoxee.internal.model.request.GetAppConfig
import com.appoxee.internal.model.request.GetDevice
import com.appoxee.internal.model.request.MessageBody
import com.appoxee.internal.model.request.OptIn
import com.appoxee.internal.model.request.OptOut
import com.appoxee.internal.model.request.RegisterDevice
import com.appoxee.internal.model.request.RequestBody
import com.appoxee.internal.model.request.SetAlias
import com.appoxee.internal.model.request.Tags
import com.appoxee.internal.model.request.TagsAction
import com.appoxee.internal.model.request.events.InappEvent
import com.appoxee.internal.model.request.events.MessageContext
import com.appoxee.internal.model.request.events.Tracking
import com.appoxee.internal.model.request.events.TrackingKey
import com.appoxee.internal.model.response.AppConfigPayload
import com.appoxee.internal.model.response.DefaultResponse
import com.appoxee.internal.model.response.DevicePayload
import com.appoxee.internal.model.response.RegisterPayload
import com.appoxee.internal.model.response.ResponseData
import com.appoxee.internal.model.response.inapp.InappResponse
import com.appoxee.internal.model.response.inbox.InboxMessagesResponse
import com.appoxee.internal.network.exceptions.DeviceNotRegisteredException
import com.appoxee.internal.network.response.BaseAdapter
import com.appoxee.internal.network.response.InappAdapter
import com.appoxee.internal.network.response.InboxAdapter
import com.appoxee.internal.network.response.Response
import com.appoxee.internal.network.response.StatusAdapter
import com.appoxee.internal.provider.DeviceProvider
import com.appoxee.internal.storage.Storage
import com.appoxee.internal.util.toMap
import com.appoxee.shared.AppoxeeOptions
import java.util.Date
import java.util.UUID

internal class EngageApiImpl(
    private val networkClient: NetworkClient,
    private val storage: Storage,
    private val options: AppoxeeOptions,
    deviceProvider: DeviceProvider,
) :
    EngageApi {
    private val devicePathV3 = "api/v3/device"
    private val inboxPathV5 = "api/v5/device/inapp/inbox"
    private val inappPathV5 = "api/v5/device/nativeinapp"
    private val inappEventsPathV5 = "api/v5/device/inapp/tracking"

    private val sdkKeyHeader = mapOf("X_KEY" to options.sdkKey)
    private val uniqueDeviceId = deviceProvider.getUniqueDeviceId()

    override suspend fun registerDevice(
        register: RegisterDevice
    ): Response<ResponseData<RegisterPayload>> {
        val deviceModel = RequestBody(key = uniqueDeviceId, actions = register)

        val request = Request
            .Put(path = devicePathV3, requestBody = deviceModel)
            .addHeader(sdkKeyHeader)

        val response = networkClient.execute(request, BaseAdapter {
            RegisterPayload.fromJSON(it)
        })

        return response
    }

    override suspend fun getDevice(): Response<ResponseData<DevicePayload>> {
        val requestBody = RequestBody(key = uniqueDeviceId, GetDevice())
        val request = Request
            .Put(path = devicePathV3, requestBody = requestBody)
            .addHeader(sdkKeyHeader)

        val response = networkClient.execute(request, BaseAdapter {
            DevicePayload.fromJSON(it.getJSONObject("get"))
        })

        return response
    }

    override suspend fun activate(timeSpent: Long): Response<ResponseData<DefaultResponse>> {
        val requestBody = RequestBody(key = uniqueDeviceId, Activation(timeSpent))

        val request = Request
            .Put(path = devicePathV3, requestBody = requestBody)
            .addHeader(sdkKeyHeader)

        val response = networkClient.execute(request, BaseAdapter {
            DefaultResponse.fromJSON(it)
        })

        return response
    }

    override suspend fun setAlias(
        alias: String,
    ): Response<ResponseData<DefaultResponse>> {
        val requestBody = RequestBody(key = uniqueDeviceId, actions = SetAlias(alias))

        val request = Request
            .Put(path = devicePathV3, requestBody = requestBody)
            .addHeader(sdkKeyHeader)

        val response = networkClient.execute(request, BaseAdapter {
            DefaultResponse.fromJSON(it)
        })

        return response
    }

    override suspend fun getAlias(): Response<ResponseData<DevicePayload>> {
        return getDevice()
    }

    override suspend fun optIn(pushToken: String): Response<ResponseData<DefaultResponse>> {
        val requestBody =
            RequestBody(key = uniqueDeviceId, actions = OptIn(pushToken))

        val request = Request
            .Put(path = devicePathV3, requestBody = requestBody)
            .addHeader(sdkKeyHeader)

        val response = networkClient.execute(request, BaseAdapter {
            DefaultResponse.fromJSON(it)
        })
        return response
    }

    override suspend fun optOut(
        pushTokenBk: String
    ): Response<ResponseData<DefaultResponse>> {
        val requestBody =
            RequestBody(key = uniqueDeviceId, actions = OptOut(pushTokenBk))

        val request = Request
            .Put(path = devicePathV3, requestBody = requestBody)
            .addHeader(sdkKeyHeader)

        val response = networkClient.execute(request, BaseAdapter {
            DefaultResponse.fromJSON(it)
        })
        return response
    }

    override suspend fun getAppConfig(): Response<ResponseData<AppConfigPayload>> {
        val requestBody = RequestBody(key = uniqueDeviceId, actions = GetAppConfig())

        val request = Request
            .Put(path = devicePathV3, requestBody = requestBody)
            .addHeader(sdkKeyHeader)

        val response = networkClient.execute(request, BaseAdapter {
            AppConfigPayload.fromJson(it.getJSONObject("app_conf"))
        })

        return response
    }

    override suspend fun fetchInboxMessages(eventName: String): Response<InboxMessagesResponse> {
        val device =
            storage.getDevicePayload() ?: return Response.error(DeviceNotRegisteredException())
        val requestBody =
            MessageBody(
                timestamp = Date().time,
                id = UUID.randomUUID().toString(),
                userId = device.dmcUserId ?: "",
                alias = device.alias ?: "",
                eventKey = eventName
            )

        val request = Request.Post(path = inboxPathV5, requestBody = requestBody)
            .setPathType(Request.PathType.CEP)
            .addHeader(mapOf("tenant_id" to options.tenantId))
            .addHeader(mapOf("app_id" to options.appId))

        return networkClient.execute(request, InboxAdapter())
    }

    override suspend fun fetchInApp(eventName: String): Response<InappResponse> {
        val device =
            storage.getDevicePayload() ?: return Response.error(DeviceNotRegisteredException())

        val requestBody =
            MessageBody(
                timestamp = Date().time,
                id = UUID.randomUUID().toString(),
                userId = device.dmcUserId ?: "",
                alias = device.alias ?: "",
                eventKey = eventName
            )

        val request = Request.Post(path = inappPathV5, requestBody = requestBody)
            .setPathType(Request.PathType.CEP)
            .addHeader(mapOf("tenant_id" to options.tenantId))
            .addHeader(mapOf("app_id" to options.appId))

        return networkClient.execute(request, InappAdapter())
    }

    override suspend fun addTags(tags: List<String>): Response<ResponseData<DefaultResponse>> {
        val requestBody = RequestBody(key = uniqueDeviceId, Tags(tags, TagsAction.SET))
        val request = Request
            .Put(path = devicePathV3, requestBody = requestBody)
            .addHeader(sdkKeyHeader)

        val response = networkClient.execute(request, BaseAdapter {
            DefaultResponse.fromJSON(it)
        })

        return response
    }

    override suspend fun removeTags(tags: List<String>): Response<ResponseData<DefaultResponse>> {
        val requestBody = RequestBody(key = uniqueDeviceId, Tags(tags, TagsAction.REMOVE))
        val request = Request
            .Put(path = devicePathV3, requestBody = requestBody)
            .addHeader(sdkKeyHeader)

        val response = networkClient.execute(request, BaseAdapter {
            DefaultResponse.fromJSON(it)
        })

        return response
    }

    override suspend fun addCustomAttributes(attributes: Map<String, Any?>): Response<ResponseData<DefaultResponse>> {
        val alias = storage.getDevicePayload()?.alias ?: return Response.error(
            DeviceNotRegisteredException()
        )
        val attributeSet = AttributesSet(attributes = attributes)
        val requestBody = RequestBody(key = uniqueDeviceId, alias = alias, actions = attributeSet)
        val request = Request
            .Put(path = devicePathV3, requestBody = requestBody)
            .addHeader(sdkKeyHeader)
            .setPathType(Request.PathType.BASE)

        val response = networkClient.execute(request, BaseAdapter {
            DefaultResponse.fromJSON(it)
        })

        return response
    }

    override suspend fun getCustomAttributes(attributes: List<String>): Response<ResponseData<Map<String, Any?>>> {
        val alias = storage.getDevicePayload()?.alias ?: return Response.error(
            DeviceNotRegisteredException()
        )
        val attributeGet = AttributesGet(attributes = attributes)
        val requestBody = RequestBody(key = uniqueDeviceId, alias = alias, actions = attributeGet)
        val request = Request
            .Put(path = devicePathV3, requestBody = requestBody)
            .addHeader(sdkKeyHeader)

        val response = networkClient.execute(request, BaseAdapter {
            it.toMap<Any?>(excludeNulls = false)
        })

        return response
    }

    override suspend fun inappEvent(
        originalEventId: String,
        templateId: Long,
        trackingKey: TrackingKey,
        trackingAttributes: Map<String, Any>
    ): Response<ResponseData<Boolean>> {
        val device =
            storage.getDevicePayload() ?: return Response.error(DeviceNotRegisteredException())

        val messageContext =
            MessageContext(originalEventId = originalEventId, templateId = templateId)

        val tracking = Tracking(trackingKey = trackingKey, trackingAttributes = trackingAttributes)

        val requestBody =
            InappEvent(device = device, messageContext = messageContext, tracking = tracking)

        val request = Request.Post(path = inappEventsPathV5, requestBody = requestBody)
            .setPathType(Request.PathType.CEP)
            .addHeader(mapOf("tenant_id" to options.tenantId))
            .addHeader(mapOf("app_id" to options.appId))

        return networkClient.execute(request, StatusAdapter())
    }
}