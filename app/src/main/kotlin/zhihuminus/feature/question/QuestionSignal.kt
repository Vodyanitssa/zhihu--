package com.zhihuminus.feature.question

import com.zhihuminus.navigation.NavDestination

sealed interface QuestionEvent {
    /** 下拉刷新回答流。 */
    data object Refresh : QuestionEvent

    data object LoadMore : QuestionEvent

    data class ChangeSort(
        val sort: QuestionSort,
    ) : QuestionEvent

    data object ToggleFollow : QuestionEvent

    data class Navigate(
        val destination: NavDestination,
    ) : QuestionEvent

    /** 打开知乎 Web 端的回答编辑记录。 */
    data object OpenHistoryLog : QuestionEvent
}

sealed interface QuestionEffect {
    data class ShowMessage(
        val message: String,
    ) : QuestionEffect

    data class Navigate(
        val destination: NavDestination,
    ) : QuestionEffect

    data class OpenExternalUrl(
        val url: String,
    ) : QuestionEffect

    /**
     * 问题详情加载成功；宿主应把它记入导航历史（postHistoryDestination）。
     */
    data class ContentOpened(
        val title: String,
    ) : QuestionEffect
}
