package com.appoxee.internal.network

import com.appoxee.shared.AppoxeeOptions
import com.appoxee.internal.model.request.RequestBody
import com.appoxee.internal.model.request.RegisterDeviceModel
import com.appoxee.shared.NotificationMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.util.UUID

class NetworkClientImplTest {

    val options: AppoxeeOptions = AppoxeeOptions(
        server = AppoxeeOptions.Server.L3,
        sdkKey = "183408d0cd3632.83592719",
        tenantId = "206974",
        appId = "5963",
    ).also {
        it.logType = AppoxeeOptions.LogLevel.DEBUG
        it.notificationMode = NotificationMode.BACKGROUND_AND_FOREGROUND
    }
    val coroutineScope = CoroutineScope(Dispatchers.IO)
    val client = NetworkClientImpl(options)

    @Test
    fun execute() = runBlocking {
        val register = RegisterDeviceModel(
            osName = "Android",
            pushToken = "",
            appVersion = "1.0.0",
            clientVersion = "7.0.0",
            osNumber = "13"
        )
        val device =
            RequestBody(
                key = UUID.randomUUID().toString(),
                actions = RegisterDeviceModel()
            )
        val request = Request.Put(path = "api/v3/device", requestBody = device).apply {
            headers["X_KEY"] = "183408d0cd3632.83592719"
        }
        client.execute(request)
    }
}