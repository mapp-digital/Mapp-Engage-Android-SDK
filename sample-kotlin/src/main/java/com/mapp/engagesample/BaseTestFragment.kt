package com.mapp.engagesample

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.appoxee.Appoxee
import com.appoxee.internal.model.response.DevicePayload
import com.appoxee.shared.AppoxeeObserver
import com.appoxee.shared.MappCallback
import com.appoxee.shared.MappResult
import eu.brrm.shared_ui.Util
import eu.brrm.shared_ui.databinding.FragmentBaseTestBinding
import kotlinx.coroutines.launch

class BaseTestFragment : Fragment(), AppoxeeObserver {

    private var _binding: FragmentBaseTestBinding? = null

    private val binding: FragmentBaseTestBinding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBaseTestBinding.inflate(layoutInflater)
        Appoxee.instance().subscribe(this)
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
                val result = Appoxee.instance().fetchInappMessages("app_open").asSuspend()
                Util.showDialog(
                    requireContext(),
                    "Inapp Messages",
                    if (result.isSuccess()) result.getData().toString()
                    else result.getError().toString()
                )
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

    override fun onReadyStatusChanged(status: Boolean, mappResult: MappResult<DevicePayload>) {
        binding.switchReady.apply {
            isEnabled = false
            isChecked = status
        }
    }

    override fun onDestroyView() {
        _binding = null
        Appoxee.instance().unsubscribe(this)
        super.onDestroyView()
    }
}