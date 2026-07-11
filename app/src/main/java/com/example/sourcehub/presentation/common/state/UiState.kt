package com.example.sourcehub.presentation.common.state

data class UiState<T>(
    val data: T? = null,
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val isSuccess: Boolean get() = data != null && !isLoading && error == null
    val isEmpty: Boolean get() = data == null && !isLoading && error == null
}

sealed class UiEvent {
    data class Navigate(val route: String) : UiEvent()
    data class ShowSnackbar(val message: String) : UiEvent()
    data object NavigateBack : UiEvent()
}
