package com.appoxee.internal.network

import androidx.annotation.VisibleForTesting
import com.appoxee.internal.model.request.Activation
import com.appoxee.internal.model.request.GetAppConfig
import com.appoxee.internal.model.request.GetAttributes
import com.appoxee.internal.model.request.GetDevice
import com.appoxee.internal.model.request.MessageBody
import com.appoxee.internal.model.request.OptIn
import com.appoxee.internal.model.request.OptOut
import com.appoxee.internal.model.request.RegisterDevice
import com.appoxee.internal.model.request.RequestBody
import com.appoxee.internal.model.request.SetAlias
import com.appoxee.internal.model.request.SetAttributes
import com.appoxee.internal.model.request.Tags
import com.appoxee.internal.model.request.TagsAction
import com.appoxee.internal.model.request.UpdateDevice
import com.appoxee.internal.model.request.events.ClickType
import com.appoxee.internal.model.request.events.EventType
import com.appoxee.internal.model.request.events.InappEvent
import com.appoxee.internal.model.request.events.MessageContext
import com.appoxee.internal.model.request.events.PushEvent
import com.appoxee.internal.model.request.events.Tracking
import com.appoxee.internal.model.request.events.TrackingKey
import com.appoxee.internal.model.request.geo.GeoEvent
import com.appoxee.internal.model.request.geo.GetRegions
import com.appoxee.internal.model.request.geo.RegionStatus
import com.appoxee.internal.model.response.AppConfigPayload
import com.appoxee.internal.model.response.DefaultResponse
import com.appoxee.internal.model.response.DevicePayload
import com.appoxee.internal.model.response.RegisterPayload
import com.appoxee.internal.model.response.ResponseData
import com.appoxee.internal.model.response.attributes.CustomAttributesPayload
import com.appoxee.internal.model.response.geo.RegionsResponse
import com.appoxee.internal.model.response.inapp.InappResponse
import com.appoxee.internal.model.response.inbox.InboxMessagesResponse
import com.appoxee.internal.network.exceptions.DeviceNotRegisteredException
import com.appoxee.internal.network.response.BaseAdapter
import com.appoxee.internal.network.response.InappAdapter
import com.appoxee.internal.network.response.InboxAdapter
import com.appoxee.internal.network.response.Response
import com.appoxee.internal.network.response.ResponseAdapter
import com.appoxee.internal.network.response.StatusAdapter
import com.appoxee.internal.provider.DeviceProvider
import com.appoxee.internal.storage.Storage
import java.util.Date
import java.util.TimeZone
import java.util.UUID

