package com.appoxee.internal.network

import com.appoxee.AppoxeeOptions
import com.appoxee.internal.model.request.ActionModel
import com.appoxee.internal.model.request.DeviceModel
import com.appoxee.internal.model.request.RegisterModel
import com.appoxee.push.NotificationMode
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
    val client = NetworkClientImpl(options, coroutineScope)

    @Test
    fun execute() = runBlocking {
        val register = RegisterModel(
            osName = "Android",
            pushToken = "",
            appVersion = "1.0.0",
            clientVersion = "7.0.0",
            osNumber = "13"
        )
        val device =
            DeviceModel(
                key = UUID.randomUUID().toString(),
                actions = ActionModel(register = register)
            )
        val request = Request.Put(path = "api/v3/device", requestBody = device).apply {
            headers?.put("X_KEY", "183408d0cd3632.83592719")
        }
        client.execute(request)
    }
}