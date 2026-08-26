package com.zhihuminus.feature.comment

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CommentViewModel(
    private val contentType: CommentContentType,
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
            is CommentEvent.OpenLink -> sendEffect(CommentEffect.OpenExternalUrl(event.url))
            is CommentEvent.Reply -> uiState = uiState.copy(replyToComment = event.comment)
            is CommentEvent.DismissReply -> uiState = uiState.copy(replyToComment = null)
            is CommentEvent.ConsumeAnchor -> uiState = uiState.copy(anchorRootId = null, anchorTargetId = null)
        }
    }

    private fun loadInitial() {
        uiState = uiState.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            try {
                // 锚点解析与首页数据并行拉取，避免串行阻塞首屏
                val anchorDeferred = async {
                    withContext(Dispatchers.Default) { resolveAnchor() }
                }
                val pageDeferred = async {
                    withContext(Dispatchers.Default) {
                        repository.getRootComments(contentType, contentId, uiState.sortOrder, 0)
                    }
                }
                val anchor = anchorDeferred.await()
                val page = pageDeferred.await()
                items.clear()
                items.addAll(page.comments.map { it.toUiState() })
                nextUrl = page.nextUrl

                var autoOpenRoot: Comment? = null
                if (anchor != null) {
                    val rootInList = items.any { it.comment.id == anchor.rootId }
                    // 根评论列表接口会忽略锚点参数，目标根评论可能不在第一页，需注入置顶
                    if (!rootInList && anchor.root != null) {
                        items.add(0, anchor.root.toUiState())
                    }
                    uiState = uiState.copy(
                        anchorRootId = anchor.rootId,
                        anchorTargetId = anchor.target.id.takeIf { it != anchor.rootId },
                    )
                    if (anchor.target.id != anchor.rootId && (rootInList || anchor.root != null)) {
                        autoOpenRoot =
                            anchor.root ?: items.first { it.comment.id == anchor.rootId }.comment
                    }
                }
                emitState()
                autoOpenRoot?.let { openChildComments(it) }
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
                    .map { it.toUiState() }
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
                items.addAll(page.comments.map { it.toUiState() })
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
                items.addAll(page.comments.map { it.toUiState() })
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
                items.add(0, newComment.toUiState())
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

        val existing = items.find { it.comment.id == comment.id }
        if (existing?.childrenComplete == true) return

        // 完整列表未加载过：无数据时先占位显示 loading；已有预览数据则保留展示、增量刷新
        if (existing?.children == null) {
            updateItemChildren(comment.id, emptyList(), complete = false)
        }
        childNextUrls[comment.id] = null

        viewModelScope.launch {
            try {
                val page = withContext(Dispatchers.Default) {
                    repository.getChildComments(comment.id, 0)
                }
                val childItems = page.comments.map { it.toUiState() }
                updateItemChildren(comment.id, childItems, complete = true)
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
                    .map { it.toUiState() }
                updateItemChildren(parentId, children + newItems, complete = true)
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
     * @param complete 本次写入的是否为子评论接口返回的完整列表
     */
    private fun updateItemChildren(
        parentId: String,
        children: List<CommentItemUiState>,
        complete: Boolean,
    ) {
        for (i in items.indices) {
            if (items[i].comment.id == parentId) {
                items[i] = items[i].copy(children = children, childrenComplete = complete)
                emitState()
                return
            }
        }
    }

    /**
     * 领域模型转 UI 状态：将接口内嵌的 childComments 提升为 children，
     * 并清空原字段，保证子评论只有 UI 树这一个数据源。
     */
    private fun Comment.toUiState(): CommentItemUiState = CommentItemUiState(
        comment = copy(childComments = emptyList()),
        children = childComments.map { it.toUiState() }.ifEmpty { null },
    )

    // endregion

    private fun emitState() {
        uiState = uiState.copy(
            items = items.toList(),
            isLoading = false,
        )
    }

    /**
     * 深链锚点解析：定位锚点评论及其所属根评论。
     * 任何失败（评论不存在、网络错误等）都返回 null，静默降级为普通加载。
     */
    private suspend fun resolveAnchor(): ResolvedAnchor? {
        val initialId = initialCommentId ?: return null
        return try {
            val target = repository.getComment(initialId)
            val rootId = target.replyRootCommentId
                ?.takeIf { it.isNotBlank() && it != target.id }
            if (rootId == null) {
                ResolvedAnchor(target = target, rootId = target.id, root = target)
            } else {
                val root = try {
                    repository.getComment(rootId)
                } catch (_: Exception) {
                    null
                }
                ResolvedAnchor(target = target, rootId = rootId, root = root)
            }
        } catch (_: Exception) {
            null
        }
    }

    private data class ResolvedAnchor(
        val target: Comment,
        val rootId: String,
        /** 解析出的根评论；拉取失败时为 null，此时仅当它出现在列表中才能定位 */
        val root: Comment?,
    )

    private fun sendEffect(effect: CommentEffect) {
        viewModelScope.launch {
            _effect.send(effect)
        }
    }
}
