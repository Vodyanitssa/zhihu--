package com.zhihuminus.data.zhihu.repository

import com.zhihuminus.data.zhihu.api.ZhihuHistoryApi
import com.zhihuminus.data.zhihu.dto.HistoryDeletePairDto
import com.zhihuminus.data.zhihu.dto.HistoryItemDto
import com.zhihuminus.feature.history.HistoryDeletePair
import com.zhihuminus.feature.history.HistoryItem
import com.zhihuminus.feature.history.HistoryPageResult
import com.zhihuminus.feature.history.HistoryRepository

/**
 * 在线浏览历史记录的 API 操作封装。
 *
 * 负责与知乎 `read_history` 系列接口交互，包括获取列表、删除单条和清空全部。
 * 将 API DTO 转换为业务层 [HistoryItem] 对象，ViewModel 不感知 API 细节。
 */
class ZhihuHistoryRepository(
    private val api: ZhihuHistoryApi,
) : HistoryRepository {
    override suspend fun fetchHistory(nextUrl: String?): HistoryPageResult {
        val url = nextUrl ?: DEFAULT_HISTORY_URL
        val page = api.fetchHistoryPage(url)
        return HistoryPageResult(
            items = page.items.map { it.toHistoryItem() },
            nextUrl = page.nextUrl,
            isEnd = page.isEnd,
        )
    }

    override suspend fun deleteItem(pair: HistoryDeletePair) {
        api.deleteHistoryItems(
            listOf(HistoryDeletePairDto(contentToken = pair.contentToken, contentType = pair.contentType)),
        )
    }

    override suspend fun clearAll() {
        api.clearHistory()
    }

    companion object {
        const val DEFAULT_HISTORY_URL = "https://api.zhihu.com/unify-consumption/read_history?offset=0&limit=10"
    }
}

private fun HistoryItemDto.toHistoryItem() = HistoryItem(
    title = data.header.title,
    summary = data.content?.summary ?: "",
    details = data.matrix
        ?.firstOrNull()
        ?.data
        ?.text ?: data.extra.contentType,
    authorName = data.content?.authorName,
    contentTypeLabel = when (data.extra.contentType) {
        "answer" -> "回答"
        "article" -> "文章"
        "pin" -> "想法"
        else -> data.extra.contentType
    },
    actionUrl = data.action.url,
    contentToken = data.extra.contentToken,
    contentType = data.extra.contentType,
)
