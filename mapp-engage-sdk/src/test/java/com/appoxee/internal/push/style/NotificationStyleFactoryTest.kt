package com.appoxee.internal.push.style

import com.appoxee.internal.network.MockData
import com.google.common.truth.Truth
import io.mockk.spyk
import org.junit.After
import org.junit.Before
import org.junit.Test

class NotificationStyleFactoryTest {
    private lateinit var factory: NotificationStyleFactory

    @Before
    fun setUp() {
        factory = spyk(NotificationStyleFactory())
    }

    @After
    fun tearDown() {

    }

    @Test
    fun build_notification_text_style() {
        val style = factory.buildNotificationStyle(MockData.getPushData("text"))
        Truth.assertThat(style).isInstanceOf(NotificationTextStyle::class.java)
    }

    @Test
    fun build_notification_video_style() {
        val style = factory.buildNotificationStyle(MockData.getPushData("video"))
        Truth.assertThat(style).isInstanceOf(NotificationVideoStyle::class.java)
    }

    @Test
    fun build_notification_gif_style() {
        val style = factory.buildNotificationStyle(MockData.getPushData("gif"))
        Truth.assertThat(style).isInstanceOf(NotificationImageStyle::class.java)
    }

    @Test
    fun build_notification_image_style() {
        val style = factory.buildNotificationStyle(MockData.getPushData("image"))
        Truth.assertThat(style).isInstanceOf(NotificationImageStyle::class.java)
    }
}