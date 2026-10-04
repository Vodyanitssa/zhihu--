package com.zhihuminus.feature.people.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zhihuminus.data.DataHolder
import com.zhihuminus.feature.people.FollowedQuestion
import com.zhihuminus.feature.people.FollowedTopic
import com.zhihuminus.feature.people.PaginatedTabState
import com.zhihuminus.feature.people.PeopleSubscriptionTab
import com.zhihuminus.ui.components.PaginatedList
import com.zhihuminus.ui.components.ProgressIndicatorFooter

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PeopleFollowingSubscriptionsTab(
    selectedIndex: Int,
    onTabSelect: (Int) -> Unit,
    onLoadMore: (Int) -> Unit,
    columnsState: PaginatedTabState<DataHolder.Column>,
    topicsState: PaginatedTabState<FollowedTopic>,
    questionsState: PaginatedTabState<FollowedQuestion>,
    collectionsState: PaginatedTabState<DataHolder.Collection>,
    onColumnClick: (DataHolder.Column) -> Unit,
    onTopicClick: (FollowedTopic) -> Unit,
    onQuestionClick: (FollowedQuestion) -> Unit,
    onCollectionClick: (DataHolder.Collection) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PeopleSubscriptionTab.TITLES.forEachIndexed { index, title ->
                OutlinedButton(
                    onClick = { onTabSelect(index) },
                    modifier = Modifier,
                    shape = RoundedCornerShape(8.dp),
                    colors = if (selectedIndex == index) {
                        ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    } else {
                        ButtonDefaults.outlinedButtonColors()
                    },
                ) {
                    Text(title)
                }
            }
        }

        when (selectedIndex) {
            0 -> PaginatedList(
                items = columnsState.items,
                onLoadMore = { onLoadMore(0) },
                isEnd = { columnsState.isEnd },
                footer = ProgressIndicatorFooter,
                modifier = Modifier.fillMaxSize(),
                key = { it.id },
            ) { column ->
                PeopleColumnListItem(
                    column = column,
                    onClick = { onColumnClick(column) },
                )
            }

            1 -> PaginatedList(
                items = topicsState.items,
                onLoadMore = { onLoadMore(1) },
                isEnd = { topicsState.isEnd },
                footer = ProgressIndicatorFooter,
                modifier = Modifier.fillMaxSize(),
                key = { it.id },
            ) { topic ->
                PeopleFollowedTopicListItem(
                    topic = topic,
                    onClick = { onTopicClick(topic) },
                )
            }

            2 -> PaginatedList(
                items = questionsState.items,
                onLoadMore = { onLoadMore(2) },
                isEnd = { questionsState.isEnd },
                footer = ProgressIndicatorFooter,
                modifier = Modifier.fillMaxSize(),
                key = { it.id },
            ) { question ->
                PeopleFollowedQuestionListItem(
                    question = question,
                    onClick = { onQuestionClick(question) },
                )
            }

            3 -> PaginatedList(
                items = collectionsState.items,
                onLoadMore = { onLoadMore(3) },
                isEnd = { collectionsState.isEnd },
                footer = ProgressIndicatorFooter,
                modifier = Modifier.fillMaxSize(),
                key = { it.id },
            ) { collection ->
                PeopleCollectionListItem(
                    collection = collection,
                    onClick = { onCollectionClick(collection) },
                )
            }
        }
    }
}