internal class EngageApiImpl(
    val networkClient: NetworkClient,
    val storage: Storage,
    val deviceProvider: DeviceProvider,
) :
    EngageApi {
    private val devicePathV3 = "api/v3/device"
    private val inboxPathV5 = "api/v5/device/inapp/inbox"
    private val inappPathV5 = "api/v5/device/nativeinapp"
    private val inappEventsPathV5 = "api/v5/device/inapp/tracking"
    private val pushEventsPath = "api/push/event"

    private val uniqueDeviceId = deviceProvider.getUniqueDeviceId()

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    private suspend fun getSdkKeyHeader(): Map<String, String> {
        return storage.getInitOptions()?.let {
            mapOf("X_KEY" to it.sdkKey)
        } ?: throw DeviceNotRegisteredException()
    }

    private suspend fun getAppId(): String {
        return storage.getInitOptions()?.appId ?: throw DeviceNotRegisteredException()
    }

    private suspend fun getTenantId(): String {
        return storage.getInitOptions()?.tenantId ?: throw DeviceNotRegisteredException()
    }

    private suspend fun getDeviceOrError(): DevicePayload {
        return storage.getDevicePayload() ?: throw DeviceNotRegisteredException()
    }

    private suspend fun createMessageBody(
        eventName: String,
        device: DevicePayload
    ): MessageBody {
        return MessageBody(
            timestamp = Date().time,
            id = UUID.randomUUID().toString(),
            userId = device.dmcUserId ?: "",
            alias = device.alias ?: "",
            eventKey = eventName,
            deviceId = device.udidHashed ?: ""
        )
    }

    private suspend fun getCepHeaders(): Map<String, String> {
        return mapOf(
            "tenant_id" to getTenantId(),
            "app_id" to getAppId()
        )
    }

    private suspend fun <T> executeDevicePutRequest(
        actions: NetworkData,
        alias: String? = null,
        adapter: ResponseAdapter<T>,
        pathType: Request.PathType = Request.PathType.BASE
    ): Response<T> {
        val requestBody = RequestBody(
            key = uniqueDeviceId,
            actions = actions,
            alias = alias
        )
        val request = Request.Put(path = devicePathV3, requestBody = requestBody)
            .addHeader(getSdkKeyHeader())
            .setPathType(pathType)
        return networkClient.execute(request, adapter)
    }

    private suspend fun executeDefaultResponseRequest(
        actions: NetworkData,
        alias: String? = null,
        pathType: Request.PathType = Request.PathType.BASE
    ): Response<ResponseData<DefaultResponse>> {
        return executeDevicePutRequest(
            actions = actions,
            alias = alias,
            adapter = BaseAdapter { DefaultResponse.fromJSON(it) },
            pathType = pathType
        )
    }

    override suspend fun registerDevice(
        register: RegisterDevice
    ): Response<ResponseData<RegisterPayload>> {
        return executeDevicePutRequest(
            actions = register,
            adapter = BaseAdapter { RegisterPayload.fromJSON(it) }
        )
    }

    override suspend fun updateDevice(alias: String, updateDevice: UpdateDevice): Response<ResponseData<DefaultResponse>> {
        return executeDefaultResponseRequest(
            actions = updateDevice,
            alias = alias
        )
    }

    override suspend fun getDevice(): Response<ResponseData<DevicePayload>> {
        return executeDevicePutRequest(
            actions = GetDevice(),
            adapter = BaseAdapter { DevicePayload.fromJSON(it.getJSONObject("get")) }
        )
    }

    override suspend fun activate(timeSpent: Long): Response<ResponseData<DefaultResponse>> {
        val alias = storage.getDevicePayload()?.alias
            ?: return Response.error(DeviceNotRegisteredException())

        return executeDefaultResponseRequest(
            actions = Activation(timeSpent),
            alias = alias
        )
    }

    override suspend fun setAlias(
        alias: String,
    ): Response<ResponseData<DefaultResponse>> {
        return executeDefaultResponseRequest(
            actions = SetAlias(alias)
        )
    }

    override suspend fun getAlias(): Response<ResponseData<DevicePayload>> {
        return getDevice()
    }

    override suspend fun optIn(pushToken: String): Response<ResponseData<DefaultResponse>> {
        return executeDefaultResponseRequest(
            actions = OptIn(pushToken)
        )
    }

    override suspend fun optOut(
        pushTokenBk: String
    ): Response<ResponseData<DefaultResponse>> {
        return executeDefaultResponseRequest(
            actions = OptOut(pushTokenBk)
        )
    }

    override suspend fun getAppConfig(): Response<ResponseData<AppConfigPayload>> {
        return executeDevicePutRequest(
            actions = GetAppConfig(),
            adapter = BaseAdapter { AppConfigPayload.fromJson(it.getJSONObject("app_conf")) }
        )
    }

    override suspend fun fetchInboxMessages(eventName: String): Response<InboxMessagesResponse> {
        val device = try {
            getDeviceOrError()
        } catch (e: DeviceNotRegisteredException) {
            return Response.error(e)
        }

        val requestBody = createMessageBody(eventName, device)

        val request = Request.Post(path = inboxPathV5, requestBody = requestBody)
            .setPathType(Request.PathType.CEP)
            .addHeader(getCepHeaders())

        return networkClient.execute(request, InboxAdapter(eventKey = eventName))
    }

    override suspend fun fetchInApp(eventName: String): Response<InappResponse> {
        val device = try {
            getDeviceOrError()
        } catch (e: DeviceNotRegisteredException) {
            return Response.error(e)
        }

        val requestBody = createMessageBody(eventName, device)

        val request = Request.Post(path = inappPathV5, requestBody = requestBody)
            .setPathType(Request.PathType.CEP)
            .addHeader(getCepHeaders())

        return networkClient.execute(request, InappAdapter())
    }

    override suspend fun addTags(tags: List<String>): Response<ResponseData<DefaultResponse>> {
        return executeDefaultResponseRequest(
            actions = Tags(tags, TagsAction.SET)
        )
    }

    override suspend fun removeTags(tags: List<String>): Response<ResponseData<DefaultResponse>> {
        return executeDefaultResponseRequest(
            actions = Tags(tags, TagsAction.REMOVE)
        )
    }

    override suspend fun addCustomAttributes(attributes: Map<String, Any?>): Response<ResponseData<DefaultResponse>> {
        val alias = storage.getDevicePayload()?.alias
            ?: return Response.error(DeviceNotRegisteredException())

        return executeDefaultResponseRequest(
            actions = SetAttributes(attributes = attributes),
            alias = alias,
            pathType = Request.PathType.BASE
        )
    }

    override suspend fun getCustomAttributes(attributes: List<String>): Response<ResponseData<Map<String, Any?>>> {
        val alias = storage.getDevicePayload()?.alias
            ?: return Response.error(DeviceNotRegisteredException())

        return executeDevicePutRequest(
            actions = GetAttributes(attributes = attributes),
            alias = alias,
            adapter = BaseAdapter { CustomAttributesPayload.fromJson(it) }
        )
    }

    override suspend fun inappEvent(
        originalEventId: String,
        templateId: Long,
        trackingKey: TrackingKey,
        trackingAttributes: Map<String, *>
    ): Response<ResponseData<Boolean>> {
        val device = try {
            getDeviceOrError()
        } catch (e: DeviceNotRegisteredException) {
            return Response.error(e)
        }

        val messageContext = MessageContext(originalEventId = originalEventId, templateId = templateId)
        val tracking = Tracking(trackingKey = trackingKey, trackingAttributes = trackingAttributes)
        val requestBody = InappEvent(device = device, messageContext = messageContext, tracking = tracking)

        val request = Request.Post(path = inappEventsPathV5, requestBody = requestBody)
            .setPathType(Request.PathType.CEP)
            .addHeader(getCepHeaders())

        return networkClient.execute(request, StatusAdapter())
    }

    override suspend fun pushEvent(
        messageId: Long,
        sendoutId: Long,
        clickType: ClickType,
        eventType: EventType
    ): Response<ResponseData<Boolean>> {
        val dmcUserId =
            storage.getDevicePayload()?.dmcUserId ?: return Response.error(
                DeviceNotRegisteredException()
            )
        val pushEvent = PushEvent(
            tenantId = getTenantId(),
            messageId = messageId,
            sendoutId = sendoutId,
            dmcUserId = dmcUserId,
            eventType = eventType,
            clickType = clickType,
        )

        val request = Request.Post(path = pushEventsPath, requestBody = pushEvent)
            .addHeader(getSdkKeyHeader())
            .setPathType(Request.PathType.BASE)

        return networkClient.execute(request, StatusAdapter())
    }

    override suspend fun getRegions(
        lat: Double,
        lng: Double,
        version: Int,
        pageSize: Int
    ): Response<ResponseData<RegionsResponse>> {
        val alias = storage.getDevicePayload()?.alias
            ?: return Response.error(DeviceNotRegisteredException())

        val appId = getAppId().toLongOrNull() ?: 0L
        val getRegions = GetRegions(lat, lng, version, appId, pageSize)

        return executeDevicePutRequest(
            actions = getRegions,
            alias = alias,
            adapter = BaseAdapter { RegionsResponse.fromJSON(it) },
            pathType = Request.PathType.BASE
        )
    }

    override suspend fun regionEvent(
        geoEvent: GeoEvent,
        latitude: Double,
        longitude: Double,
        regionId: Long,
        version: Int
    ): Response<ResponseData<DefaultResponse>> {
        val device = try {
            getDeviceOrError()
        } catch (e: DeviceNotRegisteredException) {
            return Response.error(e)
        }

        val dmcUserId = device.dmcUserId
            ?: return Response.error(DeviceNotRegisteredException())

        val regionStatus = RegionStatus(
            timestamp = Date().time,
            geoEvent = geoEvent,
            dmcUserId = dmcUserId,
            latitude = latitude,
            longitude = longitude,
            regionId = regionId,
            timeZone = TimeZone.getDefault().displayName,
            version = version,
            applicationId = getAppId()
        )

        return executeDefaultResponseRequest(
            actions = regionStatus,
            pathType = Request.PathType.BASE
        )
    }
}