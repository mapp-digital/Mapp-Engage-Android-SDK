package com.appoxee.internal.network

import com.appoxee.internal.push.model.PushData
import com.appoxee.shared.AppoxeeOptions
import com.appoxee.shared.NotificationMode

object MockData {
    internal fun getPushData(type: String = "text"): PushData {
        return PushData(
            id = 1L,
            title = "Push title",
            alert = "New alert message",
            bigText = "Big text for a push message",
            type = type
        )
    }

    val appoxeeOptions =
        AppoxeeOptions(server = AppoxeeOptions.Server.L3, "12345.67890", "1234", "5678").also {
            it.connectionTimeout = 5000
            it.readTimeout = 5000
            it.notificationMode = NotificationMode.BACKGROUND_AND_FOREGROUND
            it.logType = AppoxeeOptions.LogLevel.DEBUG
        }

    const val GET_DEVICE_RESPONSE = "{\n" +
            "    \"links\": [],\n" +
            "    \"metadata\": {\n" +
            "        \"error\": false,\n" +
            "        \"statusCode\": 200\n" +
            "    },\n" +
            "    \"payload\": {\n" +
            "        \"get\": {\n" +
            "            \"dmcUserId\": \"45290582851\",\n" +
            "            \"UDIDHashed\": \"3C3AF105F6D9222FCA0023B4221AA00FA48A7A2CB6E957E715ED728F74F762D6\",\n" +
            "            \"pushToken_bk\": null,\n" +
            "            \"alias\": \"AUTO_206974_3C3AF105F6D9222FCA0023B4221AA00FA48A7A2CB6E957E715ED728F74F762D6\",\n" +
            "            \"pushToken\": \"d9IZ3_EcSWKlYgjXkljLrU:APA91bGifXcW4vPHXN4p84ieXDDrAf59EE-mlACzpQT-zCCsjh6LiCJxL2ma4_xsGGvY_vPXRFg4vg5o1Mq5uRZ4_Ne58WGWd-6HCVQRCpin_JoS4D8zY3Bq0sMGEQ0kX10IezP4wsXH\"\n" +
            "        }\n" +
            "    }\n" +
            "}"
}