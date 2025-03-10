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
import eu.brrm.shared_ui.Util
import eu.brrm.shared_ui.Util.toColor
import eu.brrm.shared_ui.databinding.FragmentBaseTestBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BaseTestFragment : Fragment() {

    private val TAG = this::class.java.simpleName
    private var _binding: FragmentBaseTestBinding? = null

    private val binding: FragmentBaseTestBinding
        get() = _binding!!


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

        binding.switchReady.isEnabled = false
        binding.btnSetAlias.setOnClickListener {
            setAlias()
        }

        binding.btnGetAlias.setOnClickListener {
            getAlias()
        }

        binding.btnGetDevice.setOnClickListener {
            getDevice()
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

        binding.btnSetCustomAttributes.setOnClickListener {
            setCustomAttributes()
        }

        binding.btnGetCustomAttributes.setOnClickListener {
            getCustomAttributes()
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

    private fun pushEnable(enabled: Boolean) {
        lifecycleScope.launch {
            val call = Appoxee.instance().enablePush(enabled)
            val result = call.asSuspend()
            val actionStatus = if (result.isSuccess()) "SUCCESSFUL" else "UNSUCCESSFUL"
            binding.switchPushEnabled.text =
                if (result.getData() == true) "Opted In" else "Opted Out"
            binding.switchPushEnabled.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    enabled.toColor()
                )
            )
            Util.showDialog(
                requireContext(),
                "Push status",
                "Action $actionStatus\nStatus:${enabled}"
            )
        }
    }

    private fun isPushEnabled(devicePayload: DevicePayload?) {
        val enabled = !devicePayload?.pushToken.isNullOrEmpty()
        binding.switchPushEnabled.also {
            it.setOnCheckedChangeListener(null)
            it.isChecked = enabled
            it.text = if (enabled) "Opted In" else "Opted Out"
            it.setTextColor(ContextCompat.getColor(requireContext(), enabled.toColor()))
            it.setOnCheckedChangeListener { _, isChecked ->
                pushEnable(isChecked)
            }
        }
    }

    private fun getFirebaseToken() {
        lifecycleScope.launch {
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
                    Util.showDialog(requireContext(), "Firebase token", it)
                }
            }
        }
    }

    private fun setAlias() {
        lifecycleScope.launch {
            val etAlias = binding.editTextAlias
            val alias = etAlias.text?.toString() ?: ""
            val result = Appoxee.instance().setAlias(alias).asSuspend()
            if (result.isSuccess()) {
                etAlias.text?.clear()
            }
            Util.showDialog(
                requireContext(),
                "DmcUserId",
                if (result.isSuccess()) result.getData().toString()
                else result.getError().toString()
            )
        }
    }

    private fun getAlias() {
        lifecycleScope.launch {
            val result = Appoxee.instance().getAlias().asSuspend()
            Util.showDialog(
                requireContext(), "Alias", if (result.isSuccess()) result.getData().toString()
                else result.getError().toString()
            )
        }
    }

    private fun getDevice() {
        lifecycleScope.launch {
            val result = Appoxee.instance().getDevice().asSuspend()
            Util.showDialog(
                requireContext(), "Device", if (result.isSuccess()) result.getData().toString()
                else result.getError().toString()
            )
        }
    }

    private fun fetchInappMessages() {
        lifecycleScope.launch {
            val result =
                Appoxee.instance().triggerInApp(requireActivity(), "app_open").asSuspend()
            if (!result.isSuccess()) {
                Util.showDialog(
                    requireContext(),
                    "Error",
                    result.getError()?.message ?: "Unknown error"
                )
            }
        }
    }

    private fun setTags() {
        lifecycleScope.launch {
            val result =
                Appoxee.instance().addTags(listOf("female", "makeup", "fashion")).asSuspend()
            if (result.isSuccess()) {
                Util.showDialog(requireContext(), "Set tags", result.getData().toString())
            } else {
                Util.showDialog(
                    requireContext(),
                    "Error",
                    result.getError()?.message ?: "Unknown error"
                )
            }
        }
    }

    private fun removeTags() {
        lifecycleScope.launch {
            val result = Appoxee.instance().removeTags(listOf("female", "makeup")).asSuspend()
            if (result.isSuccess()) {
                Util.showDialog(requireContext(), "Remove tags", result.getData().toString())
            } else {
                Util.showDialog(
                    requireContext(),
                    "Error",
                    result.getError()?.message ?: "Unknown error"
                )
            }
        }
    }

    private fun setCustomAttributes() {
        lifecycleScope.launch {
            val result = Appoxee.instance()
                .addCustomAttributes(mapOf("currency" to "EUR", "phone" to "+381991234567"))
                .asSuspend()
            if (result.isSuccess()) {
                Util.showDialog(
                    requireContext(),
                    "Set Custom Attributes",
                    result.getData().toString()
                )
            } else {
                Util.showDialog(
                    requireContext(),
                    "Error",
                    result.getError()?.message ?: "Unknown error"
                )
            }
        }
    }

    private fun getCustomAttributes() {
        lifecycleScope.launch {
            val result =
                Appoxee.instance().getCustomAttributes(listOf("currency", "phone")).asSuspend()
            if (result.isSuccess()) {
                Util.showDialog(
                    requireContext(),
                    "Get Custom Attributes",
                    result.getData().toString()
                )
            } else {
                Util.showDialog(
                    requireContext(),
                    "Error",
                    result.getError()?.message ?: "Unknown error"
                )
            }
        }
    }

    private fun startGeofencing() {
        lifecycleScope.launch {
            val result = Appoxee.instance().startGeofencing<GeoStatus>(0).asSuspend()
            result.getData()?.let { geoStatus ->
                if (geoStatus is GeoStatus.GeoStartedOk) {
                    Util.showDialog(
                        requireContext(),
                        "Geofencing",
                        "Geofencing started successfully!"
                    )
                } else if (geoStatus is GeoStatus.GeoLocationPermissionsNotGranted) {
                    handleLocationPermissionsNotGranted()
                } else {
                    Util.showDialog(requireContext(), "Geofencing Error", geoStatus.status)
                }
            }
        }
    }

    private fun stopGeofencing() {
        lifecycleScope.launch {
            val result = Appoxee.instance().stopGeofencing<GeoStatus>().asSuspend()
            result.getData()?.let { geoStatus ->
                if (geoStatus is GeoStatus.GeoStoppedOk) {
                    Util.showDialog(
                        requireContext(),
                        "Geofencing",
                        "Geofencing stopped successfully!"
                    )
                } else {
                    Util.showDialog(requireContext(), "Geofencing Error", geoStatus.status)
                }
            }
        }
    }

    private fun checkGeofencingStatus() {
        lifecycleScope.launch {
            val result = Appoxee.instance().isGeofencingActive().asSuspend()
            if (result.isSuccess()) {
                val message =
                    if (result.getData() == true) "Geofencing is active" else "Geofencing is inactive"
                Util.showDialog(requireContext(), "Geofencing status", message)
            } else {
                Util.showDialog(
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