package com.mapp.engagesample

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.mapp.engagesample.configuration.ConfigurationStatus
import com.mapp.engagesample.configuration.SdkConfigurationFragment
import com.mapp.engagesample.databinding.FragmentHomeBinding
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding: FragmentHomeBinding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnBaseTests.setOnClickListener {
            (activity as MainActivity?)?.navigate(BaseTestFragment())
        }

        binding.btnNotificationTests.setOnClickListener {
            (activity as MainActivity?)?.navigate(PushTestFragment())
        }
        binding.btnSdkConfiguration.setOnClickListener {
            (activity as MainActivity?)?.navigate(SdkConfigurationFragment())
        }
        val manager = (requireActivity().application as SampleApplication).sdkConfiguration
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                manager.state.collect { state ->
                    val binding = _binding ?: return@collect
                    val ready = state.status == ConfigurationStatus.READY
                    binding.btnBaseTests.isEnabled = ready
                    binding.btnNotificationTests.isEnabled = ready
                    binding.configurationSummary.text = when (state.status) {
                        ConfigurationStatus.READY -> getString(
                            R.string.sdk_home_ready, state.configuration.server.name,
                            state.configuration.appId, state.configuration.tenantId,
                        )
                        ConfigurationStatus.APPLYING -> getString(R.string.sdk_home_applying, state.configuration.server.name)
                        ConfigurationStatus.FAILED -> getString(R.string.sdk_home_failed)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}