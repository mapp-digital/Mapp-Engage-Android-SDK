package com.mapp.engagesample.configuration

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.appoxee.shared.AppoxeeOptions
import com.appoxee.shared.NotificationMode
import com.mapp.engagesample.R
import com.mapp.engagesample.SampleApplication
import com.mapp.engagesample.databinding.FragmentSdkConfigurationBinding
import kotlinx.coroutines.launch

class SdkConfigurationFragment : Fragment() {
    private var _binding: FragmentSdkConfigurationBinding? = null
    private val manager: SdkConfigurationManager
        get() = (requireActivity().application as SampleApplication).sdkConfiguration

    private val servers = AppoxeeOptions.Server.entries.filterNot { it == AppoxeeOptions.Server.STAGING }
    private val modes = listOf(
        NotificationMode.BACKGROUND_AND_FOREGROUND,
        NotificationMode.BACKGROUND_ONLY,
        NotificationMode.SILENT_ONLY,
    )
    private val levels = AppoxeeOptions.LogLevel.entries.toList()
    private var selectedServer = AppoxeeOptions.Server.L3
    private var selectedMode = NotificationMode.BACKGROUND_AND_FOREGROUND
    private var selectedLevel = AppoxeeOptions.LogLevel.DEBUG

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = FragmentSdkConfigurationBinding.inflate(inflater, container, false)
        _binding = binding
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = requireNotNull(_binding)
        binding.serverDropdown.setAdapter(ArrayAdapter(
            requireContext(), android.R.layout.simple_dropdown_item_1line,
            servers.map { it.name.replace('_', ' ') },
        ))
        binding.modeDropdown.setAdapter(ArrayAdapter(
            requireContext(), android.R.layout.simple_dropdown_item_1line,
            modes.map { modeLabel(it) },
        ))
        binding.logLevelDropdown.setAdapter(ArrayAdapter(
            requireContext(), android.R.layout.simple_dropdown_item_1line,
            levels.map { levelLabel(it) },
        ))
        binding.serverDropdown.setOnItemClickListener { _, _, position, _ ->
            selectedServer = servers[position]
            binding.serverLayout.helperText = selectedServer.value
        }
        binding.modeDropdown.setOnItemClickListener { _, _, position, _ -> selectedMode = modes[position] }
        binding.logLevelDropdown.setOnItemClickListener { _, _, position, _ -> selectedLevel = levels[position] }
        showConfiguration(restoreDraft(savedInstanceState, manager.state.value.configuration))

