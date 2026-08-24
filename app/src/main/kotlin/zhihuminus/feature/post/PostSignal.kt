package com.zhihuminus.feature.post

import com.zhihuminus.data.Collection
import com.zhihuminus.navigation.NavDestination

sealed interface PostEvent {
    data object Refresh : PostEvent

    data object ShowCollectionDialog : PostEvent

    data object DismissCollectionDialog : PostEvent

    data object Share : PostEvent

    data class OpenLink(
        val url: String,
    ) : PostEvent

    data object VoteUp : PostEvent

    data object VoteDown : PostEvent

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

    data object DismissActionsMenu : PostEvent

    data object DismissComments : PostEvent

    data class Navigate(
        val destination: NavDestination,
    ) : PostEvent

    data object RefreshCollections : PostEvent

    data object FollowAuthor : PostEvent
}

sealed interface PostEffect {
    data class ShareText(
        val text: String,
    ) : PostEffect

    data class CopyLink(
        val link: String,
    ) : PostEffect

    data class ShowMessage(
        val message: String,
    ) : PostEffect

    data class OpenExternalUrl(
        val url: String,
    ) : PostEffect

    data class Navigate(
        val destination: NavDestination,
    ) : PostEffect
}
