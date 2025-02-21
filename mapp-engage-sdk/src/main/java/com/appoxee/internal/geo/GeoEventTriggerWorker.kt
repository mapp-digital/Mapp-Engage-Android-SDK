package com.appoxee.internal.geo

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.appoxee.internal.container.AppoxeeContainer
import com.appoxee.internal.container.GeoContainer
import com.appoxee.internal.model.request.geo.GeoEvent
import com.appoxee.internal.util.Logger

/**
 * Worker to send geolocation event data
 * When device enters or exit some location of interest, Google location service triggers corresponding event.
 * SDK monitors for those events and sends them to the Mapp's backend server.
 * Mapp system uses this event to send a push message pre-scheduled for this event and location
 */
internal class GeoEventTriggerWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    companion object {
        val WORK_NAME = "geo_trigger_work"
    }

    private val TAG = GeoEventTriggerWorker::class.java.simpleName

    private val appoxeeContainer: AppoxeeContainer by lazy {
        AppoxeeContainer.getInstance(context)
    }

    private val geoContainer: GeoContainer
        get() = appoxeeContainer.geoContainer


    override suspend fun doWork(): Result {
        val geoEventIndex = inputData.getInt(GeoData.EVENT_KEY, GeoEvent.ENTER.ordinal)
        val geoEvent = GeoEvent.entries[geoEventIndex]
        val latitude = inputData.getDouble(GeoData.LATITUDE_KEY, 0.0)
        val longitude = inputData.getDouble(GeoData.LONGITUDE_KEY, 0.0)
        val regionId = inputData.getString(GeoData.REGION_ID_KEY)?.toLongOrNull() ?: 0
        val version = inputData.getInt(GeoData.VERSION_KEY, 0)
        try {
            val response =
                geoContainer.engageApi.regionEvent(
                    geoEvent,
                    latitude,
                    longitude,
                    regionId,
                    version
                )
            return if (response.isSuccess()) {
                Result.success()
            } else {
                Logger.e(TAG, response.error.toString())
                throw Exception(response.error)
            }
        } catch (e: Exception) {
            Logger.e(TAG, e.toString())
            return if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}