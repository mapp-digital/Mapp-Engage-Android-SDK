package eu.brrm.shared_ui.attributes.get

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

class GetCustomAttributesViewModel : ViewModel() {

    private val coroutineContext = Dispatchers.Default + CoroutineExceptionHandler { c, t ->
        t.printStackTrace()
    }

    private val _attributeNamesFlow =
        MutableStateFlow(setOf("num1", "FirstName"))
    val attributeNamesFlow: StateFlow<Set<String>> = _attributeNamesFlow

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    fun add(name: String) {
        viewModelScope.launch(coroutineContext) {
            val items = _attributeNamesFlow.value.toMutableSet()
            items.add(name)
            _attributeNamesFlow.emit(items)
        }
    }

    fun getAttributes() {
        viewModelScope.launch(coroutineContext) {
            _uiState.emit(UiState(isLoading = true))
            val attributes = attributeNamesFlow.value
            val result = Appoxee.instance().getCustomAttributes(attributes).asSuspend()
            if (result.isSuccess()) {
                val customAttributes = result.getData()
                    ?.map { CustomAttribute(it.key, it.value) }
                    .orEmpty()
                    .toList()
                _uiState.emit(UiState(data = customAttributes))
            } else {
                _uiState.emit(UiState(throwable = result.getError()))
            }
        }
    }

    fun remove(name: String) {
        viewModelScope.launch(coroutineContext) {
            val items = _attributeNamesFlow.value.toMutableSet()
            items.remove(name)
            _attributeNamesFlow.emit(items)
        }
    }

    fun deleteAttributes() {
        viewModelScope.launch(coroutineContext) {
            _uiState.emit(UiState(isLoading = true))
            val items = _attributeNamesFlow.value
            val result = Appoxee.instance().removeCustomAttributes(items).asSuspend()
            if (result.isSuccess()) {
                _uiState.emit(
                    UiState(
                        isLoading = false,
                        data = emptyList(),
                        message = "Successfully deleted attributes: ${items.joinToString(", ")}"
                    )
                )
            } else {
                _uiState.emit(
                    UiState(
                        isLoading = false,
                        data = emptyList(),
                        throwable = result.getError()
                    )
                )
            }
        }
    }
}