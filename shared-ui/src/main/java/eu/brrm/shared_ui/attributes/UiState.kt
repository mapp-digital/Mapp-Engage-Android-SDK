package eu.brrm.shared_ui.attributes

data class UiState(
    val isLoading: Boolean = false,
    val data: List<CustomAttribute> = emptyList(),
    val throwable: Throwable? = null,
    val message: String? = null,
)
