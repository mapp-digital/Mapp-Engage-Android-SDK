package com.appoxee.internal.push.style

import com.appoxee.internal.ui.push.model.PushData
import com.appoxee.internal.ui.push.style.NotificationImageStyle
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test

class NotificationImageStyleTest {

    private lateinit var notificationStyle: NotificationImageStyle
    private lateinit var pushData: PushData

    @Before
    fun setUp() {
        pushData = mockk(relaxed = true)
        notificationStyle = spyk(NotificationImageStyle(pushData))
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