package com.appoxee.internal.model.response.inapp

import com.appoxee.internal.util.getLongOrDefault
import org.json.JSONObject

data class Behaviour(
    val delaySeconds: Int,
    val displaySeconds: Int
) {
    companion object {
        fun fromJSON(json: JSONObject): Behaviour {
            return Behaviour(
                delaySeconds = json.getLongOrDefault("delay_seconds").toInt(),
                displaySeconds = json.getLongOrDefault("display_seconds").toInt()
            )
        }
    }
}
