package eu.brrm.shared_ui.attributes.set

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appoxee.Appoxee
import eu.brrm.shared_ui.attributes.CustomAttribute
import eu.brrm.shared_ui.attributes.UiState
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CustomAttributesViewModel : ViewModel() {
    private val coroutineContext = Dispatchers.Default + CoroutineExceptionHandler { c, t ->
        t.printStackTrace()
    }
    private val _attributesFlow: MutableStateFlow<UiState> =
        MutableStateFlow(UiState())
    val attributesFlow: StateFlow<UiState> = _attributesFlow

    fun addAttribute(customAttribute: CustomAttribute) {
        viewModelScope.launch(coroutineContext) {
            val attributes = _attributesFlow.value.data.toMutableList()
            attributes.add(customAttribute)
            _attributesFlow.emit(UiState(data = attributes))
        }
    }

    fun removeAttribute(customAttribute: CustomAttribute) {
        viewModelScope.launch(coroutineContext) {
            val attributes = _attributesFlow.value.data.toMutableList()
            attributes.remove(customAttribute)
            _attributesFlow.emit(UiState(data = attributes))
        }
    }

    fun updateAttributes() {
        viewModelScope.launch(coroutineContext) {
            val data = _attributesFlow.value.data.toList()
            val attributes = data.associate {
                it.name to it.value
            }
            _attributesFlow.emit(UiState(isLoading = true, data = data))

            val result = Appoxee.instance().addCustomAttributes(attributes).asSuspend()
            if (result.isSuccess()) {
                _attributesFlow.emit(
                    UiState(
                        isLoading = false,
                        data = emptyList(),
                        message = "Custom attributes are synced successfully!"
                    )
                )
            } else {
                _attributesFlow.emit(UiState(data = data, throwable = result.getError()))
            }
        }
    }
}