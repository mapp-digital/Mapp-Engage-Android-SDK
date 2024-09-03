package com.mapp.engagesample

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.appoxee.Appoxee
import com.appoxee.internal.model.response.DevicePayload
import com.appoxee.shared.AppoxeeObserver
import com.appoxee.shared.MappCallback
import com.appoxee.shared.MappResult
import com.google.android.material.button.MaterialButton
import eu.brrm.shared_ui.PermissionHelper
import eu.brrm.shared_ui.Util
import eu.brrm.shared_ui.Util.permissionsToString
import eu.brrm.shared_ui.databinding.FragmentBaseTestBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BaseTestFragment : Fragment(), AppoxeeObserver {

    private var _binding: FragmentBaseTestBinding? = null

    private val binding: FragmentBaseTestBinding
        get() = _binding!!

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
//                val result = Appoxee.instance().fetchInappMessages("app_open").asSuspend()
//                Util.showDialog(
//                    requireContext(),
//                    "Inapp Messages",
//                    if (result.isSuccess()) result.getData().toString()
//                    else result.getError().toString()
//                )
                Appoxee.instance().triggerInApp(requireActivity(), "app_open")
            }
        }

        binding.btnTestCallExecute.setOnClickListener {
            lifecycleScope.launch {
                val result = Appoxee.instance().testCall().asSuspend()
                Util.showDialog(
                    requireContext(),
                    "Test Call (execute)",
                    if (result.isSuccess()) result.getData().toString()
                    else result.getError().toString()
                )
            }
        }

        binding.btnTestCallEnqueue.setOnClickListener {
            Appoxee.instance().testCall().enqueue(object : MappCallback<String> {
                override fun onResult(result: MappResult<String>) {
                    Util.showDialog(
                        requireContext(),
                        "Test Call (enqueue)",
                        if (result.isSuccess()) result.getData().toString()
                        else result.getError().toString()
                    )
                }
            })
        }
    }

    override fun onResume() {
        super.onResume()
        Appoxee.instance().subscribe(this)
        binding.llInnerContainer.children.forEach {
            (it as? MaterialButton)?.let { btn ->
                btn.isEnabled = btn.hasOnClickListeners()
            }
        }
    }

    override fun onReadyStatusChanged(status: Boolean, mappResult: MappResult<DevicePayload>) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            binding.switchReady.apply {
                isEnabled = false
                isChecked = status
            }

            isPushEnabled()

            mappResult.getData()?.let {
                val device = "UDIDHashed\n${it.udidHashed}"
                binding.tvDevice.text = device
            }


        }
    }

    private fun pushEnable(enabled: Boolean) {
        lifecycleScope.launch {
            val call = Appoxee.instance().enablePush(enabled, null)
            val result = call.asSuspend()
            val actionStatus = if (result.isSuccess()) "SUCCESSFUL" else "UNSUCCESSFUL"
            Util.showDialog(
                requireContext(),
                "Push status",
                "Action $actionStatus\nStatus:${enabled}"
            )
        }
    }

    private fun isPushEnabled() {
        lifecycleScope.launch {
            val call = Appoxee.instance().isPushEnabled()
            val result = call.asSuspend()
            if (result.isSuccess()) {
                val enabled = result.getData() ?: false
                binding.switchPushEnabled.also {
                    it.setOnCheckedChangeListener(null)
                    it.isChecked = enabled
                    it.setOnCheckedChangeListener { _, isChecked ->
                        pushEnable(isChecked)
                    }
                }
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

                    // show dialog with token value
                    Util.showDialog(requireContext(), "Firebase token", it)
                }
            }
        }
    }

    override fun onPause() {
        Appoxee.instance().unsubscribe(this)
        super.onPause()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}