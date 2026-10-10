package com.zhihuminus.data.zhihu.api

import com.zhihuminus.data.zhihu.dto.HistoryDeletePairDto
import com.zhihuminus.data.zhihu.dto.HistoryPage

interface ZhihuHistoryApi {
    /**
     * 记录阅读历史（read_history/add）
     * @param contentToken 内容 token（数字 ID 字符串）
     * @param contentType 内容类型: "answer", "article", "pin"
     */
    suspend fun addHistory(contentToken: String, contentType: String)

    /**
     * 标记已读（lastread/touch）
     * @param contentToken 内容 token（数字 ID 字符串）
     * @param contentType 内容类型: "answer", "article", "pin"
     */
    suspend fun markAsRead(contentToken: String, contentType: String)

    /**
     * 获取一页在线浏览历史记录。
     * @param url 分页 URL（首页或续页）
     */
    suspend fun fetchHistoryPage(url: String): HistoryPage

    /**
     * 批量删除在线浏览历史记录。
     * @param pairs 要删除的记录标识列表
     */
    suspend fun deleteHistoryItems(pairs: List<HistoryDeletePairDto>)

    /**
     * 清空全部在线浏览历史记录。
     */
    suspend fun clearHistory()
}
