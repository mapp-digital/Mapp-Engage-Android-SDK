package com.appoxee.internal.network

import com.appoxee.shared.AppoxeeOptions
import com.appoxee.internal.model.request.ActivationModel
import com.appoxee.internal.model.request.RequestBody
import com.appoxee.internal.model.request.GetDeviceModel
import com.appoxee.internal.model.request.OptInModel
import com.appoxee.internal.model.request.OptOutModel
import com.appoxee.internal.model.request.RegisterDeviceModel
import com.appoxee.internal.model.request.SetAliasModel
import com.appoxee.internal.model.response.DefaultResponse
import com.appoxee.internal.model.response.DevicePayload
import com.appoxee.internal.model.response.Metadata
import com.appoxee.internal.model.response.RegisterPayload
import com.appoxee.internal.model.response.ResponseData
import com.appoxee.internal.provider.DeviceProvider

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
        val request = Request.Put(path = devicePathV3, requestBody = deviceModel)
            .also {
                it.headers.putAll(header)
            }
        val response = networkClient.execute(request = request)
        val metadata = response?.getJSONObject("metadata")?.let {
            Metadata(it)
        }
        val payload = response?.getJSONObject("payload")?.let {
            RegisterPayload(it)
        }

        return ResponseData(
            metadata = metadata,
            payload = payload
        )
    }

    override suspend fun getDevice(): ResponseData<DevicePayload> {
        val requestBody = RequestBody(key = uniqueDeviceId, GetDeviceModel())
        val request = Request.Put(path = devicePathV3, requestBody = requestBody).also {
            it.headers.putAll(header)
        }
        val response = networkClient.execute(request)
        val metadata = response?.getJSONObject("metadata")?.let {
            Metadata(it)
        }
        val payload = response?.getJSONObject("payload")?.let {
            DevicePayload.fromJSON(it.getJSONObject("get"))
        }
        return ResponseData(metadata = metadata, payload = payload)
    }

    override suspend fun activate(timeSpent: Long): ResponseData<DefaultResponse> {
        val requestBody = RequestBody(key = uniqueDeviceId, ActivationModel(timeSpent))
        val request = Request.Put(path = devicePathV3, requestBody = requestBody).also {
            it.headers.putAll(header)
        }

        val response = networkClient.execute(request = request)
        val metadata = response?.getJSONObject("metadata")?.let {
            Metadata(it)
        }
        val payload = response?.getJSONObject("payload")?.let {
            DefaultResponse(it)
        }
        return ResponseData(metadata = metadata, payload = payload)
    }

    override suspend fun setAlias(
        alias: String,
    ): ResponseData<DefaultResponse> {
        val requestBody =
            RequestBody(key = uniqueDeviceId, actions = SetAliasModel(alias))

        val request = Request.Put(path = devicePathV3, requestBody = requestBody).also {
            it.headers.putAll(header)
        }

        val response = networkClient.execute(request = request)
        val metadata = response?.getJSONObject("metadata")?.let {
            Metadata(it)
        }
        val payload = response?.getJSONObject("payload")?.let {
            DefaultResponse(it)
        }
        return ResponseData(metadata = metadata, payload = payload)
    }

    override suspend fun getAlias(): ResponseData<DevicePayload> {
        return getDevice()
    }

    override suspend fun optIn(pushToken: String): ResponseData<Boolean> {
        val requestBody =
            RequestBody(key = uniqueDeviceId, actions = OptInModel(pushToken))

        val request = Request.Put(path = devicePathV3, requestBody = requestBody).also {
            it.headers.putAll(header)
        }

        val response = networkClient.execute(request = request)
        val metadata = response?.getJSONObject("metadata")?.let {
            Metadata(it)
        }
        return ResponseData(metadata = metadata, payload = true)
    }

    override suspend fun optOut(
        pushTokenBk: String
    ): ResponseData<Boolean> {
        val requestBody =
            RequestBody(key = uniqueDeviceId, actions = OptOutModel(pushTokenBk))

        val request = Request.Put(path = devicePathV3, requestBody = requestBody).also {
            it.headers.putAll(header)
        }

        val response = networkClient.execute(request = request)
        val metadata = response?.getJSONObject("metadata")?.let {
            Metadata(it)
        }
        return ResponseData(metadata = metadata, payload = true)
    }

}