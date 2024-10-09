package com.mapp.engagesample

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.appoxee.Appoxee
import com.appoxee.internal.model.response.DevicePayload
import com.appoxee.shared.AppoxeeObserver
import com.appoxee.shared.MappResult
import com.google.android.material.button.MaterialButton
import eu.brrm.shared_ui.Util
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

                val devicePayload = mappResult.getData()
                isPushEnabled(devicePayload)

                devicePayload?.let {
                    val device = "UDIDHashed\n${it.udidHashed}"
                    binding.tvDevice.text = device
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
        binding.btnSetAlias.setOnClickListener {
            lifecycleScope.launch {
                val etAlias = binding.editTextAlias
                val alias = etAlias.text.toString()
                if (alias.isBlank()) {
                    Util.showDialog(requireContext(), "Set Alias", "Alias can't be empty!")
                    return@launch
                }
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

        binding.btnGetAlias.setOnClickListener {
            lifecycleScope.launch {
                val result = Appoxee.instance().getAlias().asSuspend()
                Util.showDialog(
                    requireContext(), "Alias", if (result.isSuccess()) result.getData().toString()
                    else result.getError().toString()
                )
            }
        }

        binding.btnGetDevice.setOnClickListener {
            lifecycleScope.launch {
                val result = Appoxee.instance().getDevice().asSuspend()
                Util.showDialog(
                    requireContext(), "Device", if (result.isSuccess()) result.getData().toString()
                    else result.getError().toString()
                )
            }
        }

        binding.btnGetFbToken.setOnClickListener {
            getFirebaseToken()
        }

        binding.btnFetchInboxMessages.setOnClickListener {
            lifecycleScope.launch {
                val result = Appoxee.instance().fetchInboxMessages("app_inbox").asSuspend()
                Util.showDialog(
                    requireContext(),
                    "Inbox Messages",
                    if (result.isSuccess()) result.getData().toString()
                    else result.getError().toString()
                )
            }
        }

        binding.btnFetchInappMessages.setOnClickListener {
            lifecycleScope.launch {
                Appoxee.instance().triggerInApp(requireActivity(), "app_open")
            }
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

    override fun onPause() {
        Appoxee.instance().unsubscribe(appoxeeObserver)
        super.onPause()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}