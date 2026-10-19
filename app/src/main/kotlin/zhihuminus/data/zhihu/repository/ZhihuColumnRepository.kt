package com.zhihuminus.data.zhihu.repository

import com.zhihuminus.data.zhihu.api.ZhihuColumnApi
import com.zhihuminus.data.zhihu.mapper.toFeedDisplayItem
import com.zhihuminus.feature.column.ColumnArticleResult
import com.zhihuminus.feature.column.ColumnRepository

class ZhihuColumnRepository(
    private val api: ZhihuColumnApi,
) : ColumnRepository {
    override suspend fun getColumnArticles(columnId: String, nextUrl: String?): ColumnArticleResult {
        val page = api.getColumnArticles(columnId, nextUrl)
        val articles = page.data.map { it.toFeedDisplayItem() }
        return ColumnArticleResult(
            articles = articles,
            nextUrl = page.paging.nextUrl,
            isEnd = !page.paging.hasMore,
            totals = page.paging.totals,
        )
    }
}
