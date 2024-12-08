package com.appoxee.internal.geo

import android.Manifest
import androidx.annotation.RequiresPermission
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.appoxee.internal.geo.LocationUpdateWorker.Companion.WORK_NAME
import java.util.concurrent.TimeUnit

class LocationUpdateScheduler(private val workManager: WorkManager) :
    Scheduler {
    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    override fun schedule(
        data: Data?,
        constraints: Constraints?,
        repeatIntervalMs: Long
    ) {
        val work = PeriodicWorkRequestBuilder<LocationUpdateWorker>(2, TimeUnit.HOURS)

        constraints?.let { work.setConstraints(it) }
        data?.let { work.setInputData(it) }
        work.setBackoffCriteria(BackoffPolicy.LINEAR, 1, TimeUnit.HOURS)

        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            work.build()
        )
    }

    override fun cancel() {
        workManager.cancelUniqueWork(WORK_NAME)
    }
}