package com.appoxee.internal.ui.inapp

import android.app.Activity
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.appoxee.internal.TestDispatchersProvider
import com.appoxee.internal.model.request.events.TrackingKey
import com.appoxee.internal.model.response.inapp.InappResponse
import com.appoxee.internal.model.response.inapp.Message
import com.appoxee.internal.model.response.inapp.NativeInappMessage
import com.appoxee.internal.model.response.inapp.TrackingParams
import com.appoxee.internal.model.response.inapp.WebInappMessage
import com.appoxee.internal.stats.StatsClient
import com.appoxee.internal.ui.inapp.nativ.NativeFactory
import com.appoxee.internal.ui.inapp.web.WebFactory
import com.appoxee.internal.util.DispatchersProvider
import com.google.common.truth.Truth
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class InAppManagerImplTest {

    // Mocks
    private lateinit var nativeFactory: NativeFactory
    private lateinit var webFactory: WebFactory
    private lateinit var dispatchersProvider: DispatchersProvider

    // Class under test
    private lateinit var inAppManager: InAppManagerImpl

    // Mock objects
    private lateinit var activity: Activity
    private lateinit var nativeMessage: NativeInappMessage
    private lateinit var webMessage: WebInappMessage
    private lateinit var messageList: List<Message>
    private lateinit var context: Application

    private lateinit var scope: CoroutineScope

    private lateinit var statsClient: StatsClient

    @Before
    fun setUp() {
        // Initialize mocks using Mockk
        context = ApplicationProvider.getApplicationContext()
        nativeFactory = mockk()
        webFactory = mockk()
        dispatchersProvider = TestDispatchersProvider()
        scope = TestScope(StandardTestDispatcher())
        statsClient = mockk(relaxed = true)
        // Create the class under test
        inAppManager = InAppManagerImpl(
            nativeFactory = nativeFactory,
            webFactory = webFactory,
            statsClient = statsClient,
            scope = scope,
            dispatchersProvider = dispatchersProvider
        )

        // Mock other objects
        activity = mockk(relaxed = true)
        nativeMessage = mockk(relaxed = true)
        webMessage = mockk(relaxed = true)

        // Prepare message list
        messageList = listOf(nativeMessage, webMessage)
    }

    @After
    fun tearDown() {
        // Clean up after tests
        unmockkAll()
    }

    @Test
    fun handleMessages_should_display_first_message_and_report_it() = runTest {
        // Arrange
        every { nativeFactory.createBanner(any(), nativeMessage, any(), any()) } just runs
        coEvery {
            inAppManager.reportInappEvent(
                nativeMessage, TrackingKey.IA_MSG_DISPLAYED, TrackingParams()
            )
        } just runs
        coEvery {
            statsClient.reportInappEvent(
                any(), any(), any(), any()
            )
        } just runs

        // Act
        inAppManager.handleMessages(activity, messageList)

        // Assert
        // Verify the first message is displayed
        verify { nativeFactory.createBanner(any(), nativeMessage, any(), any()) }
        verify { inAppManager.showMessage(any(), nativeMessage, any(), any()) }
    }

    @Test
    fun parseResponse_should_return_sorted_list_of_messages() = runTest {
        // Arrange
        val inappResponse = mockk<InappResponse> {
            every { webMessages } returns listOf(webMessage)
            every { nativeMessages } returns listOf(nativeMessage)
        }

        // Act
        val result = inAppManager.parseResponse(inappResponse)

        // Assert
        Truth.assertThat(result).hasSize(2)
        Truth.assertThat(result[0]).isEqualTo(webMessage)
        Truth.assertThat(result[1]).isEqualTo(nativeMessage)
    }

    @Test
    fun handleMessages_should_not_call_showMessage_if_messages_list_is_empty() = runTest {
        // Arrange
        val emptyMessages = emptyList<Message>()

        // Act
        inAppManager.handleMessages(activity, emptyMessages)

        // Assert
        coVerify(exactly = 0) { nativeFactory.createBanner(any(), any(), any(), any()) }
        coVerify(exactly = 0) { webFactory.createBanner(any(), any(), any(), any()) }
    }
}
