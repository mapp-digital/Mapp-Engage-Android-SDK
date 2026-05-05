package com.appoxee.internal.provider

import androidx.annotation.Keep
import com.appoxee.internal.model.response.DevicePayload
import com.appoxee.shared.AppoxeeObserver
import com.appoxee.shared.MappResult

@Keep
internal class ObserversProvider {

    private val observers: MutableSet<AppoxeeObserver> = mutableSetOf()

    fun addObserver(observer: AppoxeeObserver) {
        observers.add(observer)
    }

    fun removeObserver(observer: AppoxeeObserver) {
        observers.remove(observer)
    }

    fun notify(status: Boolean, result: MappResult<DevicePayload>) {
        observers.forEach {
            it.onReadyStatusChanged(status, result)
        }
    }
}