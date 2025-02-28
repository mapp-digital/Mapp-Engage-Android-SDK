package com.appoxee.internal

import TestDispatchersProvider
import android.app.Application
import android.util.Log
import com.appoxee.internal.container.AppoxeeContainer
import com.appoxee.internal.migration.MigrationHelper
import com.appoxee.internal.migration.data.OldRegistration
import com.appoxee.internal.model.request.RegisterDevice
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
import com.appoxee.shared.AppoxeeObserver
import com.appoxee.shared.AppoxeeOptions
import com.google.common.truth.Truth
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class AppoxeeImplTestUnit {

    private lateinit var sut: AppoxeeImpl

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

    @OptIn(ExperimentalCoroutinesApi::class)
    private val testDispatcher = UnconfinedTestDispatcher(TestCoroutineScheduler())
    private val testDispatchersProvider: TestDispatchersProvider =
        TestDispatchersProvider(testDispatcher)
    private val mockScope = TestScope(testDispatcher)

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } answers { 0 }
        every { Log.e(any(), any(), any()) } answers { 0 }

        mockApplication = mockk(relaxed = true)
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
            timestamp = 1
        )

        mockRegisterDevice = RegisterDevice(
            osName = "Android",
            appVersion = "1.0.0",
            clientVersion = "A1",
            locale = "en",
            timeZone = "n/a",
            hardwareType = "samsung s24",
            density = "380dpi",
            vendorID = "Samsung",
            osNumber = "34",
            resolution = "1920x1080"
        )

        mockDeviceProvider = mockk(relaxed = true)
        mockStorage = mockk(relaxed = true)
        mockEngageApi = mockk(relaxed = true)
        mockMigrationHelper = mockk(relaxed = true)
        observersProvider = mockk(relaxed = true)
        mockAppoxeeAdapter = spyk(AppoxeeAdapter(mockEngageApi, mockStorage))


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

        mockAppoxeeContainer = mockk(relaxed = true, relaxUnitFun = true)

        sut = spyk(
            AppoxeeImpl(
                mockApplication,
                mockOptions,
                mockScope,
                testDispatchersProvider,
                observersProvider
            ),
            recordPrivateCalls = true
        )
        every { sut.appoxeeAdapter } returns mockAppoxeeAdapter
        every { sut.appoxeeContainer } returns mockAppoxeeContainer
        every { sut.storage } returns mockStorage
        every { sut.deviceProvider } returns mockDeviceProvider
        every { sut.migrationHelper } returns mockMigrationHelper
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() {
        unmockkAll()
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
    fun `migrate from v6 to v7 with the same channel`() = runTest {
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
            mockMigrationHelper.deleteOldRegistration()
        }

        // verify that no registration call is triggered
        coVerify(exactly = 0) {
            mockAppoxeeAdapter.register(any())
        }
    }

    @Test
    fun `migrate from v6 to v7 with the changed channel`() = runTest {
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
            clientVersion = "A1",
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
        val storage = mockk<Storage>(relaxed = true) {
            coEvery { this@mockk.getInitOptions() } coAnswers { appoxeeOptions }
        }

        coEvery { sut.storage } coAnswers { storage }

        coEvery { sut.updateReadyStatus(any(), any()) } just runs
        coEvery { mockDeviceProvider.generateRegistrationDevice() } coAnswers { mockRegisterDevice }
        coEvery { sut.updateOptStatus(any(), any()) } just runs

        // execute validation
        sut.initializeSdk()

        coVerifyOrder {
            storage.getInitOptions()
            storage.clearRegistration()
            storage.saveInitOptions(any())
            sut.validateRegistration()
            mockAppoxeeAdapter.register(mockRegisterDevice)
            sut.updateOptStatus(any(), any())
            mockAppoxeeAdapter.getDevice()
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
        coEvery { mockEngageApi.getAlias() } coAnswers { throw Exception() }

        val result = sut.getAlias().asSuspend()

        Truth.assertThat(result.isSuccess()).isFalse()
        Truth.assertThat(result.getError()).isInstanceOf(Exception::class.java)
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

        val result = sut.fetchInboxMessages().asSuspend()

        coVerify { mockEngageApi.fetchInboxMessages("app_inbox") }

        Truth.assertThat(result.isSuccess()).isFalse()
        Truth.assertThat(result.getError()).isInstanceOf(Exception::class.java)
    }

    @Test
    fun `subscribe should add observer to the list`() = runTest {
        mockScope.launch {
            // Mock dependencies
            val observer = mockk<AppoxeeObserver>(relaxed = true)

            // Call the method
            sut.subscribe(observer)

            // Verify observer was added
            verifyOrder {
                observersProvider.addObserver(observer)
                observersProvider.notify(any(), any())
            }
        }
    }
}