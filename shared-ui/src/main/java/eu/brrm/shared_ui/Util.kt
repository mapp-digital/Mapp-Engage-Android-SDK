package eu.brrm.shared_ui

import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.regex.Pattern

object Util {
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
}