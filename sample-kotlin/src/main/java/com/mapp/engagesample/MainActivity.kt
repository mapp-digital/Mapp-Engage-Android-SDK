package com.mapp.engagesample

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import eu.brrm.shared_ui.PermissionHelper
import eu.brrm.shared_ui.Util
import eu.brrm.shared_ui.Util.camelCaseToWords
import eu.brrm.shared_ui.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.name

    private lateinit var binding: ActivityMainBinding

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportFragmentManager.addOnBackStackChangedListener(onBackStackChangedListener)
        onBackPressedDispatcher.addCallback(this@MainActivity, onBackPressedCallback)
        navigate(HomeFragment())
        requestPostNotificationPermission()
    }

    fun <T : Fragment> navigate(fragment: T) {
        supportFragmentManager.beginTransaction()
            .addToBackStack(fragment.javaClass.simpleName)
            .replace(binding.fragmentContainerView.id, fragment)
            .commit()
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
            val permissionHelper = PermissionHelper(this@MainActivity.activityResultRegistry)
            val permissions: MutableList<String> = ArrayList()
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            permissionHelper.requestPermissions(this@MainActivity, permissions) { result ->
                Toast.makeText(
                    this@MainActivity,
                    "Permission(s) granted: \n" + Util.permissionsToString(result),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onDestroy() {
        supportFragmentManager.removeOnBackStackChangedListener(onBackStackChangedListener)
        super.onDestroy()
    }
}