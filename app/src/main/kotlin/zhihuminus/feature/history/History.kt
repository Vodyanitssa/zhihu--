package com.zhihuminus.feature.history

data class HistoryDeletePair(
    val contentToken: String,
    val contentType: String,
)

/**
 * 在线浏览历史记录的业务模型。
 *
 * 由 [com.zhihuminus.data.zhihu.ZhihuHistoryRepository] 从 API DTO 解析而来，
 * Feature 层只依赖此类型，不感知 API 细节。
 */
data class HistoryItem(
    val title: String,
    val summary: String,
    val details: String,
    val authorName: String?,
    val contentTypeLabel: String,
    val actionUrl: String,
    val contentToken: String,
    val contentType: String,
)

data class HistoryPageResult(
    val items: List<HistoryItem>,
    val nextUrl: String?,
    val isEnd: Boolean,
)

interface HistoryRepository {
    suspend fun fetchHistory(nextUrl: String? = null): HistoryPageResult

    suspend fun deleteItem(pair: HistoryDeletePair)

    suspend fun clearAll()
}
