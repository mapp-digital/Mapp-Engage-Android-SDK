package eu.brrm.shared_ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.SimpleAdapter
import android.widget.Toast
import androidx.annotation.ColorRes
import com.appoxee.internal.model.response.DevicePayload
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

object Util {
    private val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)

    @JvmStatic
    fun showDialog(context: Context, title: String, message: String? = "") {
        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setMessage(message ?: "")
            .setPositiveButton("Ok") { dialog, which ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    @JvmStatic
    fun showDeviceInfoDialog(
        context: Context,
        devicePayload: DevicePayload?,
        clipboard: ClipboardManager?
    ) {
        if (devicePayload == null) {
            showDialog(context, "Error", "Device Payload is null")
            return
        }

        val alias = mapOf("title" to "Alias", "subtitle" to devicePayload.alias)
        val pushToken = mapOf("title" to "Push Token", "subtitle" to devicePayload.pushToken)
        val pushTokenBk = mapOf("title" to "Push Token BK", "subtitle" to devicePayload.pushTokenBk)
        val dmcUserId = mapOf("title" to "Dmc User ID", "subtitle" to devicePayload.dmcUserId)
        val udidHashed = mapOf("title" to "UDID Hashed", "subtitle" to devicePayload.udidHashed)

        val data = listOf(alias, pushToken, pushTokenBk, dmcUserId, udidHashed)
        val adapter =
            SimpleAdapter(
                context,
                data,
                R.layout.row_item_subitem,
                arrayOf("title", "subtitle"),
                arrayOf(R.id.tvTitle, R.id.tvSubtitle).toIntArray(),
            )
        MaterialAlertDialogBuilder(context)
            .setTitle("Device")
            .setAdapter(adapter) { _, position ->
                val selectedValue = data[position].values.toList()[1]
                // copy token to clipboard

                val clip = ClipData.newPlainText("selectedValue", selectedValue)
                clip?.let { clipboard?.setPrimaryClip(clip) }
                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
            }
            .setPositiveButton("Ok") { dialog, position ->
                dialog.dismiss()
            }.show()
    }

    @JvmStatic
    fun permissionsToString(map: Map<String, Boolean>): String {
        val sb = StringBuilder()
        map.entries.asSequence()
            .filter { it.value }
            .map { it.key.substring(it.key.lastIndexOf(".") + 1) }
            .forEach { sb.append(it).append("\n") }
        return sb.toString()
    }

    @JvmStatic
    fun String?.camelCaseToWords(): String {
        if (this.isNullOrEmpty()) return ""
        return this.split(Pattern.compile("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])"), 0)
            .joinToString(separator = " ")
    }

    @JvmStatic
    @ColorRes
    fun Boolean.toColor(): Int {
        return if (this) return R.color.green else R.color.red
    }

    @JvmStatic
    fun Date?.toUtcString(): String? {
        if (this == null) return null
        return try {
            sdf.format(this)
        } catch (e: Exception) {
            null
        }
    }
}