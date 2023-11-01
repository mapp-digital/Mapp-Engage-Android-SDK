package com.appoxee.internal.network

import com.appoxee.internal.model.request.ActivationModel
import com.appoxee.internal.model.request.GetAppConfigModel
import com.appoxee.internal.model.request.GetDeviceModel
import com.appoxee.internal.model.request.MessageBody
import com.appoxee.internal.model.request.OptInModel
import com.appoxee.internal.model.request.OptOutModel
import com.appoxee.internal.model.request.RegisterDeviceModel
import com.appoxee.internal.model.request.RequestBody
import com.appoxee.internal.model.request.SetAliasModel
import com.appoxee.internal.model.response.AppConfigPayload
import com.appoxee.internal.model.response.DefaultResponse
import com.appoxee.internal.model.response.DevicePayload
import com.appoxee.internal.model.response.Metadata
import com.appoxee.internal.model.response.RegisterPayload
import com.appoxee.internal.model.response.ResponseData
import com.appoxee.internal.model.response.inapp.InappResponse
import com.appoxee.internal.model.response.inbox.InboxMessagesResponse
import com.appoxee.internal.network.exceptions.DeviceNotRegisteredException
import com.appoxee.internal.network.response.BaseAdapter
import com.appoxee.internal.network.response.InappAdapter
import com.appoxee.internal.network.response.InboxAdapter
import com.appoxee.internal.network.response.Response
import com.appoxee.internal.provider.DeviceProvider
import com.appoxee.internal.storage.Storage
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
    private val header = mapOf("X_KEY" to options.sdkKey)
    private val uniqueDeviceId = deviceProvider.getUniqueDeviceId()

    override suspend fun registerDevice(
        register: RegisterDeviceModel
    ): Response<ResponseData<RegisterPayload>> {
        val deviceModel = RequestBody(key = uniqueDeviceId, actions = register)

        val request = Request
            .Put(path = devicePathV3, requestBody = deviceModel)
            .addHeader(header)

        val response = networkClient.execute(request, BaseAdapter {
            RegisterPayload.fromJSON(it)
        })

        return response
    }

    override suspend fun getDevice(): Response<ResponseData<DevicePayload>> {
        val requestBody = RequestBody(key = uniqueDeviceId, GetDeviceModel())
        val request = Request
            .Put(path = devicePathV3, requestBody = requestBody)
            .addHeader(header)

        val response = networkClient.execute(request, BaseAdapter {
            DevicePayload.fromJSON(it.getJSONObject("get"))
        })

        return response
    }

    override suspend fun activate(timeSpent: Long): Response<ResponseData<DefaultResponse>> {
        val requestBody = RequestBody(key = uniqueDeviceId, ActivationModel(timeSpent))

        val request = Request
            .Put(path = devicePathV3, requestBody = requestBody)
            .addHeader(header)

        val response = networkClient.execute(request, BaseAdapter {
            DefaultResponse.fromJSON(it)
        })

        return response
    }

    override suspend fun setAlias(
        alias: String,
    ): Response<ResponseData<DefaultResponse>> {
        val requestBody = RequestBody(key = uniqueDeviceId, actions = SetAliasModel(alias))

        val request = Request
            .Put(path = devicePathV3, requestBody = requestBody)
            .addHeader(header)

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
            RequestBody(key = uniqueDeviceId, actions = OptInModel(pushToken))

        val request = Request
            .Put(path = devicePathV3, requestBody = requestBody)
            .addHeader(header)

        val response = networkClient.execute(request, BaseAdapter {
            DefaultResponse.fromJSON(it)
        })
        return response
    }

    override suspend fun optOut(
        pushTokenBk: String
    ): Response<ResponseData<DefaultResponse>> {
        val requestBody =
            RequestBody(key = uniqueDeviceId, actions = OptOutModel(pushTokenBk))

        val request = Request
            .Put(path = devicePathV3, requestBody = requestBody)
            .addHeader(header)

        val response = networkClient.execute(request, BaseAdapter {
            DefaultResponse.fromJSON(it)
        })
        return response
    }

    override suspend fun getAppConfig(): Response<ResponseData<AppConfigPayload>> {
        val requestBody = RequestBody(key = uniqueDeviceId, actions = GetAppConfigModel())

        val request = Request
            .Put(path = devicePathV3, requestBody = requestBody)
            .addHeader(header)

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

        val response = networkClient.execute(request, InboxAdapter())

        return response
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

        val response = networkClient.execute(request, InappAdapter())

        return response
    }
}