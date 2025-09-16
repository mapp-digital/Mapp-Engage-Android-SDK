package com.mapp.engagesample

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultCallback
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.appoxee.Appoxee
import com.appoxee.internal.model.response.DevicePayload
import com.appoxee.shared.AppoxeeObserver
import com.appoxee.shared.MappResult
import eu.brrm.shared_ui.PermissionHelper
import eu.brrm.shared_ui.Util
import eu.brrm.shared_ui.Util.camelCaseToWords
import eu.brrm.shared_ui.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.name

    private lateinit var binding: ActivityMainBinding
    private lateinit var permissionHelper: PermissionHelper

    private val onBackPressedCallback =
        object : OnBackPressedCallback(supportFragmentManager.backStackEntryCount <= 1) {
            override fun handleOnBackPressed() {
                if (supportFragmentManager.backStackEntryCount > 1) {
                    supportFragmentManager.popBackStack()
                } else {
                    finish()
                }
            }
        }

    private val onBackStackChangedListener = FragmentManager.OnBackStackChangedListener {
        supportFragmentManager.backStackEntryCount.let {
            val title = supportFragmentManager.getBackStackEntryAt(it - 1).name
            supportActionBar?.let { actionBar ->
                actionBar.title = title.camelCaseToWords()
                actionBar.setDisplayHomeAsUpEnabled(it > 1)
            }
            onBackPressedCallback.isEnabled = it <= 1
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private val postNotificationResultCallback: ActivityResultCallback<MutableMap<String, Boolean>> =
        ActivityResultCallback { result ->
            if (java.lang.Boolean.TRUE == result[Manifest.permission.POST_NOTIFICATIONS]) {
//                Toast.makeText(
//                    this@MainActivity,
//                    "Permission(s) granted: \n" + Util.permissionsToString(result),
//                    Toast.LENGTH_SHORT
//                ).show()
                binding.topPanel.isVisible = false
            } else {
                binding.topPanel.isVisible = true
                binding.btnOpenSettings.setOnClickListener {
                    permissionHelper.openApplicationSettings(this@MainActivity)
                }
            }
        }

    private val appoxeeObserver = object : AppoxeeObserver {
        override fun onReadyStatusChanged(status: Boolean, mappResult: MappResult<DevicePayload>) {
            if (!mappResult.isSuccess()) {
                val errMessage = mappResult.getError()?.message ?: "Unknown message"
                Util.showDialog(this@MainActivity, "Error", errMessage)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        permissionHelper = PermissionHelper(this@MainActivity.activityResultRegistry)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportFragmentManager.addOnBackStackChangedListener(onBackStackChangedListener)
        onBackPressedDispatcher.addCallback(this@MainActivity, onBackPressedCallback)
        Appoxee.instance().setPushBroadcast(MyPushBroadcast::class.java)
        Appoxee.instance().subscribe(appoxeeObserver)
        navigate(HomeFragment())
    }

    override fun onStart() {
        super.onStart()
        requestPostNotificationPermission()
    }

    fun <T : Fragment> navigate(fragment: T) {
        supportFragmentManager.beginTransaction().apply {
            addToBackStack(fragment.javaClass.simpleName)
            replace(binding.fragmentContainerView.id, fragment)
        }.commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == android.R.id.home) {
            onBackPressedCallback.handleOnBackPressed()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    private fun requestPostNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissions: MutableList<String> = ArrayList()
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            permissionHelper.requestPermissions(
                this@MainActivity,
                permissions,
                postNotificationResultCallback
            )
        }
    }

    override fun onDestroy() {
        supportFragmentManager.removeOnBackStackChangedListener(onBackStackChangedListener)
        super.onDestroy()
    }
}