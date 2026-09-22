package com.appoxee.internal

import android.annotation.SuppressLint
import androidx.annotation.VisibleForTesting
import com.appoxee.internal.model.request.RegisterDevice
import com.appoxee.internal.model.request.UpdateDevice
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
import com.appoxee.internal.util.LibraryExtensions.toUtcString
import com.appoxee.internal.util.Logger
import java.util.Date
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
        return getDevice()
    }

    internal suspend fun register(deviceModel: RegisterDevice): RegisterPayload? {
        val response = engageApi.registerDevice(deviceModel)
        val payload = if (response.isSuccess()) response.data?.payload else null
        payload?.let {
            val device = storage.getDevicePayload()
            storage.saveDevicePayload(
                DevicePayload(
                    dmcUserId = it.dmcUserId.takeIf { id -> id.isNotBlank() } ?: device?.dmcUserId,
                    udidHashed = device?.udidHashed,
                    pushTokenBk = device?.pushTokenBk,
                    pushToken = device?.pushToken,
                    alias = it.alias?.takeIf { alias -> alias.isNotBlank() } ?: device?.alias
                )
            )
        }
        return payload
    }

    internal suspend fun updateDevice(
        alias: String,
        params: Map<String, String>
    ): Response<ResponseData<DefaultResponse>> {
        val deviceToUpdate = UpdateDevice(params)
        val response = engageApi.updateDevice(alias = alias, updateDevice = deviceToUpdate)
        return response
    }

    internal suspend fun setAlias(
        alias: String,
        resendCustomAttributes: Boolean = false
    ): DevicePayload? {
        require(!(alias.isEmpty())) { throw IllegalArgumentException("Alias can not be empty!") }
        val device = storage.getDevicePayload()
        // new alias same as old alias
        if (Objects.equals(device?.alias, alias)) {
            return device
        }
        // alias has changed, update value to a server
        val response = engageApi.setAlias(alias)
        if (response.isSuccess() && response.data?.metadata?.statusCode == 200 && !response.data.metadata.error
        ) {
            val dmcUserId = response.data.payload?.dmcUserId
                ?.takeIf { it.isNotBlank() }
                ?: throw Throwable("Missing dmcUserId in set alias response")
            val updatedDevice = DevicePayload(
                dmcUserId = dmcUserId,
                udidHashed = device?.udidHashed,
                pushTokenBk = device?.pushTokenBk,
                pushToken = device?.pushToken,
                alias = alias
            )
            storage.saveDevicePayload(updatedDevice)
            if (resendCustomAttributes) {
                resyncCustomAttributes()
            }
            return updatedDevice
        } else {
            throw Throwable(
                response.error?.message ?: response.data?.metadata?.errorMessage
                ?: "Failed to set alias"
            )
        }
    }

    internal suspend fun getAlias(): String {
        return storage.getDevicePayload()?.alias ?: ""
    }

    internal suspend fun getDevice(): DevicePayload? {
        val result = engageApi.getDevice()
        if (result.isSuccess() && result.data?.metadata?.error != true) {
            result.data?.payload?.let { devicePayload ->
                storage.saveDevicePayload(devicePayload)
                storage.updateDeviceFetchTimestamp()
            }
            return result.data?.payload
        }
        return null
    }

    internal suspend fun optIn(pushToken: String, refreshDevice: Boolean = true): Boolean {
        val device = storage.getDevicePayload()
        if (pushToken == device?.pushToken) {
            return true
        }
        val response = engageApi.optIn(pushToken = pushToken)
        if (refreshDevice) getDevice()
        return response.isSuccess()
    }

    internal suspend fun optOut(pushToken: String, refreshDevice: Boolean = true): Boolean {
        val device = storage.getDevicePayload()
        if (Objects.equals(pushToken, device?.pushTokenBk)) {
            return true
        }
        val response = engageApi.optOut(pushTokenBk = pushToken)
        if (refreshDevice) getDevice()
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

    internal suspend fun addTags(tags: Set<String>): Response<ResponseData<DefaultResponse>> {
        val existingTags = storage.getTags()
        val tagsToSync = tags.filterNot { existingTags.contains(it) }.toList()
        if (tagsToSync.isNotEmpty()) {
            val response = engageApi.addTags(tagsToSync)
            if (response.isSuccess()) {
                storage.addTags(tagsToSync)
            }
            return response
        }
        return Response.success(200, null)
    }

    internal suspend fun removeTags(tags: Set<String>): Response<ResponseData<DefaultResponse>> {
        val existingTags = storage.getTags()
        val tagsToRemove = tags.filter { existingTags.contains(it) }.toList()
        if (tagsToRemove.isNotEmpty()) {
            val response = engageApi.removeTags(tagsToRemove)
            if (response.isSuccess()) {
                storage.removeTags(tagsToRemove)
            }
            return response
        }
        return Response.success(200, null)
    }

    internal suspend fun getTags(): List<String> {
        return storage.getTags()
    }

    internal suspend fun resyncCustomAttributes() {
        // get cached attributes
        val cachedAttributes = storage.getCustomAttributesCache().attributes
        if (cachedAttributes.isEmpty()) return
        val response = engageApi.addCustomAttributes(cachedAttributes)
        if (response.isSuccess()) {
            storage.setCustomAttributesCache(cachedAttributes)
        } else {
            Logger.e(
                "AppoxeeAdapter",
                "resyncCustomAttributes() failed: ${response.error?.message}"
            )
        }
    }

    internal suspend fun addCustomAttributes(attributes: Map<String, Any?>): Response<ResponseData<DefaultResponse>> {
        // get cached attributes
        val cachedAttributes = storage.getCustomAttributesCache()

        // holder map for real updates to a backend
        val attributesToUpdate = mutableMapOf<String, Any?>()

        // cache validity indicator
        val cacheIsValid = cachedAttributes.isCacheValid()

        // iterate over custom attributes and compare with cached values
        // take only non-existing or changed value to update to a backend
        attributes.forEach { (key, value) ->
            val cachedValue = cachedAttributes.attributes.getOrElse(key) { null }

            val valueToUpdate = if (value is Date) {
                value.toUtcString()
            } else {
                value
            }

            if (!cacheIsValid || cachedValue != valueToUpdate) {
                attributesToUpdate[key] = valueToUpdate
            }
        }

        // send custom attributes to a backend if map is not empty
        return if (attributesToUpdate.isNotEmpty()) {
            val response = engageApi.addCustomAttributes(attributesToUpdate)
            if (response.isSuccess()) {
                val mergedAttributes =
                    cachedAttributes.attributes.toMutableMap().apply { putAll(attributesToUpdate) }
                storage.setCustomAttributesCache(mergedAttributes)
            }
            response
        } else {
            // return success for case when there is no new custom attributes to update
            Response.success(
                data = ResponseData(),
                statusCode = 200
            )
        }
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
