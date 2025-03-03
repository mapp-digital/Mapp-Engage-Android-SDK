package com.appoxee.internal.ui.inapp.nativ

import android.app.Activity
import com.appoxee.internal.TestDispatchersProvider
import com.appoxee.internal.container.ActionContainer
import com.appoxee.internal.model.request.events.TrackingKey
import com.appoxee.internal.model.response.inapp.BannerPosition
import com.appoxee.internal.model.response.inapp.Behaviour
import com.appoxee.internal.model.response.inapp.ContentTemplates
import com.appoxee.internal.model.response.inapp.Location
import com.appoxee.internal.model.response.inapp.Message
import com.appoxee.internal.model.response.inapp.NativeInappMessage
import com.appoxee.internal.model.response.inapp.TrackingParams
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

class NativeFactoryTest {
    private lateinit var factory: NativeFactory
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
        factory = spyk(NativeFactory(scope, dispatchersProvider,actionContainer))
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun check_if_all_methods_are_called() = runTest {
        val message = mockk<NativeInappMessage>(relaxed = true) {
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
    fun fullscreen_content_type_creates_fullscreen_template() {
        // Arrange
        val actionHandler = mockk<InappActionHandler>()
        val message = mockk<NativeInappMessage> {
            every { contentTemplateId } returns ContentTemplates.FULLSCREEN
        }

        every {
            factory.createTemplate(
                any(),
                actionHandler,
                message,
                null
            )
        } returns mockk<FullscreenNativeTemplate<*>>(relaxed = true)

        // Act
        val result = factory.createTemplate(activity, actionHandler, message)

        // Assert
        Truth.assertThat(result).isInstanceOf(FullscreenNativeTemplate::class.java)
    }

    @Test
    fun standard_content_type_creates_standard_template() {
        // Arrange
        val actionHandler = mockk<InappActionHandler>()
        val message = mockk<NativeInappMessage> {
            every { contentTemplateId } returns ContentTemplates.STANDARD
        }

        val onClose = mockk<(Message, TrackingKey, TrackingParams) -> Unit>(relaxed = true)
        every {
            factory.createTemplate(
                any(),
                actionHandler,
                message,
                onClose
            )
        } returns mockk<StandardNativeTemplate<*>>(relaxed = true)

        // Act
        val result = factory.createTemplate(activity, actionHandler, message, onClose)

        // Assert
        Truth.assertThat(result).isInstanceOf(StandardNativeTemplate::class.java)
    }

    @Test
    fun background_image_content_type_creates_standard_image_template() {
        // Arrange
        val actionHandler = mockk<InappActionHandler>()
        val message = mockk<NativeInappMessage> {
            every { contentTemplateId } returns ContentTemplates.BACKGROUND_IMAGE_STANDARD
        }

        every {
            factory.createTemplate(
                any(),
                actionHandler,
                message,
                null
            )
        } returns mockk<StandardImageNativeTemplate<*>>(relaxed = true)

        // Act
        val result = factory.createTemplate(activity, actionHandler, message)

        // Assert
        Truth.assertThat(result).isInstanceOf(StandardImageNativeTemplate::class.java)
    }

    @Test
    fun fullscreen_image_content_type_creates_fullscreen_image_template() {
        // Arrange
        val actionHandler = mockk<InappActionHandler>()
        val message = mockk<NativeInappMessage> {
            every { contentTemplateId } returns ContentTemplates.BACKGROUND_IMAGE_FULLSCREEN
        }

        every {
            factory.createTemplate(
                any(),
                actionHandler,
                message,
                null
            )
        } returns mockk<FullscreenImageNativeTemplate<*>>(relaxed = true)

        // Act
        val result = factory.createTemplate(activity, actionHandler, message)

        // Assert
        Truth.assertThat(result).isInstanceOf(FullscreenImageNativeTemplate::class.java)
    }

    @Test
    fun banner_content_type_creates_banner_template() {
        // Arrange
        val actionHandler = mockk<InappActionHandler>()
        val message = mockk<NativeInappMessage> {
            every { contentTemplateId } returns ContentTemplates.BANNER_TOP
            every { location } returns Location(BannerPosition.TOP, 0, 0)
        }

        every {
            factory.createTemplate(
                any(),
                actionHandler,
                message,
                null
            )
        } returns mockk<BannerNativeTemplate<*>>(relaxed = true)

        // Act
        val result = factory.createTemplate(activity, actionHandler, message)

        // Assert
        Truth.assertThat(result).isInstanceOf(BannerNativeTemplate::class.java)
    }
}