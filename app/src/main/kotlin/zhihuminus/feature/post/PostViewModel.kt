package com.zhihuminus.feature.post

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuminus.data.Collection
import com.zhihuminus.data.VoteUpState
import com.zhihuminus.feature.post.components.PostBottomBarState
import com.zhihuminus.feature.post.components.PostEvent
import com.zhihuminus.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PostViewModel(
    private val postId: Long,
    private val postType: PostType,
    private val repository: PostRepository,
) : ViewModel() {
    var uiState: PostUiState by mutableStateOf(PostUiState.Loading)
        private set

    var bottomBarState: PostBottomBarState by mutableStateOf(PostBottomBarState())
        private set

    val collections = mutableStateListOf<Collection>()
    var isCollected: Boolean by mutableStateOf(false)
        private set

    private val _effect = Channel<PostEffect>(capacity = Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private val shareText: String
        get() {
            val state = uiState as? PostUiState.Success ?: return ""
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
            val state = uiState as? PostUiState.Success ?: return ""
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
                handleVote(if (bottomBarState.voteUpState == VoteUpState.Up) VoteUpState.Neutral else VoteUpState.Up)
            }

            is PostEvent.VoteDown -> {
                handleVote(if (bottomBarState.voteUpState == VoteUpState.Down) VoteUpState.Neutral else VoteUpState.Down)
            }

            is PostEvent.Comment -> {
                // TODO: Implement comment
            }

            is PostEvent.Share -> {
                sendEffect(PostEffect.ShareText(shareText))
            }

            is PostEvent.CopyLink -> {
                sendEffect(PostEffect.CopyLink(contentLink))
            }

            is PostEvent.Export -> {
                // TODO: Implement export
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
                // Handled by PostScreen
            }

            is PostEvent.ShowMoreMenu -> {
                bottomBarState = bottomBarState.copy(showMoreMenu = true)
            }
        }
    }

    private fun sendEffect(effect: PostEffect?) {
        if (effect == null) return
        viewModelScope.launch {
            _effect.send(effect)
        }
    }

    private fun loadPost() {
        uiState = PostUiState.Loading
        viewModelScope.launch {
            try {
                val post = withContext(Dispatchers.Default) {
                    repository.getPost(postType, postId)
                }
                uiState = PostUiState.Success(post)
                bottomBarState = PostBottomBarState(
                    voteUpState = post.voteState,
                    voteUpCount = post.voteCount,
                    commentCount = post.commentCount,
                )
                if (postType != PostType.Pin) {
                    loadCollections()
                }
            } catch (e: Exception) {
                Log.e("PostViewModel", "Failed to load post", e)
                uiState = PostUiState.Error(e)
            }
        }
    }

    fun loadCollections() {
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.Default) {
                    repository.getCollections(postType, postId)
                }
                collections.clear()
                collections.addAll(result)
                isCollected = collections.any { it.isFavorited }
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

        val previousState = bottomBarState.voteUpState
        val previousCount = bottomBarState.voteUpCount
        val countDelta = when {
            previousState == VoteUpState.Up && newState == VoteUpState.Neutral -> -1
            previousState == VoteUpState.Down && newState == VoteUpState.Neutral -> +1
            previousState == VoteUpState.Neutral && newState == VoteUpState.Up -> +1
            previousState == VoteUpState.Neutral && newState == VoteUpState.Down -> -1
            previousState == VoteUpState.Up && newState == VoteUpState.Down -> -2
            previousState == VoteUpState.Down && newState == VoteUpState.Up -> +2
            else -> 0
        }
        bottomBarState = bottomBarState.copy(
            voteUpState = newState,
            voteUpCount = (previousCount + countDelta).coerceAtLeast(0),
        )

        viewModelScope.launch {
            try {
                val newCount = withContext(Dispatchers.Default) {
                    repository.vote(postType, postId, voteKey)
                }
                bottomBarState = bottomBarState.copy(voteUpCount = newCount)
            } catch (e: Exception) {
                Log.e("PostViewModel", "Vote failed", e)
                bottomBarState = bottomBarState.copy(
                    voteUpState = previousState,
                    voteUpCount = previousCount,
                )
            }
        }
    }
}
