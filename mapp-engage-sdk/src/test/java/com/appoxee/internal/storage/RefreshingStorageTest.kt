package com.appoxee.internal.storage

import TestDispatchersProvider
import com.appoxee.internal.model.response.AppConfigPayload
import com.appoxee.internal.model.response.DevicePayload
import com.appoxee.internal.model.response.ResponseData
import com.appoxee.internal.network.EngageApi
import com.appoxee.internal.network.response.Response
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.concurrent.TimeUnit

class RefreshingStorageTest {
    private val ttl = TimeUnit.DAYS.toMillis(1)
    private var clock = ttl * 2
    private val api = mockk<EngageApi>()
    private val raw = object : Storage by InMemoryStorageImpl() {
        var deviceTime = 0L
        var configTime = 0L
        override suspend fun getDeviceTimestamp() = deviceTime
        override suspend fun getTimestamp() = configTime
        override suspend fun updateDeviceTimestamp() { deviceTime = clock }
        override suspend fun updateCacheTimestamp() { configTime = clock }
        override suspend fun saveRefreshedDevicePayload(devicePayload: DevicePayload) {
            saveDevicePayload(devicePayload)
            updateDeviceTimestamp()
        }
    }

    private fun cache(dispatcher: kotlinx.coroutines.test.TestDispatcher) =
        RefreshingStorage(raw, { api }, TestDispatchersProvider(dispatcher), now = { clock })

    private fun device(alias: String) = DevicePayload(udidHashed = "device-id", alias = alias)

    @Test
    fun `fresh reads use cache and device expires exactly at 24 hours`() = runTest {
        val cache = cache(StandardTestDispatcher(testScheduler))
        val old = device("old")
        val fresh = device("fresh")
        raw.saveRefreshedDevicePayload(old)
        coEvery { api.getDevice() } returns Response.success(200, ResponseData(payload = fresh))

        clock += ttl - 1
        assertThat(cache.getDevicePayload()).isSameInstanceAs(old)
        coVerify(exactly = 0) { api.getDevice() }
        clock++
        assertThat(cache.getDevicePayload()).isSameInstanceAs(fresh)
        assertThat(cache.getDevicePayload()).isSameInstanceAs(fresh)
        coVerify(exactly = 1) { api.getDevice() }
    }

    @Test
    fun `configuration expires on access while the same cache instance stays alive`() = runTest {
        val cache = cache(StandardTestDispatcher(testScheduler))
        val old = mockk<AppConfigPayload>()
        val fresh = mockk<AppConfigPayload>()
        cache.saveAppConfig(old)
        coEvery { api.getAppConfig() } returns Response.success(200, ResponseData(payload = fresh))

        clock += ttl - 1
        assertThat(cache.getAppConfig()).isSameInstanceAs(old)
        coVerify(exactly = 0) { api.getAppConfig() }
        clock++
        assertThat(cache.getAppConfig()).isSameInstanceAs(fresh)
        assertThat(cache.getAppConfig()).isSameInstanceAs(fresh)
        coVerify(exactly = 1) { api.getAppConfig() }
    }

    @Test
    fun `timestamps survive replacing the cache and expire independently`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val first = cache(dispatcher)
        first.saveRefreshedDevicePayload(device("old"))
        clock += ttl / 2
        val config = mockk<AppConfigPayload>()
        first.saveAppConfig(config)
        clock += ttl / 2
        val next = cache(dispatcher)
        coEvery { api.getDevice() } returns Response.success(200, ResponseData(payload = device("fresh")))

