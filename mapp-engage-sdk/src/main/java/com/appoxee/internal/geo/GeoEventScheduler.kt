package com.appoxee.internal.geo

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.appoxee.internal.util.Dispatchers
import kotlinx.coroutines.withContext

internal class GeoEventScheduler(
    context: Context,
    private val workManager: WorkManager,
    private val dispatchers: Dispatchers
) : Scheduler {
    private val WORK_NAME = "geo_trigger_work"
    override suspend fun schedule(
        data: Data?,
        constraints: Constraints?,
        repeatIntervalMs: Long
    ) =
        withContext(dispatchers.ioDispatcher) {
            val oneTimeWorkRequest = OneTimeWorkRequestBuilder<GeoEventTriggerWorker>()

            data?.let { oneTimeWorkRequest.setInputData(it) }
            constraints?.let { oneTimeWorkRequest.setConstraints(it) }

            workManager.enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                oneTimeWorkRequest.build()
            )
            Unit
        }

    override fun cancel() {
        workManager.cancelUniqueWork(WORK_NAME)
    }
}