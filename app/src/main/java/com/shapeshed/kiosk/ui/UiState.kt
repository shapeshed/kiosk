package com.shapeshed.kiosk.ui

/** Simple screen state: loading, a terminal error, or loaded content. */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Error(val cause: Throwable? = null) : UiState<Nothing>
    data class Content<T>(val data: T) : UiState<T>
}
