package com.appoxee.internal.geo

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.appoxee.internal.util.DispatchersProvider
import com.appoxee.internal.util.Logger
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

interface GeofenceScheduler {
    suspend fun postGeofenceEvent(data: Data?, constraints: Constraints? = null)

    suspend fun scheduleRefreshGeofencesPeriodicWorker(
        data: Data?,
        constraints: Constraints? = null,
        repeatIntervalMs: Long = TimeUnit.HOURS.toMillis(2)
    )

    fun cancel()
}

internal class GeofenceSchedulerImpl(
    dispatchersProvider: DispatchersProvider,
    private val workManager: WorkManager
) : GeofenceScheduler {
    private val TAG = this::class.java.simpleName

    private val coroutineContext = SupervisorJob() + dispatchersProvider.defaultDispatcher

    override suspend fun postGeofenceEvent(data: Data?, constraints: Constraints?) =
        withContext(coroutineContext) {
            val oneTimeWorkRequest = OneTimeWorkRequestBuilder<GeoEventTriggerWorker>()
                .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.SECONDS)

            data?.let { oneTimeWorkRequest.setInputData(it) }
            constraints?.let { oneTimeWorkRequest.setConstraints(it) }

            workManager.enqueueUniqueWork(
                GeoEventTriggerWorker.WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                oneTimeWorkRequest.build()
            )
            Unit
        }

    override suspend fun scheduleRefreshGeofencesPeriodicWorker(
        data: Data?,
        constraints: Constraints?,
        repeatIntervalMs: Long
    ) = withContext(coroutineContext) {
        val workBuilder = PeriodicWorkRequestBuilder<LocationUpdateWorker>(
            repeatIntervalMs,
            TimeUnit.MILLISECONDS
        ).setInitialDelay(repeatIntervalMs, TimeUnit.MILLISECONDS)

        constraints?.let { workBuilder.setConstraints(it) }
        data?.let { workBuilder.setInputData(it) }

        workBuilder.setBackoffCriteria(
            BackoffPolicy.LINEAR,
            60_000 * 5, // 5 minutes
            TimeUnit.MILLISECONDS
        )

        val periodicWork = workBuilder.build()
        workManager.enqueueUniquePeriodicWork(
            LocationUpdateWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            periodicWork
        )

        Logger.d(TAG, "Work scheduled successfully!")
    }

    override fun cancel() {
        workManager.cancelUniqueWork(LocationUpdateWorker.WORK_NAME)
        workManager.cancelUniqueWork(GeoEventTriggerWorker.WORK_NAME)
    }
}