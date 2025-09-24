package eu.brrm.shared_ui.attributes.get

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import eu.brrm.shared_ui.Util
import eu.brrm.shared_ui.databinding.ActivityGetCustomAttributesBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class GetCustomAttributesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGetCustomAttributesBinding

    private lateinit var viewModel: GetCustomAttributesViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGetCustomAttributesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[GetCustomAttributesViewModel::class]

        val adapter = StringListAdapter { item ->
            viewModel.remove(item)
        }

        binding.recycler.adapter = adapter

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.uiState.collectLatest { state ->
                    binding.includedWaitDialog.root.isVisible = state.isLoading
                    if (state.data.isNotEmpty()) {
                        val sb = StringBuilder()
                        state.data.forEach {
                            sb.append(it.name).append(":").append(it.value).append("\n\n")
                        }
                        Util.showDialog(
                            this@GetCustomAttributesActivity,
                            "Custom Attributes",
                            sb.toString()
                        )
                    }
                    state.message?.let {
                        Util.showDialog(this@GetCustomAttributesActivity,"Info", it)
                    }

                    state.throwable?.let {
                        Util.showDialog(this@GetCustomAttributesActivity,"Error", it.message)
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.attributeNamesFlow.collectLatest {
                    adapter.submitList(it.toList())
                    binding.btnGet.isEnabled = it.isNotEmpty()
                    binding.btnDelete.isEnabled=it.isNotEmpty()
                    binding.tvNoItems.isVisible = it.isEmpty()
                    binding.recycler.isVisible = it.isNotEmpty()
                }
            }
        }

        binding.btnAdd.setOnClickListener {
            binding.etInputAttributeName.text?.toString()?.trim()?.let { text ->
                if (text.isNotEmpty()) {
                    viewModel.add(text)
                    binding.etInputAttributeName.text.clear()
                }
            }
        }

        binding.btnGet.setOnClickListener {
            viewModel.getAttributes()
        }

        binding.btnDelete.setOnClickListener {
            viewModel.deleteAttributes()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}