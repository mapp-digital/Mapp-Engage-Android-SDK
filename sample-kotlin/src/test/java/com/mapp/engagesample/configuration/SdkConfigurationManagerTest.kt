package com.mapp.engagesample.configuration

import com.appoxee.shared.AppoxeeOptions
import com.appoxee.shared.NotificationMode
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SdkConfigurationManagerTest {
    private fun configuration(key: String = "build-key") = SdkConfiguration(
        AppoxeeOptions.Server.L3, key, "app", "tenant",
        NotificationMode.BACKGROUND_AND_FOREGROUND, AppoxeeOptions.LogLevel.DEBUG,
    )

    private class Store(var current: SdkConfiguration? = null) : SdkConfigurationStorage {
        val writes = mutableListOf<SdkConfiguration?>()
        val startedWrites = mutableListOf<SdkConfiguration?>()
        var nextSaveGate: CompletableDeferred<Unit>? = null
        var saveError: Exception? = null
        override fun load() = current
        override suspend fun save(configuration: SdkConfiguration?) {
            startedWrites.add(configuration)
            val gate = nextSaveGate
            nextSaveGate = null
            gate?.await()
            saveError?.let { throw it }
            current = configuration
            writes.add(configuration)
        }
    }

    private class Client : SdkConfigurationClient {
        val applied = mutableListOf<SdkConfiguration>()
        val callbacks = mutableListOf<(Boolean, Throwable?) -> Unit>()
        var active: SdkConfiguration? = null
        var ready = false
        var applyError: Exception? = null
        override fun apply(configuration: SdkConfiguration, onStatus: (Boolean, Throwable?) -> Unit) {
            applyError?.let { throw it }
            if (active != configuration) ready = false
            active = configuration
            applied.add(configuration)
            callbacks.add(onStatus)
        }
        override fun isReady() = ready
        fun succeed(index: Int = callbacks.lastIndex) {
            if (index == callbacks.lastIndex) ready = true
            callbacks[index](true, null)
        }
        fun fail(index: Int = callbacks.lastIndex) {
            if (index == callbacks.lastIndex) ready = false
            callbacks[index](false, IllegalStateException("Rejected configuration"))
        }
    }

    @Test
    fun `startup prefers saved settings and does not rewrite them`() = runTest {
        val saved = configuration("saved-key")
        val store = Store(saved)
        val client = Client()
        val manager = SdkConfigurationManager(configuration(), store, client, this)
        manager.start()
        manager.start()
        assertThat(client.applied).containsExactly(saved)
        assertThat(manager.state.value.source).isEqualTo(ConfigurationSource.SAVED)
        client.succeed()
        runCurrent()
        assertThat(manager.state.value.status).isEqualTo(ConfigurationStatus.READY)
        assertThat(store.writes).isEmpty()
    }

    @Test
    fun `new settings are saved only after readiness and used on restart`() = runTest {
        val store = Store()
        val client = Client()
        val manager = SdkConfigurationManager(configuration(), store, client, this)
        manager.start()
        client.succeed()
        runCurrent()
        val next = configuration("new-key").copy(
            server = AppoxeeOptions.Server.TEST_55,
            notificationMode = NotificationMode.SILENT_ONLY,
            logLevel = AppoxeeOptions.LogLevel.RELEASE,
        )
        manager.apply(next)
        assertThat(store.current).isNull()
        assertThat(manager.state.value.status).isEqualTo(ConfigurationStatus.APPLYING)
        client.succeed()
        runCurrent()
        assertThat(store.current).isEqualTo(next)
        assertThat(manager.state.value.status).isEqualTo(ConfigurationStatus.READY)
        val restartedClient = Client()
        SdkConfigurationManager(configuration(), store, restartedClient, this).start()
        assertThat(restartedClient.applied).containsExactly(next)
    }

    @Test
    fun `failed apply leaves the previously saved startup configuration intact`() = runTest {
        val saved = configuration("last-good")
        val store = Store(saved)
        val client = Client()
        val manager = SdkConfigurationManager(configuration(), store, client, this)
        manager.start()
        client.succeed()
        runCurrent()
        manager.apply(configuration("rejected"))
        client.fail()
        runCurrent()
        assertThat(manager.state.value.status).isEqualTo(ConfigurationStatus.FAILED)
        assertThat(store.current).isEqualTo(saved)
        assertThat(store.writes).isEmpty()
        assertThat(manager.state.value.error).isEqualTo("Rejected configuration")
    }

    @Test
    fun `build defaults clear the override only after successful application`() = runTest {
        val saved = configuration("saved")
        val defaults = configuration()
        val store = Store(saved)
        val client = Client()
        val manager = SdkConfigurationManager(defaults, store, client, this)
        manager.start()
        client.succeed()
        runCurrent()
        manager.apply(defaults)
        assertThat(store.current).isEqualTo(saved)
        client.succeed()
        runCurrent()
        assertThat(store.current).isNull()
        assertThat(manager.state.value.source).isEqualTo(ConfigurationSource.BUILD_DEFAULTS)
        val restarted = Client()
        SdkConfigurationManager(defaults, store, restarted, this).start()
        assertThat(restarted.applied).containsExactly(defaults)
    }

    @Test
    fun `already ready configuration saves even without another SDK callback`() = runTest {
        val defaults = configuration()
        val client = Client().apply { active = defaults; ready = true }
        val store = Store()
        val manager = SdkConfigurationManager(defaults, store, client, this)
        manager.start()
        manager.apply(defaults)
        runCurrent()
        assertThat(manager.state.value.status).isEqualTo(ConfigurationStatus.READY)
        assertThat(store.writes).hasSize(1)
        assertThat(store.writes.single()).isNull()
    }

    @Test
    fun `late callbacks from an earlier apply cannot overwrite the latest configuration`() = runTest {
        val store = Store()
        val client = Client()
        val manager = SdkConfigurationManager(configuration(), store, client, this)
        manager.start()
        manager.apply(configuration("A"))
        val a = client.callbacks.lastIndex
        val bConfig = configuration("B")
        manager.apply(bConfig)
        client.succeed(a)
        client.succeed()
        runCurrent()
        client.fail(a)
        runCurrent()
        assertThat(store.writes).containsExactly(bConfig)
        assertThat(manager.state.value.configuration).isEqualTo(bConfig)
        assertThat(manager.state.value.status).isEqualTo(ConfigurationStatus.READY)
    }

    @Test
    fun `slow earlier persistence finishes before the later successful save`() = runTest {
        val store = Store()
        val client = Client()
        val manager = SdkConfigurationManager(configuration(), store, client, this)
        manager.start()
        client.succeed()
        runCurrent()
        val release = CompletableDeferred<Unit>()
        val a = configuration("A")
        val b = configuration("B")
        store.nextSaveGate = release
        try {
            manager.apply(a)
            client.succeed()
            runCurrent()
            manager.apply(b)
            client.succeed()
            runCurrent()
            assertThat(store.startedWrites).containsExactly(a)
            release.complete(Unit)
            runCurrent()
            assertThat(store.writes).containsExactly(a, b).inOrder()
            assertThat(store.current).isEqualTo(b)
            assertThat(manager.state.value.configuration).isEqualTo(b)
        } finally {
            release.complete(Unit)
        }
    }

    @Test
    fun `persistence failure reports unsaved settings while keeping SDK actions available`() = runTest {
        val store = Store(configuration("saved"))
        val client = Client()
        val manager = SdkConfigurationManager(configuration(), store, client, this)
        manager.start()
        client.succeed()
        runCurrent()
        store.saveError = IllegalStateException("Disk unavailable")
        manager.apply(configuration("new"))
        client.succeed()
        runCurrent()
        assertThat(manager.state.value.status).isEqualTo(ConfigurationStatus.READY)
        assertThat(manager.state.value.source).isEqualTo(ConfigurationSource.UNSAVED)
        assertThat(manager.state.value.error).isEqualTo("Disk unavailable")
        assertThat(store.current).isEqualTo(configuration("saved"))
        store.saveError = null
        manager.apply(configuration("new")) // Native SDK is already ready; no new callback is required.
        runCurrent()
        assertThat(store.current).isEqualTo(configuration("new"))
    }

    @Test
    fun `missing required fields cannot start an apply or change saved settings`() = runTest {
        val store = Store()
        val client = Client()
        val manager = SdkConfigurationManager(configuration(), store, client, this)
        manager.start()
        assertThrows(IllegalArgumentException::class.java) {
            manager.apply(configuration().copy(sdkKey = " "))
        }
        assertThat(client.applied).hasSize(1)
        assertThat(store.writes).isEmpty()
    }

    @Test
    fun `synchronous SDK failure is shown and can be retried`() = runTest {
        val store = Store()
        val client = Client()
        val manager = SdkConfigurationManager(configuration(), store, client, this)
        manager.start()
        client.applyError = IllegalStateException("Initialization failed")
        manager.apply(configuration("new"))
        assertThat(manager.state.value.status).isEqualTo(ConfigurationStatus.FAILED)
        assertThat(store.writes).isEmpty()
        client.applyError = null
        manager.apply(configuration("new"))
        client.succeed()
        runCurrent()
        assertThat(manager.state.value.status).isEqualTo(ConfigurationStatus.READY)
        assertThat(store.current).isEqualTo(configuration("new"))
    }
}
