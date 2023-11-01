package eu.brrm.shared_ui

import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder

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
}