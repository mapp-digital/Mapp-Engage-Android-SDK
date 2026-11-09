package com.appoxee.internal.storage

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.appoxee.internal.TestDispatchersProvider
import com.appoxee.internal.model.request.RegisterDevice
import com.appoxee.internal.model.response.DevicePayload
import com.appoxee.internal.util.DispatchersProvider
import com.appoxee.shared.AppoxeeOptions
import com.appoxee.shared.LocalPushBroadcast
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
    fun clearRegistration_removes_all_persisted_state() = runBlocking {
        storage.saveDevicePayload(devicePayload)
        storage.saveRegistrationDevice(RegisterDevice())
        storage.saveInitOptions(AppoxeeOptions(AppoxeeOptions.Server.L3, "sdk", "app", "tenant"))
        storage.setBroadcastClass(LocalPushBroadcast::class.java)
        storage.addTags(listOf("customer-tag"))
        storage.setCustomAttributesCache(mapOf("email" to "customer@example.com"))
        storage.updateCacheTimestamp()
        storage.updateDeviceFetchTimestamp()

        storage.clearRegistration()

        // A new instance must also observe the cleared on-disk state.
        val restored = PrefsStorageImpl(application, dispatchersProvider)
        Truth.assertThat(restored.getDevicePayload()).isNull()
        Truth.assertThat(restored.getRegistrationDevice()).isNull()
        Truth.assertThat(restored.getInitOptions()).isNull()
        Truth.assertThat(restored.getAppConfig()).isNull()
        Truth.assertThat(restored.getBroadcastClass()).isNull()
        Truth.assertThat(restored.getTags()).isEmpty()
        Truth.assertThat(restored.getCustomAttributesCache().attributes).isEmpty()
        Truth.assertThat(restored.getTimestamp()).isEqualTo(0L)
        Truth.assertThat(restored.getDeviceFetchTimestamp()).isEqualTo(0L)
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
