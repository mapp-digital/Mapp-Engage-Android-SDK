package eu.brrm.shared_ui.attributes.newattribute

import androidx.lifecycle.ViewModel
import eu.brrm.shared_ui.attributes.AttributeDataType
import eu.brrm.shared_ui.attributes.CustomAttribute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class CreateAttributeViewModel : ViewModel() {
    private val _dataTypeSelectedFlow: MutableStateFlow<AttributeDataType> = MutableStateFlow(
        AttributeDataType.STRING
    )
    val dataTypeFlow: StateFlow<AttributeDataType> = _dataTypeSelectedFlow


    private val _customAttributeKeyFlow: MutableStateFlow<String?> = MutableStateFlow(null)
    val customAttributeKeyFlow: StateFlow<String?> = _customAttributeKeyFlow

    private val _customAttributeValueFlow: MutableStateFlow<Any?> = MutableStateFlow(null)
    val customAttributeValueFlow: StateFlow<Any?> = _customAttributeValueFlow

    fun setDataType(dataType: AttributeDataType) {
        _dataTypeSelectedFlow.value = dataType
    }

    fun setCustomAttributeKey(key: String?) {
        _customAttributeKeyFlow.value = key
    }

    fun setCustomAttributeValue(value: Any?) {
        _customAttributeValueFlow.value = value
    }
}