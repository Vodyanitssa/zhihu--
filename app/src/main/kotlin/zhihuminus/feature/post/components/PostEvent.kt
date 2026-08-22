package com.zhihuminus.feature.post.components

import com.zhihuminus.data.Collection

sealed interface PostEvent {
    data object Refresh : PostEvent

    data object ShowCollectionDialog : PostEvent

    data object Share : PostEvent

    data class OpenImage(
        val url: String,
    ) : PostEvent

    data class OpenLink(
        val url: String,
    ) : PostEvent

    data object VoteUp : PostEvent

    data object VoteDown : PostEvent

    data object LikePin : PostEvent

    data class VotePoll(
        val pollId: String,
        val optionId: String,
    ) : PostEvent

    data object Comment : PostEvent

    data object CopyLink : PostEvent

    data object Export : PostEvent

    data class CreateCollection(
        val title: String,
        val description: String,
        val isPublic: Boolean,
    ) : PostEvent

    data class ToggleCollection(
        val collection: Collection,
    ) : PostEvent

    data object ShowMoreMenu : PostEvent

    data object ShowVoters : PostEvent

    data object LoadMoreVoters : PostEvent

    data object FollowAuthor : PostEvent
}
