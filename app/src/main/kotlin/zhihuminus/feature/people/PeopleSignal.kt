package com.zhihuminus.feature.people

import com.zhihuminus.navigation.NavDestination
import com.zhihuminus.navigation.Person

sealed interface PeopleEvent {
    data object RefreshProfile : PeopleEvent

    data object ToggleFollow : PeopleEvent

    data class ToggleItemFollow(
        val people: PeopleMemberItem,
    ) : PeopleEvent

    data object ToggleBlock : PeopleEvent

    data class PrimaryTabSelected(
        val tab: PeoplePrimaryTab,
    ) : PeopleEvent

    data class CreationTabSelected(
        val tab: PeopleCreationTab,
    ) : PeopleEvent

    data class LoadMoreCreation(
        val tab: PeopleCreationTab,
    ) : PeopleEvent

    data class RefreshCreation(
        val tab: PeopleCreationTab,
    ) : PeopleEvent

    data class ChangeAnswersSort(
        val sortBy: String,
    ) : PeopleEvent

    data class ChangeArticlesSort(
        val sortBy: String,
    ) : PeopleEvent

    data class LoadMorePrimary(
        val tab: PeoplePrimaryTab,
    ) : PeopleEvent

    data class RefreshPrimary(
        val tab: PeoplePrimaryTab,
    ) : PeopleEvent

    data class LoadUserListIfNeeded(
        val type: PeopleUserListType,
    ) : PeopleEvent

    data class LoadMoreUserList(
        val type: PeopleUserListType,
    ) : PeopleEvent

    data class RefreshUserList(
        val type: PeopleUserListType,
    ) : PeopleEvent

    data class SubscriptionTabSelected(
        val index: Int,
    ) : PeopleEvent

    data class LoadMoreSubscription(
        val index: Int,
    ) : PeopleEvent

    data class RefreshSubscription(
        val index: Int,
    ) : PeopleEvent
}

sealed interface PeopleEffect {
    data class ShowMessage(
        val message: String,
    ) : PeopleEffect

    data class ProfileLoaded(
        val person: Person,
    ) : PeopleEffect

    data class Navigate(
        val destination: NavDestination,
    ) : PeopleEffect
}
