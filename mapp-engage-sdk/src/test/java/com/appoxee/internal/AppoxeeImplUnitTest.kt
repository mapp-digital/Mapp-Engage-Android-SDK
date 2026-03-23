package com.appoxee.internal

import TestDispatchersProvider
import android.app.Application
import android.content.Context
import android.util.Log
import com.appoxee.internal.container.AppoxeeContainer
import com.appoxee.internal.container.PushContainer
import com.appoxee.internal.migration.MigrationHelper
import com.appoxee.internal.migration.data.OldRegistration
import com.appoxee.internal.model.request.RegisterDevice
import com.appoxee.internal.model.response.AppConfigPayload
import com.appoxee.internal.model.response.DefaultResponse
import com.appoxee.internal.model.response.DevicePayload
import com.appoxee.internal.model.response.ResponseData
import com.appoxee.internal.model.response.inbox.InboxMessagesResponse
import com.appoxee.internal.network.EngageApi
import com.appoxee.internal.network.NetworkClient
import com.appoxee.internal.network.response.Response
import com.appoxee.internal.provider.DeviceProvider
import com.appoxee.internal.provider.ObserversProvider
import com.appoxee.internal.storage.Storage
import com.appoxee.internal.ui.push.base.PushManager
import com.appoxee.internal.ui.push.base.PushManagerImpl
import com.appoxee.internal.util.Logger
import com.appoxee.shared.AppoxeeObserver
import com.appoxee.shared.AppoxeeOptions
import com.appoxee.shared.LocalPushBroadcast
import com.appoxee.shared.MappResult
import com.google.common.truth.Truth
import com.google.firebase.messaging.RemoteMessage
import io.mockk.Ordering
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

class AppoxeeImplUnitTest {

    private lateinit var sut: AppoxeeImpl

    private lateinit var mockContext: Context
    private lateinit var mockApplication: Application

    private lateinit var mockOptions: AppoxeeOptions

    private lateinit var mockAppoxeeContainer: AppoxeeContainer

    private lateinit var mockMigrationHelper: MigrationHelper

    private lateinit var mockStorage: Storage

    private lateinit var mockEngageApi: EngageApi

    private lateinit var mockDeviceProvider: DeviceProvider

    private lateinit var mockRegisterDevice: RegisterDevice

    private lateinit var mockAppoxeeAdapter: AppoxeeAdapter

    private lateinit var mockNetworkClient: NetworkClient

    private lateinit var mockDevicePayload: DevicePayload

    private lateinit var mockOldRegistration: OldRegistration

    private lateinit var observersProvider: ObserversProvider

    private lateinit var testScope: TestScope
    private lateinit var testDispatcher: TestDispatcher

    private lateinit var testDispatchersProvider: TestDispatchersProvider

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        testScope=TestScope(testDispatcher)
        testDispatchersProvider = TestDispatchersProvider(testDispatcher)
        Dispatchers.setMain(testDispatchersProvider.mainDispatcher)
        mockkStatic(Log::class)
        mockkStatic(Logger::class)
        every { Logger.d(any(), any()) } just runs
        every { Log.d(any(), any()) } answers { 0 }
        every { Log.e(any(), any(), any()) } answers { 0 }

        mockContext = mockk<Context>(relaxed = true)
        mockApplication = mockk<Application>(relaxed = true)
        every { mockApplication.applicationContext } returns mockContext
        mockNetworkClient = mockk(relaxUnitFun = true, relaxed = true)
        mockOptions = spyk(
            AppoxeeOptions(
                server = AppoxeeOptions.Server.TEST,
                sdkKey = "1111.2222",
                appId = "33333",
                tenantId = "44444"
            )
        )

        mockDevicePayload = DevicePayload(
            dmcUserId = "user1234",
            udidHashed = "348203948",
            pushTokenBk = "",
            pushToken = "token.1234",
            alias = "user@mapp.com"
        )

