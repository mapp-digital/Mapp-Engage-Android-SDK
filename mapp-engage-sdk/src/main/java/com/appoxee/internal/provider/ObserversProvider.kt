package com.appoxee.internal.provider

import androidx.annotation.Keep
import com.appoxee.internal.model.response.DevicePayload
import com.appoxee.shared.AppoxeeObserver
import com.appoxee.shared.MappResult
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

@Keep
internal class ObserversProvider {

    private val observers = ConcurrentHashMap<AppoxeeObserver, AtomicBoolean>()

    fun addObserver(observer: AppoxeeObserver) {
        observers.putIfAbsent(observer, AtomicBoolean(false))
    }

    fun removeObserver(observer: AppoxeeObserver) {
        observers.remove(observer)
    }

    fun notify(status: Boolean, result: MappResult<DevicePayload>) {
        for ((observer, notified) in observers.entries) {
            notified.set(true)
            observer.onReadyStatusChanged(status, result)
        }
    }

    /** Avoid replaying readiness when bootstrap already notified this subscription. */
    fun notifyIfUnnotified(observer: AppoxeeObserver, status: Boolean, result: MappResult<DevicePayload>) {
        val notified = observers[observer] ?: return
        if (notified.compareAndSet(false, true)) observer.onReadyStatusChanged(status, result)
    }
}
