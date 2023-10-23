package com.appoxee.internal.model.response

import org.json.JSONObject

public data class DevicePayload(
    private val json: JSONObject
) {
    var dmcUserId: String? = null /* Unique user id */
        private set
    var udidHashed: String? = null /* UDIDHashed */
        private set
    var pushTokenBk: String? = null /* OptOut Token */
        private set
    var pushToken: String? = null /* Opt In Token*/
        private set
    var alias: String? = null /* User Alias */
        private set

    init {
        if (json.has("dmcUserId")) {
            dmcUserId = json.getString("dmsUserId")
        }
        if (json.has("UDIDHashed")) {
            udidHashed = json.getString("UDIDHashed")
        }
        if (json.has("pushToken")) {
            pushToken = json.getString("pushToken")
        }
        if (json.has("pushToken_bk")) {
            pushTokenBk = json.getString("pushToken_bk")
        }
        if (json.has("alias")) {
            alias = json.getString("alias")
        }
    }
}