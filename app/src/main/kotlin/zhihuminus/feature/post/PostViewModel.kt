package com.zhihuminus.feature.post

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuminus.core.content.renderer.PictureRenderer
import com.zhihuminus.core.platform.FileExporter
import com.zhihuminus.data.Collection
import com.zhihuminus.data.VoteUpState
import com.zhihuminus.feature.post.components.PostBottomBarState
import com.zhihuminus.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PostViewModel(
    application: Application,
    private val postId: Long,
    private val postType: PostType,
    private val repository: PostRepository,
) : AndroidViewModel(application) {
    var uiState: PostUiState by mutableStateOf(PostUiState())
        private set

    private val _effect = Channel<PostEffect>(capacity = Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

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
            is PostEvent.Refresh -> loadPost()
            is PostEvent.VoteUp -> {
                handleVote(if (uiState.bottomBarState.voteUpState == VoteUpState.Up) VoteUpState.Neutral else VoteUpState.Up)
            }

            is PostEvent.VoteDown -> {
                handleVote(if (uiState.bottomBarState.voteUpState == VoteUpState.Down) VoteUpState.Neutral else VoteUpState.Down)
            }

            is PostEvent.LikePin -> {
                handlePinLike()
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

            is PostEvent.OpenImage -> {
                sendEffect(PostEffect.OpenImage(event.url))
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
                result = withContext(Dispatchers.Default) {
                    PictureRenderer.render(context, post)
                }
                withContext(Dispatchers.IO) {
                    FileExporter(context).saveToGallery(result.displayName, result.bitmap)
                }
                uiState = uiState.copy(isExporting = false)
                sendEffect(PostEffect.ShowMessage("图片已保存到相册"))
            } catch (e: Exception) {
                Log.e("PostViewModel", "Image export failed", e)
                uiState = uiState.copy(isExporting = false)
                sendEffect(PostEffect.ShowMessage("图片导出失败: ${e.message}"))
            } finally {
                result?.bitmap?.recycle()
            }
        }
    }

    private fun loadPost() {
        // Only show loading if not already displaying content (avoids dialog flicker on refresh)
        if (uiState.loadState !is PostLoadState.Success) {
            uiState = uiState.copy(loadState = PostLoadState.Loading)
        }
        viewModelScope.launch {
            try {
                val post = withContext(Dispatchers.Default) {
                    repository.getPost(postType, postId)
                }
                uiState = uiState.copy(
                    loadState = PostLoadState.Success(post),
                    bottomBarState = PostBottomBarState(
                        voteUpState = post.voteState,
                        voteUpCount = post.voteCount,
                        commentCount = post.commentCount,
                    ),
                )
                loadCollections()
                viewModelScope.launch {
                    try {
                        withContext(Dispatchers.Default) {
                            repository.recordHistory(postType, postId)
                        }
                    } catch (e: Exception) {
                        Log.e("PostViewModel", "Failed to record history", e)
                    }
                }
            } catch (e: Exception) {
                Log.e("PostViewModel", "Failed to load post", e)
                uiState = uiState.copy(loadState = PostLoadState.Error(e.message))
            }
        }
    }

    fun loadCollections() {
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.Default) {
                    repository.getCollections(postType, postId)
                }
                uiState = uiState.copy(
                    collections = result,
                    isCollected = result.any { it.isFavorited },
                )
            } catch (e: Exception) {
                Log.e("PostViewModel", "Failed to load collections", e)
            }
        }
    }

    private fun toggleCollection(collection: Collection) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.Default) {
                    if (collection.isFavorited) {
                        repository.removeFromCollection(postType, postId, collection.id)
                    } else {
                        repository.addToCollection(postType, postId, collection.id)
                    }
                }
                loadCollections()
            } catch (e: Exception) {
                Log.e("PostViewModel", "Failed to toggle collection", e)
            }
        }
    }

    private fun createCollection(title: String, description: String, isPublic: Boolean) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.Default) {
                    repository.createCollection(title, description, isPublic)
                }
                loadCollections()
            } catch (e: Exception) {
                Log.e("PostViewModel", "Failed to create collection", e)
            }
        }
    }

    private fun handleVote(newState: VoteUpState) {
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
                val newCount = withContext(Dispatchers.Default) {
                    repository.vote(postType, postId, voteKey)
                }
                uiState = uiState.copy(bottomBarState = uiState.bottomBarState.copy(voteUpCount = newCount))
            } catch (e: Exception) {
                Log.e("PostViewModel", "Vote failed", e)
                uiState = uiState.copy(
                    bottomBarState = bar.copy(
                        voteUpState = previousState,
                        voteUpCount = previousCount,
                    ),
                )
            }
        }
    }

    private fun handlePinLike() {
        val bar = uiState.bottomBarState
        val previousState = bar.voteUpState
        val previousCount = bar.voteUpCount
        val newLiked = previousState != VoteUpState.Up
        uiState = uiState.copy(
            bottomBarState = bar.copy(
                voteUpState = if (newLiked) VoteUpState.Up else VoteUpState.Neutral,
                voteUpCount = (previousCount + if (newLiked) 1 else -1).coerceAtLeast(0),
            ),
        )

        viewModelScope.launch {
            try {
                val newCount = withContext(Dispatchers.Default) {
                    repository.likePin(postId)
                }
                uiState = uiState.copy(bottomBarState = uiState.bottomBarState.copy(voteUpCount = newCount))
            } catch (e: Exception) {
                Log.e("PostViewModel", "Pin like failed", e)
                uiState = uiState.copy(
                    bottomBarState = bar.copy(
                        voteUpState = previousState,
                        voteUpCount = previousCount,
                    ),
                )
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
                withContext(Dispatchers.Default) {
                    repository.submitPinPollVote(pollId, optionId)
                }
                // Auto-like after voting
                handlePinLike()
            } catch (e: Exception) {
                Log.e("PostViewModel", "Poll vote failed", e)
                // Revert optimistic update
                uiState = uiState.copy(loadState = loadState.copy(post = loadState.post.copy(poll = poll)))
            }
        }
    }

    private fun handleFollowAuthor() {
        val loadState = uiState.loadState as? PostLoadState.Success ?: return
        val author = loadState.post.author
        val newFollowing = !author.isFollowing

        // Optimistic update
        uiState = uiState.copy(loadState = loadState.copy(post = loadState.post.copy(author = author.copy(isFollowing = newFollowing))))

        viewModelScope.launch {
            try {
                withContext(Dispatchers.Default) {
                    repository.followMember(author.urlToken, newFollowing)
                }
            } catch (e: Exception) {
                Log.e("PostViewModel", "Follow failed", e)
                // Rollback
                uiState = uiState.copy(loadState = loadState.copy(post = loadState.post.copy(author = author)))
            }
        }
    }
}