        mockOldRegistration = OldRegistration(
            alias = "user@mapp.com",
            isRegistered = true,
            pushEnabled = true,
            pushToken = "token.1234",
            timestamp = 1,
            tags = setOf("tag1", "tag2"),
            customAttributes = mapOf("color" to "blue", "age" to 30)
        )

        mockRegisterDevice = RegisterDevice(
            osName = "Android",
            appVersion = "1.0.0",
            clientVersion = "7.0.0",
            locale = "en",
            timeZone = "n/a",
            hardwareType = "samsung s24",
            density = "380dpi",
            vendorID = "Samsung",
            osNumber = "34",
            resolution = "1920x1080"
        )

        mockDeviceProvider = mockk(relaxed = true)
        mockStorage = mockk<Storage>(relaxed = true)
        mockEngageApi = mockk(relaxed = true)
        mockMigrationHelper = mockk(relaxed = true)
        observersProvider = mockk(relaxed = true)
        mockAppoxeeAdapter = spyk(AppoxeeAdapter(mockEngageApi, mockStorage))

        coEvery { mockStorage.getDevicePayload() } coAnswers { mockDevicePayload }

        every { mockOptions.sdkKey } returns "1111.22222"
        every { mockOptions.server } returns AppoxeeOptions.Server.TEST
        every { mockOptions.appId } returns "33333"
        every { mockOptions.tenantId } returns "44444"

        coEvery { mockAppoxeeAdapter.register(any()) } coAnswers {
            mockk(
                relaxed = true,
                relaxUnitFun = true
            )
        }
        coEvery { mockAppoxeeAdapter.getDevice() } coAnswers {
            mockk(
                relaxed = true,
                relaxUnitFun = true
            )
        }

        //mockAppoxeeContainer=spyk(AppoxeeContainer.getInstance(mockk(),testDispatchersProvider))
        mockAppoxeeContainer = mockk<AppoxeeContainer>(relaxed = true, relaxUnitFun = true)
        every { mockAppoxeeContainer.appoxeeAdapter } returns mockAppoxeeAdapter
        every { mockAppoxeeContainer.storage } returns mockStorage
        every { mockAppoxeeContainer.deviceProvider } returns mockDeviceProvider
        every { mockAppoxeeContainer.engageApi } returns mockEngageApi
        every { mockAppoxeeContainer.networkClient } returns mockNetworkClient

        sut = spyk(
            AppoxeeImpl(
                mockApplication,
                mockOptions,
                testDispatchersProvider,
                observersProvider,
                mockAppoxeeContainer
            ),
            recordPrivateCalls = true
        )

