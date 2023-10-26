package com.appoxee.internal

import com.appoxee.internal.model.response.DevicePayload

internal interface AppoxeeObservable {
    fun updateReadyStatus(status: Boolean, devicePayload: DevicePayload?)
}