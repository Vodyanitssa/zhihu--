package com.zhihuminus.feature.comment

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuminus.feature.comment.components.CommentEvent
import com.zhihuminus.feature.post.PostType
import com.zhihuminus.util.Log
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
        private const val TAG = "CommentViewModel"
        private const val PAGE_SIZE = 20
    }

    var uiState: CommentUiState by mutableStateOf(CommentUiState.Loading)
        private set

    val comments = mutableStateListOf<Comment>()

    var sortOrder: CommentSortOrder by mutableStateOf(CommentSortOrder.SCORE)
        private set

    var isLoading: Boolean by mutableStateOf(true)
        private set

    var isEnd: Boolean by mutableStateOf(false)
        private set

    var errorMessage: String? by mutableStateOf(null)
        private set

    // 子评论状态
    var activeParentComment: Comment? by mutableStateOf(null)
        private set

    var childComments = mutableStateListOf<Comment>()

    var isChildLoading: Boolean by mutableStateOf(false)
        private set

    var isChildEnd: Boolean by mutableStateOf(false)
        private set

    private val _effect = Channel<CommentEffect>(capacity = Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var nextUrl: String? = null
    private var childNextUrl: String? = null
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
        }
    }

    private fun loadInitial() {
        isLoading = true
        uiState = CommentUiState.Loading
        viewModelScope.launch {
            try {
                // 如果有深链锚点，先解析
                if (!initialCommentResolved && initialCommentId != null) {
                    initialCommentResolved = true
                    resolveInitialComment(initialCommentId)
                }
                val page = withContext(Dispatchers.Default) {
                    repository.getRootComments(contentType, contentId, sortOrder, 0)
                }
                comments.clear()
                comments.addAll(page.comments)
                nextUrl = page.nextUrl
                isEnd = page.isEnd
                isLoading = false
                uiState = CommentUiState.Success(
                    comments = comments.toList(),
                    sortOrder = sortOrder,
                    isLoadingMore = false,
                    isEnd = isEnd,
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load comments", e)
                isLoading = false
                errorMessage = e.message
                uiState = CommentUiState.Error(e.message ?: "加载评论失败")
            }
        }
    }

    private fun loadMore() {
        val url = nextUrl
        if (isLoading || isEnd || url == null) return
        isLoading = true
        updateSuccessState(isLoadingMore = true)
        viewModelScope.launch {
            try {
                val page = withContext(Dispatchers.Default) {
                    repository.getNextPage(url)
                }
                val newComments = page.comments.filter { new ->
                    comments.none { it.id == new.id }
                }
                comments.addAll(newComments)
                nextUrl = page.nextUrl
                isEnd = page.isEnd
                isLoading = false
                updateSuccessState(isLoadingMore = false)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load more comments", e)
                isLoading = false
                errorMessage = e.message
                sendEffect(CommentEffect.ShowMessage("加载更多失败: ${e.message}"))
                updateSuccessState(isLoadingMore = false)
            }
        }
    }

    private fun refresh() {
        nextUrl = null
        isEnd = false
        errorMessage = null
        comments.clear()
        isLoading = true
        updateSuccessState(isLoadingMore = false)
        viewModelScope.launch {
            try {
                val page = withContext(Dispatchers.Default) {
                    repository.getRootComments(contentType, contentId, sortOrder, 0)
                }
                comments.clear()
                comments.addAll(page.comments)
                nextUrl = page.nextUrl
                isEnd = page.isEnd
                isLoading = false
                updateSuccessState(isLoadingMore = false)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to refresh comments", e)
                isLoading = false
                errorMessage = e.message
                uiState = CommentUiState.Error(e.message ?: "刷新评论失败")
            }
        }
    }

    private fun changeSortOrder(order: CommentSortOrder) {
        if (sortOrder == order) return
        sortOrder = order
        nextUrl = null
        isEnd = false
        comments.clear()
        isLoading = true
        updateSuccessState(isLoadingMore = false)
        viewModelScope.launch {
            try {
                val page = withContext(Dispatchers.Default) {
                    repository.getRootComments(contentType, contentId, sortOrder, 0)
                }
                comments.clear()
                comments.addAll(page.comments)
                nextUrl = page.nextUrl
                isEnd = page.isEnd
                isLoading = false
                updateSuccessState(isLoadingMore = false)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to change sort order", e)
                isLoading = false
                errorMessage = e.message
                uiState = CommentUiState.Error(e.message ?: "切换排序失败")
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
                comments.add(0, newComment)
                updateSuccessState()
                sendEffect(CommentEffect.ScrollToTop)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to submit comment", e)
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
                Log.e(TAG, "Failed to like comment", e)
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
                Log.e(TAG, "Failed to unlike comment", e)
                updateCommentLike(commentId, liked = true)
                sendEffect(CommentEffect.ShowMessage("取消点赞失败: ${e.message}"))
            }
        }
    }

    private fun updateCommentLike(commentId: String, liked: Boolean) {
        // 更新根评论列表
        val index = comments.indexOfFirst { it.id == commentId }
        if (index >= 0) {
            val comment = comments[index]
            comments[index] = comment.copy(
                liked = liked,
                likeCount = if (liked) comment.likeCount + 1 else (comment.likeCount - 1).coerceAtLeast(0),
            )
        }
        // 更新子评论列表
        val childIndex = childComments.indexOfFirst { it.id == commentId }
        if (childIndex >= 0) {
            val comment = childComments[childIndex]
            childComments[childIndex] = comment.copy(
                liked = liked,
                likeCount = if (liked) comment.likeCount + 1 else (comment.likeCount - 1).coerceAtLeast(0),
            )
        }
    }

    private fun deleteComment(commentId: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.Default) {
                    repository.deleteComment(commentId)
                }
                comments.removeAll { it.id == commentId }
                childComments.removeAll { it.id == commentId }
                // 更新父评论的 childCommentCount
                val parentIndex = comments.indexOfFirst { it.childComments.any { c -> c.id == commentId } }
                if (parentIndex >= 0) {
                    val parent = comments[parentIndex]
                    comments[parentIndex] = parent.copy(
                        childComments = parent.childComments.filter { it.id != commentId },
                        childCommentCount = (parent.childCommentCount - 1).coerceAtLeast(0),
                    )
                }
                updateSuccessState()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete comment", e)
                sendEffect(CommentEffect.ShowMessage("删除评论失败: ${e.message}"))
            }
        }
    }

    private fun openChildComments(comment: Comment) {
        activeParentComment = comment
        childComments.clear()
        childNextUrl = null
        isChildEnd = false
        isChildLoading = true
        viewModelScope.launch {
            try {
                val page = withContext(Dispatchers.Default) {
                    repository.getChildComments(comment.id, 0)
                }
                childComments.clear()
                childComments.addAll(page.comments)
                childNextUrl = page.nextUrl
                isChildEnd = page.isEnd
                isChildLoading = false
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load child comments", e)
                isChildLoading = false
                sendEffect(CommentEffect.ShowMessage("加载子评论失败: ${e.message}"))
            }
        }
    }

    fun loadMoreChildComments() {
        val url = childNextUrl
        if (isChildLoading || isChildEnd || url == null) return
        isChildLoading = true
        viewModelScope.launch {
            try {
                val page = withContext(Dispatchers.Default) {
                    repository.getNextPage(url)
                }
                val newComments = page.comments.filter { new ->
                    childComments.none { it.id == new.id }
                }
                childComments.addAll(newComments)
                childNextUrl = page.nextUrl
                isChildEnd = page.isEnd
                isChildLoading = false
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load more child comments", e)
                isChildLoading = false
                sendEffect(CommentEffect.ShowMessage("加载更多子评论失败: ${e.message}"))
            }
        }
    }

    private fun dismissChildComments() {
        activeParentComment = null
        childComments.clear()
        childNextUrl = null
        isChildEnd = false
    }

    private suspend fun resolveInitialComment(commentId: String) {
        try {
            val target = withContext(Dispatchers.Default) {
                repository.getComment(commentId)
            }
            // 如果目标评论本身就是根评论，直接添加
            // 否则需要找到其根评论（这里简化处理：直接加载根评论列表，目标评论会在列表中出现）
            // TODO: 更精确的锚点解析（找到根评论并置于顶部）
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resolve initial comment anchor", e)
        }
    }

    private fun updateSuccessState(isLoadingMore: Boolean = false) {
        uiState = CommentUiState.Success(
            comments = comments.toList(),
            sortOrder = sortOrder,
            isLoadingMore = isLoadingMore,
            isEnd = isEnd,
        )
    }

    private fun sendEffect(effect: CommentEffect) {
        viewModelScope.launch {
            _effect.send(effect)
        }
    }
}
