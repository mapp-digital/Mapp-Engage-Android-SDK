package com.appoxee.internal

import android.content.Context
import android.provider.Settings
import android.provider.Settings.Secure.ANDROID_ID
import com.appoxee.AppoxeeOptions
import com.appoxee.internal.model.request.BaseBodyModel
import com.appoxee.internal.model.request.RegisterModel
import com.appoxee.internal.model.response.RegisterResponse
import com.appoxee.internal.model.response.Response
import com.appoxee.internal.network.EngageApi
import com.appoxee.internal.network.EngageApiImpl
import com.appoxee.internal.network.NetworkClientImpl
import java.util.UUID

internal class AppoxeeAdapter(
    private val context: Context,
    options: AppoxeeOptions
) {
    private val client = NetworkClientImpl(options)

    private val engageApi: EngageApi = EngageApiImpl(client, options)

    internal suspend fun register(): Response<RegisterResponse> {
        val register = RegisterModel(
            osName = "Android",
            pushToken = "",
            appVersion = "1.0.0",
            clientVersion = "7.0.0",
            locale = "en-US",
            timeZone = "America/Bogota",
            hardwareType = "Samsung s22+",
            density = "326",
            vendorID = UUID.randomUUID().toString(),
            osNumber = "13",
            resolution = "1080x1920"
        )

        val device =
            BaseBodyModel(
                key = Settings.Secure.getString(context.contentResolver, ANDROID_ID),
                actions = register
            )

        return engageApi.register(device)
    }

    suspend fun setAlias(alias: String): Boolean {
        return true
    }

    suspend fun getAlias(): String? {
        return ""
    }
}