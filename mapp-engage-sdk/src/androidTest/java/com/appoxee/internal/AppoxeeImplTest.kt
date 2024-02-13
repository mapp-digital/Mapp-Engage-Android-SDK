package com.appoxee.internal

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.appoxee.Appoxee
import com.appoxee.internal.model.response.DevicePayload
import com.appoxee.internal.model.response.ResponseData
import com.appoxee.internal.network.EngageApiImpl
import com.appoxee.internal.network.response.Response
import com.appoxee.internal.storage.Storage
import com.appoxee.shared.AppoxeeOptions
import com.appoxee.shared.MappResult
import com.google.common.truth.Truth
import io.mockk.Called
import io.mockk.called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

import org.junit.After
import org.junit.Before
import org.junit.Test

class AppoxeeImplTest {

    private lateinit var appoxee: AppoxeeImpl
    private lateinit var engageApiImpl: EngageApiImpl
    private lateinit var storage: Storage

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val appoxeeOptions = spyk(
            AppoxeeOptions(
                server = AppoxeeOptions.Server.L3,
                sdkKey = "12345.67890",
                tenantId = "12345",
                appId = "6789"
            ).also {
                it.readTimeout = 5000
                it.connectionTimeout = 5000
            })
        engageApiImpl = mockk<EngageApiImpl>(relaxed = true)
        storage = mockk<Storage>(relaxed = true)

        appoxee = spyk(AppoxeeImpl(context, appoxeeOptions))

        every { appoxee.appoxeeAdapter } answers { spyk(AppoxeeAdapter(engageApiImpl, storage)) }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun isReady() {
        every { appoxee.isReady() } answers { true }
        val ready = appoxee.isReady()
        verify(exactly = 1) { appoxee.isReady() }
        Truth.assertThat(ready).isTrue()
    }

    @Test
    fun setAlias() {
        runBlocking {
            appoxee.setAlias("test@mapp.com").execute()
            coVerify(exactly = 1) { engageApiImpl.setAlias(match { it == "test@mapp.com" }) }
        }
    }

    @Test
    fun getAlias() {
        runBlocking {
            //coEvery { storage.getDevicePayload() } coAnswers { DevicePayload(alias = "test@mapp.com") }
            coEvery { engageApiImpl.getAlias() } coAnswers {
                Response.success(
                    200,
                    ResponseData(metadata = null, payload = DevicePayload(alias = "test@mapp.com"))
                )
            }
            val result = appoxee.getAlias().execute()
            //coVerify(exactly = 1) { storage.getDevicePayload() }
            coVerify(exactly = 1) { engageApiImpl.getAlias() }
            Truth.assertThat(result.isSuccess()).isTrue()
            Truth.assertThat(result.getData()).isEqualTo("test@mapp.com")
        }
    }

    @Test
    fun fetchInboxMessages() {
    }

    @Test
    fun fetchInappMessages() {
    }

    @Test
    fun enablePush() {
    }

    @Test
    fun isPushEnabled() {
    }

    @Test
    fun getFirebaseToken() {
    }

    @Test
    fun addTags() {
    }

    @Test
    fun removeTags() {
    }

    @Test
    fun addCustomAttributes() {
    }

    @Test
    fun getCustomAttributes() {
    }

    @Test
    fun getDevice() {
    }

    @Test
    fun updateReadyStatus() {
    }

    @Test
    fun subscribe() {
    }

    @Test
    fun unsubscribe() {
    }

    @Test
    fun handlePushMessage() {
    }

    @Test
    fun isPushMessageFromMapp() {
    }

    @Test
    fun testActivate() {
    }

    @Test
    fun closeNotification() {
    }
}