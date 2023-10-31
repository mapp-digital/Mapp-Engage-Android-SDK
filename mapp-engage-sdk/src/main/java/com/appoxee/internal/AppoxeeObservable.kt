package com.appoxee.internal

import com.appoxee.internal.model.response.DevicePayload
import com.appoxee.shared.MappResult

internal interface AppoxeeObservable {
    fun updateReadyStatus(status: Boolean, mappResult: MappResult<DevicePayload>)
}