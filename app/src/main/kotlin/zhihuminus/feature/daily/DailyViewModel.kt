package com.zhihuminus.feature.daily

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuminus.core.util.Log
import com.zhihuminus.core.util.friendlyErrorMessage
import com.zhihuminus.data.zhihu.dto.DailyStoriesResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

data class DailyUiState(
    val sections: List<DailySection> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
)

class DailyViewModel(
    private val repository: DailyRepository,
) : ViewModel() {
    var uiState by mutableStateOf(DailyUiState())
        private set

    private val _effect = Channel<DailyEffect>(capacity = Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var loadJob: Job? = null
    private var nextDate: String? = null

    fun onEvent(event: DailyEvent) {
        when (event) {
            is DailyEvent.Refresh -> loadLatest()
            is DailyEvent.LoadMore -> loadMore()
            is DailyEvent.SelectDate -> loadDate(event.date)
        }
    }

    private fun loadLatest() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                uiState = uiState.copy(isLoading = true, error = null)
                val data: DailyStoriesResponse = repository.getLatestDaily()
                uiState = uiState.copy(
                    sections = if (data.stories.isEmpty()) emptyList() else listOf(DailySection(data.date, data.stories)),
                    isLoading = false,
                )
                nextDate = data.date
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("DailyViewModel", "Failed to load latest daily", e)
                uiState = uiState.copy(
                    isLoading = false,
                    error = "加载失败: ${friendlyErrorMessage(e)}",
                )
            }
        }
    }

    private fun loadDate(date: String) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                uiState = uiState.copy(isLoading = true, sections = emptyList(), error = null)
                val nextApiDate = LocalDate
                    .parse("${date.substring(0, 4)}-${date.substring(4, 6)}-${date.substring(6, 8)}")
                    .plus(1, DateTimeUnit.DAY)
                    .toString()
                    .replace("-", "")
                val data: DailyStoriesResponse = repository.getDailyStoriesBefore(nextApiDate)
                uiState = uiState.copy(
                    sections = if (data.stories.isEmpty()) emptyList() else listOf(DailySection(data.date, data.stories)),
                    isLoading = false,
                )
                nextDate = data.date
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("DailyViewModel", "Failed to load daily for date $date", e)
                uiState = uiState.copy(
                    isLoading = false,
                    error = "加载失败: ${friendlyErrorMessage(e)}",
                )
            }
        }
    }

    private fun loadMore() {
        val date = nextDate ?: return
        if (uiState.isLoadingMore) return

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                uiState = uiState.copy(isLoadingMore = true)
                val data: DailyStoriesResponse = repository.getDailyStoriesBefore(date)
                if (data.stories.isNotEmpty()) {
                    uiState = uiState.copy(
                        sections = uiState.sections + DailySection(data.date, data.stories),
                    )
                }
                nextDate = data.date
                uiState = uiState.copy(isLoadingMore = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("DailyViewModel", "Failed to load more daily stories", e)
                uiState = uiState.copy(isLoadingMore = false)
                _effect.send(DailyEffect.ShowMessage("加载更多失败: ${friendlyErrorMessage(e)}"))
            }
        }
    }
}
