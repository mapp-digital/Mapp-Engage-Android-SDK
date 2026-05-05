package com.appoxee.internal

import com.appoxee.internal.model.response.DevicePayload
import com.appoxee.shared.MappResult

internal fun interface AppoxeeObservable {
    suspend fun updateReadyStatus(status: Boolean, mappResult: MappResult<DevicePayload>)
}