package com.appoxee.internal.ui.inapp

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.appoxee.internal.model.response.inapp.ActionData
import com.appoxee.internal.model.response.inapp.InappActionType
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

class InappActionHandlerImplTest {

    private lateinit var inappActionHandler: InappActionHandler
    private lateinit var context: Context

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        mockkStatic(Uri::class)
        mockkStatic(Intent::class)
        mockkConstructor(Intent::class)

        every { Uri.parse(any()) } returns mockk()
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0

        context = mockk(relaxed = true)
        inappActionHandler = spyk(InappActionHandlerImpl(context))
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `inapp with action type APP_STORE should call handleAppStore`() {
        val actionData = mockk<ActionData>(relaxed = true)
        every { actionData.actionType } answers { InappActionType.APP_STORE }
        every { inappActionHandler.handleAppStore(any()) } just runs

        inappActionHandler.handleAction(actionData)

        verify { inappActionHandler.handleAppStore(any()) }
    }

    @Test
    fun `inapp with action type DEEPLINK should call handleDeeplink`() {
        val actionData = mockk<ActionData>(relaxed = true)
        every { actionData.actionType } answers { InappActionType.DEEPLINK }
        every { inappActionHandler.handleDeeplink(any()) } just runs

        inappActionHandler.handleAction(actionData)

        verify { inappActionHandler.handleDeeplink(any()) }
    }

    @Test
    fun `inapp with action type DIALER should call handleDialer`() {
        val actionData = mockk<ActionData>(relaxed = true)
        every { actionData.actionType } answers { InappActionType.DIALER }
        every { inappActionHandler.handleDialer(any()) } just runs

        inappActionHandler.handleAction(actionData)

        verify { inappActionHandler.handleDialer(any()) }
    }

    @Test
    fun `inapp with action type LANDING_PAGE should call handleLandingPageInApp when openInApp is true`() {
        val actionData = mockk<ActionData>(relaxed = true)
        every { actionData.actionType } answers { InappActionType.LANDING_PAGE }
        every { actionData.openInApp } answers { true }
        every { inappActionHandler.handleLandingPageInApp(any()) } just runs

        inappActionHandler.handleAction(actionData)

        verify { inappActionHandler.handleLandingPageInApp(any()) }
    }

    @Test
    fun `inapp with action type LANDING_PAGE should call handleLandingPageExternal when openInApp is false`() {
        val actionData = mockk<ActionData>(relaxed = true)
        every { actionData.actionType } answers { InappActionType.LANDING_PAGE }
        every { actionData.openInApp } answers { false }
        every { inappActionHandler.handleLandingPageExternal(any()) } just runs

        inappActionHandler.handleAction(actionData)

        verify { inappActionHandler.handleLandingPageExternal(any()) }
    }

}