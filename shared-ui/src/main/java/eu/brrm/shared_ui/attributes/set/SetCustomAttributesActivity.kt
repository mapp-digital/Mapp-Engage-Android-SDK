package eu.brrm.shared_ui.attributes.set

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import eu.brrm.shared_ui.R
import eu.brrm.shared_ui.attributes.AttributeDataType
import eu.brrm.shared_ui.attributes.CustomAttribute
import eu.brrm.shared_ui.attributes.newattribute.CreateAttributeDialog
import eu.brrm.shared_ui.databinding.ActivitySetCustomAttributesBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SetCustomAttributesActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySetCustomAttributesBinding

    private lateinit var viewModel: CustomAttributesViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetCustomAttributesBinding.inflate(layoutInflater)
        viewModel = ViewModelProvider(this)[CustomAttributesViewModel::class]
        setContentView(binding.root)

        val adapter = AttributesAdapter {
            viewModel.removeAttribute(it)
        }

        binding.recycler.layoutManager = LinearLayoutManager(this)
        binding.recycler.adapter = adapter

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.attributesFlow.collectLatest { state ->
                    binding.includedWaitDialog.root.isVisible = state.isLoading

                    val data = state.data
                    binding.tvNoItems.isVisible = data.isEmpty()
                    binding.recycler.isActivated = data.isNotEmpty()

                    adapter.submitList(data)

                    state.message?.let {
                        show("Custom Attributes", it)
                    }

                    state.throwable?.let {
                        show("Custom Attributes", it.message)
                    }
                }
            }
        }

        binding.btnCreateAttribute.setOnClickListener {
            //showCreateAttributeDialog()
            CreateAttributeDialog().show(supportFragmentManager, CreateAttributeDialog.TAG)
        }

        binding.btnUpdateAttributes.setOnClickListener {
            viewModel.updateAttributes()
        }
    }

    private fun showCreateAttributeDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.layout_new_attribute, null)
        val etKey = view.findViewById<AppCompatEditText>(R.id.etKey)
        val etValue = view.findViewById<AppCompatEditText>(R.id.etValue)
        val rgTypes = view.findViewById<RadioGroup>(R.id.rgTypes)
        val rgBooleanValues = view.findViewById<RadioGroup>(R.id.rgBooleanValues)
        val tvDatePreview = view.findViewById<TextView>(R.id.tvDatePreview)

        AlertDialog.Builder(this)
            .setView(view)
            .setCancelable(false)
            .setPositiveButton("Create") { d, i ->
                val name = etKey.text?.toString()
                val value = etValue.text?.toString()
                val type = when (rgTypes.checkedRadioButtonId) {
                    R.id.rbNumber -> AttributeDataType.NUMBER
                    R.id.rbBoolean -> AttributeDataType.BOOLEAN
                    R.id.rbDate -> AttributeDataType.DATE
                    else -> AttributeDataType.STRING
                }
                if (!name.isNullOrEmpty() && !value.isNullOrEmpty()) {
                    val customAttribute =
                        CustomAttribute(name = name, value = value, type = type)
                    viewModel.addAttribute(customAttribute)
                    d.dismiss()
                } else {
                    show(
                        "Create custom attribute",
                        "Name and value of the attribute must be set!"
                    )
                }
            }
            .setNegativeButton("Cancel") { d, i -> d.dismiss() }
            .show()
    }

    private fun show(title: String?, message: String?) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Ok", null)
            .show()
    }
}