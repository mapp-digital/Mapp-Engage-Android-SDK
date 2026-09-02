package com.mapp.engagesample

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.appoxee.Appoxee
import com.appoxee.internal.model.response.DevicePayload
import com.appoxee.shared.AppoxeeObserver
import com.appoxee.shared.GeoStatus
import com.appoxee.shared.MappResult
import com.google.android.material.button.MaterialButton
import com.mapp.engagesample.inbox.InboxMessagesActivity
import eu.brrm.shared_ui.attributes.set.SetCustomAttributesActivity
import eu.brrm.shared_ui.Util
import eu.brrm.shared_ui.Util.showDialog
import eu.brrm.shared_ui.Util.toColor
import eu.brrm.shared_ui.attributes.get.GetCustomAttributesActivity
import eu.brrm.shared_ui.databinding.FragmentBaseTestBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BaseTestFragment : Fragment() {

    private val TAG = this::class.java.simpleName
    private var _binding: FragmentBaseTestBinding? = null

    private val binding: FragmentBaseTestBinding
        get() = _binding!!

    private var clipboard: ClipboardManager? = null

    private val onPushEnabledListener= CompoundButton.OnCheckedChangeListener { view, isChecked ->
        pushEnable(isChecked)
    }

    private val appoxeeObserver = object : AppoxeeObserver {
        override fun onReadyStatusChanged(status: Boolean, mappResult: MappResult<DevicePayload>) {
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                binding.switchReady.apply {
                    isEnabled = false
                    isChecked = status
                }
                if (mappResult.isSuccess()) {
                    val devicePayload = mappResult.getData()
                    isPushEnabled(devicePayload)

                    devicePayload?.let {
                        val device = "UDIDHashed\n${it.udidHashed}"
                        binding.tvDevice.text = device
                    }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBaseTestBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        clipboard = getSystemService(requireContext(), ClipboardManager::class.java)

        binding.switchReady.isEnabled = false

        binding.btnLogoutAndOptIn.setOnClickListener {
            logout(true)
        }

        binding.btnLogoutAndOptOut.setOnClickListener {
            logout(false)
        }

        binding.btnSetAlias.setOnClickListener {
            setAlias()
        }

        binding.btnGetAlias.setOnClickListener {
            getAlias()
        }

        binding.btnGetDevice.setOnClickListener {
            getDevice(clipboard)
        }

        binding.btnGetFbToken.setOnClickListener {
            getFirebaseToken()
        }

        binding.btnFetchInboxMessages.setOnClickListener {
            val intent = Intent(requireContext(), InboxMessagesActivity::class.java)
            startActivity(intent)
        }

        binding.btnFetchInappMessages.setOnClickListener {
            fetchInappMessages()
        }

        binding.btnSetTags.setOnClickListener {
            setTags()
        }

        binding.btnRemoveTags.setOnClickListener {
            removeTags()
        }

        binding.btnGetTags.setOnClickListener {
            getTags()
        }

        binding.btnSetCustomAttributes.setOnClickListener {
            openCustomAttributesSetup()
        }

        binding.btnGetCustomAttributes.setOnClickListener {
            openGetCustomAttributes()
        }

        binding.btnStartGeofencing.setOnClickListener {
            startGeofencing()
        }

        binding.btnStopGeofencing.setOnClickListener {
            stopGeofencing()
        }

        binding.btnGeofencingStatus.setOnClickListener {
            checkGeofencingStatus()
        }
    }

    override fun onResume() {
        super.onResume()
        Appoxee.instance().subscribe(appoxeeObserver)
        binding.llInnerContainer.children.forEach {
            (it as? MaterialButton)?.let { btn ->
                btn.isEnabled = btn.hasOnClickListeners()
            }
        }
    }

    private fun updatePushEnabledStatus(pushEnabled: Boolean){
        binding.switchPushEnabled.setOnCheckedChangeListener(null)
        binding.switchPushEnabled.isChecked=pushEnabled
        binding.switchPushEnabled.text = if (pushEnabled) "Opted In" else "Opted Out"
        binding.switchPushEnabled.setTextColor(ContextCompat.getColor(requireContext(), pushEnabled.toColor()))
        binding.switchPushEnabled.setOnCheckedChangeListener(onPushEnabledListener)
    }


    private fun logout(pushEnabled: Boolean) {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = Appoxee.instance().logout(pushEnabled).asSuspend()
            if(result.isSuccess()){
                val msg=if(pushEnabled) "Opted in" else "Opted out"
                showDialog(requireContext(),"Logout", "Device successfully logged out and $msg")
                updatePushEnabledStatus(pushEnabled)
            }
        }
    }

    private fun pushEnable(enabled: Boolean) {
        viewLifecycleOwner.lifecycleScope.launch {
            val call = Appoxee.instance().enablePush(enabled)
            val result = call.asSuspend()
            updatePushEnabledStatus(enabled)
            val actionStatus = if (result.isSuccess()) "SUCCESSFUL" else "UNSUCCESSFUL"
            showDialog(
                requireContext(),
                "Push status",
                "Action $actionStatus\nStatus:${enabled}"
            )
        }
    }

    private fun isPushEnabled(devicePayload: DevicePayload?) {
        val enabled = !devicePayload?.pushToken.isNullOrEmpty()
        binding.switchPushEnabled.also {
            updatePushEnabledStatus(enabled)
        }
    }

    private fun getFirebaseToken() {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = Appoxee.instance().getFirebaseToken().asSuspend()
            if (result.isSuccess()) {
                result.getData()?.let {
                    // copy token to clipboard
                    val clipboard = getSystemService(
                        requireContext(),
                        ClipboardManager::class.java
                    ) as ClipboardManager
                    val clip = ClipData.newPlainText("token", it)
                    clipboard.setPrimaryClip(clip)

                    Log.d(TAG, "FIREBASE TOKEN: $it")
                    // show dialog with token value
                    showDialog(requireContext(), "Firebase token", it)
                }
            }
        }
    }

    private fun setAlias() {
        viewLifecycleOwner.lifecycleScope.launch {
            val etAlias = binding.editTextAlias
            val alias = etAlias.text?.toString() ?: ""
            if (alias.isBlank()) {
                showDialog(
                    requireContext(),
                    "Set Alias Error",
                    "Alias can't be empty. Please enter alias value!"
                )
                return@launch
            }

            val result = Appoxee.instance().setAlias(alias, true).asSuspend()
            if (result.isSuccess()) {
                etAlias.text?.clear()
            }
            showDialog(
                requireContext(),
                "DmcUserId",
                if (result.isSuccess()) result.getData().toString()
                else result.getError().toString()
            )
        }
    }

    private fun getAlias() {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = Appoxee.instance().getAlias().asSuspend()
            showDialog(
                requireContext(), "Alias", if (result.isSuccess()) result.getData().toString()
                else result.getError().toString()
            )
        }
    }

    private fun getDevice(clipboard: ClipboardManager?) {
        viewLifecycleOwner.lifecycleScope.launch {
            val button = binding.btnGetDevice
            button.isEnabled = false
            button.text = "Loading..."
            try {
                val result = Appoxee.instance().getDevice().asSuspend()
                Util.showDeviceInfoDialog(requireContext(), result.getData(), clipboard)
            } finally {
                button.text = "Get Device"
                button.isEnabled = true
            }
        }
    }

    private fun fetchInappMessages() {
        viewLifecycleOwner.lifecycleScope.launch {
            val result =
                Appoxee.instance().triggerInApp(requireActivity(), "app_open").asSuspend()
            if (!result.isSuccess()) {
                showDialog(
                    requireContext(),
                    "Error",
                    result.getError()?.message ?: "Unknown error"
                )
            }
        }
    }

    private fun setTags() {
        viewLifecycleOwner.lifecycleScope.launch {
            val result =
                Appoxee.instance().addTags(setOf("female", "makeup", "fashion")).asSuspend()
            if (result.isSuccess()) {
                showDialog(requireContext(), "Set tags", result.getData().toString())
            } else {
                showDialog(
                    requireContext(),
                    "Error",
                    result.getError()?.message ?: "Unknown error"
                )
            }
        }
    }

    private fun removeTags() {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = Appoxee.instance().removeTags(setOf("female", "makeup")).asSuspend()
            if (result.isSuccess()) {
                showDialog(requireContext(), "Remove tags", result.getData().toString())
            } else {
                showDialog(
                    requireContext(),
                    "Error",
                    result.getError()?.message ?: "Unknown error"
                )
            }
        }
    }

    private fun getTags() {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = Appoxee.instance().getTags().asSuspend()
            if (result.isSuccess()) {
                showDialog(requireContext(), "Tags", result.getData()?.joinToString(", "))
            }
        }
    }

    private fun openCustomAttributesSetup() {
        viewLifecycleOwner.lifecycleScope.launch {
            val intent = Intent(requireContext(), SetCustomAttributesActivity::class.java)
            startActivity(intent)
        }
    }

    private fun openGetCustomAttributes() {
        viewLifecycleOwner.lifecycleScope.launch {
            val intent = Intent(requireContext(), GetCustomAttributesActivity::class.java)
            startActivity(intent)
        }
    }

    private fun startGeofencing() {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = Appoxee.instance().startGeofencing<GeoStatus>(0).asSuspend()
            result.getData()?.let { geoStatus ->
                if (geoStatus is GeoStatus.GeoStartedOk) {
                    showDialog(
                        requireContext(),
                        "Geofencing",
                        "Geofencing started successfully!"
                    )
                } else if (geoStatus is GeoStatus.GeoLocationPermissionsNotGranted) {
                    handleLocationPermissionsNotGranted()
                } else {
                    showDialog(requireContext(), "Geofencing Error", geoStatus.status)
                }
            }
        }
    }

    private fun stopGeofencing() {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = Appoxee.instance().stopGeofencing<GeoStatus>().asSuspend()
            result.getData()?.let { geoStatus ->
                if (geoStatus is GeoStatus.GeoStoppedOk) {
                    showDialog(
                        requireContext(),
                        "Geofencing",
                        "Geofencing stopped successfully!"
                    )
                } else {
                    showDialog(requireContext(), "Geofencing Error", geoStatus.status)
                }
            }
        }
    }

    private fun checkGeofencingStatus() {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = Appoxee.instance().isGeofencingActive().asSuspend()
            if (result.isSuccess()) {
                val message =
                    if (result.getData() == true) "Geofencing is active" else "Geofencing is inactive"
                showDialog(requireContext(), "Geofencing status", message)
            } else {
                showDialog(
                    requireContext(),
                    "Geofencing status error",
                    result.getError()?.message
                )
            }
        }
    }

    private fun handleLocationPermissionsNotGranted() {
        AlertDialog.Builder(requireContext())
            .setTitle("Location permission needed")
            .setView(eu.brrm.shared_ui.R.layout.dialog_location_rationale)
            .setPositiveButton("Open settings") { d, i ->
                val uri = Uri.parse("package:" + requireContext().packageName)
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)
                d.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .create()
            .show()
    }

    override fun onPause() {
        Appoxee.instance().unsubscribe(appoxeeObserver)
        super.onPause()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
