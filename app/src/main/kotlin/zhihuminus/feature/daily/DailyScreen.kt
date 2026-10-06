package com.zhihuminus.feature.daily

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zhihuminus.core.util.formatDailyDate
import com.zhihuminus.core.util.twoDigitString
import com.zhihuminus.feature.daily.components.DailyDateHeader
import com.zhihuminus.feature.daily.components.DailyStoryCard
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun DailyScreen(
    state: DailyUiState,
    onEvent: (DailyEvent) -> Unit,
    onStoryClick: (String) -> Unit,
    listState: LazyListState = rememberLazyListState(),
    isActive: Boolean = true,
) {
    var isRefreshing by remember { mutableStateOf(false) }
    var currentViewingDate by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var pendingDateSelection by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(listState, state.sections) {
        currentViewingDate = resolveViewingDate(
            firstVisibleItemIndex = listState.firstVisibleItemIndex,
            sections = state.sections,
        )
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { index ->
                currentViewingDate = resolveViewingDate(
                    firstVisibleItemIndex = index,
                    sections = state.sections,
                )
            }
    }

    LaunchedEffect(listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            (info.visibleItemsInfo.lastOrNull()?.index ?: 0) to info.totalItemsCount
        }.collect { (last, total) ->
            if (total > 0 && last >= total - 3) {
                onEvent(DailyEvent.LoadMore)
            }
        }
    }

    LaunchedEffect(Unit) {
        if (state.sections.isEmpty() && state.isLoading) {
            onEvent(DailyEvent.Refresh)
        }
    }

    LaunchedEffect(showDatePicker, pendingDateSelection) {
        if (showDatePicker) return@LaunchedEffect
        val selectedDate = pendingDateSelection ?: return@LaunchedEffect
        withFrameNanos { }
        onEvent(DailyEvent.SelectDate(selectedDate))
        listState.scrollToItem(0)
        if (pendingDateSelection == selectedDate) {
            pendingDateSelection = null
        }
    }

    val doRefresh: () -> Unit = {
        scope.launch {
            isRefreshing = true
            onEvent(DailyEvent.Refresh)
            listState.scrollToItem(0)
            isRefreshing = false
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = Clock.System.now().toEpochMilliseconds(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showDatePicker = false
                    datePickerState.selectedDateMillis?.let { millis ->
                        pendingDateSelection = formatDailyDatePickerSelection(millis)
                    }
                }) { Text("确认") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "知乎日报",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                        if (currentViewingDate.isNotEmpty()) {
                            Text(
                                currentViewingDate,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                ),
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Filled.DateRange, contentDescription = "选择日期")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { scaffoldPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = doRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = scaffoldPadding.calculateTopPadding()),
        ) {
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "正在加载...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            )
                        }
                    }
                }

                state.error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(24.dp),
                        ) {
                            Text(
                                state.error.orEmpty(),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            TextButton(onClick = { onEvent(DailyEvent.Refresh) }) {
                                Text("重新加载")
                            }
                        }
                    }
                }

                state.sections.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "暂无内容",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp),
                    ) {
                        state.sections.forEach { section ->
                            item(key = "header_${section.date}") {
                                DailyDateHeader(date = formatDailyDate(section.date))
                            }
                            items(section.stories.size, key = { "story_${section.stories[it].id}" }) { index ->
                                DailyStoryCard(
                                    story = section.stories[index],
                                    onClick = { onStoryClick(section.stories[index].url) },
                                )
                            }
                        }

                        if (state.isLoadingMore) {
                            item(key = "loading_more") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun resolveViewingDate(
    firstVisibleItemIndex: Int,
    sections: List<DailySection>,
): String {
    var count = 0
    for (section in sections) {
        if (firstVisibleItemIndex < count + 1 + section.stories.size) {
            return formatDailyDate(section.date)
        }
        count += 1 + section.stories.size
    }
    return ""
}

@OptIn(ExperimentalTime::class)
private fun formatDailyDatePickerSelection(millis: Long): String {
    val date = Instant
        .fromEpochMilliseconds(millis)
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date
    return date.year.toString().padStart(4, '0') +
        (date.month.ordinal + 1).twoDigitString() +
        date.day.twoDigitString()
}
