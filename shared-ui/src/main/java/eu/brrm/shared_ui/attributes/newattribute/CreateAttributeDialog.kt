package eu.brrm.shared_ui.attributes.newattribute

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import eu.brrm.shared_ui.R
import eu.brrm.shared_ui.Util
import eu.brrm.shared_ui.Util.toUtcString
import eu.brrm.shared_ui.attributes.AttributeDataType
import eu.brrm.shared_ui.attributes.CustomAttribute
import eu.brrm.shared_ui.attributes.set.CustomAttributesViewModel
import eu.brrm.shared_ui.databinding.LayoutNewAttributeBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar

class CreateAttributeDialog : DialogFragment() {

    companion object {
        val TAG = CreateAttributeDialog::class.java.simpleName
    }

    private var _binding: LayoutNewAttributeBinding? = null
    private val binding: LayoutNewAttributeBinding
        get() = _binding!!

    private lateinit var viewModel: CreateAttributeViewModel
    private lateinit var customAttributeViewModel: CustomAttributesViewModel

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = LayoutNewAttributeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[CreateAttributeViewModel::class]
        customAttributeViewModel =
            ViewModelProvider(requireActivity())[CustomAttributesViewModel::class]
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.dataTypeFlow.collectLatest {
                    // reset attribute value on datatype change
                    viewModel.setCustomAttributeValue(null)

                    binding.rgBooleanValues.clearCheck()

                    // hide all datatype views
                    binding.rgBooleanValues.isVisible = false
                    binding.tvDatePreview.isVisible = false
                    binding.etValue.isVisible = false

                    // show view for selected data type
                    when (it) {
                        AttributeDataType.DATE -> {
                            binding.tvDatePreview.isVisible = true
                        }

                        AttributeDataType.BOOLEAN -> {
                            binding.rgBooleanValues.isVisible = true
                        }

                        else -> {
                            binding.etValue.isVisible = true
                            if (AttributeDataType.NUMBER == it) {
                                binding.etValue.inputType =
                                    EditorInfo.TYPE_CLASS_NUMBER or EditorInfo.TYPE_NUMBER_FLAG_DECIMAL
                            } else {
                                binding.etValue.inputType = EditorInfo.TYPE_CLASS_TEXT
                            }
                        }
                    }
                }
            }
        }

        binding.rgTypes.setOnCheckedChangeListener { radioGroup, checkedButton ->
            when (checkedButton) {
                R.id.rbDate -> viewModel.setDataType(AttributeDataType.DATE)
                R.id.rbNumber -> viewModel.setDataType(AttributeDataType.NUMBER)
                R.id.rbBoolean -> viewModel.setDataType(AttributeDataType.BOOLEAN)
                else -> viewModel.setDataType(AttributeDataType.STRING)
            }
        }

        binding.rgBooleanValues.setOnCheckedChangeListener { radioGroup, checkedButton ->
            when (checkedButton) {
                R.id.rbTrue -> viewModel.setCustomAttributeValue(true)
                else -> viewModel.setCustomAttributeValue(false)
            }
        }

        binding.tvDatePreview.setOnClickListener {
            val cal = Calendar.getInstance()
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH)
            val day = cal.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(
                requireContext(),
                { view, year, month, dayOfMonth ->
                    cal.set(year, month, dayOfMonth)
                    val selectedDate = cal.time
                    binding.tvDatePreview.text = selectedDate.toUtcString()
                    viewModel.setCustomAttributeValue(selectedDate)
                }, year, month, day
            )
                .show()
        }

        binding.etKey.doOnTextChanged { text, start, before, count ->
            viewModel.setCustomAttributeKey(text?.toString())
        }

        binding.etValue.doOnTextChanged { text, start, before, count ->
            val dataType = viewModel.dataTypeFlow.value
            if (dataType == AttributeDataType.NUMBER) {
                val value = text?.toString()?.toDoubleOrNull()
                viewModel.setCustomAttributeValue(value)
            } else {
                viewModel.setCustomAttributeValue(text?.toString())
            }
        }



        binding.btnCreate.setOnClickListener {
            val dataType = viewModel.dataTypeFlow.value
            val name = viewModel.customAttributeKeyFlow.value
            val value = viewModel.customAttributeValueFlow.value
            if (!name.isNullOrEmpty() && value != null) {
                val customAttribute = CustomAttribute(name, value, dataType)
                customAttributeViewModel.addAttribute(customAttribute)
                dismissAllowingStateLoss()
            } else {
                Util.showDialog(
                    requireContext(),
                    "Error",
                    "Name and Value must be set for attribute!"
                )
            }
        }

        binding.btnCancel.setOnClickListener {
            dismissAllowingStateLoss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}