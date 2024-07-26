package com.appoxee.internal.push.style

import com.appoxee.internal.push.model.PushData
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test

class NotificationTextStyleTest {

    private lateinit var notificationStyle: NotificationTextStyle
    private lateinit var pushData: PushData

    @Before
    fun setUp() {
        pushData = mockk(relaxed = true)
        notificationStyle = spyk(NotificationTextStyle(pushData))
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun getStyle() {
        runBlocking {
            notificationStyle.getStyle()
        }
        coVerify(exactly = 1) { notificationStyle.getStyle() }
    }
}