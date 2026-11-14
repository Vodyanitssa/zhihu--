package com.zhihuminus.data.cache

import com.zhihuminus.navigation.VideoKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VideoUrlCacheTest {
    @Test
    fun putAndGetUnexpiredUrl() {
        val now = 1_000_000L
        val cache = MemoryVideoUrlCache(nowMillis = { now })
        val url = "https://example.com/fhd.mp4?expiration=${now / 1000 + 3600}"

        assertTrue(cache.put(VideoKind.ZVIDEO, 1L, url))
        assertEquals(url, cache.get(VideoKind.ZVIDEO, 1L))
    }

    @Test
    fun expiredUrlIsRemoved() {
        var now = 1_000_000L
        val cache = MemoryVideoUrlCache(nowMillis = { now })
        val url = "https://example.com/fhd.mp4?expiration=${now / 1000 + 120}"
        assertTrue(cache.put(VideoKind.ZVIDEO, 1L, url))

        now += 121_000
        assertNull(cache.get(VideoKind.ZVIDEO, 1L))
    }

    @Test
    fun urlWithoutExpirationUsesDefaultTtl() {
        var now = 0L
        val cache = MemoryVideoUrlCache(
            nowMillis = { now },
            defaultTtlMillis = 60_000,
        )
        assertTrue(cache.put(VideoKind.ZVIDEO, 1L, "https://example.com/fhd.mp4"))
        assertEquals("https://example.com/fhd.mp4", cache.get(VideoKind.ZVIDEO, 1L))

        now = 61_000
        assertNull(cache.get(VideoKind.ZVIDEO, 1L))
    }

    @Test
    fun rejectsBlankOrExpiredUrl() {
        val now = 1_000_000L
        val cache = MemoryVideoUrlCache(nowMillis = { now })

        assertFalse(cache.put(VideoKind.ZVIDEO, 1L, null))
        assertFalse(cache.put(VideoKind.ZVIDEO, 1L, ""))
        assertFalse(cache.put(VideoKind.ZVIDEO, 1L, "https://x?expiration=${now / 1000 - 1}"))
        assertFalse(cache.put(VideoKind.ZVIDEO, 1L, "https://x?expiration=${now / 1000 + 30}"))
    }

    @Test
    fun evictsLeastRecentlyUsed() {
        val now = 1_000_000L
        val cache = MemoryVideoUrlCache(nowMillis = { now }, maxEntries = 2)
        val url = "https://example.com/v.mp4?expiration=${now / 1000 + 3600}"

        assertTrue(cache.put(VideoKind.ZVIDEO, 1L, url))
        assertTrue(cache.put(VideoKind.ZVIDEO, 2L, url))
        assertTrue(cache.put(VideoKind.ZVIDEO, 3L, url))

        assertNull(cache.get(VideoKind.ZVIDEO, 1L))
        assertEquals(url, cache.get(VideoKind.ZVIDEO, 2L))
        assertEquals(url, cache.get(VideoKind.ZVIDEO, 3L))
    }

    @Test
    fun expirationIsParsedFromUrl() {
        assertEquals(1_234_000L, videoUrlExpiresAtMillis("https://x?expiration=1234&a=1", 0, 10))
        assertEquals(10L, videoUrlExpiresAtMillis("https://x?foo=1", 0, 10))
    }
}
