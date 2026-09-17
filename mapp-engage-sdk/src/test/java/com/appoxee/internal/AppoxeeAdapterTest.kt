package com.appoxee.internal

import com.appoxee.internal.model.common.CustomAttributesCache
import com.appoxee.internal.model.request.RegisterDevice
import com.appoxee.internal.model.response.AppConfigPayload
import com.appoxee.internal.model.response.DefaultResponse
import com.appoxee.internal.model.response.DevicePayload
import com.appoxee.internal.model.response.Metadata
import com.appoxee.internal.model.response.RegisterPayload
import com.appoxee.internal.model.response.ResponseData
import com.appoxee.internal.model.response.geo.RegionsResponse
import com.appoxee.internal.model.response.inapp.InappResponse
import com.appoxee.internal.model.response.inbox.InboxMessagesResponse
import com.appoxee.internal.network.EngageApi
import com.appoxee.internal.network.exceptions.ClientException
import com.appoxee.internal.network.exceptions.ServerException
import com.appoxee.internal.network.response.Response
import com.appoxee.internal.storage.Storage
import com.appoxee.internal.util.LibraryExtensions.toUtcString
import com.google.common.truth.Truth
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.net.UnknownHostException
import java.util.Date
import java.util.concurrent.TimeoutException

class AppoxeeAdapterTest {

    private lateinit var appoxeeAdapter: AppoxeeAdapter
    private lateinit var engageApi: EngageApi
    private lateinit var storage: Storage

