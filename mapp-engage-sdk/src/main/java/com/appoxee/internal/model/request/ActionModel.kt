package com.appoxee.internal.model.request

import com.appoxee.internal.network.NetworkData
import org.json.JSONObject

internal data class ActionModel(private val register: RegisterModel) : NetworkData {
    override fun asJson(): JSONObject {
        return JSONObject().put("register", register.asJson())
    }

    override fun asString(): String {
        return asJson().toString()
    }
}