package com.zhihuminus.feature.post

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuminus.core.content.renderer.PictureRenderer
import com.zhihuminus.core.platform.FileExporter
import com.zhihuminus.core.util.Log
import com.zhihuminus.core.util.friendlyErrorMessage
import com.zhihuminus.data.local.LocalWatchLaterRepository
import com.zhihuminus.feature.collection.Collection
import com.zhihuminus.feature.watchlater.WatchLaterItem
import com.zhihuminus.feature.watchlater.WatchLaterRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

class PostViewModel(
    application: Application,
    private val postId: Long,
    private val postType: PostType,
    private val repository: PostRepository,
    private val watchLaterRepository: WatchLaterRepository? = null,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : AndroidViewModel(application) {
    var uiState: PostUiState by mutableStateOf(PostUiState(postType = postType))
        private set

    private val _effect = Channel<PostEffect>(capacity = Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private val authorRelationRequests = mutableSetOf<String>()

    private val shareText: String
        get() {
            val state = uiState.loadState as? PostLoadState.Success ?: return ""
            val post = state.post
            return buildString {
                append(post.title)
                append("\n\n")
                append(post.excerpt.take(200))
                if (post.excerpt.length > 200) append("...")
                append("\n\n")
                append("—— ${post.author.name}")
            }
        }

    private val contentLink: String
        get() {
            val state = uiState.loadState as? PostLoadState.Success ?: return ""
            val post = state.post
            return when (post.type) {
                PostType.Answer -> "https://www.zhihu.com/question/${post.questionId}/answer/${post.id}"
                PostType.Article -> "https://zhuanlan.zhihu.com/p/${post.id}"
                PostType.Pin -> "https://www.zhihu.com/pin/${post.id}"
            }
        }

    init {
        loadPost()
    }

    fun onEvent(event: PostEvent) {
        when (event) {
            is PostEvent.Refresh -> loadPost(forceNetwork = true)
            is PostEvent.VoteUp -> {
                handleVote(if (uiState.bottomBarState.voteUpState == VoteUpState.Up) VoteUpState.Neutral else VoteUpState.Up)
            }

            is PostEvent.VoteDown -> {
                handleVote(if (uiState.bottomBarState.voteUpState == VoteUpState.Down) VoteUpState.Neutral else VoteUpState.Down)
            }

            is PostEvent.VotePoll -> {
                handlePollVote(event.pollId, event.optionId)
            }

            is PostEvent.Comment -> {
                uiState = uiState.copy(showComments = true)
            }

            is PostEvent.Share -> {
                sendEffect(PostEffect.ShareText(shareText))
            }

            is PostEvent.CopyLink -> {
                sendEffect(PostEffect.CopyLink(contentLink))
            }

            is PostEvent.Export -> {
                exportImage()
            }

            is PostEvent.OpenLink -> {
                sendEffect(PostEffect.OpenExternalUrl(event.url))
            }

            is PostEvent.CreateCollection -> {
                createCollection(event.title, event.description, event.isPublic)
            }

            is PostEvent.ToggleCollection -> {
                toggleCollection(event.collection)
            }

            is PostEvent.ShowCollectionDialog -> {
                uiState = uiState.copy(showCollectionDialog = true)
            }

            is PostEvent.DismissCollectionDialog -> {
                uiState = uiState.copy(showCollectionDialog = false)
            }

            is PostEvent.ShowMoreMenu -> {
                uiState = uiState.copy(showActionsMenu = true)
            }

            is PostEvent.DismissActionsMenu -> {
                uiState = uiState.copy(showActionsMenu = false)
            }

            is PostEvent.DismissComments -> {
                uiState = uiState.copy(showComments = false)
            }

            is PostEvent.Navigate -> {
                sendEffect(PostEffect.Navigate(event.destination))
            }

            is PostEvent.RefreshCollections -> {
                loadCollections()
            }

            is PostEvent.FollowAuthor -> {
                handleFollowAuthor()
            }

            is PostEvent.AddToWatchLater -> {
                handleAddToWatchLater()
            }
        }
    }

    private fun handleAddToWatchLater() {
        val post = (uiState.loadState as? PostLoadState.Success)?.post
        if (post == null) {
            sendEffect(PostEffect.ShowMessage("内容未加载完成"))
            return
        }
        viewModelScope.launch {
            try {
                val item = WatchLaterItem(
                    id = post.id,
                    type = when (post.type) {
                        PostType.Answer -> "answer"
                        PostType.Article -> "article"
                        PostType.Pin -> "pin"
                    },
                    title = post.title,
                    excerpt = post.excerpt,
                    authorName = post.author.name,
                    authorAvatar = post.author.avatarUrl,
                    coverImage = post.coverUrl,
                    addedAt = System.currentTimeMillis(),
                )
                val repo = watchLaterRepository ?: LocalWatchLaterRepository.getInstance(getApplication())
                repo.addItem(item)
                uiState = uiState.copy(showActionsMenu = false)
                sendEffect(PostEffect.ShowMessage("已添加到稍后再看"))
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("PostViewModel", "Failed to add to watch later", e)
                sendEffect(PostEffect.ShowMessage("添加失败: ${friendlyErrorMessage(e)}"))
            }
        }
    }

    private fun sendEffect(effect: PostEffect?) {
        if (effect == null) return
        viewModelScope.launch {
            _effect.send(effect)
        }
    }

    private fun exportImage() {
        val post = (uiState.loadState as? PostLoadState.Success)?.post
        if (post == null) {
            sendEffect(PostEffect.ShowMessage("内容未加载完成"))
            return
        }
        if (uiState.isExporting) return

        uiState = uiState.copy(isExporting = true)
        viewModelScope.launch {
            var result: PictureRenderer.RenderResult? = null
            try {
                val context = getApplication<Application>()
                result = withContext(defaultDispatcher) {
                    PictureRenderer.render(context, post)
                }
                withContext(Dispatchers.IO) {
                    FileExporter(context).saveToGallery(result.displayName, result.bitmap)
                }
                uiState = uiState.copy(isExporting = false)
                sendEffect(PostEffect.ShowMessage("图片已保存到相册"))
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("PostViewModel", "Image export failed", e)
                uiState = uiState.copy(isExporting = false)
                sendEffect(PostEffect.ShowMessage("图片导出失败: ${friendlyErrorMessage(e)}"))
            } finally {
                result?.bitmap?.recycle()
            }
        }
    }

    private fun loadPost(forceNetwork: Boolean = false) {
        viewModelScope.launch {
            val cached = if (forceNetwork) {
                null
            } else {
                try {
                    withContext(defaultDispatcher) {
                        repository.getCachedPost(postType, postId)
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Log.e("PostViewModel", "Failed to get cached post", e)
                    null
                }
            }
            val post = if (cached != null) {
                yield()
                applyPost(cached)
                if (cached.ipInfo.isNullOrBlank()) {
                    viewModelScope.launch {
                        try {
                            val fresh = withContext(defaultDispatcher) {
                                repository.getPost(postType, postId)
                            }
                            applyPost(fresh)
                        } catch (e: Exception) {
                            if (e is CancellationException) throw e
                            Log.w("PostViewModel", "Background refresh for post details failed", e)
                        }
                    }
                }
                cached
            } else {
                // Only show loading if not already displaying content (avoids dialog flicker on refresh)
                if (uiState.loadState !is PostLoadState.Success) {
                    uiState = uiState.copy(loadState = PostLoadState.Loading)
                }
                try {
                    val fresh = withContext(defaultDispatcher) {
                        repository.getPost(postType, postId)
                    }
                    applyPost(fresh)
                    fresh
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Log.e("PostViewModel", "Failed to load post", e)
                    val friendlyMsg = friendlyErrorMessage(e)
                    if (uiState.loadState is PostLoadState.Success) {
                        sendEffect(PostEffect.ShowMessage("刷新失败: $friendlyMsg"))
                    } else {
                        uiState = uiState.copy(loadState = PostLoadState.Error(friendlyMsg))
                        sendEffect(PostEffect.ShowMessage(friendlyMsg))
                    }
                    return@launch
                }
            }
            applyCollectedState((uiState.loadState as? PostLoadState.Success)?.post ?: post)
            try {
                withContext(defaultDispatcher) {
                    repository.recordHistory(postType, postId)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("PostViewModel", "Failed to record history", e)
            }
        }
    }

    private fun applyPost(post: Post) {
        val currentBar = uiState.bottomBarState
        val previousPost = (uiState.loadState as? PostLoadState.Success)?.post
        val mergedPost = post.mergeFallbackFrom(previousPost)
        val currentVote = previousPost?.voteState
        val voteState = if (currentBar.voteUpState != VoteUpState.Neutral && currentBar.voteUpState != currentVote) {
            currentBar.voteUpState
        } else {
            mergedPost.voteState
        }
        uiState = uiState.copy(
            loadState = PostLoadState.Success(mergedPost),
            bottomBarState = currentBar.copy(
                voteUpState = voteState,
                voteUpCount = mergedPost.voteCount,
                commentCount = mergedPost.commentCount,
            ),
        )
        resolveAuthorFollowing(mergedPost)
    }

    private fun resolveAuthorFollowing(post: Post) {
        if (post.author.isFollowing != null) return
        val token = post.author.urlToken.ifBlank { post.author.id }
        if (token.isBlank() || !authorRelationRequests.add(token)) return
        viewModelScope.launch {
            try {
                val known = withContext(defaultDispatcher) {
                    repository.getAuthorFollowing(token)
                } ?: return@launch
                val current = (uiState.loadState as? PostLoadState.Success)?.post ?: return@launch
                if (current.author.isFollowing != null) return@launch
                val updatedPost = current.copy(author = current.author.copy(isFollowing = known))
                uiState = uiState.copy(loadState = PostLoadState.Success(updatedPost))
                withContext(defaultDispatcher) {
                    repository.putCachedPost(updatedPost)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.w("PostViewModel", "Failed to resolve author following state", e)
            } finally {
                authorRelationRequests.remove(token)
            }
        }
    }

    /**
     * 收藏状态已知（缓存的 reaction.relation.faved）时直接采用，省去收藏夹列表请求；
     * 未知时走原来的网络加载。收藏夹对话框打开时会自行补拉完整列表。
     */
    private fun applyCollectedState(post: Post) {
        val faved = post.isFaved
        if (faved == null) {
            loadCollections()
        } else {
            uiState = uiState.copy(
                isCollected = faved,
                bottomBarState = uiState.bottomBarState.copy(isCollected = faved),
            )
        }
    }

    fun loadCollections() {
        viewModelScope.launch {
            try {
                val result = withContext(defaultDispatcher) {
                    repository.getCollections(postType, postId)
                }
                val collected = result.any { it.isFavorited }
                uiState = uiState.copy(
                    collections = result,
                    isCollected = collected,
                    bottomBarState = uiState.bottomBarState.copy(isCollected = collected),
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("PostViewModel", "Failed to load collections", e)
            }
        }
    }

    private fun toggleCollection(collection: Collection) {
        viewModelScope.launch {
            try {
                val isFavorited = collection.isFavorited
                withContext(defaultDispatcher) {
                    if (isFavorited) {
                        repository.removeFromCollection(postType, postId, collection.id)
                    } else {
                        repository.addToCollection(postType, postId, collection.id)
                    }
                }
                loadCollections()
                sendEffect(
                    PostEffect.ShowMessage(
                        if (isFavorited) "已从「${collection.title}」移除" else "已收藏到「${collection.title}」",
                    ),
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("PostViewModel", "Failed to toggle collection", e)
                sendEffect(PostEffect.ShowMessage("操作收藏夹失败: ${friendlyErrorMessage(e)}"))
            }
        }
    }

    private fun createCollection(title: String, description: String, isPublic: Boolean) {
        viewModelScope.launch {
            try {
                withContext(defaultDispatcher) {
                    repository.createCollection(title, description, isPublic)
                }
                loadCollections()
                sendEffect(PostEffect.ShowMessage("收藏夹已创建"))
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("PostViewModel", "Failed to create collection", e)
                sendEffect(PostEffect.ShowMessage("创建收藏夹失败: ${friendlyErrorMessage(e)}"))
            }
        }
    }

    private fun handleVote(
        newState: VoteUpState,
        showFeedback: Boolean = true,
    ) {
        val voteKey = when (newState) {
            VoteUpState.Up -> "up"
            VoteUpState.Down -> "down"
            VoteUpState.Neutral -> "neutral"
        }

        val bar = uiState.bottomBarState
        val previousState = bar.voteUpState
        val previousCount = bar.voteUpCount
        val countDelta = when {
            previousState == VoteUpState.Up && newState == VoteUpState.Neutral -> -1
            previousState == VoteUpState.Down && newState == VoteUpState.Neutral -> +1
            previousState == VoteUpState.Neutral && newState == VoteUpState.Up -> +1
            previousState == VoteUpState.Neutral && newState == VoteUpState.Down -> -1
            previousState == VoteUpState.Up && newState == VoteUpState.Down -> -2
            previousState == VoteUpState.Down && newState == VoteUpState.Up -> +2
            else -> 0
        }
        uiState = uiState.copy(
            bottomBarState = bar.copy(
                voteUpState = newState,
                voteUpCount = (previousCount + countDelta).coerceAtLeast(0),
            ),
        )

        viewModelScope.launch {
            try {
                val newCount = withContext(defaultDispatcher) {
                    repository.vote(postType, postId, voteKey)
                }
                uiState = uiState.copy(bottomBarState = uiState.bottomBarState.copy(voteUpCount = newCount))
                if (showFeedback) {
                    val message = when (newState) {
                        VoteUpState.Up -> if (postType == PostType.Pin) "已点赞" else "已赞同"
                        VoteUpState.Down -> "已反对"
                        VoteUpState.Neutral -> {
                            when (previousState) {
                                VoteUpState.Up -> if (postType == PostType.Pin) "已取消点赞" else "已取消赞同"
                                VoteUpState.Down -> "已取消反对"
                                VoteUpState.Neutral -> null
                            }
                        }
                    }
                    message?.let { sendEffect(PostEffect.ShowMessage(it)) }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("PostViewModel", "Vote failed", e)
                uiState = uiState.copy(
                    bottomBarState = bar.copy(
                        voteUpState = previousState,
                        voteUpCount = previousCount,
                    ),
                )
                sendEffect(PostEffect.ShowMessage("投票失败: ${friendlyErrorMessage(e)}"))
            }
        }
    }

    private fun handlePollVote(pollId: String, optionId: String) {
        val loadState = uiState.loadState as? PostLoadState.Success ?: return
        val poll = loadState.post.poll ?: return
        if (poll.isVoted || !poll.acceptsVote()) return

        // Optimistic update: mark poll as voted
        val updatedPoll = poll.copy(
            isVoted = true,
            votingCount = poll.votingCount + 1,
            memberCount = poll.memberCount + 1,
            options = poll.options.map { opt ->
                if (opt.id == optionId) {
                    opt.copy(votingCount = opt.votingCount + 1, isSelected = true)
                } else {
                    opt
                }
            },
        )
        uiState = uiState.copy(loadState = loadState.copy(post = loadState.post.copy(poll = updatedPoll)))

        viewModelScope.launch {
            try {
                withContext(defaultDispatcher) {
                    repository.submitPinPollVote(pollId, optionId)
                }
                sendEffect(PostEffect.ShowMessage("投票成功"))
                // Auto-like after voting (silent so we don't display a duplicate like toast)
                handleVote(VoteUpState.Up, showFeedback = false)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("PostViewModel", "Poll vote failed", e)
                // Revert optimistic update
                uiState = uiState.copy(loadState = loadState.copy(post = loadState.post.copy(poll = poll)))
                sendEffect(PostEffect.ShowMessage("投票失败: ${friendlyErrorMessage(e)}"))
            }
        }
    }

    private fun handleFollowAuthor() {
        val loadState = uiState.loadState as? PostLoadState.Success ?: return
        val author = loadState.post.author
        val newFollowing = author.isFollowing != true
        val targetToken = author.urlToken.takeIf { it.isNotBlank() } ?: author.id
        if (targetToken.isBlank()) return

        val updatedPost = loadState.post.copy(author = author.copy(isFollowing = newFollowing))

        // Optimistic update
        uiState = uiState.copy(loadState = loadState.copy(post = updatedPost))

        viewModelScope.launch {
            try {
                withContext(defaultDispatcher) {
                    repository.followMember(targetToken, newFollowing)
                    repository.putCachedPost(updatedPost)
                }
                sendEffect(PostEffect.ShowMessage(if (newFollowing) "已关注" else "已取消关注"))
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("PostViewModel", "Follow failed", e)
                // Rollback
                uiState = uiState.copy(loadState = loadState.copy(post = loadState.post.copy(author = author)))
                sendEffect(PostEffect.ShowMessage("关注失败: ${friendlyErrorMessage(e)}"))
            }
        }
    }
}
