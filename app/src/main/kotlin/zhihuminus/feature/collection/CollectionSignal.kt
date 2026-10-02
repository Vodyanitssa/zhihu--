package com.zhihuminus.feature.collection

sealed interface CollectionEvent {
    data object Refresh : CollectionEvent

    data object LoadMore : CollectionEvent

    data object OpenCreateDialog : CollectionEvent

    data object DismissCreateDialog : CollectionEvent

    data class CreateCollection(
        val title: String,
        val description: String,
        val isPublic: Boolean,
    ) : CollectionEvent

    data class RequestDelete(
        val collection: Collection,
    ) : CollectionEvent

    data object DismissDeleteDialog : CollectionEvent

    data object ConfirmDelete : CollectionEvent
}

sealed interface CollectionEffect {
    data class ShowMessage(
        val message: String,
    ) : CollectionEffect
}
