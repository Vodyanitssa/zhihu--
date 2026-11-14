package com.zhihuminus.data.cache

import com.zhihuminus.navigation.VideoKind
import kotlin.time.Clock

interface VideoUrlCache {
    fun get(kind: VideoKind, id: Long): String?

    fun put(kind: VideoKind, id: Long, url: String?): Boolean

    fun clear()

    companion object : VideoUrlCache by MemoryVideoUrlCache()
}

class MemoryVideoUrlCache(
    private val nowMillis: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    internal var defaultTtlMillis: Long = DEFAULT_TTL_MILLIS,
    internal var maxEntries: Int = DEFAULT_MAX_ENTRIES,
) : VideoUrlCache {
    private class Entry(
        val url: String,
        val expiresAtMillis: Long,
    )

    private val entries = LinkedHashMap<String, Entry>(16, 0.75f, true)
    private val lock = Any()

    override fun get(kind: VideoKind, id: Long): String? = synchronized(lock) {
        val key = cacheKey(kind, id)
        val entry = entries[key] ?: return@synchronized null
        if (nowMillis() >= entry.expiresAtMillis) {
            entries.remove(key)
            return@synchronized null
        }
        entry.url
    }

    override fun put(kind: VideoKind, id: Long, url: String?): Boolean {
        val normalized = url?.takeIf { it.isNotBlank() } ?: return false
        val now = nowMillis()
        val expirationSeconds = videoUrlExpirationSeconds(normalized)
        val expiresAt = expirationSeconds?.times(1000L) ?: (now + defaultTtlMillis)
        if (expirationSeconds != null && expiresAt - EXPIRY_SAFETY_MARGIN_MILLIS <= now) return false
        if (expiresAt <= now) return false
        synchronized(lock) {
            entries[cacheKey(kind, id)] = Entry(normalized, expiresAt)
            evictLocked()
        }
        return true
    }

    override fun clear() {
        synchronized(lock) { entries.clear() }
    }

    private fun evictLocked() {
        val iterator = entries.entries.iterator()
        while (entries.size > maxEntries && iterator.hasNext()) {
            iterator.next()
            iterator.remove()
        }
    }

    private fun cacheKey(kind: VideoKind, id: Long): String = "${kind.name}:$id"

    companion object {
        internal const val DEFAULT_TTL_MILLIS = 10 * 60 * 1000L
        internal const val DEFAULT_MAX_ENTRIES = 128
        internal const val EXPIRY_SAFETY_MARGIN_MILLIS = 60 * 1000L
    }
}

internal fun videoUrlExpiresAtMillis(
    url: String,
    nowMillis: Long,
    defaultTtlMillis: Long,
): Long = videoUrlExpirationSeconds(url)?.times(1000L) ?: (nowMillis + defaultTtlMillis)

internal fun videoUrlExpirationSeconds(url: String): Long? =
    url
        .substringAfter("expiration=", "")
        .substringBefore('&')
        .toLongOrNull()
        ?.takeIf { it > 0 }
