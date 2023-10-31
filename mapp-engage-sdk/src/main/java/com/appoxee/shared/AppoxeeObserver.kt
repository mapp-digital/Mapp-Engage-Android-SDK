package com.appoxee.shared

import com.appoxee.internal.model.response.DevicePayload

interface AppoxeeObserver {
    fun onReadyStatusChanged(status: Boolean, mappResult: MappResult<DevicePayload>)
}