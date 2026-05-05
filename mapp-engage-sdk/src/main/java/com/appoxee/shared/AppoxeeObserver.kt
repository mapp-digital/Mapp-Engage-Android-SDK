package com.appoxee.shared

import com.appoxee.internal.model.response.DevicePayload

fun interface AppoxeeObserver {
    fun onReadyStatusChanged(status: Boolean, mappResult: MappResult<DevicePayload>)
}