package eu.brrm.shared_ui.attributes

import eu.brrm.shared_ui.Util.toUtcString
import java.util.Date

data class CustomAttribute(
    val name: String,
    val value: Any?,
    val type: AttributeDataType = AttributeDataType.STRING
) {
    fun asString(): String? {
        if (value == null) return null
        return when (value) {
            is Number -> value.toDouble().toString()
            is Boolean -> value.toString()
            is Date -> value.toUtcString()
            else -> value.toString()
        }
    }
}
