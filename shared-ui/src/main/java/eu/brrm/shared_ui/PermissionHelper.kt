package eu.brrm.shared_ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import kotlin.random.Random

class PermissionHelper(private val registry: ActivityResultRegistry) {
    private val requestCode = Random(1000).nextInt(1, 10000)

    fun requestPermissions(
        context: Context,
        permissions: List<String>,
        callback: ActivityResultCallback<MutableMap<String, Boolean>>
    ) {
        val grantedPermissions = mutableMapOf<String, Boolean>()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            permissions.forEach { grantedPermissions[it] = true }
            callback.onActivityResult(grantedPermissions)
            return
        }

        permissions.forEach {
            val result = context.checkSelfPermission(it)
            if (result == PackageManager.PERMISSION_GRANTED) {
                grantedPermissions[it] = true
            }
        }

        val notGrantedPermissions = permissions.filterNot { grantedPermissions.containsKey(it) }

        createLauncher { result ->
            val finalResult = mutableMapOf<String, Boolean>()
            if (result.isNotEmpty())
                finalResult.putAll(result)
            if (grantedPermissions.isNotEmpty())
                finalResult.putAll(grantedPermissions)
            callback.onActivityResult(finalResult)
        }.launch(notGrantedPermissions.toTypedArray())
    }

    fun handlePermissionNotGranted(
        activity: Activity,
        permission: String,
        onCallback: ActivityResultCallback<MutableMap<String, Boolean>>,
        handleNonGrantedPermissions: (() -> Unit)? = null
    ) {
        val shouldShowRationale =
            ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        if (shouldShowRationale) {
            // requestPermissions(activity, listOf(permission), onCallback) // it request permission immediately
        } else {
            // open settings
            handleNonGrantedPermissions?.invoke() ?: defaultHandleNonGrantedPermissions(
                activity,
                permission
            )
        }
    }

    private fun defaultHandleNonGrantedPermissions(activity: Activity, permission: String) {
        val shortPermissionName = permission.removePrefix("android.permission.")
        AlertDialog.Builder(activity)
            .setTitle("Permission(s) denied error")
            .setMessage(
                "In order to application functions properly required permission(s) must be granted:\n[${shortPermissionName}]\n\n" +
                        "Do you want to open application settings and grant all permissions?"
            )
            .setPositiveButton("Yes") { d, i ->
                openApplicationSettings(activity)
            }.setNegativeButton("No", null)
            .show()
    }

    fun openApplicationSettings(activity: Activity) {
        val uri = Uri.parse("package:" + activity.packageName)
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        activity.startActivity(intent)
    }

    private fun createLauncher(callback: ActivityResultCallback<Map<String, Boolean>>): ActivityResultLauncher<Array<String>> {
        return registry.register(
            "permissions-${requestCode}",
            ActivityResultContracts.RequestMultiplePermissions(),
            callback
        )
    }
}