package com.appoxee.internal

import android.annotation.SuppressLint
import androidx.annotation.VisibleForTesting
import com.appoxee.internal.model.request.RegisterDevice
import com.appoxee.internal.model.request.geo.GeoEvent
import com.appoxee.internal.model.response.AppConfigPayload
import com.appoxee.internal.model.response.DefaultResponse
import com.appoxee.internal.model.response.DevicePayload
import com.appoxee.internal.model.response.RegisterPayload
import com.appoxee.internal.model.response.ResponseData
import com.appoxee.internal.model.response.geo.RegionsResponse
import com.appoxee.internal.model.response.inapp.InappResponse
import com.appoxee.internal.model.response.inbox.InboxMessagesResponse
import com.appoxee.internal.network.EngageApi
import com.appoxee.internal.network.response.Response
import com.appoxee.internal.storage.Storage
import java.util.Objects

@SuppressLint("HardwareIds")
internal class AppoxeeAdapter(
    private val engageApi: EngageApi,
    private val storage: Storage,
) {
    /**
     * Get device payload from server and save it to a local storage
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal suspend fun refreshDevicePayload(): DevicePayload? {
        val response = engageApi.getDevice()
        if (response.isSuccess()) {
            return response.data?.payload?.let {
                storage.saveDevicePayload(it)
                it
            }
        }
        return null
    }

    internal suspend fun register(deviceModel: RegisterDevice): RegisterPayload? {
        val response = engageApi.registerDevice(deviceModel)
        return if (response.isSuccess()) response.data?.payload else null
    }

    internal suspend fun setAlias(alias: String): DevicePayload? {
        if (alias.isEmpty()) throw IllegalArgumentException("Alias can not be empty!")
        val device = storage.getDevicePayload()
        // new alias same as old alias
        if (Objects.equals(device?.alias, alias)) {
            return device
        }
        // alias has changed, update value to a server
        val response = engageApi.setAlias(alias)
        if (response.isSuccess()) {
            val updatedDevice = refreshDevicePayload()
            return updatedDevice
        } else {
            throw Throwable(response.error?.message)
        }
    }

    internal suspend fun getAlias(): String {
        val response = engageApi.getAlias()
        return response.data?.payload?.alias ?: ""
    }

    internal suspend fun getDevice(): DevicePayload? {
        val result = engageApi.getDevice()
        return result.data?.payload
    }

    internal suspend fun optIn(pushToken: String): Boolean {
        val device = storage.getDevicePayload()
        if (Objects.equals(pushToken, device?.pushToken)) {
            return true
        }
        val response = engageApi.optIn(pushToken = pushToken)
        refreshDevicePayload()
        return response.isSuccess()
    }

    internal suspend fun optOut(pushToken: String): Boolean {
        val device = storage.getDevicePayload()
        if (Objects.equals(pushToken, device?.pushTokenBk)) {
            return true
        }
        val response = engageApi.optOut(pushTokenBk = pushToken)
        refreshDevicePayload()
        return response.isSuccess()
    }

    internal suspend fun getAppConfig(): Response<ResponseData<AppConfigPayload>> {
        return engageApi.getAppConfig()
    }

    internal suspend fun fetchInboxMessages(event: String): InboxMessagesResponse? {
        val response = engageApi.fetchInboxMessages(eventName = event)
        return if (response.isSuccess()) response.data
        else throw Throwable(response.error)
    }

    internal suspend fun fetchInappMessages(event: String): InappResponse? {
        val response = engageApi.fetchInApp(eventName = event)
        return response.data
    }

    internal suspend fun addTags(tags: List<String>): Response<ResponseData<DefaultResponse>> {
        return engageApi.addTags(tags)
    }

    internal suspend fun removeTags(tags: List<String>): Response<ResponseData<DefaultResponse>> {
        return engageApi.removeTags(tags)
    }

    internal suspend fun addCustomAttributes(attributes: Map<String, Any?>): Response<ResponseData<DefaultResponse>> {
        return engageApi.addCustomAttributes(attributes)
    }

    internal suspend fun getCustomAttributes(attributes: List<String>): Response<ResponseData<Map<String, Any?>>> {
        return engageApi.getCustomAttributes(attributes)
    }

    internal suspend fun getRegions(
        lat: Double,
        lng: Double,
        version: Int,
        pageSize: Int
    ): Response<ResponseData<RegionsResponse>> {
        return engageApi.getRegions(lat, lng, version, pageSize)
    }

    internal suspend fun eventRegions(
        geoEvent: GeoEvent,
        latitude: Double,
        longitude: Double,
        regionId: Long,
        version: Int
    ): Response<ResponseData<DefaultResponse>> {
        return engageApi.regionEvent(geoEvent, latitude, longitude, regionId, version)
    }

    internal suspend fun activate(timestamp: Long): Response<ResponseData<DefaultResponse>> {
        return engageApi.activate(timestamp)
    }

    internal suspend fun logout(
        device: RegisterDevice,
    ): Response<ResponseData<RegisterPayload>> =
        engageApi.registerDevice(device)
}