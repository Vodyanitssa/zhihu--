package com.zhihuminus.feature.column

import com.zhihuminus.data.FeedDisplayItem

interface ColumnRepository {
    /**
     * 获取专栏文章列表
     * @param columnId 专栏 ID
     * @param nextUrl 分页续页 URL（为 null 时从第一页开始）
     * @return 文章列表 + 分页信息
     */
    suspend fun getColumnArticles(columnId: String, nextUrl: String?): ColumnArticleResult
}

data class ColumnArticleResult(
    val articles: List<FeedDisplayItem>,
    val nextUrl: String?,
    val isEnd: Boolean,
    val totals: Int,
)
