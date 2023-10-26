package com.appoxee.internal.model.response

import com.appoxee.internal.util.getNonNullString
import org.json.JSONObject

class DevicePayload() {
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

    fun toJSON(): JSONObject {
        return JSONObject().apply {
            put("dmcUserId", dmcUserId)
            put("UDIDHashed", udidHashed)
            put("pushToken", pushToken)
            put("pushToken_bk", pushTokenBk)
            put("alias", alias)
        }
    }

    companion object {
        fun fromJSON(json: JSONObject): DevicePayload {
            return DevicePayload().apply {
                dmcUserId = json.getNonNullString("dmcUserId")
                udidHashed = json.getNonNullString("UDIDHashed")
                pushToken = json.getNonNullString("pushToken")
                pushTokenBk = json.getNonNullString("pushToken_bk")
                alias = json.getNonNullString("alias")
            }
        }
    }

    override fun toString(): String {
        return "DevicePayload(dmcUserId=$dmcUserId, udidHashed=$udidHashed, pushTokenBk=$pushTokenBk, pushToken=$pushToken, alias=$alias)"
    }


}