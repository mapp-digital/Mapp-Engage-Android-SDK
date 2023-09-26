package com.appoxee.internal.model.request

import org.json.JSONObject

internal data class RegisterActions(val register: RegisterModel) : Actions<RegisterModel> {
    private val json = JSONObject()

    init {
        json.put("register", register.asJson())
    }

    override fun get(): RegisterModel {
        return register
    }

    override fun asJson(): JSONObject {
        return json
    }

    override fun asString(): String {
        return json.toString()
    }
}