        every { sut.internalScope } returns testScope
        every { sut.application } returns mockApplication
        every { sut.appoxeeContainer } returns mockAppoxeeContainer
        every { sut.appoxeeAdapter } returns mockAppoxeeAdapter
        every { sut.deviceProvider } returns mockDeviceProvider
        every { sut.migrationHelper } returns mockMigrationHelper
        every { sut.observersProvider } returns observersProvider
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
    }

    @Test
    fun `fresh install application with v7 sdk`() = runTest {
        coEvery { mockStorage.getDevicePayload() } coAnswers { null }
        coEvery { mockStorage.getRegistrationDevice() } coAnswers { null }

        coEvery { mockDeviceProvider.generateRegistrationDevice() } coAnswers { mockRegisterDevice }

        coEvery { mockAppoxeeAdapter.register(any()) } coAnswers { mockk() }
        coEvery { mockAppoxeeAdapter.getDevice() } coAnswers { mockDevicePayload }

        coEvery { sut.updateOptStatus(any(), any()) } just runs

        // execute validation
        sut.validateRegistration()

        // verify order or calling functions when application installed for the first time
        coVerifyOrder {
            mockAppoxeeAdapter.register(mockRegisterDevice)
            sut.updateOptStatus(null, any())
            mockAppoxeeAdapter.getDevice()
            mockStorage.saveRegistrationDevice(mockRegisterDevice)
            mockStorage.saveDevicePayload(mockDevicePayload)
        }
    }

    @Test
    fun `migrate from v6 to v7 with the same channel`() = testScope.runTest {
        coEvery { mockStorage.getDevicePayload() } coAnswers { null }
        coEvery { mockStorage.getRegistrationDevice() } coAnswers { null }
        coEvery { mockMigrationHelper.getRegistrationOptions() } coAnswers { mockOptions }
        coEvery { mockMigrationHelper.fetchRegistrationData() } coAnswers { mockOldRegistration }
        coEvery { mockAppoxeeAdapter.getDevice() } coAnswers { mockDevicePayload }
        coEvery { mockDeviceProvider.generateRegistrationDevice() } coAnswers { mockRegisterDevice }
        coEvery { sut.updateOptStatus(any(), any()) } just runs

        // execute validation
        sut.validateRegistration()

        // verify that only registration data fetched from server and updated opt state
        coVerifyOrder {
            mockMigrationHelper.getRegistrationOptions()
            mockMigrationHelper.fetchRegistrationData()
            mockAppoxeeAdapter.getDevice()
            sut.updateOptStatus(mockDevicePayload, mockOldRegistration)
            // deleteOldRegistration must be called only after a successful getDevice()
            // (i.e. udidHashed is not null) — verifying the fix is exercised on the happy path
            mockMigrationHelper.deleteOldRegistration()
        }

        // verify that no registration call is triggered
        coVerify(exactly = 0) {
            mockAppoxeeAdapter.register(any())
        }

        // verify deleteOldRegistration is called exactly once (from migration guard, not fallback)
        coVerify(exactly = 1) {
            mockMigrationHelper.deleteOldRegistration()
        }

        // verify tags and custom attributes are seeded into v7 storage before v6 data is deleted
        coVerify(exactly = 1) {
            mockStorage.addTags(listOf("tag1", "tag2"))
        }
        coVerify(exactly = 1) {
            mockStorage.setCustomAttributesCache(mapOf("color" to "blue", "age" to 30))
        }
    }

    @Test
    fun `migrate from v6 to v7 - getDevice returns no valid payload - v6 data preserved until fallback registration`() =
        testScope.runTest {
            coEvery { mockStorage.getDevicePayload() } coAnswers { null }
            coEvery { mockStorage.getRegistrationDevice() } coAnswers { null }
            coEvery { mockMigrationHelper.getRegistrationOptions() } coAnswers { mockOptions }
            coEvery { mockMigrationHelper.fetchRegistrationData() } coAnswers { mockOldRegistration }
            // getDevice() returns a payload with null udidHashed (simulates network/server failure)
            coEvery { mockAppoxeeAdapter.getDevice() } coAnswers { DevicePayload(udidHashed = null) }
            coEvery { mockDeviceProvider.generateRegistrationDevice() } coAnswers { mockRegisterDevice }
            coEvery { sut.updateOptStatus(any(), any()) } just runs

            sut.validateRegistration()

            // register() must be called because getDevice() did not return a valid device
            coVerify(exactly = 1) { mockAppoxeeAdapter.register(mockRegisterDevice) }

            // deleteOldRegistration must be called exactly once — only from the fallback
            // re-registration path, NOT from the migration guard (which is the fix).
            // Without the fix this would be called twice.
            coVerify(exactly = 1) { mockMigrationHelper.deleteOldRegistration() }

            // tags and attributes must NOT be written when device confirmation failed
            coVerify(exactly = 0) { mockStorage.addTags(any()) }
            coVerify(exactly = 0) { mockStorage.setCustomAttributesCache(any()) }
        }

    @Test
    fun `migrate from v6 to v7 - empty tags and attributes - storage not written`() =
        testScope.runTest {
            val emptyOldRegistration = OldRegistration(
                alias = "user@mapp.com",
                isRegistered = true,
                pushEnabled = true,
                pushToken = "token.1234",
                timestamp = 1,
                tags = emptySet(),
                customAttributes = emptyMap()
            )
            coEvery { mockStorage.getDevicePayload() } coAnswers { null }
            coEvery { mockStorage.getRegistrationDevice() } coAnswers { null }
            coEvery { mockMigrationHelper.getRegistrationOptions() } coAnswers { mockOptions }
            coEvery { mockMigrationHelper.fetchRegistrationData() } coAnswers { emptyOldRegistration }
            coEvery { mockAppoxeeAdapter.getDevice() } coAnswers { mockDevicePayload }
            coEvery { mockDeviceProvider.generateRegistrationDevice() } coAnswers { mockRegisterDevice }
            coEvery { sut.updateOptStatus(any(), any()) } just runs

            sut.validateRegistration()

            // migration guard ran (device confirmed) but no redundant writes
            coVerify(exactly = 1) { mockMigrationHelper.deleteOldRegistration() }
            coVerify(exactly = 0) { mockStorage.addTags(any()) }
            coVerify(exactly = 0) { mockStorage.setCustomAttributesCache(any()) }
        }

    @Test
    fun `migrate from v6 to v7 with the changed channel`() = testScope.runTest {
        val oldOptions = spyk(
            AppoxeeOptions(
                server = AppoxeeOptions.Server.TEST_55,
                sdkKey = "1234.5678",
                appId = "999999",
                tenantId = "888888"
            )
        )
        coEvery { mockStorage.getDevicePayload() } coAnswers { null }
        coEvery { mockStorage.getRegistrationDevice() } coAnswers { null }
        coEvery { mockMigrationHelper.getRegistrationOptions() } coAnswers { oldOptions }
        coEvery { mockMigrationHelper.fetchRegistrationData() } coAnswers { mockOldRegistration }
        coEvery { mockAppoxeeAdapter.getDevice() } coAnswers { mockDevicePayload }
        coEvery { mockDeviceProvider.generateRegistrationDevice() } coAnswers { mockRegisterDevice }
        coEvery { sut.updateOptStatus(any(), any()) } just runs

        // execute validation
        sut.validateRegistration()

        // verify that only registration data fetched from server and updated opt state
        coVerifyOrder {
            mockMigrationHelper.getRegistrationOptions()
            mockMigrationHelper.fetchRegistrationData()
            mockAppoxeeAdapter.register(mockRegisterDevice)
            sut.updateOptStatus(null, mockOldRegistration)
            mockMigrationHelper.deleteOldRegistration()
            mockAppoxeeAdapter.getDevice()
        }
    }

    @Test
    fun `validate registration when already on v7 and channel not changed`() = runTest {
        val savedRegistrationDevice = RegisterDevice(
            osName = "Android",
            appVersion = "1.0.0",
            clientVersion = "7.0.0",
            locale = "en",
            timeZone = "n/a",
            hardwareType = "samsung s24",
            density = "380dpi",
            vendorID = "Samsung",
            osNumber = "34",
            resolution = "1920x1080"
        )

        coEvery { mockStorage.getDevicePayload() } coAnswers { mockDevicePayload }
        coEvery { mockStorage.getRegistrationDevice() } coAnswers { mockRegisterDevice }
        coEvery { mockDeviceProvider.generateRegistrationDevice() } coAnswers { savedRegistrationDevice }
        coEvery { sut.updateOptStatus(any(), any()) } just runs

        // execute validation
        sut.validateRegistration()

        // verify that no registration, get device or updating OPT state was called
        coVerify(exactly = 0) {
            mockAppoxeeAdapter.register(mockRegisterDevice)
            mockAppoxeeAdapter.getDevice()
            sut.updateOptStatus(mockDevicePayload, mockOldRegistration)
        }
    }

    @Test
    fun `validate registration when already on v7 and channel changed`() = runTest {
        val appoxeeOptions = AppoxeeOptions(AppoxeeOptions.Server.L3, "abcd.efgh", "001122", "0987")

        coEvery { sut.updateOptStatus(any(), any()) } just runs
        coEvery { sut.storage } coAnswers { mockStorage }
        coEvery { mockDeviceProvider.generateRegistrationDevice() } coAnswers { mockRegisterDevice }

        // this is in case when new options with changed value are passed;
        // the cached value will be deleted and method will return null
        coEvery { mockStorage.getDevicePayload() } coAnswers { null }
        // execute validation
        sut.initializeSdk()

        coVerify {
            mockAppoxeeAdapter.register(any())
        }
    }

    @Test
    fun `update device should be called when some device param changed`() =
        runTest {
            val savedRegistrationDevice = RegisterDevice(
                osName = "Android",
                appVersion = "1.0.0",
                clientVersion = "7.0.2", //changed value from 7.0.0
                locale = "en",
                timeZone = "n/a",
                hardwareType = "samsung s24",
                density = "380dpi",
                vendorID = "Samsung",
                osNumber = "34",
                resolution = "1920x1080"
            )

            coEvery { mockStorage.getDevicePayload() } coAnswers { mockDevicePayload }
            coEvery { mockStorage.getRegistrationDevice() } coAnswers { mockRegisterDevice }
            coEvery { mockDeviceProvider.generateRegistrationDevice() } coAnswers { savedRegistrationDevice }
            coEvery { sut.updateOptStatus(any(), any()) } just runs

            // execute validation
            sut.validateRegistration()

            // verify that no registration, get device or updating OPT state was called
            coVerify(exactly = 1) {
                mockAppoxeeAdapter.updateDevice(
                    alias = mockDevicePayload.alias ?: "",
                    params = any()
                )
            }
        }

    @Test
    fun `set alias with empty string throws exception`() = runTest {
        every { sut.appoxeeAdapter } answers { mockAppoxeeAdapter }

        val result = sut.setAlias("").asSuspend()

        Truth.assertThat(result.getError()).isInstanceOf(Exception::class.java)
    }

    @Test
    fun `set alias with valid string returns success`() = runTest {
        coEvery { mockStorage.getDevicePayload() } coAnswers { mockDevicePayload }
        coEvery { mockAppoxeeAdapter.refreshDevicePayload() } coAnswers { mockDevicePayload }

        val mockResponse = Response.success(200, ResponseData<DefaultResponse>(mockk(), mockk()))
        coEvery { mockEngageApi.setAlias("user@mapp.com") } coAnswers { mockResponse }

        val result = sut.setAlias("user@mapp.com").asSuspend()

        Truth.assertThat(result.isSuccess()).isTrue()
    }

    @Test
    fun `get alias returns success`() = runTest {
        val mockResponse = Response.success(200, ResponseData(mockk(), payload = mockDevicePayload))
        coEvery { mockEngageApi.getAlias() } coAnswers { mockResponse }

        val result = sut.getAlias().asSuspend()

        Truth.assertThat(result.isSuccess()).isTrue()
        Truth.assertThat(result.getData()).isEqualTo(mockDevicePayload.alias)

    }

    @Test
    fun `get alias returns error when API throws exception`() = runTest {
        coEvery { mockAppoxeeAdapter.getAlias() } coAnswers { throw Exception("Error") }

        val result = kotlin.runCatching { sut.getAlias().asSuspend() }.getOrNull()

        Truth.assertThat(result?.getData()).isNull()
        Truth.assertThat(result?.isSuccess()).isFalse()
        //Truth.assertThat(result.getError()).isInstanceOf(Exception::class.java)
    }

    @Test
    fun `fetch inbox messages returns success`() = runTest {
        val mockResponse = Response.success(
            200, InboxMessagesResponse("1", listOf(mockk(), mockk()))
        )
        coEvery { mockEngageApi.fetchInboxMessages(any()) } coAnswers { mockResponse }

        val result = sut.fetchInboxMessages().asSuspend()

        coVerify { mockEngageApi.fetchInboxMessages("app_inbox") }

        Truth.assertThat(result.isSuccess()).isTrue()
        Truth.assertThat(result.getData()?.messages).hasSize(2)
        Truth.assertThat(result.getData()?.eventId).isEqualTo("1")
    }

    @Test
    fun `fetch inbox messages returns error when engageApi throws exception`() = runTest {
        coEvery { mockEngageApi.fetchInboxMessages(any()) } coAnswers { throw Exception("Error") }

        val result = kotlin.runCatching { sut.fetchInboxMessages().asSuspend() }.getOrNull()

        coVerify { mockEngageApi.fetchInboxMessages("app_inbox") }

        Truth.assertThat(result?.isSuccess()).isFalse()
        Truth.assertThat(result?.getError()).isNotNull()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `subscribe should add observer to the list`() = runTest {
        // Mock dependencies
        val observer = mockk<AppoxeeObserver>(relaxed = true)

        every { sut.internalScope } returns this
        every { sut.isReady() } answers { true }
        every { observersProvider.addObserver(any()) } just runs
        coEvery { mockStorage.getDevicePayload() } coAnswers { mockDevicePayload }
        // Call the method
        sut.subscribe(observer)
        advanceUntilIdle() // wait for coroutine to complete
        // Verify observer was added
        verify {
            observersProvider.addObserver(observer)
        }
        coVerify {
            observersProvider.notify(any(), any())
        }
    }

    @Test
    fun `unsubscribe should remove observer from the list`() = runTest {
        // Mock dependencies
        val observer = mockk<AppoxeeObserver>(relaxed = true)

        // Call the method
        sut.unsubscribe(observer)
        // Verify observer was removed
        verify(exactly = 1) {
            observersProvider.removeObserver(observer)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `update ready status successfully and notify observers`() = runTest {
        val mockResult = MappResult.Success(data = mockDevicePayload)
        val mockPushContainer = mockk<PushContainer>(relaxed = true)

        every { observersProvider.notify(any(), any()) } just runs
        every { sut.pushContainer } returns mockPushContainer
        every { sut.application } returns mockApplication

        sut.updateReadyStatus(true, mockResult)
        advanceUntilIdle() // wait for any coroutines to complete

        verify(exactly = 1) {
            observersProvider.notify(true, mockResult)
        }
        Truth.assertThat(sut.isReady()).isTrue()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when isReady is true it delegates to pushManager`() = testScope.runTest {
        // arrange
        val application = mockk<Application>(relaxed = true)
        val pushManager = mockk<PushManager>(relaxed = true)
        val pushContainer = mockk<PushContainer> {
            every { this@mockk.pushManager } returns pushManager
        }

        every { sut.pushContainer } returns pushContainer
        every { sut.application } returns application
        every { sut.isReady() } returns true

        val remoteMessage = mockkClass(RemoteMessage::class, relaxed = true, relaxUnitFun = true)

        // act
        sut.handlePushMessage(remoteMessage)
        advanceUntilIdle() // let launched coroutine finish

        // assert – pushManager called once with context + message
        coVerify(exactly = 1) {
            pushManager.handlePushMessage(
                application.applicationContext,
                remoteMessage
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when isReady is false it adds message to queue and does not call pushManager`() = runTest {
        // arrange
        val application = mockk<Application>(relaxed = true)
        val pushManager = mockk<PushManager>(relaxed = true)
        val pushContainer = mockk<PushContainer> {
            every { this@mockk.pushManager } returns pushManager
        }

        val mockkQueue = spyk<ConcurrentLinkedQueue<RemoteMessage>>(recordPrivateCalls = true)
        every { sut.pushQueue } returns mockkQueue
        every { sut.pushContainer } returns pushContainer
        every { sut.application } returns application
        every { sut.isReady() } returns false
        every { mockkQueue.add(any()) } answers { true }

        val remoteMessage = mockkClass(RemoteMessage::class, relaxed = true, relaxUnitFun = true)

        // act
        sut.handlePushMessage(remoteMessage)
        advanceUntilIdle()

        // assert – pushManager must NOT be called
        coVerify(exactly = 0) {
            pushManager.handlePushMessage(any(), any())
        }

        verify {
            mockkQueue.add(any())
        }
    }

    @Test
    fun `ifPushMessageFromMapp returns true for messages having 'p' parameter`() =
        runTest {
            // Mock dependencies
            val mockRemoteMessage = mockk<RemoteMessage>(relaxed = true) {
                every { this@mockk.data["p"] } returns "1234"
            }

            val mockIsPushReady = mockk<AtomicBoolean>(relaxed = true) {
                every { get() } returns true
            }


            val mockPushManager = mockk<PushManagerImpl>(relaxed = true) {
                every { this@mockk.isPushMessageFromMapp(any()) } answers { callOriginal() }
            }
            val pushContainer = mockk<PushContainer>(relaxed = true) {
                every { this@mockk.pushManager } returns mockPushManager
            }

            coEvery { sut.getProperty("mIsReady") } returns mockIsPushReady

            every { sut.pushContainer } returns pushContainer

            // Call the method
            val result = sut.isPushMessageFromMapp(remoteMessage = mockRemoteMessage)
            // Verify observer was added
            coVerify(exactly = 1) {
                mockPushManager.isPushMessageFromMapp(mockRemoteMessage)
            }

            Truth.assertThat(result).isTrue()
        }

    @Test
    fun `ifPushMessageFromMapp returns false for messages not having 'p' parameter`() =
        runTest {
            // Mock dependencies
            val mockRemoteMessage = mockk<RemoteMessage>(relaxed = true) {
                every { this@mockk.data["p"] } returns null
            }

            val mockIsPushReady = mockk<AtomicBoolean>(relaxed = true) {
                every { get() } returns true
            }


            val mockPushManager = mockk<PushManagerImpl>(relaxed = true) {
                every { this@mockk.isPushMessageFromMapp(any()) } answers { callOriginal() }
            }
            val pushContainer = mockk<PushContainer>(relaxed = true) {
                every { this@mockk.pushManager } returns mockPushManager
            }

            coEvery { sut.getProperty("mIsReady") } returns mockIsPushReady

            every { sut.pushContainer } returns pushContainer

            // Call the method
            val result = sut.isPushMessageFromMapp(remoteMessage = mockRemoteMessage)
            // Verify observer was added
            coVerify(exactly = 1) {
                mockPushManager.isPushMessageFromMapp(mockRemoteMessage)
            }

            Truth.assertThat(result).isFalse()
        }

    @Test
    fun `fetchConfig runs and get configuration successfully`() = runTest {
        val mockConfiguration = mockk<AppConfigPayload>(relaxed = true)

        val mockResponse = Response.success(200, ResponseData(null, mockConfiguration))

        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0

        coEvery { mockEngageApi.getAppConfig() } coAnswers { mockResponse }

        sut.fetchAppConfig()

        coVerifyOrder {
            mockEngageApi.getAppConfig()
            mockStorage.saveAppConfig(mockConfiguration)
            mockStorage.updateCacheTimestamp()
            Log.d(any(), any())
        }
    }

    @Test
    fun `fetchConfig runs and get error when engageApi throws exception`() = runTest {
        val mockResponse = Response.error<ResponseData<AppConfigPayload>>(Throwable("Error"))

        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0

        coEvery { mockEngageApi.getAppConfig() } coAnswers { mockResponse }

        kotlin.runCatching { sut.fetchAppConfig() }

        coVerifyOrder {
            mockEngageApi.getAppConfig()
            Log.e(any(), any(), any())
        }

        coVerify(ordering = Ordering.UNORDERED, exactly = 0) {
            mockStorage.saveAppConfig(any())
            mockStorage.updateCacheTimestamp()
        }
    }

    private abstract class ValidPushBroadcast : LocalPushBroadcast()

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `setPushBroadcast class which is subtype of LocalPushBroadcast is successful`() = runTest {
        coEvery { mockStorage.setBroadcastClass(any()) } just runs
        every { sut.internalScope } returns this
        val result = kotlin.runCatching { sut.setPushBroadcast(ValidPushBroadcast::class.java) }
            .exceptionOrNull()

        advanceUntilIdle()

        Truth.assertThat(result).isNotInstanceOf(Exception::class.java)
        //coVerify { mockStorage.setBroadcastClass(any()) }
    }
}