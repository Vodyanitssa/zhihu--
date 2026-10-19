package com.zhihuminus.data.zhihu.mapper

import com.zhihuminus.data.FeedDisplayItem
import com.zhihuminus.data.FeedImageLayout
import com.zhihuminus.data.toFeedDisplayItemNavDestinationJson
import com.zhihuminus.data.zhihu.dto.ColumnArticleDto
import com.zhihuminus.feature.post.PostType
import com.zhihuminus.navigation.PostDestination

/**
 * 知乎专栏文章数据模型映射器。
 */
object ColumnMapper {
    fun toFeedDisplayItem(dto: ColumnArticleDto): FeedDisplayItem {
        val articleId = dto.id.toLongOrNull() ?: 0L
        val nav = PostDestination(
            title = dto.title,
            type = PostType.Article,
            id = articleId,
            authorName = dto.author?.name ?: "未知作者",
            authorBio = dto.author?.headline.orEmpty(),
            avatarSrc = dto.author?.avatarUrl,
            excerpt = dto.excerpt.orEmpty(),
        )
        val titleImg = dto.titleImage?.takeIf { it.isNotBlank() }
        val images = listOfNotNull(titleImg)
        return FeedDisplayItem(
            title = dto.title,
            summary = dto.excerpt
                .orEmpty()
                .ifEmpty { dto.excerptTitle.orEmpty() }
                .takeIf { it.isNotBlank() },
            details = "文章 · ${dto.voteupCount} 赞 · ${dto.commentCount} 评论",
            navDestinationJson = nav.toFeedDisplayItemNavDestinationJson(),
            avatarSrc = dto.author?.avatarUrl,
            authorName = dto.author?.name ?: "未知作者",
            contentTypeLabel = "文章",
            publishTimeSeconds = dto.created.takeIf { it > 0 },
            images = images,
            imageLayout = if (images.isNotEmpty()) FeedImageLayout.RIGHT_THUMBNAIL else FeedImageLayout.NONE,
        )
    }
}

fun ColumnArticleDto.toFeedDisplayItem(): FeedDisplayItem = ColumnMapper.toFeedDisplayItem(this)
