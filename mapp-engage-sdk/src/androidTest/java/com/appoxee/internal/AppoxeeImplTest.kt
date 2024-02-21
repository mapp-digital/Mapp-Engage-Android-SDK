package com.appoxee.internal

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.appoxee.internal.container.PushContainer
import com.appoxee.internal.model.response.DefaultResponse
import com.appoxee.internal.model.response.DevicePayload
import com.appoxee.internal.model.response.ResponseData
import com.appoxee.internal.model.response.inapp.InappResponse
import com.appoxee.internal.model.response.inapp.NativeInappMessage
import com.appoxee.internal.model.response.inbox.InboxMessage
import com.appoxee.internal.model.response.inbox.InboxMessagesResponse
import com.appoxee.internal.network.EngageApiImpl
import com.appoxee.internal.network.response.Response
import com.appoxee.internal.push.base.PushManagerImpl
import com.appoxee.internal.storage.InMemoryStorageImpl
import com.appoxee.internal.storage.Storage
import com.appoxee.shared.AppoxeeObserver
import com.appoxee.shared.AppoxeeOptions
import com.appoxee.shared.MappResult
import com.google.common.truth.Truth
import com.google.firebase.messaging.RemoteMessage
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import net.bytebuddy.utility.RandomString
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Random
import java.util.UUID

class AppoxeeImplTest {

