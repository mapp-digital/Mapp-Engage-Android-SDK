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

    @Test
    fun readiness_replay_does_not_duplicate_bootstrap_notification_or_notify_other_subscribers() {
        val provider = ObserversProvider()
        val firstCalls = mutableListOf<Boolean>()
        val secondCalls = mutableListOf<Boolean>()
        val first = AppoxeeObserver { status, _ -> firstCalls += status }
        val second = AppoxeeObserver { status, _ -> secondCalls += status }
        val result = MappResult.Success<DevicePayload>()

        provider.addObserver(first)
        provider.notify(true, result)
        provider.notifyIfUnnotified(first, true, result)
        provider.addObserver(second)
        provider.notifyIfUnnotified(second, true, result)

        assertThat(firstCalls).containsExactly(true)
        assertThat(secondCalls).containsExactly(true)
        // A later initialization still notifies both retained subscriptions.
        provider.notify(true, result)
        assertThat(firstCalls).containsExactly(true, true)
        assertThat(secondCalls).containsExactly(true, true)
    }

    @Test
    fun removing_a_subscription_prevents_its_pending_readiness_replay() {
        val provider = ObserversProvider()
        val calls = mutableListOf<Boolean>()
        val observer = AppoxeeObserver { status, _ -> calls += status }
        provider.addObserver(observer)
        provider.removeObserver(observer)

        provider.notifyIfUnnotified(observer, true, MappResult.Success())

        assertThat(calls).isEmpty()
    }
}
