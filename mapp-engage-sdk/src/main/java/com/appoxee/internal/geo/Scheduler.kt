package com.appoxee.internal.geo

import androidx.work.Constraints
import androidx.work.Data
import java.util.concurrent.TimeUnit

interface Scheduler {
    suspend fun schedule(
        data: Data? = null,
        constraints: Constraints? = null,
        repeatIntervalMs: Long = TimeUnit.HOURS.toMillis(2)
    )

    fun cancel()
}