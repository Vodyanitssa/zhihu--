package com.zhihuminus.feature.post

sealed interface PostUiState {
    data object Loading : PostUiState

    data class Success(
        val post: Post,
    ) : PostUiState

    data class Error(
        val error: Throwable,
    ) : PostUiState
}
