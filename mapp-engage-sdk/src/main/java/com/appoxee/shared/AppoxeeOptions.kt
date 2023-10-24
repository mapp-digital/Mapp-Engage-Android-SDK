package com.appoxee.shared

class AppoxeeOptions(
    val server: Server,
    val sdkKey: String,
    val appId: String,
    val tenantId: String,
) {
    private var mCepUrl: String? = null
    var cepUrl: String?
        get() {
            return if (mCepUrl.isNullOrEmpty()) server.internalCepUrl else mCepUrl
        }
        set(value) {
            mCepUrl = value
        }

    var forceResend: Boolean = false
    var onStartRemoveNotification: Boolean = false
    var logType: LogLevel = LogLevel.RELEASE
    var notificationMode: NotificationMode = NotificationMode.BACKGROUND_ONLY
    /*val plugins:List<Class<? extends AppoxeePlugin>>,
    * val customNotificationCreator:NotificationCreator,
    * */


    enum class Server(val value: String, internal val internalCepUrl: String) {
        L3(
            value = "https://jamie.g.shortest-route.com/charon",
            internalCepUrl = "https://jamie.m.shortest-route.com/charon"
        ),
        L3_US(
            value = "https://jamie.a.shortest-route.com/charon",
            internalCepUrl = "https://jamie.m.shortest-route.com/charon"
        ),
        EMC(
            value = "https://jamie.h.shortest-route.com/charon",
            internalCepUrl = "https://jamie.m.shortest-route.com/charon"
        ),
        EMC_US(
            value = "https://jamie.c.shortest-route.com/charon",
            internalCepUrl = "https://jamie.m.shortest-route.com/charon"
        ),
        CROC(
            value = "https://jamie.m.shortest-route.com/charon",
            internalCepUrl = "https://jamie.m.shortest-route.com/charon"
        ),
        TEST(
            value = "https://charon-test.shortest-route.com",
            internalCepUrl = "https://jamie-test.shortest-route.com"
        ),
        TEST_55(
            value = "https://charon-qa.shortest-route.com",
            internalCepUrl = "https://jamie-test.shortest-route.com"
        ),
        TEST_61(
            value = "https://charon-qa-61.shortest-route.com",
            internalCepUrl = "https://jamie-test.shortest-route.com"
        );

        fun get(name: String): Server {
            return valueOf(name)
        }
    }

    enum class LogLevel(value: String) {
        DEBUG("debug"),
        RELEASE("release");
    }

    override fun toString(): String {
        return "AppoxeeOptions(server=$server, sdkKey='$sdkKey', appId='$appId', tenantId='$tenantId', cepUrl=$cepUrl, forceResend=$forceResend, onStartRemoveNotification=$onStartRemoveNotification, logType=$logType, notificationMode=$notificationMode)"
    }
}