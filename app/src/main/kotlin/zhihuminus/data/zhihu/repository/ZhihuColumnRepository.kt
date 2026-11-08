package com.zhihuminus.data.zhihu.repository

import com.zhihuminus.data.zhihu.api.ZhihuColumnApi
import com.zhihuminus.data.zhihu.api.ZhihuHistoryApi
import com.zhihuminus.data.zhihu.api.ZhihuPeopleApi
import com.zhihuminus.data.zhihu.mapper.toColumnDetail
import com.zhihuminus.data.zhihu.mapper.toFeedDisplayItem
import com.zhihuminus.feature.column.ColumnArticleResult
import com.zhihuminus.feature.column.ColumnDetail
import com.zhihuminus.feature.column.ColumnRepository

class ZhihuColumnRepository(
    private val api: ZhihuColumnApi,
    private val peopleApi: ZhihuPeopleApi,
    private val historyApi: ZhihuHistoryApi,
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

    override suspend fun getColumnDetail(columnId: String): ColumnDetail =
        api.getColumnDetail(columnId).toColumnDetail()

    override suspend fun recordRead(columnId: String) {
        historyApi.addHistory(contentToken = columnId, contentType = "column")
        api.reportRead(columnId)
    }

    override suspend fun setFollowing(columnId: String, following: Boolean) =
        api.setFollowing(columnId, following)

    override suspend fun setAuthorFollowing(urlToken: String, following: Boolean) =
        peopleApi.followMember(urlToken, following)
}