    @Before
    fun setUp() {
        engageApi = mockk(relaxed = true)
        storage = mockk(relaxed = true)

        appoxeeAdapter = spyk(AppoxeeAdapter(engageApi, storage))
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `register saves returned identity before any device refresh`() = runTest {
        val deviceModel = mockk<RegisterDevice>()
        val payload = RegisterPayload("registered-user", "AUTO_app_hash")
        coEvery { storage.peekDevicePayload() } returns null
        coEvery { engageApi.registerDevice(deviceModel) } returns Response.success(
            200, ResponseData(payload = payload)
        )

        Truth.assertThat(appoxeeAdapter.register(deviceModel)).isEqualTo(payload)

        coVerify(exactly = 1) {
            storage.saveDevicePayload(match {
                it?.alias == "AUTO_app_hash" && it.dmcUserId == "registered-user" &&
                    it.udidHashed == null && it.pushToken == null && it.pushTokenBk == null
            })
        }
        coVerify(exactly = 0) { engageApi.getDevice() }
    }

    @Test
    fun `register preserves cached fields not supplied by response`() = runTest {
        val deviceModel = mockk<RegisterDevice>()
        coEvery { storage.peekDevicePayload() } returns DevicePayload(
            dmcUserId = "old-user", alias = "old-alias", udidHashed = "device-id",
            pushToken = "push-token", pushTokenBk = "backup-token"
        )
        coEvery { engageApi.registerDevice(deviceModel) } returns Response.success(
            200, ResponseData(payload = RegisterPayload("new-user", null))
        )

        appoxeeAdapter.register(deviceModel)

        coVerify(exactly = 1) {
            storage.saveDevicePayload(match {
                it?.dmcUserId == "new-user" && it.alias == "old-alias" &&
                    it.udidHashed == "device-id" && it.pushToken == "push-token" &&
                    it.pushTokenBk == "backup-token"
            })
        }
    }

    @Test
    fun `register error or missing payload does not change cached device`() = runTest {
        val deviceModel = mockk<RegisterDevice>()
        coEvery { engageApi.registerDevice(deviceModel) } returnsMany listOf(
            Response.error(UnknownHostException()),
            Response.success(200, ResponseData<RegisterPayload>(payload = null))
        )

        Truth.assertThat(appoxeeAdapter.register(deviceModel)).isNull()
        Truth.assertThat(appoxeeAdapter.register(deviceModel)).isNull()

        coVerify(exactly = 0) { storage.saveDevicePayload(any()) }
    }

    @Test
    fun `setAlias saves returned identity and preserves device fields without GET`() = runTest {
        val testAlias = "test@alias.com"
        var cachedDevice = DevicePayload(
            dmcUserId = "old-user",
            udidHashed = "5678",
            pushTokenBk = "backup-token",
            pushToken = "abc.1234",
            alias = "AUTO_app_hash"
        )
        coEvery { storage.getDevicePayload() } answers { cachedDevice }
        coEvery { storage.saveDevicePayload(any()) } answers {
            cachedDevice = firstArg<DevicePayload>()
        }
        coEvery { engageApi.setAlias(testAlias) } returns Response.success(
            200, ResponseData(
                metadata = Metadata(error = false, statusCode = 200),
                payload = DefaultResponse("106254147", emptyList())
            )
        )

        val response = appoxeeAdapter.setAlias(testAlias)

        Truth.assertThat(response).isSameInstanceAs(cachedDevice)
        Truth.assertThat(cachedDevice.alias).isEqualTo(testAlias)
        Truth.assertThat(cachedDevice.dmcUserId).isEqualTo("106254147")
        Truth.assertThat(cachedDevice.udidHashed).isEqualTo("5678")
        Truth.assertThat(cachedDevice.pushToken).isEqualTo("abc.1234")
        Truth.assertThat(cachedDevice.pushTokenBk).isEqualTo("backup-token")
        Truth.assertThat(appoxeeAdapter.getAlias()).isEqualTo(testAlias)
        coVerify(exactly = 1) { engageApi.setAlias(testAlias) }
        coVerify(exactly = 1) { storage.saveDevicePayload(any()) }
        coVerify(exactly = 0) { engageApi.getDevice() }
    }

    @Test
    fun `setAlias saves identity before resending custom attributes`() = runTest {
        val testAlias = "test@alias.com"
        val customAttributes = mapOf("a" to 1, "b" to false, "c" to "lorem ipsum")
        coEvery { storage.getDevicePayload() } returns DevicePayload(alias = "old-alias")
        coEvery { engageApi.setAlias(testAlias) } returns Response.success(
            200, ResponseData(
                metadata = Metadata(error = false, statusCode = 200),
                payload = DefaultResponse("106254147", emptyList())
            )
        )
        coEvery { storage.getCustomAttributesCache() } returns CustomAttributesCache(
            customAttributes
        )
        coEvery { engageApi.addCustomAttributes(customAttributes) } returns Response.success(
            200, ResponseData(payload = DefaultResponse("106254147", emptyList()))
        )

        val response = appoxeeAdapter.setAlias(testAlias, true)

        Truth.assertThat(response?.dmcUserId).isEqualTo("106254147")
        coVerifyOrder {
            engageApi.setAlias(testAlias)
            storage.saveDevicePayload(match {
                it?.alias == testAlias && it.dmcUserId == "106254147"
            })
            engageApi.addCustomAttributes(customAttributes)
        }
        coVerify(exactly = 0) { engageApi.getDevice() }
    }

    @Test
    fun `setAlias does not resend custom attributes when disabled`() = runTest {
        val testAlias = "test@alias.com"
        coEvery { storage.getDevicePayload() } returns DevicePayload(alias = "old-alias")
        coEvery { engageApi.setAlias(testAlias) } returns Response.success(
            200, ResponseData(
                metadata = Metadata(error = false, statusCode = 200),
                payload = DefaultResponse("106254147", emptyList())
            )
        )

        val response = appoxeeAdapter.setAlias(testAlias, false)

        Truth.assertThat(response?.alias).isEqualTo(testAlias)
        coVerify(exactly = 1) { storage.saveDevicePayload(any()) }
        coVerify(exactly = 0) { engageApi.addCustomAttributes(any()) }
        coVerify(exactly = 0) { engageApi.getDevice() }
    }

    @Test
    fun `setAlias rejects invalid responses without changing storage or resending attributes`() =
        runTest {
            val testAlias = "test@alias.com"
            val device = DevicePayload(alias = "AUTO_app_hash", dmcUserId = "old-user")
            val validMetadata = Metadata(error = false, statusCode = 200)
            val validPayload = DefaultResponse("106254147", emptyList())
            val invalidResponses = listOf(
                Response.success(
                    500,
                    ResponseData(metadata = validMetadata, payload = validPayload)
                ),
                Response.success(
                    200,
                    ResponseData(metadata = Metadata(true, 200), payload = validPayload)
                ),
                Response.success(
                    200,
                    ResponseData(metadata = Metadata(false, 404), payload = validPayload)
                ),
                Response.success(200, ResponseData(metadata = null, payload = validPayload)),
                Response.success(
                    200,
                    ResponseData<DefaultResponse>(metadata = validMetadata, payload = null)
                ),
                Response.success(
                    200,
                    ResponseData(
                        metadata = validMetadata,
                        payload = DefaultResponse("", emptyList())
                    )
                ),
                Response.success(
                    200,
                    ResponseData(
                        metadata = validMetadata,
                        payload = DefaultResponse("   ", emptyList())
                    )
                ),
                Response.success<ResponseData<DefaultResponse>>(200, null)
            )
            coEvery { storage.getDevicePayload() } returns device

            for (invalidResponse in invalidResponses) {
                coEvery { engageApi.setAlias(testAlias) } returns invalidResponse

                val result = runCatching { appoxeeAdapter.setAlias(testAlias, true) }

                Truth.assertThat(result.isFailure).isTrue()
                Truth.assertThat(appoxeeAdapter.getAlias()).isEqualTo("AUTO_app_hash")
            }
            coVerify(exactly = 0) { storage.saveDevicePayload(any()) }
            coVerify(exactly = 0) { engageApi.addCustomAttributes(any()) }
            coVerify(exactly = 0) { engageApi.getDevice() }
        }

    /**
     * Test when set alias is called with existing value
     * Network call should not be executed and returned value is from a local database
     */
    @Test
    fun `setAlias with existing value successful`() {
        runTest {
            coEvery { storage.getDevicePayload() } coAnswers {
                DevicePayload(
                    alias = "12345", dmcUserId = "user12345"
                )
            }

            val response = appoxeeAdapter.setAlias("12345")
            Truth.assertThat(response).isNotNull()
            coVerify(exactly = 0) { engageApi.setAlias(any(String::class)) } //no network call
        }
    }

    /**
     * Test setAlias and get some error response
     */
    @Test
    fun `setAlias with new value error`() = runTest {
        val testAlias = "test@alias.com"
        coEvery { engageApi.setAlias(testAlias) } answers {
            Response.error(TimeoutException())
        }
        coEvery { storage.getDevicePayload() } answers { null }

        val response = runCatching { appoxeeAdapter.setAlias(testAlias) }

        Truth.assertThat(response.exceptionOrNull()).isNotNull()
        coVerify { engageApi.setAlias(any(String::class)) }
    }

    /**
     * Test get alias and get successful response
     */
    @Test
    fun `getAlias with successful response`() = runTest {
        coEvery { storage.getDevicePayload() } coAnswers { spyk(DevicePayload(alias = "user@mapp.com")) }
        val alias = appoxeeAdapter.getAlias()
        Truth.assertThat(alias).isEqualTo("user@mapp.com")
        coVerify { storage.getDevicePayload() }
    }

    /**
     * Test get Alias and get some error response
     */
    @Test
    fun `getAlias with error response`() = runTest {
        coEvery { storage.getDevicePayload() } coAnswers { null }
        val alias = appoxeeAdapter.getAlias()
        coVerify { storage.getDevicePayload() }
        Truth.assertThat(alias).isAnyOf("", null)
    }

    @Test
    fun `getDevice calls network with successful response`() = runTest {
        coEvery { engageApi.getDevice() } coAnswers {
            Response.success(
                200, ResponseData(
                    metadata = null,
                    payload = DevicePayload(dmcUserId = "user12345", alias = "user@mapp.com")
                )
            )
        }

        val response = appoxeeAdapter.getDevice()
        coVerify { engageApi.getDevice() }
        Truth.assertThat(response).isNotNull()
        Truth.assertThat(response?.alias).isEqualTo("user@mapp.com")
        coVerify(exactly = 1) { storage.saveRefreshedDevicePayload(any()) }
    }

    @Test
    fun `getDevice from local cache with error response`() {
        runTest {
            coEvery { engageApi.getDevice() } coAnswers {
                Response.error(Throwable("Error getting data"))
            }

            val response = appoxeeAdapter.getDevice()
            coVerify { engageApi.getDevice() }
            Truth.assertThat(response).isNull()
            Truth.assertThat(engageApi.getDevice().error).isInstanceOf(Throwable::class.java)
        }
    }

    @Test
    fun `optIn with successful response`() {
        runTest {
            coEvery { engageApi.optIn(any(String::class)) } coAnswers {
                Response.success(
                    200, ResponseData(
                        metadata = null,
                        payload = DefaultResponse(dmcUserId = "user12345", set = emptyList())
                    )
                )
            }
            val mockDeviceResponse = Response.success(
                statusCode = 200,
                data = ResponseData(metadata = null, payload = DevicePayload(alias = "abc"))
            )
            coEvery { engageApi.getDevice() } coAnswers { mockDeviceResponse }

            val response = appoxeeAdapter.optIn("1243abcdxyz")
            coVerify { engageApi.optIn(any(String::class)) }
            coVerify(exactly = 1) { engageApi.getDevice() }
            Truth.assertThat(response).isNotNull()
            Truth.assertThat(response).isTrue()
        }
    }

    @Test
    fun `optIn can skip device refresh`() = runTest {
        coEvery { storage.peekDevicePayload() } returns DevicePayload(pushToken = "old-token")
        coEvery { engageApi.optIn("new-token") } returns Response.success(
            200, ResponseData(payload = DefaultResponse("user12345", emptyList()))
        )

        Truth.assertThat(appoxeeAdapter.optIn("new-token", refreshDevice = false)).isTrue()

        coVerify(exactly = 1) { engageApi.optIn("new-token") }
        coVerify(exactly = 0) { engageApi.getDevice() }
    }

    @Test
    fun `optOut can skip device refresh`() = runTest {
        coEvery { storage.peekDevicePayload() } returns DevicePayload(pushTokenBk = "old-token")
        coEvery { engageApi.optOut("new-token") } returns Response.success(
            200, ResponseData(payload = DefaultResponse("user12345", emptyList()))
        )

        Truth.assertThat(appoxeeAdapter.optOut("new-token", refreshDevice = false)).isTrue()

        coVerify(exactly = 1) { engageApi.optOut("new-token") }
        coVerify(exactly = 0) { engageApi.getDevice() }
    }

    @Test
    fun `optIn with error response`() {
        runTest {
            coEvery { engageApi.optIn(any(String::class)) } coAnswers {
                Response.error(ClientException(400, "Bad request!", null))
            }
            val mockDeviceResponse = Response.success(
                statusCode = 200,
                data = ResponseData(metadata = null, payload = DevicePayload(alias = "abc"))
            )
            coEvery { engageApi.getDevice() } coAnswers { mockDeviceResponse }

            val response = appoxeeAdapter.optIn("1243abcdxyz")
            coVerify { engageApi.optIn(any(String::class)) }
            Truth.assertThat(engageApi.optIn("1243abcdxyz").error)
                .isInstanceOf(ClientException::class.java)
            Truth.assertThat(response).isFalse()
        }
    }

    @Test
    fun `optOut with successful response`() = runTest {
        val responseData = ResponseData(
            metadata = null,
            payload = DefaultResponse(dmcUserId = "user12345", set = emptyList())
        )
        val mockDeviceResponse = Response.success(
            statusCode = 200,
            data = ResponseData(metadata = null, payload = DevicePayload(alias = "abc"))
        )
        coEvery { engageApi.getDevice() } coAnswers { mockDeviceResponse }

        coEvery { engageApi.optOut(any()) } coAnswers {
            Response.success(statusCode = 200, data = responseData)
        }

        val response = appoxeeAdapter.optOut("1243abcdxyz")
        coVerify(exactly = 1) { engageApi.optOut("1243abcdxyz") }
        coVerify(exactly = 1) { engageApi.getDevice() }
        Truth.assertThat(response).isNotNull()
        Truth.assertThat(response).isTrue()
    }

    @Test
    fun `optOut with error response`() {
        runTest {
            coEvery { engageApi.optOut(any(String::class)) } coAnswers {
                Response.error(ClientException(400, "Bad request!", null))
            }
            val mockDeviceResponse = Response.success(
                statusCode = 200,
                data = ResponseData(metadata = null, payload = DevicePayload(alias = "abc"))
            )
            coEvery { engageApi.getDevice() } coAnswers { mockDeviceResponse }

            val response = appoxeeAdapter.optOut("1243abcdxyz")
            coVerify { engageApi.optOut(any(String::class)) }
            Truth.assertThat(engageApi.optOut("1243abcdxyz").error)
                .isInstanceOf(ClientException::class.java)
            Truth.assertThat(response).isFalse()
        }
    }

    @Test
    fun `getAppConfig with successful response`() {
        runTest {
            coEvery { engageApi.getAppConfig() } coAnswers {
                Response.success(
                    200, ResponseData(
                        metadata = null, payload = mockk()
                    )
                )
            }

            val response = appoxeeAdapter.getAppConfig()
            coVerify { engageApi.getAppConfig() }
            Truth.assertThat(response.data).isNotNull()
            Truth.assertThat(response.data?.payload).isInstanceOf(AppConfigPayload::class.java)
        }
    }

    @Test
    fun `getAppConfig with error response`() {
        runTest {
            coEvery { engageApi.getAppConfig() } coAnswers {
                Response.error(NotImplementedError(""))
            }

            val response = appoxeeAdapter.getAppConfig()
            coVerify { engageApi.getAppConfig() }
            Truth.assertThat(response.data).isNull()
            Truth.assertThat(response.data?.payload).isNull()
        }
    }

    @Test
    fun `fetchInboxMessages with successful response`() {
        runTest {
            coEvery { engageApi.fetchInboxMessages(any(String::class)) } coAnswers {
                Response.success(
                    200, InboxMessagesResponse(
                        eventId = "", messages = listOf(mockk(), mockk())
                    )
                )
            }

            val eventName = "app_open"
            val response = appoxeeAdapter.fetchInboxMessages(eventName)
            coVerify { engageApi.fetchInboxMessages(eventName) }
            Truth.assertThat(response?.eventId).isEmpty()
            Truth.assertThat(response?.messages).hasSize(2)
        }
    }

    @Test
    fun `fetchInboxMessages with error response`() {
        runTest {
            coEvery { engageApi.fetchInboxMessages(any(String::class)) } coAnswers {
                Response.error(ServerException(500, "Server error!", null))
            }

            val eventName = "app_open"
            val response = runCatching { appoxeeAdapter.fetchInboxMessages(eventName) }
            coVerify { engageApi.fetchInboxMessages(eventName) }
            Truth.assertThat(response.exceptionOrNull()).isNotNull()
            Truth.assertThat(engageApi.fetchInboxMessages(eventName).error)
                .isInstanceOf(ServerException::class.java)
        }
    }

    @Test
    fun `fetchInappMessages with successful response`() {
        runTest {
            val eventName = "app_open"
            coEvery { engageApi.fetchInApp(eventName) } coAnswers {
                Response.success(
                    200, InappResponse(
                        "1234", eventName, webMessages = emptyList(), nativeMessages = listOf(
                            mockk(), mockk()
                        )
                    )
                )
            }

            val response = appoxeeAdapter.fetchInappMessages(eventName)
            coVerify { engageApi.fetchInApp(eventName) }
            Truth.assertThat(response?.webMessages).isEmpty()
            Truth.assertThat(response?.nativeMessages).hasSize(2)
        }
    }

    @Test
    fun `fetchInappMessages with error response`() {
        runTest {
            val eventName = "app_open"
            coEvery { engageApi.fetchInApp(eventName) } coAnswers {
                Response.error(NoSuchMethodException())
            }

            val response = appoxeeAdapter.fetchInappMessages(eventName)
            coVerify { engageApi.fetchInApp(eventName) }
            Truth.assertThat(response?.webMessages).isNull()
            Truth.assertThat(response?.nativeMessages).isNull()
            Truth.assertThat(engageApi.fetchInApp(eventName).error)
                .isInstanceOf(NoSuchMethodException::class.java)
        }
    }

    @Test
    fun `addTags with successful response`() {
        runTest {
            coEvery { engageApi.addTags(allAny()) } coAnswers {
                Response.success(
                    200, ResponseData(
                        metadata = null,
                        payload = DefaultResponse(dmcUserId = "user1234", emptyList())
                    )
                )
            }
            val response = appoxeeAdapter.addTags(setOf("tag1", "tag2", "tag3"))
            coVerify { engageApi.addTags(allAny()) }
            Truth.assertThat(response.statusCode).isEqualTo(200)
        }
    }

    @Test
    fun `addTags with error response`() {
        runTest {
            coEvery { engageApi.addTags(allAny()) } coAnswers {
                Response.error(TimeoutException())
            }
            val response = appoxeeAdapter.addTags(setOf("tag1", "tag2", "tag3"))
            coVerify { engageApi.addTags(allAny()) }
            Truth.assertThat(response.error).isInstanceOf(TimeoutException::class.java)
        }
    }

    @Test
    fun `removeTags with successful response`() {
        runTest {
            coEvery { engageApi.removeTags(allAny()) } coAnswers {
                Response.success(
                    200, ResponseData(
                        metadata = null,
                        payload = DefaultResponse(dmcUserId = "user1234", emptyList())
                    )
                )
            }
            val tags = setOf("tag1", "tag2", "tag3")
            coEvery { storage.getTags() } coAnswers { tags.toList() }

            val response = appoxeeAdapter.removeTags(tags)
            coVerify { engageApi.removeTags(allAny()) }
            Truth.assertThat(response.statusCode).isEqualTo(200)
        }
    }

    @Test
    fun `removeTags with error response`() {
        runTest {
            coEvery { engageApi.removeTags(allAny()) } coAnswers {
                Response.error(TimeoutException())
            }
            val tags = setOf("tag1", "tag2", "tag3")
            coEvery { storage.getTags() } coAnswers { tags.toList() }

            val response = appoxeeAdapter.removeTags(tags)
            coVerify { engageApi.removeTags(allAny()) }
            Truth.assertThat(response.error).isInstanceOf(TimeoutException::class.java)
        }
    }

    @Test
    fun `add Custom Attributes with successful response`() {
        runTest {
            coEvery { engageApi.addCustomAttributes(allAny()) } coAnswers {
                Response.success(
                    200, ResponseData(
                        metadata = null,
                        payload = DefaultResponse(dmcUserId = "user1234", emptyList())
                    )
                )
            }
            val response = appoxeeAdapter.addCustomAttributes(mapOf("a" to 1, "b" to 2))
            coVerify { engageApi.addCustomAttributes(allAny()) }
            Truth.assertThat(response.statusCode).isEqualTo(200)
        }
    }

    @Test
    fun `add Custom Attributes with different type and successful response`() {
        runTest {
            coEvery { engageApi.addCustomAttributes(allAny()) } coAnswers {
                Response.success(
                    200, ResponseData(
                        metadata = null,
                        payload = DefaultResponse(dmcUserId = "user1234", emptyList())
                    )
                )
            }

            val currentDate = Date()
            val attributes = mapOf(
                "a" to 1, "b" to true, "c" to currentDate, "d" to "test attribute"
            )

            coEvery { storage.getCustomAttributesCache() } coAnswers {
                CustomAttributesCache(
                    attributes = emptyMap()
                )
            }

            val response = appoxeeAdapter.addCustomAttributes(attributes)
            coVerify {
                engageApi.addCustomAttributes(
                    mapOf(
                        "a" to 1,
                        "b" to true,
                        "c" to currentDate.toUtcString(),
                        "d" to "test attribute"
                    )
                )
            }
            Truth.assertThat(response.statusCode).isEqualTo(200)
        }
    }

    @Test
    fun `add Custom Attributes with date type and successful response`() {
        runTest {
            coEvery { engageApi.addCustomAttributes(allAny()) } coAnswers {
                Response.success(
                    200, ResponseData(
                        metadata = null,
                        payload = DefaultResponse(dmcUserId = "user1234", emptyList())
                    )
                )
            }

            val currentDate = Date()
            val attributes = mapOf(
                "date" to currentDate
            )

            coEvery { storage.getCustomAttributesCache() } coAnswers {
                CustomAttributesCache(
                    attributes = emptyMap()
                )
            }

            val response = appoxeeAdapter.addCustomAttributes(attributes)
            // verify that date is converted to UTC String before sending
            coVerify { engageApi.addCustomAttributes(mapOf("date" to currentDate.toUtcString())) }
            Truth.assertThat(response.statusCode).isEqualTo(200)
        }
    }

    @Test
    fun `add custom attributes that not exist only with successful response`() {
        runTest {
            coEvery { engageApi.addCustomAttributes(allAny()) } coAnswers {
                Response.success(
                    200, ResponseData(
                        metadata = null,
                        payload = DefaultResponse(dmcUserId = "user1234", emptyList())
                    )
                )
            }

            val date = Date()
            val allAttributes = mapOf(
                "a" to 124.5, "b" to true, "c" to date, "d" to "test attribute"
            )

            val cachedAttributes = mapOf(
                "c" to date.toUtcString(), "d" to "test attribute"
            )

            val diffAttributes =
                allAttributes.filterNot {
                    val item = (it.value as? Date)?.toUtcString() ?: it.value
                    cachedAttributes.getOrDefault(it.key) { null } == item
                }

            coEvery { storage.getCustomAttributesCache() } coAnswers {
                CustomAttributesCache(
                    attributes = cachedAttributes
                )
            }

            val response = appoxeeAdapter.addCustomAttributes(allAttributes)
            coVerify { engageApi.addCustomAttributes(diffAttributes) }
            Truth.assertThat(response.statusCode).isEqualTo(200)
        }
    }

    @Test
    fun `add Custom Attributes with error response`() {
        runTest {
            coEvery { engageApi.addCustomAttributes(allAny()) } coAnswers {
                Response.error(TimeoutException())
            }
            val response = appoxeeAdapter.addCustomAttributes(mapOf("a" to 1, "b" to 2))
            coVerify { engageApi.addCustomAttributes(allAny()) }
            Truth.assertThat(response.error).isInstanceOf(TimeoutException::class.java)
        }
    }

    @Test
    fun `getRegions with successful response`() {
        runTest {
            coEvery { engageApi.getRegions(any(), any(), any(), any()) } coAnswers {
                Response.success(
                    200, ResponseData(
                        metadata = null, payload = RegionsResponse(1, listOf(mockk(), mockk()))
                    )
                )
            }
            val response = appoxeeAdapter.getRegions(
                0.0, 0.5, 1, 20
            )
            coVerify { engageApi.getRegions(any(), any(), any(), any()) }
            Truth.assertThat(response.isSuccess()).isTrue()
            Truth.assertThat(response.data?.payload?.regions).isNotEmpty()
        }
    }

    @Test
    fun `eventRegions with successfull response`() {
        runTest {
            coEvery { engageApi.regionEvent(any(), any(), any(), any(), any()) } coAnswers {
                Response.success(
                    200, ResponseData(
                        metadata = null,
                        payload = DefaultResponse(dmcUserId = "user1234", emptyList())
                    )
                )
            }

            val response = appoxeeAdapter.eventRegions(mockk(), 0.5, 0.5, 1, 20)
            coVerify { engageApi.regionEvent(any(), any(), any(), any(), any()) }
            Truth.assertThat(response.isSuccess()).isTrue()
            Truth.assertThat(response.data?.payload?.dmcUserId).isEqualTo("user1234")
        }
    }

    @Test
    fun `activate mapp_engage_sdk`() {
        runTest {
            coEvery { engageApi.activate(any()) } coAnswers {
                Response.success(
                    200, ResponseData(
                        metadata = null,
                        payload = DefaultResponse(dmcUserId = "user1234", emptyList())
                    )
                )
            }
            val response = appoxeeAdapter.activate(10_000)
            coVerify { engageApi.activate(any(Long::class)) }
            Truth.assertThat(response.isSuccess()).isTrue()
            Truth.assertThat(response.data?.payload?.dmcUserId).isEqualTo("user1234")
        }
    }
}
