package com.zhihuminus.data.cache

import com.zhihuminus.feature.post.PostType
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import kotlin.time.Clock

/**
 * 内容详情内存缓存，以 canonical 详情 URL（无 query）为 key。
 *
 * 写入来源：
 * - [ZhihuApiImpl.fetchFeedPage] 预热的 feed target（回答/文章，含全文 HTML）
 * - [com.zhihuminus.data.zhihu.ZhihuPostRepository.getPost] 网络详情回写
 *
 * feed target 的赞同状态在 `relationship.voting`（回答）或顶层 `voting`（文章，1/0/-1），
 * 与详情接口的 `reaction.relation.vote`（"up"/"neutral"/"down"）不同，
 * 统一在写入时归一化为后者，读取方无感知。
 */
object PostContentCache {
    private class Entry(
        val payload: JsonObject,
        val cachedAt: Long,
    )

    private val entries = LinkedHashMap<String, Entry>(16, 0.75f, true)
    private val mutex = Mutex()

    internal var ttlMillis = DEFAULT_TTL_MILLIS
    internal var maxEntries = DEFAULT_MAX_ENTRIES

    suspend fun get(
        type: PostType,
        id: Long,
    ): JsonObject? {
        val key = cacheKey(type, id)
        val now = Clock.System.now().toEpochMilliseconds()
        return mutex.withLock {
            val entry = entries[key] ?: return@withLock null
            if (now - entry.cachedAt >= ttlMillis) {
                entries.remove(key)
                null
            } else {
                entry.payload
            }
        }
    }

    suspend fun put(
        type: PostType,
        id: Long,
        payload: JsonObject,
    ) {
        val key = cacheKey(type, id)
        val now = Clock.System.now().toEpochMilliseconds()
        mutex.withLock {
            entries[key] = Entry(normalizeVote(payload, type), now)
            evictLocked()
        }
    }

    /**
     * 从 feed 条目的 `target` JsonObject 预热缓存。
     * 仅处理带全文的回答/文章；想法等内容形态不匹配，跳过。
     */
    suspend fun putFromFeedTarget(target: JsonObject) {
        val type = when ((target["type"] as? JsonPrimitive)?.contentOrNull) {
            "answer" -> PostType.Answer
            "article" -> PostType.Article
            else -> return
        }
        val id = (target["id"] as? JsonPrimitive)?.longOrNull ?: return
        if ((target["content"] as? JsonPrimitive)?.contentOrNull.isNullOrBlank()) return
        put(type, id, target)
    }

    suspend fun clear() = mutex.withLock { entries.clear() }

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
    ): String = when (type) {
        PostType.Answer -> "https://www.zhihu.com/api/v4/answers/$id"
        PostType.Article -> "https://www.zhihu.com/api/v4/articles/$id"
        PostType.Pin -> "https://www.zhihu.com/api/v4/pins/$id"
    }

    /**
     * 把 feed 形态的赞同状态归一化为详情接口形态：
     * `reaction.relation.vote` = "up"/"neutral"/"down"（1/0/-1）。
     * 已有 `vote` 的 payload 原样返回；无法取得 voting 时保持原样。
     */
    private fun normalizeVote(
        payload: JsonObject,
        type: PostType,
    ): JsonObject {
        val reaction = payload["reaction"] as? JsonObject ?: return payload
        val relation = reaction["relation"] as? JsonObject ?: return payload
        if ("vote" in relation) return payload
        val voting = when (type) {
            PostType.Answer -> votingInt((payload["relationship"] as? JsonObject)?.get("voting"))
            PostType.Article -> votingInt(payload["voting"])
            PostType.Pin -> null
        } ?: return payload
        val vote = when (voting) {
            1 -> "up"
            -1 -> "down"
            else -> "neutral"
        }
        val newRelation = JsonObject(relation + ("vote" to JsonPrimitive(vote)))
        return JsonObject(payload + ("reaction" to JsonObject(reaction + ("relation" to newRelation))))
    }

    private fun votingInt(element: JsonElement?): Int? = (element as? JsonPrimitive)?.intOrNull

    internal const val DEFAULT_TTL_MILLIS = 30 * 60 * 1000L
    internal const val DEFAULT_MAX_ENTRIES = 100
}
