package com.appoxee.internal.push.style

import com.appoxee.internal.network.MockData
import com.appoxee.internal.push.model.PushData
import com.google.common.truth.Truth
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Test

class NotificationStyleFactoryTest {
    private lateinit var factory: NotificationStyleFactory

    private lateinit var pushData: PushData

    @Before
    fun setUp() {
        pushData = mockk(relaxed = true)
        factory = spyk(NotificationStyleFactory())
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun build_notification_text_style() {
        mockk<PushData>() {
            every { pushData.type } answers { "text" }
        }
        val style = factory.buildNotificationStyle(pushData)
        Truth.assertThat(style).isInstanceOf(NotificationTextStyle::class.java)
    }

    @Test
    fun build_notification_video_style() {
        mockk<PushData>() {
            every { pushData.type } answers { "video" }
        }
        val style = factory.buildNotificationStyle(pushData)
        Truth.assertThat(style).isInstanceOf(NotificationVideoStyle::class.java)
    }

    @Test
    fun build_notification_gif_style() {
        mockk<PushData>() {
            every { pushData.type } answers { "gif" }
        }
        val style = factory.buildNotificationStyle(pushData)
        Truth.assertThat(style).isInstanceOf(NotificationImageStyle::class.java)
    }

    @Test
    fun build_notification_image_style() {
        mockk<PushData>() {
            every { pushData.type } answers { "image" }
        }
        val style = factory.buildNotificationStyle(pushData)
        Truth.assertThat(style).isInstanceOf(NotificationImageStyle::class.java)
    }
}