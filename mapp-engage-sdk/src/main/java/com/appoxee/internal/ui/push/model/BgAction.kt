package com.appoxee.internal.ui.push.model

import android.os.Parcelable
import com.appoxee.internal.util.getNullableString
import kotlinx.parcelize.Parcelize
import org.json.JSONObject

@Parcelize
internal data class BgAction(
    val name: String? = null,
    val todo: String? = null,
    val type: String? = null,
    val value: String? = null
) : Parcelable {
    companion object {
        private const val NAME = "name"
        private const val TODO = "todo"
        private const val TYPE = "type"
        private const val VALUE = "value"
        fun fromJSON(json: JSONObject): BgAction {
            return BgAction(
                name = json.getNullableString(NAME),
                todo = json.getNullableString(TODO),
                type = json.getNullableString(TYPE),
                value = json.getNullableString(VALUE)
            )
        }
    }
}
