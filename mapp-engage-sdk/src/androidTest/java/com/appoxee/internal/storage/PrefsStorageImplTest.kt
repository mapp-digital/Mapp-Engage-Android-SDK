package com.appoxee.internal.storage

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.appoxee.internal.TestDispatchersProvider
import com.appoxee.internal.model.response.DevicePayload
import com.appoxee.internal.util.DispatchersProvider
import com.google.common.truth.Truth
import io.mockk.coEvery
import io.mockk.every
import io.mockk.spyk
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

internal class PrefsStorageImplTest {

    private lateinit var application: Application

    private lateinit var storage: PrefsStorageImpl
    private lateinit var dispatchersProvider: DispatchersProvider

    private val devicePayload = DevicePayload(
        dmcUserId = "12345",
        udidHashed = "abcd1234",
        pushToken = "aaaabbbbcccddddeeefff",
        alias = "user1@mapp.com"
    )

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        dispatchersProvider = TestDispatchersProvider()
        storage =
            spyk(PrefsStorageImpl(application, dispatchersProvider, TimeUnit.SECONDS.toMillis(1)))
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun device_refresh_timestamp_persists_and_partial_updates_do_not_extend_it() = runBlocking {
        storage.clearRegistration()
        storage.saveRefreshedDevicePayload(devicePayload)
        val refreshedAt = storage.getDeviceTimestamp()
        Truth.assertThat(refreshedAt).isGreaterThan(0L)

        val reopened = PrefsStorageImpl(application, dispatchersProvider)
        Truth.assertThat(reopened.getDeviceTimestamp()).isEqualTo(refreshedAt)
        reopened.saveDevicePayload(DevicePayload(alias = "updated-alias"))
        Truth.assertThat(reopened.getDeviceTimestamp()).isEqualTo(refreshedAt)
        reopened.saveDevicePayload(null)
        Truth.assertThat(reopened.getDeviceTimestamp()).isEqualTo(0L)
    }

    @Test
    fun retrieve_device_payload_when_previously_saved_and_cache_valid_returns_valid_payload() =
        runBlocking {
            storage.saveDevicePayload(devicePayload)
            val saved = storage.getDevicePayload()
            every { storage invoke "isCacheValid" withArguments listOf() } answers { true }
            Truth.assertThat(saved).isNotNull()
        }

    @Test
    fun retrieve_data_after_validity_expired_returns_null() = runBlocking {
        coEvery { storage.getTimestamp() } coAnswers  { 0L }
        storage.saveDevicePayload(devicePayload)
        val saved = storage.getDevicePayload()
        Truth.assertThat(saved).isNotNull()
        Truth.assertThat(storage.isCacheValid()).isFalse()
    }

    @Test
    fun getDevicePayloadWithInvalidCache() = runBlocking {
        storage.saveDevicePayload(devicePayload)
        coEvery { storage.isCacheValid() } coAnswers { false }
        val saved = storage.getDevicePayload()
        Truth.assertThat(saved).isNotNull()
        Truth.assertThat(storage.isCacheValid()).isFalse()
    }
}