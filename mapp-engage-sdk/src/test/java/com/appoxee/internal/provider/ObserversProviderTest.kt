package com.appoxee.internal.provider

import com.appoxee.internal.model.response.DevicePayload
import com.appoxee.shared.AppoxeeObserver
import com.appoxee.shared.MappResult
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ObserversProviderTest {

    @Test
    fun notify_calls_each_unique_observer_and_remove_stops_notifications() {
        val provider = ObserversProvider()
        val calls = mutableListOf<Boolean>()
        val observer = AppoxeeObserver { status, _ -> calls += status }
        val result = MappResult.Success<DevicePayload>()

        provider.addObserver(observer)
        provider.addObserver(observer)
        provider.notify(true, result)
        provider.removeObserver(observer)
        provider.notify(false, result)

        assertThat(calls).containsExactly(true)
    }
}
