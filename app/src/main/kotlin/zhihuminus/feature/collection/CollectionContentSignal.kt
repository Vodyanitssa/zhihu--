package com.zhihuminus.feature.collection

sealed interface CollectionContentEvent {
    data object Refresh : CollectionContentEvent

    data object LoadMore : CollectionContentEvent
}

sealed interface CollectionContentEffect {
    data class ShowMessage(
        val message: String,
    ) : CollectionContentEffect
}
