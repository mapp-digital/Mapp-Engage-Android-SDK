package com.appoxee.internal.network

import com.appoxee.internal.model.request.ActivationModel
import com.appoxee.internal.model.request.GetAppConfigModel
import com.appoxee.internal.model.request.GetDeviceModel
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
import com.appoxee.internal.network.response.BaseResponseAdapter
import com.appoxee.internal.provider.DeviceProvider
import com.appoxee.shared.AppoxeeOptions

internal class EngageApiImpl(
    private val networkClient: NetworkClient,
    deviceProvider: DeviceProvider,
    options: AppoxeeOptions
) :
    EngageApi {
    private val devicePathV3 = "api/v3/device"
    private val header = mapOf("X_KEY" to options.sdkKey)
    private val uniqueDeviceId = deviceProvider.getUniqueDeviceId()

    override suspend fun registerDevice(
        register: RegisterDeviceModel
    ): ResponseData<RegisterPayload> {
        val deviceModel = RequestBody(key = uniqueDeviceId, actions = register)

        val request = Request
            .Put(path = devicePathV3, requestBody = deviceModel)
            .addHeader(header)

        val response = networkClient.execute<RegisterPayload>(request, BaseResponseAdapter())

        return response.parse {
            RegisterPayload(it)
        }
    }

    override suspend fun getDevice(): ResponseData<DevicePayload> {
        val requestBody = RequestBody(key = uniqueDeviceId, GetDeviceModel())
        val request = Request
            .Put(path = devicePathV3, requestBody = requestBody)
            .addHeader(header)

        val response = networkClient.execute<DevicePayload>(request, BaseResponseAdapter())

        return response.parse {
            DevicePayload.fromJSON(it.getJSONObject("get"))
        }
    }

    override suspend fun activate(timeSpent: Long): ResponseData<DefaultResponse> {
        val requestBody = RequestBody(key = uniqueDeviceId, ActivationModel(timeSpent))

        val request = Request
            .Put(path = devicePathV3, requestBody = requestBody)
            .addHeader(header)

        val response = networkClient.execute<DefaultResponse>(request, BaseResponseAdapter())

        return response.parse { DefaultResponse(it) }
    }

    override suspend fun setAlias(
        alias: String,
    ): ResponseData<DefaultResponse> {
        val requestBody = RequestBody(key = uniqueDeviceId, actions = SetAliasModel(alias))

        val request = Request
            .Put(path = devicePathV3, requestBody = requestBody)
            .addHeader(header)

        val response = networkClient.execute<DefaultResponse>(request, BaseResponseAdapter())

        return response.parse { DefaultResponse(it) }
    }

    override suspend fun getAlias(): ResponseData<DevicePayload> {
        return getDevice()
    }

    override suspend fun optIn(pushToken: String): ResponseData<Boolean> {
        val requestBody =
            RequestBody(key = uniqueDeviceId, actions = OptInModel(pushToken))

        val request = Request
            .Put(path = devicePathV3, requestBody = requestBody)
            .addHeader(header)

        val response = networkClient.execute<Boolean>(request, BaseResponseAdapter())
        return response.parse { response.statusCode == 200 }
    }

    override suspend fun optOut(
        pushTokenBk: String
    ): ResponseData<Boolean> {
        val requestBody =
            RequestBody(key = uniqueDeviceId, actions = OptOutModel(pushTokenBk))

        val request = Request
            .Put(path = devicePathV3, requestBody = requestBody)
            .addHeader(header)

        val response = networkClient.execute<Boolean>(request, BaseResponseAdapter())
        return response.parse { response.statusCode == 200 }
    }

    override suspend fun getAppConfig(): ResponseData<AppConfigPayload> {
        val requestBody = RequestBody(key = uniqueDeviceId, actions = GetAppConfigModel())

        val request = Request
            .Put(path = devicePathV3, requestBody = requestBody)
            .addHeader(header)

        val response = networkClient.execute<AppConfigPayload>(request, BaseResponseAdapter())

        return response.parse { AppConfigPayload.fromJson(it.getJSONObject("app_conf")) }
    }
}