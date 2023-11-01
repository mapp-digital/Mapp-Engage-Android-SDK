package com.appoxee.internal.model.response

import com.appoxee.internal.util.getNullableString
import org.json.JSONObject

class DevicePayload(
    val dmcUserId: String? = null, /* Unique user id */
    val udidHashed: String? = null, /* UDIDHashed */
    val pushTokenBk: String? = null, /* OptOut Token */
    val pushToken: String? = null, /* Opt In Token*/
    val alias: String? = null, /* User Alias */
) {

    private lateinit var json: JSONObject
    fun toJSON(): JSONObject {
        if (!::json.isInitialized) {
            json = JSONObject().apply {
                put("dmcUserId", dmcUserId)
                put("UDIDHashed", udidHashed)
                put("pushToken", pushToken)
                put("pushToken_bk", pushTokenBk)
                put("alias", alias)
            }
        }
        return json
    }

    companion object {
        fun fromJSON(json: JSONObject): DevicePayload {
            return DevicePayload(
                dmcUserId = json.getNullableString("dmcUserId"),
                udidHashed = json.getNullableString("UDIDHashed"),
                pushToken = json.getNullableString("pushToken"),
                pushTokenBk = json.getNullableString("pushToken_bk"),
                alias = json.getNullableString("alias")
            )
        }
    }

    override fun toString(): String {
        return "DevicePayload(dmcUserId=$dmcUserId, udidHashed=$udidHashed, pushTokenBk=$pushTokenBk, pushToken=$pushToken, alias=$alias)"
    }


}