package com.appoxee.internal.geo

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.appoxee.internal.geo.LocationUpdateWorker.Companion.WORK_NAME
import com.appoxee.internal.util.Dispatchers
import com.appoxee.internal.util.Logger
import com.appoxee.shared.GeoStatus
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

internal class LocationUpdateScheduler(
    private val context: Context,
    private val workManager: WorkManager,
    private val dispatchers: Dispatchers
) :
    Scheduler {

    private val TAG = LocationUpdateScheduler::class.java.simpleName

    private val coroutineContext = SupervisorJob() + dispatchers.ioDispatcher

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    override suspend fun schedule(
        data: Data?,
        constraints: Constraints?,
        repeatIntervalMs: Long
    ) = withContext(coroutineContext) {
        if (!hasRequiredPermissions()) {
            throw GeofenceException(GeoStatus.GeoLocationPermissionsNotGranted())
        }

        val workBuilder = PeriodicWorkRequestBuilder<LocationUpdateWorker>(2, TimeUnit.MINUTES)

        constraints?.let { workBuilder.setConstraints(it) }
        data?.let { workBuilder.setInputData(it) }
        workBuilder.setBackoffCriteria(BackoffPolicy.LINEAR, 1, TimeUnit.HOURS)

        val periodicWork = workBuilder.build()
        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            periodicWork
        )

        val workInfo = workManager.getWorkInfoById(periodicWork.id).get()

        Logger.d(TAG, workInfo.toString())
    }

    override fun cancel() {
        workManager.cancelUniqueWork(WORK_NAME)
    }

    private fun hasRequiredPermissions(): Boolean {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
            return true
        } else {
            val requiredPermissions = mutableListOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                requiredPermissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }

            val permissionStatus = requiredPermissions.map {
                context.checkSelfPermission(it)
            }.all { it == PackageManager.PERMISSION_GRANTED }
            return permissionStatus
        }
    }
}