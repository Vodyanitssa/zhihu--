package com.zhihuminus.feature.history

import com.zhihuminus.data.HistoryDeletePair
import com.zhihuminus.data.HistoryItem

interface HistoryRepository {
    suspend fun fetchPage(url: String): HistoryPageResult

    suspend fun deleteItem(pair: HistoryDeletePair)

    suspend fun clearAll()
}

data class HistoryPageResult(
    val items: List<HistoryItem>,
    val nextUrl: String?,
    val isEnd: Boolean,
)
