package com.zhihuminus.feature.people.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.zhihuminus.data.FeedDisplayItem
import com.zhihuminus.data.FeedImageLayout
import com.zhihuminus.feature.people.PeopleCreationItem
import com.zhihuminus.feature.people.PeopleCreationTab
import com.zhihuminus.feature.people.PeopleEvent
import com.zhihuminus.feature.people.PeopleUiState
import com.zhihuminus.feature.post.PostType
import com.zhihuminus.navigation.NavDestination
import com.zhihuminus.navigation.Question
import com.zhihuminus.ui.components.FeedCard
import com.zhihuminus.ui.components.PaginatedList
import com.zhihuminus.ui.components.ProgressIndicatorFooter

@Composable
fun PeopleCreationsTab(
    state: PeopleUiState,
    onEvent: (PeopleEvent) -> Unit,
    onNavigate: (NavDestination) -> Unit,
    onLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PeopleCreationTab.entries.forEach { tab ->
                val isSelected = state.selectedCreationTab == tab
                val countText = when (tab) {
                    PeopleCreationTab.Answers -> if (state.profile.answerCount > 0) " (${state.profile.answerCount})" else ""
                    PeopleCreationTab.Articles -> if (state.profile.articleCount > 0) " (${state.profile.articleCount})" else ""
                    PeopleCreationTab.Pins -> if (state.profile.pinsCount > 0) " (${state.profile.pinsCount})" else ""
                    PeopleCreationTab.Questions -> if (state.profile.questionCount > 0) " (${state.profile.questionCount})" else ""
                    else -> ""
                }
                val bg by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent,
                    label = "creationTabBg_${tab.name}",
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    label = "creationTabTextColor_${tab.name}",
                )

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(bg)
                        .clickable { onEvent(PeopleEvent.CreationTabSelected(tab)) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "${tab.title}$countText",
                        style = MaterialTheme.typography.labelMedium,
                        color = textColor,
                    )
                }
            }
        }

        when (state.selectedCreationTab) {
            PeopleCreationTab.Answers -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    PeopleSortBar(
                        currentSort = state.answersSort,
                        onSortChange = { onEvent(PeopleEvent.ChangeAnswersSort(it)) },
                    )
                    if (state.answersState.items.isEmpty() && state.answersState.hasLoaded && !state.answersState.isLoading) {
                        EmptyHint(message = "暂无回答")
                    } else {
                        PaginatedList(
                            items = state.answersState.items,
                            onLoadMore = { onEvent(PeopleEvent.LoadMoreCreation(PeopleCreationTab.Answers)) },
                            isEnd = { state.answersState.isEnd },
                            footer = if (state.isRefreshingProfile || state.answersState.isRefreshing) null else ProgressIndicatorFooter,
                            modifier = Modifier.fillMaxSize(),
                            key = { it.id },
                        ) { item ->
                            FeedCard(
                                item = item.toFeedDisplayItem(),
                                modifier = Modifier,
                                onClick = {
                                    onNavigate(item.toDestination())
                                },
                            )
                        }
                    }
                }
            }

            PeopleCreationTab.Articles -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    PeopleSortBar(
                        currentSort = state.articlesSort,
                        onSortChange = { onEvent(PeopleEvent.ChangeArticlesSort(it)) },
                    )
                    if (state.articlesState.items.isEmpty() && state.articlesState.hasLoaded && !state.articlesState.isLoading) {
                        EmptyHint(message = "暂无文章")
                    } else {
                        PaginatedList(
                            items = state.articlesState.items,
                            onLoadMore = { onEvent(PeopleEvent.LoadMoreCreation(PeopleCreationTab.Articles)) },
                            isEnd = { state.articlesState.isEnd },
                            footer = if (state.isRefreshingProfile || state.articlesState.isRefreshing) null else ProgressIndicatorFooter,
                            modifier = Modifier.fillMaxSize(),
                            key = { it.id },
                        ) { item ->
                            FeedCard(
                                item = item.toFeedDisplayItem(),
                                modifier = Modifier,
                                onClick = {
                                    onNavigate(item.toDestination())
                                },
                            )
                        }
                    }
                }
            }

            PeopleCreationTab.Pins -> {
                if (state.pinsState.items.isEmpty() && state.pinsState.hasLoaded && !state.pinsState.isLoading) {
                    EmptyHint(message = "暂无想法")
                } else {
                    PaginatedList(
                        items = state.pinsState.items,
                        onLoadMore = { onEvent(PeopleEvent.LoadMoreCreation(PeopleCreationTab.Pins)) },
                        isEnd = { state.pinsState.isEnd },
                        footer = if (state.isRefreshingProfile || state.pinsState.isRefreshing) null else ProgressIndicatorFooter,
                        modifier = Modifier.fillMaxSize(),
                        key = { it.id },
                    ) { item ->
                        FeedCard(
                            item = item.toFeedDisplayItem(),
                            modifier = Modifier,
                            onClick = {
                                onNavigate(item.toDestination())
                            },
                        )
                    }
                }
            }

            PeopleCreationTab.Columns -> {
                if (state.columnsState.items.isEmpty() && state.columnsState.hasLoaded && !state.columnsState.isLoading) {
                    EmptyHint(message = "暂无专栏")
                } else {
                    PaginatedList(
                        items = state.columnsState.items,
                        onLoadMore = { onEvent(PeopleEvent.LoadMoreCreation(PeopleCreationTab.Columns)) },
                        isEnd = { state.columnsState.isEnd },
                        footer = if (state.isRefreshingProfile || state.columnsState.isRefreshing) null else ProgressIndicatorFooter,
                        modifier = Modifier.fillMaxSize(),
                        key = { it.id },
                    ) { column ->
                        PeopleColumnListItem(
                            column = column,
                            onClick = { onLinkClick(column.webUrl) },
                        )
                    }
                }
            }

            PeopleCreationTab.Questions -> {
                if (state.questionsState.items.isEmpty() && state.questionsState.hasLoaded && !state.questionsState.isLoading) {
                    EmptyHint(message = "暂无提问")
                } else {
                    PaginatedList(
                        items = state.questionsState.items,
                        onLoadMore = { onEvent(PeopleEvent.LoadMoreCreation(PeopleCreationTab.Questions)) },
                        isEnd = { state.questionsState.isEnd },
                        footer = if (state.isRefreshingProfile || state.questionsState.isRefreshing) null else ProgressIndicatorFooter,
                        modifier = Modifier.fillMaxSize(),
                        key = { it.id },
                    ) { question ->
                        FeedCard(
                            item = question.toFeedDisplayItem(
                                fallbackAuthorName = state.profile.name,
                                fallbackAvatarUrl = state.profile.avatarUrl,
                            ),
                            modifier = Modifier,
                            onClick = {
                                question.id.toLongOrNull()?.let { qId ->
                                    onNavigate(Question(qId, question.title))
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

private fun PeopleCreationItem.toFeedDisplayItem(): FeedDisplayItem {
    val layout = when (type) {
        PostType.Pin -> if (images.isNotEmpty()) FeedImageLayout.BOTTOM_GALLERY else FeedImageLayout.NONE
        else -> if (images.isNotEmpty()) FeedImageLayout.RIGHT_THUMBNAIL else FeedImageLayout.NONE
    }
    return FeedDisplayItem(
        title = title,
        summary = summary,
        details = details,
        avatarSrc = avatarUrl.takeIf { it.isNotBlank() },
        authorName = authorName.takeIf { it.isNotBlank() },
        contentTypeLabel = when (type) {
            PostType.Answer -> "回答"
            PostType.Article -> "文章"
            PostType.Pin -> "想法"
        },
        publishTimeSeconds = publishTimeSeconds,
        images = images,
        imageLayout = layout,
    )
}