    private lateinit var appoxee: AppoxeeImpl
    private lateinit var engageApiImpl: EngageApiImpl
    private lateinit var storage: Storage
    private lateinit var scope: CoroutineScope

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        scope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())
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

        storage = mockk<InMemoryStorageImpl>(relaxed = true)

        appoxee = spyk(AppoxeeImpl(context, appoxeeOptions, scope))

        val appoxeeAdapter = spyk(AppoxeeAdapter(engageApiImpl, storage)) {
            coEvery { this@spyk["refreshDevicePayload"]() as Unit } just Runs
        }

        every { appoxee.appoxeeAdapter } answers { appoxeeAdapter }

        every { appoxee.storage } answers { storage }
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
        runBlocking {
            val inboxMessage = mockk<InboxMessage>()
            coEvery { engageApiImpl.fetchInboxMessages(any()) } coAnswers {
                Response.success(
                    200,
                    InboxMessagesResponse("app_open", listOf(inboxMessage))
                )
            }

            val result = appoxee.fetchInboxMessages("").asSuspend()
            coVerify(exactly = 1) { engageApiImpl.fetchInboxMessages(any()) }
            Truth.assertThat(result.isSuccess()).isTrue()
            Truth.assertThat(result.getData()?.messages).hasSize(1)
        }
    }

    @Test
    fun fetchInappMessages() {
        runBlocking {
            val inappMessage = mockk<NativeInappMessage>()
            coEvery { engageApiImpl.fetchInApp(any()) } coAnswers {
                Response.success(
                    200,
                    InappResponse(
                        eventId = "app_open",
                        eventKey = "",
                        webMessages = emptyList(),
                        nativeMessages = listOf(inappMessage)
                    )
                )
            }

            val result = appoxee.fetchInappMessages("").asSuspend()
            coVerify(exactly = 1) { engageApiImpl.fetchInApp(any()) }
            Truth.assertThat(result.isSuccess()).isTrue()
            Truth.assertThat(result.getData()?.nativeMessages).hasSize(1)
            Truth.assertThat(result.getData()?.webMessages).hasSize(0)
        }
    }

    @Test
    fun enablePush() {
        runBlocking {
            val token = UUID.randomUUID().toString()
            val dmcUserId = UUID.randomUUID().toString()
            coEvery { engageApiImpl.optIn(pushToken = token) } coAnswers {
                Response.success(
                    200,
                    ResponseData(
                        payload = DefaultResponse(
                            dmcUserId = dmcUserId,
                            set = emptyList()
                        )
                    )
                )
            }
            val result = appoxee.enablePush(true, token).asSuspend()
            coVerify(exactly = 1) { engageApiImpl.optIn(pushToken = token) }
            Truth.assertThat(result.isSuccess()).isTrue()
            Truth.assertThat(result.getData()).isTrue()
        }
    }

    @Test
    fun isPushEnabled() {
        runBlocking {
            val token = RandomString(150, Random()).toString()
            coEvery { storage.getDevicePayload() } coAnswers {
                DevicePayload(alias = "test@mapp.com", pushToken = token)
            }

            val result = appoxee.isPushEnabled().execute()
            coVerify(exactly = 1) { storage.getDevicePayload() }
            Truth.assertThat(result.isSuccess()).isTrue()
            Truth.assertThat(result.getData()).isTrue()
        }
    }

    @Test
    fun getFirebaseToken() {
        runBlocking {
            val token = RandomString(150, Random()).toString()
            coEvery { storage.getDevicePayload() } coAnswers {
                DevicePayload(alias = "test@mapp.com", pushToken = token)
            }

            val result = appoxee.getFirebaseToken().asSuspend()
            coVerify(exactly = 1) { storage.getDevicePayload() }
            Truth.assertThat(result.isSuccess()).isTrue()
            Truth.assertThat(result.getData()).isEqualTo(token)
        }
    }

    @Test
    fun addTags() {
        runBlocking {
            val tags = listOf<String>("TAG 1", "TAG 2")
            coEvery { engageApiImpl.addTags(any()) } coAnswers {
                Response.success(
                    200,
                    ResponseData(
                        metadata = null, payload = DefaultResponse("", emptyList())
                    )
                )

            }

            val result = appoxee.addTags(tags).asSuspend()
            coVerify(exactly = 1) { engageApiImpl.addTags(tags) }
            Truth.assertThat(result.isSuccess()).isTrue()
            Truth.assertThat(result.getData()).isTrue()
        }
    }

    @Test
    fun removeTags() {
        runBlocking {
            val tags = listOf<String>("TAG 1", "TAG 2")
            coEvery { engageApiImpl.removeTags(any()) } coAnswers {
                Response.success(
                    200,
                    ResponseData(
                        metadata = null, payload = DefaultResponse("", emptyList())
                    )
                )

            }

            val result = appoxee.removeTags(tags).asSuspend()
            coVerify(exactly = 1) { engageApiImpl.removeTags(tags) }
            Truth.assertThat(result.isSuccess()).isTrue()
            Truth.assertThat(result.getData()).isTrue()
        }
    }

    @Test
    fun addCustomAttributes() {
        runBlocking {
            val attributes = mapOf<String, Any>("a" to "TAG 1", "b" to "TAG 2")
            coEvery { engageApiImpl.addCustomAttributes(any()) } coAnswers {
                Response.success(
                    200,
                    ResponseData(
                        metadata = null, payload = DefaultResponse("", emptyList())
                    )
                )

            }

            val result = appoxee.addCustomAttributes(attributes).asSuspend()
            coVerify(exactly = 1) { engageApiImpl.addCustomAttributes(attributes) }
            Truth.assertThat(result.isSuccess()).isTrue()
            Truth.assertThat(result.getData()).isTrue()
        }
    }

    @Test
    fun getCustomAttributes() {
        runBlocking {
            val attributes = mapOf<String, Any>("a" to "TAG 1", "b" to "TAG 2")
            coEvery { engageApiImpl.getCustomAttributes(any()) } coAnswers {
                Response.success(
                    200,
                    ResponseData(
                        metadata = null, payload = attributes
                    )
                )

            }
            val keys = listOf("a", "b")
            val result = appoxee.getCustomAttributes(keys).asSuspend()
            coVerify(exactly = 1) { engageApiImpl.getCustomAttributes(keys) }
            Truth.assertThat(result.isSuccess()).isTrue()
            Truth.assertThat(result.getData()).isEqualTo(attributes)
        }
    }

    @Test
    fun getDevice() {
        runBlocking {
            val token = RandomString(150, Random()).toString()
            val alias = "test@mapp.com"
            coEvery { engageApiImpl.getDevice() } coAnswers {
                Response.success(
                    200,
                    data = ResponseData(
                        metadata = null,
                        payload = DevicePayload(alias = alias, pushToken = token)
                    )
                )

            }

            val result = appoxee.getDevice().asSuspend()
            coVerify(exactly = 1) { engageApiImpl.getDevice() }
            Truth.assertThat(result.isSuccess()).isTrue()
            Truth.assertThat(result.getData()?.pushToken).isEqualTo(token)
        }
    }

    @Test
    fun updateReadyStatus() {
        runBlocking {
            val status = true
            val devicePayload = mockk<DevicePayload>(relaxed = true, relaxUnitFun = true)
            val result = MappResult.Success(devicePayload)

            val observer = mockk<AppoxeeObserver>(relaxed = true, relaxUnitFun = true)

            coEvery { storage.getDevicePayload() } coAnswers { devicePayload }

            appoxee.updateReadyStatus(status, result)
            appoxee.subscribe(observer)

            coVerify(exactly = 1) { observer.onReadyStatusChanged(status, any()) }
        }
    }

    @Test
    fun subscribe() {
        runBlocking {
            val observer = mockk<AppoxeeObserver>() {
                every { onReadyStatusChanged(any(), any()) } just Runs
            }

            val observers: MutableSet<AppoxeeObserver> = mockk(relaxed = true)

            every { appoxee.getProperty("observers") as MutableSet<*> } answers { observers }

            appoxee.subscribe(observer)
            verify(exactly = 1) { observers.add(observer) }
        }
    }

    @Test
    fun unsubscribe() {
        runBlocking {
            val observer = mockk<AppoxeeObserver>() {
                every { onReadyStatusChanged(any(), any()) } just Runs
            }

            val observers: MutableSet<AppoxeeObserver> = mockk(relaxed = true)

            every { appoxee.getProperty("observers") as MutableSet<*> } answers { observers }

            appoxee.unsubscribe(observer)
            verify(exactly = 1) { observers.remove(observer) }
        }
    }

    @Test
    fun handlePushMessage() {
        runBlocking {
            val remoteMessage = mockk<RemoteMessage>(relaxed = true)
            appoxee.handlePushMessage(remoteMessage)
            verify { appoxee.handlePushMessage(any()) }
        }
    }

    @Test
    fun isPushMessageFromMapp() {
        runBlocking {
            val observer = mockk<AppoxeeObserver>() {
                every { onReadyStatusChanged(any(), any()) } just Runs
            }

            val remoteMessage: RemoteMessage = mockk(relaxed = true, relaxUnitFun = true)

            val pushContainer: PushContainer = mockk(relaxed = true)
            val pushManager: PushManagerImpl = mockk(relaxed = true)
            every { appoxee.pushContainer } answers { pushContainer }
            every { pushContainer.pushManager } answers { pushManager }
            appoxee.isPushMessageFromMapp(remoteMessage)
            verify(exactly = 1) {
                pushManager.isPushMessageFromMapp(
                    any()
                )
            }
        }
    }

    @Test
    fun testActivate() {
        runBlocking {
            coEvery { engageApiImpl.activate(any()) } coAnswers {
                Response.success(
                    200,
                    ResponseData(
                        metadata = null, payload = DefaultResponse("", emptyList())
                    )
                )

            }

            val result = appoxee.testActivate().asSuspend()
            coVerify(exactly = 1) { engageApiImpl.activate(any()) }
            Truth.assertThat(result.isSuccess()).isTrue()
            Truth.assertThat(result.getData()).isTrue()
        }
    }

    @Test
    fun closeNotification() {
        runBlocking {
            val pushManager: PushManagerImpl = mockk(relaxed = true)
            val pushContainer: PushContainer = mockk(relaxed = true)
            every { appoxee.pushContainer } answers { pushContainer }
            every { pushContainer.pushManager } answers { pushManager }
            appoxee.closeNotification(1)
            verify(atLeast = 1) { pushManager.dismissNotification(1) }
        }
    }
}