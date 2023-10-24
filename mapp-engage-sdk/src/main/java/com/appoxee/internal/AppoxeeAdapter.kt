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
    private val deviceProvider: DeviceProvider,
    private val engageApi: EngageApi,
) {
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

    internal suspend fun setAlias(alias: String): ResponseData<DefaultResponse> {
        return engageApi.setAlias(alias, "")
    }

    internal suspend fun getAlias(): String {
        return ""
    }

    internal suspend fun getDevice(): ResponseData<DevicePayload> {
        return engageApi.getDevice()
    }
}