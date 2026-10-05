package com.zhihuminus.feature.people

import com.zhihuminus.data.DataHolder
import com.zhihuminus.navigation.NavDestination
import com.zhihuminus.navigation.Person

sealed interface PeopleEvent {
    data object RefreshProfile : PeopleEvent

    data object ToggleFollow : PeopleEvent

    data class ToggleItemFollow(
        val people: DataHolder.People,
    ) : PeopleEvent

    data object ToggleBlock : PeopleEvent

    data class TabSelected(
        val index: Int,
    ) : PeopleEvent

    data class LoadMore(
        val tabIndex: Int,
    ) : PeopleEvent

    data class RefreshTab(
        val tabIndex: Int,
    ) : PeopleEvent

    data class ChangeAnswersSort(
        val sortBy: String,
    ) : PeopleEvent

    data class ChangeArticlesSort(
        val sortBy: String,
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
