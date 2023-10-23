package com.appoxee.internal

import android.annotation.SuppressLint
import android.content.Context
import com.appoxee.shared.AppoxeeOptions
import com.appoxee.internal.model.request.RegisterDeviceModel
import com.appoxee.internal.model.response.DefaultResponse
import com.appoxee.internal.model.response.DevicePayload
import com.appoxee.internal.model.response.RegisterPayload
import com.appoxee.internal.model.response.ResponseData
import com.appoxee.internal.network.EngageApi
import com.appoxee.internal.network.EngageApiImpl
import com.appoxee.internal.network.NetworkClientImpl
import com.appoxee.internal.provider.DeviceProvider

@SuppressLint("HardwareIds")
internal class AppoxeeAdapter(
    context: Context,
    private val deviceProvider: DeviceProvider,
    options: AppoxeeOptions
) {
    private val client = NetworkClientImpl(options)

    private val engageApi: EngageApi = EngageApiImpl(client, deviceProvider, options)

    internal suspend fun register(): ResponseData<RegisterPayload> {
        val device = RegisterDeviceModel(
            osName = deviceProvider.getOSName(),
            pushToken = "",
            appVersion = deviceProvider.getAppVersion(),
            clientVersion = deviceProvider.getClientVersion(),
            locale = deviceProvider.getLocale(),
            timeZone = deviceProvider.getTimeZone(),
            hardwareType = deviceProvider.getHardwareType(),
            density = deviceProvider.getDensity(),
            vendorID = deviceProvider.getVendorId(),
            osNumber = deviceProvider.getOSNumber(),
            resolution = deviceProvider.getResolution()
        )

        return engageApi.registerDevice(device)
    }

    suspend fun setAlias(alias: String): ResponseData<DefaultResponse> {
        return engageApi.setAlias(alias, "")
    }

    suspend fun getDevice(): ResponseData<DevicePayload> {
        return engageApi.getDevice()
    }

    suspend fun getAlias(): String {
        return ""
    }
}