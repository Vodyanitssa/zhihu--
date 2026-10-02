package com.zhihuminus.feature.collection

sealed interface CollectionBrowseEvent {
    data object Refresh : CollectionBrowseEvent

    data object LoadMore : CollectionBrowseEvent

    data class SelectCollection(
        val collectionId: String,
    ) : CollectionBrowseEvent

    data class ToggleRandomMode(
        val enabled: Boolean,
    ) : CollectionBrowseEvent

    data class SetFolderMenuExpanded(
        val expanded: Boolean,
    ) : CollectionBrowseEvent

    data class RequestDelete(
        val collection: Collection,
    ) : CollectionBrowseEvent

    data object DismissDeleteDialog : CollectionBrowseEvent

    data object ConfirmDelete : CollectionBrowseEvent
}

sealed interface CollectionBrowseEffect {
    data class ShowMessage(
        val message: String,
    ) : CollectionBrowseEffect
}
