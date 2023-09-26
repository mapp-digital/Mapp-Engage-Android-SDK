package com.appoxee.internal.network

import com.appoxee.AppoxeeOptions
import com.appoxee.internal.model.request.DeviceModel
import com.appoxee.internal.model.response.DefaultResponse
import com.appoxee.internal.model.response.DeviceAttributes
import com.appoxee.internal.model.response.DeviceResponse
import com.appoxee.internal.model.response.Metadata
import com.appoxee.internal.model.response.Payload
import com.appoxee.internal.model.response.RegisterResponse
import com.appoxee.internal.model.response.Response
import com.appoxee.internal.util.toList

internal class EngageApiImpl(
    private val networkClient: NetworkClient,
    options: AppoxeeOptions
) :
    EngageApi {
    private val devicePathV3 = "api/v3/device"
    private val header = mapOf("X_KEY" to options.sdkKey)

    override suspend fun register(
        deviceModel: DeviceModel
    ): Response<RegisterResponse> {
        val request = Request.Put(path = devicePathV3, requestBody = deviceModel)
            .also {
                it.headers.putAll(header)
            }
        val response = networkClient.execute(request = request)
        val metadata = response?.getJSONObject("metadata")?.let {
            val error = it.getBoolean("error")
            val statusCode = it.getInt("statusCode")
            return@let Metadata(error, statusCode)
        }
        val payload = response?.getJSONObject("payload")?.let {
            val dmcUserId = it.getString("dmcUserId")
            val register = it.getJSONArray("register").toList()
            return@let Payload(
                RegisterResponse(
                    dmcUserId,
                    register
                )
            )
        }
        return Response(
            metadata = metadata,
            payload = payload
        )
    }

    override suspend fun getDevice(deviceId: String): Response<DeviceResponse> {
        val metadata = Metadata(false, 200)
        val deviceAttributes = DeviceAttributes()
        val deviceResponse = DeviceResponse(deviceAttributes)
        val payload = Payload(deviceResponse)
        return Response(metadata = metadata, payload = payload)
    }

    override suspend fun activate(deviceId: String): Response<DefaultResponse> {
        val metadata = Metadata(false, 200)
        val defaultResponse = DefaultResponse()
        val payload = Payload(defaultResponse)
        return Response(metadata = metadata, payload = payload)
    }

    override suspend fun setAlias(
        deviceId: String,
        alias: String,
        pushToken: String?
    ): Response<DefaultResponse> {
        val metadata = Metadata(false, 200)
        val defaultResponse = DefaultResponse()
        val payload = Payload(defaultResponse)
        return Response(metadata = metadata, payload = payload)
    }

    override suspend fun optIn(deviceId: String, pushToken: String): Response<DefaultResponse> {
        val metadata = Metadata(false, 200)
        val defaultResponse = DefaultResponse()
        val payload = Payload(defaultResponse)
        return Response(metadata = metadata, payload = payload)
    }

    override suspend fun optOut(deviceId: String, pushTokenBk: String): Response<DefaultResponse> {
        val metadata = Metadata(false, 200)
        val defaultResponse = DefaultResponse()
        val payload = Payload(defaultResponse)
        return Response(metadata = metadata, payload = payload)
    }
}