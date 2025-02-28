package com.appoxee.internal.ui.inapp.web

import android.app.Activity
import com.appoxee.internal.TestDispatchersProvider
import com.appoxee.internal.container.ActionContainer
import com.appoxee.internal.model.request.events.TrackingKey
import com.appoxee.internal.model.response.inapp.BannerPosition
import com.appoxee.internal.model.response.inapp.Behaviour
import com.appoxee.internal.model.response.inapp.InappType
import com.appoxee.internal.model.response.inapp.Location
import com.appoxee.internal.model.response.inapp.Message
import com.appoxee.internal.model.response.inapp.TrackingParams
import com.appoxee.internal.model.response.inapp.WebInappMessage
import com.appoxee.internal.ui.inapp.InappActionHandler
import com.appoxee.internal.ui.inapp.Template
import com.appoxee.internal.util.DispatchersProvider
import com.google.common.truth.Truth
import io.mockk.coVerifyAll
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import io.mockk.unmockkAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

import org.junit.After
import org.junit.Before
import org.junit.Test

class WebFactoryTest {
    private lateinit var factory: WebFactory
    private lateinit var scope: CoroutineScope
    private lateinit var dispatchersProvider: DispatchersProvider
    private lateinit var activity: Activity
    private lateinit var actionContainer: ActionContainer

    @Before
    fun setUp() {
        scope = CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)
        dispatchersProvider = TestDispatchersProvider()
        activity = mockk(relaxed = true)
        actionContainer= mockk(relaxed = true)
        factory = spyk(WebFactory(scope, dispatchersProvider, actionContainer))
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun check_if_all_methods_are_called() = runTest {
        val message = mockk<WebInappMessage>(relaxed = true) {
            every { behaviour } returns Behaviour(0, 0)
        }
        val template = mockk<Template>(relaxed = true) {
            every { show() } just runs
        }
        val onShow = mockk<(Message) -> Unit>(relaxed = true)
        val onClose = mockk<(Message, TrackingKey, TrackingParams) -> Unit>(relaxed = true)
        every { factory.getDelay(any()) } returns 0
        every { factory.createTemplate(any(), any(), any(), any()) } returns template
        every { activity.isDestroyed } returns false

        factory.createBanner(
            activity,
            message,
            onShow,
            onClose
        )

        coVerifyAll {
            factory.createBanner(any(), message, onShow, onClose)
            factory.getDelay(message)
            factory.createTemplate(any(), any(), message, onClose)
            template.show()
        }
    }

    @Test
    fun fullscreen_message_type_creates_fullscreen_template() {
        // Arrange
        val actionHandler = mockk<InappActionHandler>()
        val message = mockk<WebInappMessage> {
            every { type } returns InappType.FULLSCREEN
        }

        every {
            factory.createTemplate(
                any(),
                actionHandler,
                message,
                null
            )
        } returns mockk<FullscreenWebTemplate<*>>(relaxed = true)

        // Act
        val result = factory.createTemplate(activity, actionHandler, message)

        // Assert
        Truth.assertThat(result).isInstanceOf(FullscreenWebTemplate::class.java)
    }

    @Test
    fun standard_message_type_creates_dialog_template() {
        // Arrange
        val actionHandler = mockk<InappActionHandler>()
        val message = mockk<WebInappMessage> {
            every { type } returns InappType.DIALOG
        }

        val onClose = mockk<(Message, TrackingKey, TrackingParams) -> Unit>(relaxed = true)
        every {
            factory.createTemplate(
                any(),
                actionHandler,
                message,
                onClose
            )
        } returns mockk<StandardWebTemplate<*>>(relaxed = true)

        // Act
        val result = factory.createTemplate(activity, actionHandler, message, onClose)

        // Assert
        Truth.assertThat(result).isInstanceOf(StandardWebTemplate::class.java)
    }

    @Test
    fun banner_message_type_creates_banner_template() {
        // Arrange
        val actionHandler = mockk<InappActionHandler>()
        val message = mockk<WebInappMessage> {
            every { type } returns InappType.BANNER
            every { location } returns Location(BannerPosition.TOP, 0, 0)
        }

        every {
            factory.createTemplate(
                any(),
                actionHandler,
                message,
                null
            )
        } returns mockk<BannerWebTemplate<*>>(relaxed = true)

        // Act
        val result = factory.createTemplate(activity, actionHandler, message)

        // Assert
        Truth.assertThat(result).isInstanceOf(BannerWebTemplate::class.java)
    }
}