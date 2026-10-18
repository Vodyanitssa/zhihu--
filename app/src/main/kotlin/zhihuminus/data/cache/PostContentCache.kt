package com.zhihuminus.data.cache

import com.zhihuminus.core.content.AstParser.parseContent
import com.zhihuminus.data.zhihu.dto.AnswerTargetDto
import com.zhihuminus.data.zhihu.dto.ArticleTargetDto
import com.zhihuminus.data.zhihu.dto.FeedAuthorDto
import com.zhihuminus.data.zhihu.dto.FeedTargetDto
import com.zhihuminus.data.zhihu.dto.PinTargetDto
import com.zhihuminus.feature.post.Author
import com.zhihuminus.feature.post.Post
import com.zhihuminus.feature.post.PostLinkCard
import com.zhihuminus.feature.post.PostType
import com.zhihuminus.feature.post.VoteUpState
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock

/**
 * 内容详情强类型内存缓存契约。
 */
interface PostContentCache {
    suspend fun get(
        type: PostType,
        id: Long,
    ): Post?

    suspend fun put(post: Post)

    suspend fun putFromFeed(target: FeedTargetDto)

    suspend fun updateAuthorFollowing(
        urlTokenOrId: String,
        isFollowing: Boolean,
    )

    suspend fun clear()

    companion object : PostContentCache by MemoryPostContentCache()
}

/**
 * 结构化内存缓存实现，以 `${type.name}/$id` 为键，存储已完成领域映射与 AST 解析的 [Post] 实体。
 *
 * 避免了历史实现中频繁的 JsonObject 序列化/反序列化及重复 HTML AST 解析。
 */
class MemoryPostContentCache(
    internal var ttlMillis: Long = DEFAULT_TTL_MILLIS,
    internal var maxEntries: Int = DEFAULT_MAX_ENTRIES,
) : PostContentCache {
    private class Entry(
        val post: Post,
        val cachedAt: Long,
    )

    private val entries = LinkedHashMap<String, Entry>(16, 0.75f, true)
    private val mutex = Mutex()

    override suspend fun get(
        type: PostType,
        id: Long,
    ): Post? {
        val key = cacheKey(type, id)
        val now = Clock.System.now().toEpochMilliseconds()
        return mutex.withLock {
            val entry = entries[key] ?: return@withLock null
            if (now - entry.cachedAt >= ttlMillis) {
                entries.remove(key)
                null
            } else {
                entry.post
            }
        }
    }

    override suspend fun put(post: Post) {
        val key = cacheKey(post.type, post.id)
        val now = Clock.System.now().toEpochMilliseconds()
        mutex.withLock {
            entries[key] = Entry(post, now)
            evictLocked()
        }
    }

    override suspend fun putFromFeed(target: FeedTargetDto) {
        val post = target.toCachedPost() ?: return
        put(post)
    }

    override suspend fun updateAuthorFollowing(
        urlTokenOrId: String,
        isFollowing: Boolean,
    ) {
        if (urlTokenOrId.isBlank()) return
        mutex.withLock {
            val matching = entries.mapNotNull { (key, entry) ->
                val author = entry.post.author
                if (author.urlToken == urlTokenOrId || author.id == urlTokenOrId) {
                    key to entry
                } else {
                    null
                }
            }
            for ((key, entry) in matching) {
                val updatedPost = entry.post.copy(author = entry.post.author.copy(isFollowing = isFollowing))
                entries[key] = Entry(updatedPost, entry.cachedAt)
            }
        }
    }

    override suspend fun clear() = mutex.withLock { entries.clear() }

    private fun evictLocked() {
        val iterator = entries.entries.iterator()
        while (entries.size > maxEntries && iterator.hasNext()) {
            iterator.next()
            iterator.remove()
        }
    }

    private fun cacheKey(
        type: PostType,
        id: Long,
    ): String = "${type.name}/$id"

    companion object {
        internal const val DEFAULT_TTL_MILLIS = 30 * 60 * 1000L
        internal const val DEFAULT_MAX_ENTRIES = 100
    }
}

private fun FeedTargetDto.toCachedPost(): Post? = when (this) {
    is AnswerTargetDto -> {
        if (content.isBlank()) {
            null
        } else {
            val voteState = when (relationship?.voting) {
                1 -> VoteUpState.Up
                -1 -> VoteUpState.Down
                else -> VoteUpState.Neutral
            }
            Post(
                id = id,
                type = PostType.Answer,
                title = question.title,
                author = author.toAuthor(),
                content = parseContent(content),
                voteCount = voteupCount,
                commentCount = commentCount,
                voteState = voteState,
                isFaved = relationship?.isFollowing,
                createdAt = createdTime,
                updatedAt = updatedTime,
                ipInfo = ipInfo ?: author?.ipInfo,
                excerpt = excerpt.orEmpty(),
                questionId = question.id,
            )
        }
    }

    is ArticleTargetDto -> {
        if (content.isBlank()) {
            null
        } else {
            val voteState = when (voting) {
                1 -> VoteUpState.Up
                -1 -> VoteUpState.Down
                else -> VoteUpState.Neutral
            }
            Post(
                id = id,
                type = PostType.Article,
                title = title,
                author = author.toAuthor(),
                content = parseContent(content),
                voteCount = voteupCount,
                commentCount = commentCount,
                voteState = voteState,
                isFaved = null,
                createdAt = createdTime,
                updatedAt = updatedTime,
                ipInfo = ipInfo ?: author?.ipInfo,
                excerpt = excerpt,
                coverUrl = imageUrl?.takeIf { it.isNotBlank() },
            )
        }
    }

    is PinTargetDto -> {
        if (contentHtml.isBlank()) {
            null
        } else {
            val author = author.toAuthor()
            val firstP = contentHtml.indexOf("<p>")
            val titleText = if (firstP > 0 && contentHtml.startsWith("<div>")) {
                contentHtml
                    .substring("<div>".length, firstP)
                    .trimEnd(' ', '|')
                    .takeIf { it.isNotBlank() }
            } else {
                null
            }
            val parsed = parseContent(if (titleText != null) contentHtml.removeRange(0, firstP) else contentHtml)
            val linkCards = content
                .filter { it.type == "link_card" && !it.url.isNullOrBlank() }
                .map { item ->
                    PostLinkCard(
                        dataContentId = item.dataContentId.orEmpty(),
                        dataContentType = item.dataContentType.orEmpty(),
                        url = item.url!!,
                    )
                }
            Post(
                id = id,
                type = PostType.Pin,
                title = titleText ?: "${author.name}的想法",
                author = author,
                content = parsed,
                voteCount = likeCount,
                commentCount = commentCount,
                voteState = VoteUpState.Neutral,
                createdAt = created,
                updatedAt = updated,
                ipInfo = ipInfo ?: author.ipInfo,
                excerpt = excerptTitle,
                linkCards = linkCards,
            )
        }
    }

    else -> null
}

private fun FeedAuthorDto?.toAuthor(): Author = if (this == null) {
    Author(id = "", name = "知乎用户", avatarUrl = "")
} else {
    Author(
        id = id,
        name = name,
        headline = headline,
        avatarUrl = avatarUrl,
        urlToken = urlToken.orEmpty(),
        badgeText = badgeV2?.detailBadges?.firstOrNull()?.description ?: badge?.firstOrNull()?.description,
        isFollowing = isFollowing,
        ipInfo = ipInfo,
    )
}
