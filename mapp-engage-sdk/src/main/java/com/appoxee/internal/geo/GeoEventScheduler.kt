package com.appoxee.internal.geo

import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class GeoEventScheduler(private val workManager: WorkManager) : Scheduler {
    private val WORK_NAME = "geo_trigger_work"
    override fun schedule(data: Data?, constraints: Constraints?, repeatIntervalMs: Long) {
        val oneTimeWorkRequest = OneTimeWorkRequestBuilder<GeoEventTriggerWorker>()

        data?.let { oneTimeWorkRequest.setInputData(it) }
        constraints?.let { oneTimeWorkRequest.setConstraints(it) }

        workManager.enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            oneTimeWorkRequest.build()
        )
    }

    override fun cancel() {
        workManager.cancelUniqueWork(WORK_NAME)
    }
}