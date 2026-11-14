package com.zhihuminus.feature.collection

/**
 * 待移出收藏夹的条目信息。
 *
 * 内容类型与 ID 在这里固化为接口需要的 `answer` / `article` / `pin` 字符串，
 * 避免 ViewModel 依赖导航模型。
 */
data class PendingCollectionItemRemoval(
    val type: String,
    val id: Long,
    val stableKey: String,
    val title: String,
)

sealed interface CollectionContentEvent {
    data object Refresh : CollectionContentEvent

    data object LoadMore : CollectionContentEvent

    data object RetryDetail : CollectionContentEvent

    data class RequestRemoveItem(
        val target: PendingCollectionItemRemoval,
    ) : CollectionContentEvent

    data object DismissRemoveItemDialog : CollectionContentEvent

    data object ConfirmRemoveItem : CollectionContentEvent

    data object RequestDelete : CollectionContentEvent

    data object DismissDeleteDialog : CollectionContentEvent

    data object ConfirmDelete : CollectionContentEvent
}

sealed interface CollectionContentEffect {
    data class ShowMessage(
        val message: String,
        val isError: Boolean = false,
    ) : CollectionContentEffect

    data object NavigateBack : CollectionContentEffect
}
