package com.appoxee.shared

import androidx.annotation.IntRange
import com.appoxee.internal.util.getIntOrDefault
import com.appoxee.internal.util.getLongOrDefault
import com.appoxee.internal.util.getNullableString
import com.appoxee.internal.util.getStringOrEmpty
import org.json.JSONObject


private const val MIN_TIMEOUT: Int = 5_000
private const val MAX_TIMEOUT: Int = 60_000
private const val DEFAULT_TIMEOUT: Int = 10_000

class AppoxeeOptions(
    /**
     * Server enum value from the [Server] enums
     */
    val server: Server,
    /**
     * SDK Key is from a defined channel on Mapp Engage system
     */
    val sdkKey: String,
    /**
     * AppId is from defined channel on Mapp Engage system
     */
    val appId: String,
    /**
     * TenantId represents clients unique numeric key send as String
     */
    val tenantId: String,
) {

    /**
     * Sets connection timeout in milliseconds
     */
    @IntRange(from = MIN_TIMEOUT.toLong(), to = MAX_TIMEOUT.toLong())
    var connectionTimeout: Int = DEFAULT_TIMEOUT
        set(value) {
            if (value in MIN_TIMEOUT..MAX_TIMEOUT) {
                field = value
            }
        }

    /**
     * Sets connection read timeout in milliseconds
     */
    @IntRange(from = MIN_TIMEOUT.toLong(), to = MAX_TIMEOUT.toLong())
    var readTimeout: Int = DEFAULT_TIMEOUT
        set(value) {
            if (value in MIN_TIMEOUT..MAX_TIMEOUT) {
                field = value
            }
        }
    private var cepUrl: String? = null
        get() {
            return if (field.isNullOrEmpty()) server.internalCepUrl else field
        }

    /**
     * Whether to force sending requests on failures or not
     */
    private var forceResend: Boolean = false

    /**
     * Clears notifications when library is initialized
     */
    private var onStartRemoveNotification: Boolean = false

    /**
     * Defines the level for outputting logs
     */
    var logType: LogLevel = LogLevel.RELEASE

    /**
     * Defines notification mode; It can be one of the following values:
     * [NotificationMode.BACKGROUND_ONLY] or [NotificationMode.SILENT_ONLY] and [NotificationMode.BACKGROUND_AND_FOREGROUND]
     */
    var notificationMode: NotificationMode = NotificationMode.BACKGROUND_ONLY
    /*val plugins:List<Class<? extends AppoxeePlugin>>,
    * val customNotificationCreator:NotificationCreator,
    * */


    /**
     * Supported servers for Mapp Engage
     */
    enum class Server(val value: String, internal val internalCepUrl: String) {
        L3(
            value = "https://jamie.g.shortest-route.com/charon",
            internalCepUrl = "https://jamie.g.shortest-route.com"
        ),
        L3_US(
            value = "https://jamie.a.shortest-route.com/charon",
            internalCepUrl = "https://jamie.a.shortest-route.com"
        ),
        EMC(
            value = "https://jamie.h.shortest-route.com/charon",
            internalCepUrl = "https://jamie.h.shortest-route.com"
        ),
        EMC_US(
            value = "https://jamie.c.shortest-route.com/charon",
            internalCepUrl = "https://jamie.c.shortest-route.com"
        ),
        CROC(
            value = "https://jamie.m.shortest-route.com/charon",
            internalCepUrl = "https://jamie.m.shortest-route.com"
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

        /**
         * Returns the Server enum value for the given server's name value
         */
        fun get(name: String): Server {
            return valueOf(name)
        }
    }

    /**
     * Defines supported levels for Logging
     */
    enum class LogLevel(value: String) {
        DEBUG("debug"),
        RELEASE("release");
    }

    override fun toString(): String {
        return "AppoxeeOptions(server=$server, sdkKey='$sdkKey', appId='$appId', tenantId='$tenantId', cepUrl=$cepUrl, forceResend=$forceResend, onStartRemoveNotification=$onStartRemoveNotification, logType=$logType, notificationMode=$notificationMode)"
    }

    internal fun toJSON(): JSONObject {
        return JSONObject().apply {
            put("server", server.ordinal)
            put("sdkKey", sdkKey)
            put("appId", appId)
            put("tenantId", tenantId)
            put("connectionTimeout", connectionTimeout)
            put("readTimeout", readTimeout)
            put("forceResend", forceResend)
            put("logLevel", logType.ordinal)
            put("notificationType", notificationMode.ordinal)
            put("cepUrl", cepUrl)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AppoxeeOptions

        if (server != other.server) return false
        if (sdkKey != other.sdkKey) return false
        if (appId != other.appId) return false
        if (tenantId != other.tenantId) return false
        if (logType != other.logType) return false
        if (notificationMode != other.notificationMode) return false

        return true
    }

    override fun hashCode(): Int {
        var result = server.hashCode()
        result = 31 * result + sdkKey.hashCode()
        result = 31 * result + appId.hashCode()
        result = 31 * result + tenantId.hashCode()
        result = 31 * result + logType.hashCode()
        result = 31 * result + notificationMode.hashCode()
        return result
    }


    companion object {
        internal fun fromJSON(json: JSONObject): AppoxeeOptions {
            return AppoxeeOptions(
                server = Server.values()[json.getIntOrDefault("server")],
                sdkKey = json.getStringOrEmpty("sdkKey"),
                appId = json.getStringOrEmpty("appId"),
                tenantId = json.getStringOrEmpty("tenantId"),
            ).apply {
                connectionTimeout = json.getIntOrDefault("connectionTimeout", DEFAULT_TIMEOUT)
                readTimeout = json.getIntOrDefault("readTimeout", DEFAULT_TIMEOUT)
                forceResend = json.getBoolean("forceResend")
                logType =
                    LogLevel.values()[json.getIntOrDefault("logLevel", LogLevel.DEBUG.ordinal)]
                notificationMode = NotificationMode.values()[json.getIntOrDefault(
                    "notificationType",
                    NotificationMode.BACKGROUND_AND_FOREGROUND.ordinal
                )]
                cepUrl = json.getNullableString("cepUrl")
            }
        }
    }
}