package com.zhihuminus.feature.people.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zhihuminus.feature.people.PaginatedTabState
import com.zhihuminus.feature.people.PeopleMemberItem
import com.zhihuminus.ui.components.PaginatedList
import com.zhihuminus.ui.components.PeopleListItem
import com.zhihuminus.ui.components.ProgressIndicatorFooter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeopleUserListSheet(
    title: String,
    state: PaginatedTabState<PeopleMemberItem>,
    changingItemFollowIds: Set<String>,
    onLoadMore: () -> Unit,
    onToggleFollow: (PeopleMemberItem) -> Unit,
    onPersonClick: (PeopleMemberItem) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier.statusBarsPadding(),
        dragHandle = null,
        properties = ModalBottomSheetProperties(
            shouldDismissOnBackPress = true,
            shouldDismissOnClickOutside = true,
        ),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            PaginatedList(
                items = state.items,
                onLoadMore = onLoadMore,
                isEnd = { state.isEnd },
                footer = ProgressIndicatorFooter,
                modifier = Modifier.fillMaxSize(),
                key = { it.urlToken.ifBlank { it.id } },
            ) { people ->
                PeopleListItem(
                    people = people,
                    isFollowing = people.isFollowing,
                    isChangingFollowing = people.id in changingItemFollowIds,
                    onClick = { onPersonClick(people) },
                    onToggleFollow = { onToggleFollow(people) },
                )
            }
        }
    }
}
