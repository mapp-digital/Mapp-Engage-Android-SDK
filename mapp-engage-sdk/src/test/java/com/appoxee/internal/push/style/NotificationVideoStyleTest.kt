package com.appoxee.internal.push.style

import com.appoxee.internal.ui.push.model.PushData
import com.appoxee.internal.ui.push.style.NotificationVideoStyle
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test

class NotificationVideoStyleTest {


    private lateinit var notificationStyle: NotificationVideoStyle
    private lateinit var pushData: PushData

    @Before
    fun setUp() {
        pushData = mockk(relaxed = true)
        notificationStyle = spyk(NotificationVideoStyle(pushData))
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