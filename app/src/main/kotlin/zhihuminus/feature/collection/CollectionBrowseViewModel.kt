package com.zhihuminus.feature.collection

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuminus.data.FeedDisplayItem
import com.zhihuminus.util.Log
import com.zhihuminus.util.friendlyErrorMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

data class CollectionBrowseUiState(
    val urlToken: String = "",
    val collections: List<Collection> = emptyList(),
    val selectedCollectionId: String? = null,
    val selectedCollection: Collection? = null,
    val items: List<FeedDisplayItem> = emptyList(),
    val randomMode: Boolean = false,
    val randomSeed: Int = 0,
    val isLoadingCollections: Boolean = false,
    val isLoadingItems: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isEnd: Boolean = false,
    val folderMenuExpanded: Boolean = false,
    val collectionPendingDeletion: Collection? = null,
    val isDeleting: Boolean = false,
    val deleteError: String? = null,
    val errorMessage: String? = null,
)

class CollectionBrowseViewModel(
    private val urlToken: String,
    private val repository: CollectionRepository,
) : ViewModel() {
    var uiState by mutableStateOf(
        CollectionBrowseUiState(
            urlToken = urlToken,
            randomSeed = Random.nextInt(),
        ),
    )
        private set

    private val _effect = Channel<CollectionBrowseEffect>(capacity = Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var randomPageOffsets: List<Int>? = null
    private var randomPageCursor = 0
    private var lastRandomFirstOffset: Int? = null
    private var activeRandomSeed: Int? = null
    private var activeRandomItemCount: Int? = null
    private val retainedRandomOrderKeys = mutableListOf<String>()

    private var nextUrl: String? = null

    private var loadCollectionsJob: Job? = null
    private var loadItemsJob: Job? = null

    init {
        loadCollectionsAndItems(reset = true)
    }

    fun onEvent(event: CollectionBrowseEvent) {
        when (event) {
            is CollectionBrowseEvent.Refresh -> {
                if (uiState.randomMode) {
                    uiState = uiState.copy(randomSeed = Random.nextInt())
                }
                loadCollectionsAndItems(reset = true)
            }

            is CollectionBrowseEvent.LoadMore -> {
                loadMoreItems()
            }

            is CollectionBrowseEvent.SelectCollection -> {
                if (event.collectionId != uiState.selectedCollectionId) {
                    val target = uiState.collections.firstOrNull { it.id == event.collectionId }
                    uiState = uiState.copy(
                        selectedCollectionId = event.collectionId,
                        selectedCollection = target,
                        folderMenuExpanded = false,
                    )
                    loadItemsForSelectedCollection(reset = true)
                } else {
                    uiState = uiState.copy(folderMenuExpanded = false)
                }
            }

            is CollectionBrowseEvent.ToggleRandomMode -> {
                val newSeed = if (event.enabled) Random.nextInt() else uiState.randomSeed
                uiState = uiState.copy(
                    randomMode = event.enabled,
                    randomSeed = newSeed,
                )
                viewModelScope.launch {
                    _effect.send(
                        CollectionBrowseEffect.ShowMessage(
                            if (event.enabled) "已切换为随机模式" else "已切换为顺序模式",
                        ),
                    )
                }
                loadItemsForSelectedCollection(reset = true)
            }

            is CollectionBrowseEvent.SetFolderMenuExpanded -> {
                uiState = uiState.copy(folderMenuExpanded = event.expanded)
            }

            is CollectionBrowseEvent.RequestDelete -> {
                uiState = uiState.copy(
                    collectionPendingDeletion = event.collection,
                    deleteError = null,
                    folderMenuExpanded = false,
                )
            }

            is CollectionBrowseEvent.DismissDeleteDialog -> {
                if (!uiState.isDeleting) {
                    uiState = uiState.copy(collectionPendingDeletion = null, deleteError = null)
                }
            }

            is CollectionBrowseEvent.ConfirmDelete -> {
                deleteCollection()
            }
        }
    }

    private fun loadCollectionsAndItems(reset: Boolean) {
        loadCollectionsJob?.cancel()
        loadCollectionsJob = viewModelScope.launch {
            try {
                uiState = uiState.copy(
                    isRefreshing = true,
                    isLoadingCollections = uiState.collections.isEmpty(),
                    errorMessage = null,
                )

                val result = repository.getUserCollections(urlToken = urlToken)
                val collections = result.items
                val currentSelectedId = uiState.selectedCollectionId
                val newSelectedId = if (collections.isNotEmpty() && collections.none { it.id == currentSelectedId }) {
                    pickDefaultCollectionId(collections)
                } else if (collections.isEmpty()) {
                    null
                } else {
                    currentSelectedId
                }
                val selected = collections.firstOrNull { it.id == newSelectedId }

                uiState = uiState.copy(
                    collections = collections,
                    selectedCollectionId = newSelectedId,
                    selectedCollection = selected,
                    isLoadingCollections = false,
                    isRefreshing = false,
                )

                if (newSelectedId != null) {
                    loadItemsForSelectedCollection(reset = true)
                } else {
                    uiState = uiState.copy(items = emptyList(), isEnd = true)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("CollectionBrowseVM", "Failed to load collections", e)
                uiState = uiState.copy(
                    isLoadingCollections = false,
                    isRefreshing = false,
                    errorMessage = "加载失败: ${friendlyErrorMessage(e)}",
                )
            }
        }
    }

    private fun loadItemsForSelectedCollection(reset: Boolean) {
        val collectionId = uiState.selectedCollectionId ?: return
        val selectedCollection = uiState.selectedCollection

        loadItemsJob?.cancel()
        loadItemsJob = viewModelScope.launch {
            try {
                if (reset) {
                    uiState = uiState.copy(
                        isLoadingItems = uiState.items.isEmpty(),
                        errorMessage = null,
                    )
                    val freshCollection = try {
                        repository.getCollection(collectionId)
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        selectedCollection
                    }
                    val currentItemCount = freshCollection?.itemCount ?: selectedCollection?.itemCount ?: 0

                    if (uiState.randomMode) {
                        activeRandomSeed = uiState.randomSeed
                        activeRandomItemCount = currentItemCount
                        retainedRandomOrderKeys.clear()

                        val offsets = collectionRandomPageOffsets(
                            itemCount = currentItemCount,
                            randomSeed = uiState.randomSeed,
                            previousFirstOffset = lastRandomFirstOffset ?: 0,
                        )
                        randomPageOffsets = offsets
                        randomPageCursor = 0
                        lastRandomFirstOffset = offsets.firstOrNull()

                        val firstOffset = offsets.firstOrNull() ?: 0
                        val pageResult = repository.getCollectionItems(
                            collectionId = collectionId,
                            offset = firstOffset,
                            limit = COLLECTION_PAGE_SIZE,
                        )
                        randomPageCursor = 1
                        val ordered = orderCollectionItems(
                            items = pageResult.items,
                            randomMode = true,
                            randomSeed = uiState.randomSeed,
                            previousRandomOrderKeys = emptyList(),
                        )
                        retainedRandomOrderKeys.clear()
                        retainedRandomOrderKeys.addAll(ordered.map { it.stableKey })

                        uiState = uiState.copy(
                            selectedCollection = freshCollection,
                            items = ordered,
                            isLoadingItems = false,
                            isRefreshing = false,
                            isEnd = randomPageCursor >= offsets.size,
                            errorMessage = null,
                        )
                    } else {
                        nextUrl = null
                        val pageResult = repository.getCollectionItems(
                            collectionId = collectionId,
                            nextUrl = null,
                        )
                        nextUrl = pageResult.nextUrl
                        uiState = uiState.copy(
                            selectedCollection = freshCollection,
                            items = pageResult.items,
                            isLoadingItems = false,
                            isRefreshing = false,
                            isEnd = pageResult.isEnd || pageResult.nextUrl.isNullOrBlank(),
                            errorMessage = null,
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("CollectionBrowseVM", "Failed to load collection items for $collectionId", e)
                uiState = uiState.copy(
                    isLoadingItems = false,
                    isRefreshing = false,
                    errorMessage = "加载失败: ${friendlyErrorMessage(e)}",
                )
            }
        }
    }

    private fun loadMoreItems() {
        val collectionId = uiState.selectedCollectionId ?: return
        if (uiState.isLoadingMore || uiState.isEnd) return

        loadItemsJob?.cancel()
        loadItemsJob = viewModelScope.launch {
            try {
                uiState = uiState.copy(isLoadingMore = true)

                if (uiState.randomMode) {
                    val offsets = randomPageOffsets
                    if (offsets == null || randomPageCursor >= offsets.size) {
                        uiState = uiState.copy(isLoadingMore = false, isEnd = true)
                        return@launch
                    }

                    val offset = offsets[randomPageCursor]
                    val pageResult = repository.getCollectionItems(
                        collectionId = collectionId,
                        offset = offset,
                        limit = COLLECTION_PAGE_SIZE,
                    )
                    randomPageCursor++

                    val ordered = orderCollectionItems(
                        items = uiState.items + pageResult.items,
                        randomMode = true,
                        randomSeed = uiState.randomSeed,
                        previousRandomOrderKeys = retainedRandomOrderKeys,
                    )
                    retainedRandomOrderKeys.clear()
                    retainedRandomOrderKeys.addAll(ordered.map { it.stableKey })

                    uiState = uiState.copy(
                        items = ordered,
                        isLoadingMore = false,
                        isEnd = randomPageCursor >= offsets.size,
                    )
                } else {
                    val pageResult = repository.getCollectionItems(
                        collectionId = collectionId,
                        nextUrl = nextUrl,
                    )
                    nextUrl = pageResult.nextUrl
                    val existingKeys = uiState.items.map { it.stableKey }.toSet()
                    val newItems = uiState.items + pageResult.items.filter { it.stableKey !in existingKeys }

                    uiState = uiState.copy(
                        items = newItems,
                        isLoadingMore = false,
                        isEnd = pageResult.isEnd || pageResult.nextUrl.isNullOrBlank(),
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("CollectionBrowseVM", "Failed to load more items", e)
                uiState = uiState.copy(isLoadingMore = false)
                _effect.send(CollectionBrowseEffect.ShowMessage("加载更多失败: ${friendlyErrorMessage(e)}"))
            }
        }
    }

    private fun deleteCollection() {
        val collection = uiState.collectionPendingDeletion ?: return
        if (uiState.isDeleting) return
        if (collection.isDefault) {
            uiState = uiState.copy(deleteError = "默认收藏夹不能删除")
            return
        }

        viewModelScope.launch {
            uiState = uiState.copy(isDeleting = true, deleteError = null)
            try {
                repository.deleteCollection(collection.id)
                val remainingCollections = uiState.collections.filterNot { it.id == collection.id }
                val newSelectedId = if (uiState.selectedCollectionId == collection.id) {
                    pickDefaultCollectionId(remainingCollections)
                } else {
                    uiState.selectedCollectionId
                }
                val newSelected = remainingCollections.firstOrNull { it.id == newSelectedId }

                uiState = uiState.copy(
                    isDeleting = false,
                    collectionPendingDeletion = null,
                    deleteError = null,
                    collections = remainingCollections,
                    selectedCollectionId = newSelectedId,
                    selectedCollection = newSelected,
                )
                _effect.send(CollectionBrowseEffect.ShowMessage("收藏夹已删除"))

                if (newSelectedId != null) {
                    loadItemsForSelectedCollection(reset = true)
                } else {
                    uiState = uiState.copy(items = emptyList(), isEnd = true)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("CollectionBrowseVM", "Failed to delete collection", e)
                uiState = uiState.copy(
                    isDeleting = false,
                    deleteError = friendlyErrorMessage(e),
                )
            }
        }
    }
}

internal fun pickDefaultCollectionId(collections: List<Collection>): String? =
    collections.firstOrNull { it.isDefault }?.id ?: collections.firstOrNull()?.id

internal fun shouldRefreshCollectionDataOnActivation(
    isActive: Boolean,
    useTestCollections: Boolean,
    refreshOnNextActivation: Boolean = true,
): Boolean = isActive && !useTestCollections && refreshOnNextActivation

internal fun orderCollectionItems(
    items: List<FeedDisplayItem>,
    randomMode: Boolean,
    randomSeed: Int,
    previousRandomOrderKeys: List<String> = emptyList(),
): List<FeedDisplayItem> = if (randomMode) {
    val itemsByKey = items.associateBy { it.stableKey }
    val retainedKeys = previousRandomOrderKeys.distinct().filter(itemsByKey::containsKey)
    val retainedKeySet = retainedKeys.toSet()
    val newItems = items
        .distinctBy { it.stableKey }
        .filterNot { it.stableKey in retainedKeySet }
        .map { item ->
            val rank = item.stableKey.fold(COLLECTION_RANDOM_ORDER_OFFSET_BASIS xor randomSeed.toLong()) { hash, char ->
                (hash xor char.code.toLong()) * COLLECTION_RANDOM_ORDER_PRIME
            }
            item to rank
        }.sortedWith(
            compareBy<Pair<FeedDisplayItem, Long>>(
                { it.second },
                { it.first.stableKey },
            ),
        ).map { it.first }
    retainedKeys.mapNotNull(itemsByKey::get) + newItems
} else {
    items
}

internal fun collectionRandomPageOffsets(
    itemCount: Int,
    randomSeed: Int,
    previousFirstOffset: Int? = null,
    pageSize: Int = COLLECTION_PAGE_SIZE,
): List<Int> {
    require(pageSize > 0)
    val pageCount = ((itemCount.coerceAtLeast(1) + pageSize - 1) / pageSize)
    val offsets = (0 until pageCount)
        .map { page -> page * pageSize }
        .shuffled(Random(randomSeed))
        .toMutableList()
    if (offsets.size > 1 && offsets.first() == previousFirstOffset) {
        val replacementIndex = offsets.indexOfFirst { it != previousFirstOffset }
        val first = offsets.first()
        offsets[0] = offsets[replacementIndex]
        offsets[replacementIndex] = first
    }
    return offsets
}

internal fun shouldReuseCollectionRandomSession(
    activeRandomSeed: Int?,
    activeRandomItemCount: Int?,
    requestedRandomSeed: Int,
    requestedItemCount: Int,
    hasLoadedItems: Boolean,
    isLoading: Boolean,
    isEnd: Boolean,
): Boolean =
    activeRandomSeed == requestedRandomSeed &&
        activeRandomItemCount == requestedItemCount &&
        (hasLoadedItems || isLoading || isEnd)

private const val COLLECTION_PAGE_SIZE = 20
private const val COLLECTION_RANDOM_ORDER_OFFSET_BASIS = -3750763034362895579L
private const val COLLECTION_RANDOM_ORDER_PRIME = 1099511628211L
