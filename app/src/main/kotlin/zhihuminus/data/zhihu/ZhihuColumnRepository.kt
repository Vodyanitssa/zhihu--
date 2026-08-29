package com.zhihuminus.data.zhihu

import com.zhihuminus.data.CommonFeed
import com.zhihuminus.data.Feed
import com.zhihuminus.data.FeedDisplayItem
import com.zhihuminus.data.Person
import com.zhihuminus.data.toDisplayItem
import com.zhihuminus.data.zhihu.dto.ColumnArticleDto
import com.zhihuminus.feature.column.ColumnArticleResult
import com.zhihuminus.feature.column.ColumnRepository

class ZhihuColumnRepository(
    private val api: ZhihuApi,
) : ColumnRepository {
    override suspend fun getColumnArticles(columnId: String, nextUrl: String?): ColumnArticleResult {
        val page = api.getColumnArticles(columnId, nextUrl)
        val articles = page.data.map { it.toFeedDisplayItem() }
        return ColumnArticleResult(
            articles = articles,
            nextUrl = page.paging.next,
            isEnd = page.paging.isEnd,
            totals = page.paging.totals,
        )
    }

    private fun ColumnArticleDto.toFeedDisplayItem(): FeedDisplayItem {
        val author = this.author
        val feedPerson = if (author != null) {
            Person(
                id = author.id,
                url = "/people/${author.urlToken}",
                userType = "people",
                urlToken = author.urlToken,
                name = author.name,
                headline = author.headline,
                avatarUrl = author.avatarUrl,
            )
        } else {
            Person(
                id = "",
                url = "",
                userType = "people",
                name = "未知作者",
                headline = "",
                avatarUrl = "",
            )
        }

        val articleTarget = Feed.ArticleTarget(
            id = this.id.toLongOrNull() ?: 0L,
            url = this.url,
            author = feedPerson,
            voteupCount = this.voteupCount,
            commentCount = this.commentCount,
            title = this.title,
            excerpt = this.excerpt.orEmpty(),
            created = this.created,
            updated = this.updated,
        )

        val feed = CommonFeed(
            id = this.id,
            verb = "column_article",
            createdTime = this.created,
            updatedTime = this.updated,
            target = articleTarget,
        )

        return feed.toDisplayItem()
    }
}
