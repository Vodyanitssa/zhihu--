package com.zhihuminus.feature.column

import com.zhihuminus.navigation.NavDestination

sealed interface ColumnEvent {
    data object Refresh : ColumnEvent

    data object LoadMore : ColumnEvent
}

sealed interface ColumnEffect {
    data class ShowMessage(
        val message: String,
    ) : ColumnEffect

    data class Navigate(
        val destination: NavDestination,
    ) : ColumnEffect
}
