package com.zhihuminus.data.cache

import com.zhihuminus.data.zhihu.dto.FeedTargetDto
import com.zhihuminus.data.zhihu.mapper.toPost
import com.zhihuminus.feature.post.Post
import com.zhihuminus.feature.post.PostType
import com.zhihuminus.feature.post.mergeFallbackFrom
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

    suspend fun putAndMerge(post: Post): Post

    suspend fun putFromFeed(target: FeedTargetDto)

    suspend fun getAuthorFollowing(urlTokenOrId: String): Boolean?

    suspend fun updateAuthorFollowing(
        urlTokenOrId: String,
        isFollowing: Boolean,
    )

    /**
     * 读取上次离开该内容时的阅读进度（0..1）。未命中或条目已过期返回 null。
     */
    suspend fun getReadingProgress(
        type: PostType,
        id: Long,
    ): Float?

    /**
     * 记录阅读进度（0..1）。仅在条目存在且未过期时生效，不会延长内容缓存寿命。
     */
    suspend fun saveReadingProgress(
        type: PostType,
        id: Long,
        progress: Float,
    )

    /**
     * 清除阅读进度（视为读完）。
     */
    suspend fun clearReadingProgress(
        type: PostType,
        id: Long,
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
    internal var relationTtlMillis: Long = DEFAULT_RELATION_TTL_MILLIS,
) : PostContentCache {
    private class Entry(
        val post: Post,
        val cachedAt: Long,
        val readingProgress: Float = 0f,
    )

    private class RelationEntry(
        val isFollowing: Boolean,
        val cachedAt: Long,
    )

    private val entries = LinkedHashMap<String, Entry>(16, 0.75f, true)
    private val relations = HashMap<String, RelationEntry>()
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
                applyKnownRelationLocked(entry.post, now)
            }
        }
    }

    override suspend fun put(post: Post) {
        putAndMerge(post)
    }

    override suspend fun putAndMerge(post: Post): Post {
        val key = cacheKey(post.type, post.id)
        val now = Clock.System.now().toEpochMilliseconds()
        return mutex.withLock {
            val existingEntry = entries[key]?.takeIf { now - it.cachedAt < ttlMillis }
            val existing = existingEntry?.post
            val merged = if (existing != null) post.mergeFallbackFrom(existing) else applyKnownRelationLocked(post, now)
            entries[key] = Entry(merged, now, existingEntry?.readingProgress ?: 0f)
            evictLocked()
            rememberRelationLocked(merged.author.urlToken, merged.author.id, merged.author.isFollowing, now)
            merged
        }
    }

    override suspend fun putFromFeed(target: FeedTargetDto) {
        val post = target.toPost() ?: return
        putAndMerge(post)
    }

    override suspend fun getAuthorFollowing(urlTokenOrId: String): Boolean? {
        if (urlTokenOrId.isBlank()) return null
        val now = Clock.System.now().toEpochMilliseconds()
        return mutex.withLock { knownRelationLocked(urlTokenOrId, now) }
    }

    override suspend fun updateAuthorFollowing(
        urlTokenOrId: String,
        isFollowing: Boolean,
    ) {
        if (urlTokenOrId.isBlank()) return
        val now = Clock.System.now().toEpochMilliseconds()
        mutex.withLock {
            relations[urlTokenOrId] = RelationEntry(isFollowing, now)
            val matching = entries.mapNotNull { (key, entry) ->
                val author = entry.post.author
                if (author.urlToken == urlTokenOrId || author.id == urlTokenOrId) {
                    key to entry
                } else {
                    null
                }
            }
            for ((key, entry) in matching) {
                val author = entry.post.author
                val updatedPost = entry.post.copy(author = author.copy(isFollowing = isFollowing))
                entries[key] = Entry(updatedPost, entry.cachedAt, entry.readingProgress)
                rememberRelationLocked(author.urlToken, author.id, isFollowing, now)
            }
        }
    }

    override suspend fun getReadingProgress(
        type: PostType,
        id: Long,
    ): Float? {
        val key = cacheKey(type, id)
        val now = Clock.System.now().toEpochMilliseconds()
        return mutex.withLock {
            val entry = entries[key] ?: return@withLock null
            if (now - entry.cachedAt >= ttlMillis) {
                entries.remove(key)
                null
            } else {
                entry.readingProgress
            }
        }
    }

    override suspend fun saveReadingProgress(
        type: PostType,
        id: Long,
        progress: Float,
    ) {
        if (!progress.isFinite()) return
        val clamped = progress.coerceIn(0f, 1f)
        val key = cacheKey(type, id)
        val now = Clock.System.now().toEpochMilliseconds()
        mutex.withLock {
            val entry = entries[key] ?: return@withLock
            if (now - entry.cachedAt >= ttlMillis) {
                entries.remove(key)
                return@withLock
            }
            entries[key] = Entry(entry.post, entry.cachedAt, clamped)
        }
    }

    override suspend fun clearReadingProgress(
        type: PostType,
        id: Long,
    ) {
        saveReadingProgress(type, id, 0f)
    }

    override suspend fun clear() = mutex.withLock {
        entries.clear()
        relations.clear()
    }

    private fun applyKnownRelationLocked(
        post: Post,
        now: Long,
    ): Post {
        if (post.author.isFollowing != null) return post
        val known = knownRelationLocked(post.author.urlToken, now)
            ?: knownRelationLocked(post.author.id, now)
            ?: return post
        return post.copy(author = post.author.copy(isFollowing = known))
    }

    private fun knownRelationLocked(
        tokenOrId: String,
        now: Long,
    ): Boolean? {
        if (tokenOrId.isBlank()) return null
        val entry = relations[tokenOrId] ?: return null
        if (now - entry.cachedAt >= relationTtlMillis) {
            relations.remove(tokenOrId)
            return null
        }
        return entry.isFollowing
    }

    private fun rememberRelationLocked(
        urlToken: String,
        id: String,
        isFollowing: Boolean?,
        now: Long,
    ) {
        if (isFollowing == null) return
        val entry = RelationEntry(isFollowing, now)
        if (urlToken.isNotBlank()) relations[urlToken] = entry
        if (id.isNotBlank()) relations[id] = entry
    }

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
        internal const val DEFAULT_RELATION_TTL_MILLIS = 30 * 60 * 1000L
        internal const val DEFAULT_MAX_ENTRIES = 100
    }
}
