package com.appoxee.internal.storage

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.appoxee.internal.TestDispatchers
import com.appoxee.internal.model.response.DevicePayload
import com.appoxee.internal.util.Dispatchers
import com.google.common.truth.Truth
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
    private lateinit var dispatchers: Dispatchers

    private val devicePayload = DevicePayload(
        dmcUserId = "12345",
        udidHashed = "abcd1234",
        pushToken = "aaaabbbbcccddddeeefff",
        alias = "user1@mapp.com"
    )

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        dispatchers = TestDispatchers()
        storage = spyk(PrefsStorageImpl(application, TimeUnit.SECONDS.toMillis(1), dispatchers))
    }

    @After
    fun tearDown() {
        unmockkAll()
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
        every { storage invoke "getTimestamp" withArguments listOf() } answers { 0L }
        storage.saveDevicePayload(devicePayload)
        val saved = storage.getDevicePayload()
        Truth.assertThat(saved).isNull()
    }

    @Test
    fun getDevicePayloadWithInvalidCache() = runBlocking {
        storage.saveDevicePayload(devicePayload)
        every { storage invoke "isCacheValid" withArguments listOf() } answers { false }
        val saved = storage.getDevicePayload()
        Truth.assertThat(saved).isNull()
    }

    @Test
    fun saveRegistrationDevice() {
    }

    @Test
    fun getRegistrationDevice() {
    }

    @Test
    fun saveInitOptions() {
    }

    @Test
    fun getInitOptions() {
    }
}