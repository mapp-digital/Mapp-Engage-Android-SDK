package eu.brrm.shared_ui

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts
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

    private fun createLauncher(callback: ActivityResultCallback<Map<String, Boolean>>): ActivityResultLauncher<Array<String>> {
        return registry.register(
            "permissions-${requestCode}",
            ActivityResultContracts.RequestMultiplePermissions(),
            callback
        )
    }
}