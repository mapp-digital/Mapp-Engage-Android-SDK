package com.appoxee.internal

import android.annotation.SuppressLint
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
import com.appoxee.internal.util.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Objects

@SuppressLint("HardwareIds")
internal class AppoxeeAdapter(
    private val engageApi: EngageApi,
    private val storage: Storage,
    private val dispatchers: Dispatchers,
) {
    /**
     * Get device payload from server and save it to a local storage
     */
    private suspend fun refreshDevicePayload() {
        engageApi.getDevice().data?.payload?.let {
            storage.saveDevicePayload(it)
        }
    }

    internal suspend fun register(deviceModel: RegisterDevice): RegisterPayload? {
        return withContext(dispatchers.ioDispatcher) {
            val response = engageApi.registerDevice(deviceModel)
            if (response.isSuccess()) response.data?.payload else null
        }
    }

    internal suspend fun setAlias(alias: String): String? {
        return withContext(dispatchers.ioDispatcher) {
            val device = storage.getDevicePayload()
            // new alias same as old alias
            if (Objects.equals(device?.alias, alias)) {
                return@withContext device?.dmcUserId
            }
            // alias has changed, update value to a server
            val response = engageApi.setAlias(alias)
            // get new device payload from server and save it
            refreshDevicePayload()
            // return result
            response.data?.payload?.dmcUserId
        }
    }

    internal suspend fun getAlias(): String {
        return withContext(dispatchers.ioDispatcher) {
            val response = engageApi.getAlias()
            response.data?.payload?.alias ?: ""
        }
    }

    internal suspend fun getDevice(): DevicePayload? {
        return withContext(dispatchers.ioDispatcher) {
            val result = engageApi.getDevice()
            result.data?.payload
        }
    }

    internal suspend fun optIn(pushToken: String): Boolean {
        return withContext(dispatchers.ioDispatcher) {
            val device = storage.getDevicePayload()
            if (Objects.equals(pushToken, device?.pushToken)) {
                return@withContext true
            }
            val response = engageApi.optIn(pushToken = pushToken)
            refreshDevicePayload()
            return@withContext response.isSuccess()
        }
    }

    internal suspend fun optOut(pushToken: String): Boolean {
        return withContext(dispatchers.ioDispatcher) {
            val device = storage.getDevicePayload()
            if (Objects.equals(pushToken, device?.pushTokenBk)) {
                return@withContext true
            }
            val response = engageApi.optOut(pushTokenBk = pushToken)
            refreshDevicePayload()
            return@withContext response.isSuccess()
        }
    }

    internal suspend fun getAppConfig(): Response<ResponseData<AppConfigPayload>> {
        return withContext(dispatchers.ioDispatcher) { engageApi.getAppConfig() }
    }

    internal suspend fun fetchInboxMessages(event: String): InboxMessagesResponse? {
        return withContext(dispatchers.ioDispatcher) {
            val response = engageApi.fetchInboxMessages(eventName = event)
            response.data
        }
    }

    internal suspend fun fetchInappMessages(event: String): InappResponse? {
        return withContext(dispatchers.ioDispatcher) {
            val response = engageApi.fetchInApp(eventName = event)
            response.data
        }
    }

    internal suspend fun addTags(tags: List<String>): Response<ResponseData<DefaultResponse>> {
        return withContext(dispatchers.ioDispatcher) { engageApi.addTags(tags) }
    }

    internal suspend fun removeTags(tags: List<String>): Response<ResponseData<DefaultResponse>> {
        return withContext(dispatchers.ioDispatcher) { engageApi.removeTags(tags) }
    }

    internal suspend fun addCustomAttributes(attributes: Map<String, Any?>): Response<ResponseData<DefaultResponse>> {
        return withContext(dispatchers.ioDispatcher) { engageApi.addCustomAttributes(attributes) }
    }

    internal suspend fun getCustomAttributes(attributes: List<String>): Response<ResponseData<Map<String, Any?>>> {
        return withContext(dispatchers.ioDispatcher) { engageApi.getCustomAttributes(attributes) }
    }

    internal suspend fun getRegions(
        lat: Double,
        lng: Double,
        version: Int,
        pageSize: Int
    ): Response<ResponseData<RegionsResponse>> {
        return withContext(dispatchers.ioDispatcher) {
            engageApi.getRegions(lat, lng, version, pageSize)
        }
    }

    internal suspend fun eventRegions(
        geoEvent: GeoEvent,
        latitude: Double,
        longitude: Double,
        regionId: Long,
        version: Int
    ): Response<ResponseData<DefaultResponse>> {
        return withContext(dispatchers.ioDispatcher) {
            engageApi.regionEvent(geoEvent, latitude, longitude, regionId, version)
        }
    }

    internal suspend fun activate(timestamp: Long): Response<ResponseData<DefaultResponse>> {
        return withContext(dispatchers.ioDispatcher) {
            engageApi.activate(timestamp)
        }
    }

    internal suspend fun logout(
        device: RegisterDevice,
    ): Response<ResponseData<RegisterPayload>> =
        withContext(dispatchers.ioDispatcher) {
            engageApi.registerDevice(device)
        }
}