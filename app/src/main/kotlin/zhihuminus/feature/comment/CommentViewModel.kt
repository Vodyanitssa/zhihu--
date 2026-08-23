package com.zhihuminus.feature.comment

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuminus.feature.post.PostType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CommentViewModel(
    private val contentType: PostType,
    private val contentId: Long,
    private val repository: CommentRepository,
    private val initialCommentId: String? = null,
) : ViewModel() {
    companion object {
        private const val PAGE_SIZE = 20
    }

    var uiState: CommentUiState by mutableStateOf(CommentUiState())
        private set

    /** 当前打开的子评论列表是否已加载完所有数据 */
    val isChildEnd: Boolean
        get() {
            val parentId = uiState.activeParentId ?: return true
            return childNextUrls.containsKey(parentId) && childNextUrls[parentId] == null
        }

    // 内部可变列表，用于高效操作
    private val items = mutableListOf<CommentItemUiState>()
    private val childNextUrls = mutableMapOf<String, String?>() // parentId -> nextUrl

    private val _effect = Channel<CommentEffect>(capacity = Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var nextUrl: String? = null
    private var initialCommentResolved = false

    init {
        loadInitial()
    }

    fun onEvent(event: CommentEvent) {
        when (event) {
            is CommentEvent.LoadMore -> loadMore()
            is CommentEvent.Refresh -> refresh()
            is CommentEvent.ChangeSortOrder -> changeSortOrder(event.order)
            is CommentEvent.SubmitComment -> submitComment(event.text, event.replyToCommentId)
            is CommentEvent.LikeComment -> likeComment(event.commentId)
            is CommentEvent.UnlikeComment -> unlikeComment(event.commentId)
            is CommentEvent.DeleteComment -> deleteComment(event.commentId)
            is CommentEvent.OpenChildComments -> openChildComments(event.comment)
            is CommentEvent.DismissChildComments -> dismissChildComments()
            is CommentEvent.OpenImage -> sendEffect(CommentEffect.OpenImage(event.url))
            is CommentEvent.OpenLink -> sendEffect(CommentEffect.OpenExternalUrl(event.url))
            is CommentEvent.Reply -> uiState = uiState.copy(replyToComment = event.comment)
            is CommentEvent.DismissReply -> uiState = uiState.copy(replyToComment = null)
        }
    }

    private fun loadInitial() {
        uiState = uiState.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            try {
                if (!initialCommentResolved && initialCommentId != null) {
                    initialCommentResolved = true
                    resolveInitialComment(initialCommentId)
                }
                val page = withContext(Dispatchers.Default) {
                    repository.getRootComments(contentType, contentId, uiState.sortOrder, 0)
                }
                items.clear()
                items.addAll(page.comments.map { CommentItemUiState(comment = it) })
                nextUrl = page.nextUrl
                emitState()
            } catch (e: Exception) {
                uiState = uiState.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "加载评论失败",
                )
            }
        }
    }

    private fun loadMore() {
        val url = nextUrl
        if (uiState.isLoading || uiState.isLoadingMore || uiState.isEnd || url == null) return
        uiState = uiState.copy(isLoadingMore = true)
        viewModelScope.launch {
            try {
                val page = withContext(Dispatchers.Default) {
                    repository.getNextPage(url)
                }
                val existingIds = items.map { it.comment.id }.toSet()
                val newItems = page.comments
                    .filter { it.id !in existingIds }
                    .map { CommentItemUiState(comment = it) }
                items.addAll(newItems)
                nextUrl = page.nextUrl
                uiState = uiState.copy(isLoadingMore = false, isEnd = page.isEnd)
                emitState()
            } catch (e: Exception) {
                uiState = uiState.copy(isLoadingMore = false)
                sendEffect(CommentEffect.ShowMessage("加载更多失败: ${e.message}"))
            }
        }
    }

    private fun refresh() {
        nextUrl = null
        items.clear()
        uiState = uiState.copy(isLoading = true, errorMessage = null, isEnd = false)
        viewModelScope.launch {
            try {
                val page = withContext(Dispatchers.Default) {
                    repository.getRootComments(contentType, contentId, uiState.sortOrder, 0)
                }
                items.clear()
                items.addAll(page.comments.map { CommentItemUiState(comment = it) })
                nextUrl = page.nextUrl
                emitState()
            } catch (e: Exception) {
                uiState = uiState.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "刷新评论失败",
                )
            }
        }
    }

    private fun changeSortOrder(order: CommentSortOrder) {
        if (uiState.sortOrder == order) return
        uiState = uiState.copy(sortOrder = order, isLoading = true, errorMessage = null)
        nextUrl = null
        items.clear()
        viewModelScope.launch {
            try {
                val page = withContext(Dispatchers.Default) {
                    repository.getRootComments(contentType, contentId, order, 0)
                }
                items.clear()
                items.addAll(page.comments.map { CommentItemUiState(comment = it) })
                nextUrl = page.nextUrl
                emitState()
            } catch (e: Exception) {
                uiState = uiState.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "切换排序失败",
                )
            }
        }
    }

    private fun submitComment(text: String, replyToCommentId: String?) {
        if (text.isBlank()) return
        viewModelScope.launch {
            try {
                val newComment = withContext(Dispatchers.Default) {
                    repository.submitComment(contentType, contentId, text, replyToCommentId)
                }
                items.add(0, CommentItemUiState(comment = newComment))
                emitState()
                sendEffect(CommentEffect.ScrollToTop)
            } catch (e: Exception) {
                sendEffect(CommentEffect.ShowMessage("评论发送失败: ${e.message}"))
            }
        }
    }

    private fun likeComment(commentId: String) {
        updateCommentLike(commentId, liked = true)
        viewModelScope.launch {
            try {
                withContext(Dispatchers.Default) {
                    repository.likeComment(commentId)
                }
            } catch (e: Exception) {
                updateCommentLike(commentId, liked = false)
                sendEffect(CommentEffect.ShowMessage("点赞失败: ${e.message}"))
            }
        }
    }

    private fun unlikeComment(commentId: String) {
        updateCommentLike(commentId, liked = false)
        viewModelScope.launch {
            try {
                withContext(Dispatchers.Default) {
                    repository.unlikeComment(commentId)
                }
            } catch (e: Exception) {
                updateCommentLike(commentId, liked = true)
                sendEffect(CommentEffect.ShowMessage("取消点赞失败: ${e.message}"))
            }
        }
    }

    private fun updateCommentLike(commentId: String, liked: Boolean) {
        val updated = updateCommentInTree(items, commentId) { comment ->
            comment.copy(
                liked = liked,
                likeCount = if (liked) comment.likeCount + 1 else (comment.likeCount - 1).coerceAtLeast(0),
            )
        }
        if (updated) emitState()
    }

    private fun deleteComment(commentId: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.Default) {
                    repository.deleteComment(commentId)
                }
                removeFromTree(items, commentId)
                emitState()
            } catch (e: Exception) {
                sendEffect(CommentEffect.ShowMessage("删除评论失败: ${e.message}"))
            }
        }
    }

    private fun openChildComments(comment: Comment) {
        // 设置 activeParentId，打开子 sheet
        uiState = uiState.copy(activeParentId = comment.id)

        // 如果已经有 children 数据（之前加载过），不重复加载
        val existing = items.find { it.comment.id == comment.id }
        if (existing?.children != null) return

        // 标记为加载中（children 设为 emptyList 作为占位，实际加载中）
        updateItemChildren(comment.id, emptyList())
        childNextUrls[comment.id] = null

        viewModelScope.launch {
            try {
                val page = withContext(Dispatchers.Default) {
                    repository.getChildComments(comment.id, 0)
                }
                val childItems = page.comments.map { CommentItemUiState(comment = it) }
                updateItemChildren(comment.id, childItems)
                childNextUrls[comment.id] = page.nextUrl
            } catch (e: Exception) {
                sendEffect(CommentEffect.ShowMessage("加载子评论失败: ${e.message}"))
            }
        }
    }

    fun loadMoreChildComments() {
        val parentId = uiState.activeParentId ?: return
        val url = childNextUrls[parentId] ?: return

        val parentItem = items.find { it.comment.id == parentId } ?: return
        val children = parentItem.children ?: return
        // 检查是否已有加载更多（通过判断 children 是否包含重复项）
        if (children.isEmpty()) return // 还在初次加载中

        viewModelScope.launch {
            try {
                val page = withContext(Dispatchers.Default) {
                    repository.getNextPage(url)
                }
                val existingIds = children.map { it.comment.id }.toSet()
                val newItems = page.comments
                    .filter { it.id !in existingIds }
                    .map { CommentItemUiState(comment = it) }
                updateItemChildren(parentId, children + newItems)
                childNextUrls[parentId] = page.nextUrl
            } catch (e: Exception) {
                sendEffect(CommentEffect.ShowMessage("加载更多子评论失败: ${e.message}"))
            }
        }
    }

    private fun dismissChildComments() {
        uiState = uiState.copy(activeParentId = null)
    }

    // region Tree operations

    /**
     * 递归查找并更新树中的评论。
     * @return true 如果找到了并更新了
     */
    private fun updateCommentInTree(
        list: MutableList<CommentItemUiState>,
        commentId: String,
        transform: (Comment) -> Comment,
    ): Boolean {
        for (i in list.indices) {
            val item = list[i]
            if (item.comment.id == commentId) {
                list[i] = item.copy(comment = transform(item.comment))
                return true
            }
            if (item.children != null) {
                val childList = item.children.toMutableList()
                if (updateCommentInTree(childList, commentId, transform)) {
                    list[i] = item.copy(children = childList)
                    return true
                }
            }
        }
        return false
    }

    /**
     * 递归从树中移除评论。
     * @return true 如果找到了并移除了
     */
    private fun removeFromTree(
        list: MutableList<CommentItemUiState>,
        commentId: String,
    ): Boolean {
        val iterator = list.iterator()
        while (iterator.hasNext()) {
            val item = iterator.next()
            if (item.comment.id == commentId) {
                iterator.remove()
                return true
            }
            if (item.children != null) {
                val childList = item.children.toMutableList()
                if (removeFromTree(childList, commentId)) {
                    val index = list.indexOf(item)
                    if (index >= 0) {
                        list[index] = item.copy(children = childList)
                    }
                    return true
                }
            }
        }
        return false
    }

    /**
     * 更新指定父评论的 children 列表。
     */
    private fun updateItemChildren(parentId: String, children: List<CommentItemUiState>) {
        for (i in items.indices) {
            if (items[i].comment.id == parentId) {
                items[i] = items[i].copy(children = children)
                emitState()
                return
            }
        }
    }

    // endregion

    private fun emitState() {
        uiState = uiState.copy(
            items = items.toList(),
            isLoading = false,
        )
    }

    private suspend fun resolveInitialComment(commentId: String) {
        try {
            withContext(Dispatchers.Default) {
                repository.getComment(commentId)
            }
            // TODO: 更精确的锚点解析（找到根评论并置于顶部）
        } catch (_: Exception) {
        }
    }

    private fun sendEffect(effect: CommentEffect) {
        viewModelScope.launch {
            _effect.send(effect)
        }
    }
}