        binding.sdkKeyInput.doAfterTextChanged { binding.sdkKeyLayout.error = null }
        binding.appIdInput.doAfterTextChanged { binding.appIdLayout.error = null }
        binding.tenantIdInput.doAfterTextChanged { binding.tenantIdLayout.error = null }
        binding.applyButton.setOnClickListener { applyForm() }
        binding.defaultsButton.setOnClickListener {
            showConfiguration(manager.defaults)
            applyForm()
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                manager.state.collect { render(it) }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        (activity as? AppCompatActivity)?.supportActionBar?.apply {
            title = getString(R.string.sdk_configuration_title)
            setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onStop() {
        (activity as? AppCompatActivity)?.supportActionBar?.apply {
            title = getString(R.string.app_name)
            setDisplayHomeAsUpEnabled(false)
        }
        super.onStop()
    }

    private fun showConfiguration(configuration: SdkConfiguration) {
        val binding = _binding ?: return
        selectedServer = configuration.server.takeUnless { it == AppoxeeOptions.Server.STAGING }
            ?: AppoxeeOptions.Server.TEST
        selectedMode = configuration.notificationMode
        selectedLevel = configuration.logLevel
        binding.serverDropdown.setText(selectedServer.name.replace('_', ' '), false)
        binding.serverLayout.helperText = selectedServer.value
        binding.sdkKeyInput.setText(configuration.sdkKey)
        binding.appIdInput.setText(configuration.appId)
        binding.tenantIdInput.setText(configuration.tenantId)
        binding.modeDropdown.setText(modeLabel(selectedMode), false)
        binding.logLevelDropdown.setText(levelLabel(selectedLevel), false)
        binding.sdkKeyLayout.error = null
        binding.appIdLayout.error = null
        binding.tenantIdLayout.error = null
    }

    private fun readForm(): SdkConfiguration {
        val binding = requireNotNull(_binding)
        return SdkConfiguration(
            selectedServer,
            binding.sdkKeyInput.text?.toString().orEmpty().trim(),
            binding.appIdInput.text?.toString().orEmpty().trim(),
            binding.tenantIdInput.text?.toString().orEmpty().trim(),
            selectedMode,
            selectedLevel,
        )
    }

    private fun applyForm() {
        val binding = _binding ?: return
        val configuration = readForm()
        fun missing(value: String) = value.isBlank() || value.equals("null", ignoreCase = true)
        binding.sdkKeyLayout.error = if (missing(configuration.sdkKey)) getString(R.string.sdk_required) else null
        binding.appIdLayout.error = if (missing(configuration.appId)) getString(R.string.sdk_required) else null
        binding.tenantIdLayout.error = if (missing(configuration.tenantId)) getString(R.string.sdk_required) else null
        if (!configuration.hasRequiredValues()) {
            when {
                binding.sdkKeyLayout.error != null -> binding.sdkKeyInput.requestFocus()
                binding.appIdLayout.error != null -> binding.appIdInput.requestFocus()
                else -> binding.tenantIdInput.requestFocus()
            }
            return
        }
        ViewCompat.getWindowInsetsController(binding.root)?.hide(WindowInsetsCompat.Type.ime())
        manager.apply(configuration)
    }

    private fun render(state: ConfigurationState) {
        val binding = _binding ?: return
        binding.progress.isVisible = state.status == ConfigurationStatus.APPLYING
        binding.statusConfiguration.text = getString(
            R.string.sdk_status_configuration, state.configuration.server.name,
            state.configuration.appId, state.configuration.tenantId,
        )
        binding.statusTitle.setText(when (state.status) {
            ConfigurationStatus.APPLYING -> R.string.sdk_status_applying
            ConfigurationStatus.READY -> if (state.error == null) R.string.sdk_status_ready else R.string.sdk_status_unsaved
            ConfigurationStatus.FAILED -> R.string.sdk_status_failed
        })
        binding.statusMessage.text = when (state.status) {
            ConfigurationStatus.APPLYING -> getString(R.string.sdk_applying_description, state.configuration.server.name)
            ConfigurationStatus.READY -> when (state.source) {
                ConfigurationSource.BUILD_DEFAULTS -> getString(R.string.sdk_ready_defaults)
                ConfigurationSource.SAVED -> getString(R.string.sdk_ready_saved)
                ConfigurationSource.UNSAVED -> getString(R.string.sdk_ready_unsaved, state.error.orEmpty())
            }
            ConfigurationStatus.FAILED -> getString(
                if (state.source == ConfigurationSource.UNSAVED) R.string.sdk_failed_description else R.string.sdk_failed_startup,
                state.error ?: getString(R.string.sdk_unknown_error),
            )
        }
    }

    private fun modeLabel(mode: NotificationMode): String = getString(when (mode) {
        NotificationMode.BACKGROUND_ONLY -> R.string.sdk_mode_background
        NotificationMode.BACKGROUND_AND_FOREGROUND -> R.string.sdk_mode_both
        NotificationMode.SILENT_ONLY -> R.string.sdk_mode_silent
    })

    private fun levelLabel(level: AppoxeeOptions.LogLevel): String = getString(when (level) {
        AppoxeeOptions.LogLevel.DEBUG -> R.string.sdk_logging_debug
        AppoxeeOptions.LogLevel.RELEASE -> R.string.sdk_logging_release
    })

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (_binding == null) return
        val configuration = readForm()
        outState.putString("server", configuration.server.name)
        outState.putString("sdk_key", configuration.sdkKey)
        outState.putString("app_id", configuration.appId)
        outState.putString("tenant_id", configuration.tenantId)
        outState.putString("mode", configuration.notificationMode.name)
        outState.putString("logging", configuration.logLevel.name)
    }

    private fun restoreDraft(saved: Bundle?, fallback: SdkConfiguration): SdkConfiguration {
        if (saved?.containsKey("sdk_key") != true) return fallback
        return try {
            SdkConfiguration(
                AppoxeeOptions.Server.valueOf(saved.getString("server").orEmpty()),
                saved.getString("sdk_key").orEmpty(),
                saved.getString("app_id").orEmpty(),
                saved.getString("tenant_id").orEmpty(),
                NotificationMode.valueOf(saved.getString("mode").orEmpty()),
                AppoxeeOptions.LogLevel.valueOf(saved.getString("logging").orEmpty()),
            )
        } catch (_: IllegalArgumentException) {
            fallback
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
