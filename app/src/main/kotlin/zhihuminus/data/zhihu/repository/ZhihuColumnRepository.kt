package com.zhihuminus.data.zhihu.repository

import com.zhihuminus.data.FeedDisplayItem
import com.zhihuminus.data.toFeedDisplayItemNavDestinationJson
import com.zhihuminus.data.zhihu.api.ZhihuColumnApi
import com.zhihuminus.data.zhihu.dto.ColumnArticleDto
import com.zhihuminus.feature.column.ColumnArticleResult
import com.zhihuminus.feature.column.ColumnRepository
import com.zhihuminus.feature.post.PostType
import com.zhihuminus.navigation.PostDestination

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

    private fun ColumnArticleDto.toFeedDisplayItem(): FeedDisplayItem {
        val articleId = id.toLongOrNull() ?: 0L
        val nav = PostDestination(
            title = title,
            type = PostType.Article,
            id = articleId,
            authorName = author?.name ?: "未知作者",
            authorBio = author?.headline.orEmpty(),
            avatarSrc = author?.avatarUrl,
            excerpt = excerpt.orEmpty(),
        )
        return FeedDisplayItem(
            title = title,
            summary = excerpt.orEmpty().ifEmpty { excerptTitle.orEmpty() }.takeIf { it.isNotBlank() },
            details = "文章 · $voteupCount 赞 · $commentCount 评论",
            navDestinationJson = nav.toFeedDisplayItemNavDestinationJson(),
            avatarSrc = author?.avatarUrl,
            authorName = author?.name ?: "未知作者",
            contentTypeLabel = "文章",
            publishTimeSeconds = created.takeIf { it > 0 },
        )
    }
}