        assertThat(next.getDevicePayload()?.alias).isEqualTo("fresh")
        assertThat(next.getAppConfig()).isSameInstanceAs(config)
        coVerify(exactly = 1) { api.getDevice() }
        coVerify(exactly = 0) { api.getAppConfig() }
    }

    @Test
    fun `partial identity updates do not extend full device lifetime`() = runTest {
        val cache = cache(StandardTestDispatcher(testScheduler))
        cache.saveRefreshedDevicePayload(device("old"))
        clock += ttl
        cache.saveDevicePayload(device("new-alias"))
        coEvery { api.getDevice() } returns Response.success(200, ResponseData(payload = device("new-alias")))

        cache.getDevicePayload()

        coVerify(exactly = 1) { api.getDevice() }
    }

    @Test
    fun `failed refresh retains values and retries on next access`() = runTest {
        val cache = cache(StandardTestDispatcher(testScheduler))
        val old = device("old")
        val oldConfig = mockk<AppConfigPayload>()
        cache.saveRefreshedDevicePayload(old)
        cache.saveAppConfig(oldConfig)
        val timestamp = clock
        clock += ttl
        coEvery { api.getDevice() } returns Response.error(IllegalStateException("offline"))
        coEvery { api.getAppConfig() } throws IllegalStateException("offline")

        repeat(2) {
            assertThat(cache.getDevicePayload()).isSameInstanceAs(old)
            assertThat(cache.getAppConfig()).isSameInstanceAs(oldConfig)
        }
        assertThat(raw.getDeviceTimestamp()).isEqualTo(timestamp)
        assertThat(raw.getTimestamp()).isEqualTo(timestamp)
        coVerify(exactly = 2) { api.getDevice() }
        coVerify(exactly = 2) { api.getAppConfig() }
    }

    @Test
    fun `concurrent expired reads share refresh attempts even when they fail`() = runTest {
        val cache = cache(StandardTestDispatcher(testScheduler))
        cache.saveRefreshedDevicePayload(device("old"))
        cache.saveAppConfig(mockk())
        clock += ttl
        coEvery { api.getDevice() } coAnswers {
            delay(100)
            Response.error(IllegalStateException("offline"))
        }
        coEvery { api.getAppConfig() } coAnswers {
            delay(100)
            Response.error(IllegalStateException("offline"))
        }

        (1..5).map { async { cache.getDevicePayload() } }.awaitAll()
        (1..5).map { async { cache.getAppConfig() } }.awaitAll()

        coVerify(exactly = 1) { api.getDevice() }
        coVerify(exactly = 1) { api.getAppConfig() }
    }

    @Test
    fun `concurrent reads share successful refresh and bypass identity refresh recursion`() = runTest {
        val cache = cache(StandardTestDispatcher(testScheduler))
        cache.saveRefreshedDevicePayload(device("old"))
        clock += ttl
        coEvery { api.getDevice() } coAnswers {
            assertThat(cache.peekDevicePayload()?.alias).isEqualTo("old")
            delay(100)
            Response.success(200, ResponseData(payload = device("fresh")))
        }

        val results = (1..5).map { async { cache.getDevicePayload() } }.awaitAll()

        assertThat(results.map { it?.alias }).containsExactly("fresh", "fresh", "fresh", "fresh", "fresh")
        coVerify(exactly = 1) { api.getDevice() }
    }

    @Test
    fun `legacy device without a timestamp refreshes on first access`() = runTest {
        val cache = cache(StandardTestDispatcher(testScheduler))
        raw.saveDevicePayload(device("legacy"))
        coEvery { api.getDevice() } returns Response.success(200, ResponseData(payload = device("fresh")))

        assertThat(cache.getDevicePayload()?.alias).isEqualTo("fresh")
        assertThat(raw.getDeviceTimestamp()).isEqualTo(clock)
    }

    @Test
    fun `empty refresh payloads do not replace cached values or reset expiry`() = runTest {
        val cache = cache(StandardTestDispatcher(testScheduler))
        val old = device("old")
        val config = mockk<AppConfigPayload>()
        cache.saveRefreshedDevicePayload(old)
        cache.saveAppConfig(config)
        val timestamp = clock
        clock += ttl
        coEvery { api.getDevice() } returns Response.success(200, ResponseData(payload = DevicePayload()))
        coEvery { api.getAppConfig() } returns Response.success(200, ResponseData(payload = null))

        assertThat(cache.getDevicePayload()).isSameInstanceAs(old)
        assertThat(cache.getAppConfig()).isSameInstanceAs(config)
        assertThat(raw.getDeviceTimestamp()).isEqualTo(timestamp)
        assertThat(raw.getTimestamp()).isEqualTo(timestamp)
    }

    @Test
    fun `missing configuration and incomplete device refresh on access but identity peek does not`() = runTest {
        val cache = cache(StandardTestDispatcher(testScheduler))
        val config = mockk<AppConfigPayload>()
        coEvery { api.getAppConfig() } returns Response.success(200, ResponseData(payload = config))
        val partial = DevicePayload(alias = "AUTO_app_hash", dmcUserId = "user-id")
        cache.saveDevicePayload(partial)

        assertThat(cache.getAppConfig()).isSameInstanceAs(config)
        assertThat(cache.peekDevicePayload()).isSameInstanceAs(partial)
        coVerify(exactly = 0) { api.getDevice() }
        coEvery { api.getDevice() } returns Response.success(200, ResponseData(payload = device("AUTO_app_hash")))
        assertThat(cache.getDevicePayload()?.udidHashed).isEqualTo("device-id")
        coVerify(exactly = 1) { api.getDevice() }
    }

    @Test
    fun `refresh cancellation propagates without clearing cached data`() = runTest {
        val cache = cache(StandardTestDispatcher(testScheduler))
        val old = device("old")
        cache.saveRefreshedDevicePayload(old)
        clock += ttl
        coEvery { api.getDevice() } throws CancellationException("cancelled")

        val result = runCatching { cache.getDevicePayload() }

        assertThat(result.exceptionOrNull()).isInstanceOf(CancellationException::class.java)
        assertThat(raw.getDevicePayload()).isSameInstanceAs(old)
    }
}
