package com.appoxee.internal.ui.inapp

import android.app.Activity
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.appoxee.internal.TestDispatchers
import com.appoxee.internal.container.StatsContainer
import com.appoxee.internal.model.request.events.TrackingKey
import com.appoxee.internal.model.response.inapp.InappResponse
import com.appoxee.internal.model.response.inapp.Message
import com.appoxee.internal.model.response.inapp.NativeInappMessage
import com.appoxee.internal.model.response.inapp.TrackingParams
import com.appoxee.internal.model.response.inapp.WebInappMessage
import com.appoxee.internal.ui.inapp.nativ.NativeFactory
import com.appoxee.internal.ui.inapp.web.WebFactory
import com.appoxee.internal.util.Dispatchers
import com.google.common.truth.Truth
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test

class InAppManagerImplTest {

    // Mocks
    private lateinit var nativeFactory: NativeFactory
    private lateinit var webFactory: WebFactory
    private lateinit var statsContainer: StatsContainer
    private lateinit var scope: CoroutineScope
    private lateinit var dispatchers: Dispatchers

    // Class under test
    private lateinit var inAppManager: InAppManagerImpl

    // Mock objects
    private lateinit var activity: Activity
    private lateinit var nativeMessage: NativeInappMessage
    private lateinit var webMessage: WebInappMessage
    private lateinit var messageList: List<Message>
    private lateinit var context: Application

    @Before
    fun setUp() {
        // Initialize mocks using Mockk
        context = ApplicationProvider.getApplicationContext()
        nativeFactory = mockk()
        webFactory = mockk()
        dispatchers = TestDispatchers()
        statsContainer = spyk(StatsContainer(context, dispatchers))
        // use a testing scope
        scope = CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined + SupervisorJob())

        // Create the class under test
        inAppManager = InAppManagerImpl(
            nativeFactory = nativeFactory,
            webFactory = webFactory,
            statsContainer = statsContainer,
            scope = scope,
            dispatchers = dispatchers
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
        scope.cancel()
        unmockkAll()
    }

    @Test
    fun handleMessages_should_display_first_message_and_report_it() = runBlocking {
        // Arrange
        every { nativeFactory.createBanner(any(), nativeMessage, any(), any()) } just runs
        coEvery {
            inAppManager.reportInappEvent(
                nativeMessage, TrackingKey.IA_MSG_DISPLAYED, TrackingParams()
            )
        } just runs
        coEvery {
            statsContainer.statsClient.reportInappEvent(
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
    fun handleMessages_should_call_onShow_and_trigger_reportInappDisplayed() = runBlocking {
        // Arrange
        val onShowSlot = slot<(NativeInappMessage) -> Unit>()
        val onMessageClosedSlot = slot<(NativeInappMessage, TrackingKey, TrackingParams) -> Unit>()

        // Mock showMessage to capture lambdas
        every {
            nativeFactory.createBanner(
                activity, nativeMessage, capture(onShowSlot), capture(onMessageClosedSlot)
            )
        } just runs

        coEvery {
            inAppManager.reportInappEvent(
                nativeMessage, TrackingKey.IA_MSG_DISPLAYED, TrackingParams()
            )
        } just runs

        coEvery {
            statsContainer.statsClient.reportInappEvent(
                "123", 1, TrackingKey.IA_MSG_DISPLAYED, emptyMap<String, Any>()
            )
        } just runs

        val messageList = listOf(nativeMessage)

        // Act
        inAppManager.handleMessages(activity, messageList)

        // Simulate onShow being called
        onShowSlot.captured.invoke(nativeMessage)

        // Assert
        // Verify that reportInappDisplayed was called inside the onShow lambda
        coVerify {
            inAppManager.reportInappEvent(
                nativeMessage,
                TrackingKey.IA_MSG_DISPLAYED,
                TrackingParams()
            )
        }
    }

    @Test
    fun handleMessages_should_handle_subsequent_messages_when_one_is_closed() = runBlocking {
        // Arrange
        val onShowSlot = slot<(Message) -> Unit>()
        val onMessageClosedSlot = slot<(Message, TrackingKey, TrackingParams) -> Unit>()

        every {
            nativeFactory.createBanner(
                any(), any(), capture(onShowSlot), capture(onMessageClosedSlot)
            )
        } just runs

        every {
            webFactory.createBanner(
                any(), any(), capture(onShowSlot), capture(onMessageClosedSlot)
            )
        } just runs

        //coEvery { inAppManager.reportInappDisplayed(any()) } just runs
        //coEvery { inAppManager.reportInappEvent(any(), any(), any()) } just runs

        // Act
        inAppManager.handleMessages(activity, messageList)

        onShowSlot.captured.invoke(nativeMessage)
        onMessageClosedSlot.captured.invoke(
            nativeMessage, TrackingKey.IA_MSG_DISMISSED, TrackingParams()
        )
        onShowSlot.captured.invoke(webMessage)

        // Assert
        // Verify the first message is displayed
        verify { nativeFactory.createBanner(activity, nativeMessage, any(), any()) }
        // Verify the second message is displayed after the first is closed
        verify { webFactory.createBanner(activity, webMessage, any(), any()) }
    }

    @Test
    fun parseResponse_should_return_sorted_list_of_messages() {
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
    fun handleMessages_should_not_call_showMessage_if_messages_list_is_empty() = runBlocking {
        // Arrange
        val emptyMessages = emptyList<Message>()

        // Act
        inAppManager.handleMessages(activity, emptyMessages)

        // Assert
        coVerify(exactly = 0) { nativeFactory.createBanner(any(), any(), any(), any()) }
        coVerify(exactly = 0) { webFactory.createBanner(any(), any(), any(), any()) }
    }
}
