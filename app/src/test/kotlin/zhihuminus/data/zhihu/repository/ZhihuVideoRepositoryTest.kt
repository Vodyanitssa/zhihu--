package com.zhihuminus.data.zhihu.repository

import com.zhihuminus.core.environment.ZhihuApiEnvironment
import com.zhihuminus.data.cache.MemoryVideoUrlCache
import com.zhihuminus.data.zhihu.api.ZhihuVideoApi
import com.zhihuminus.data.zhihu.dto.VideoInfoDto
import com.zhihuminus.data.zhihu.dto.VideoPlaylistDto
import com.zhihuminus.data.zhihu.dto.VideoPlaylistItemDto
import com.zhihuminus.data.zhihu.dto.VideoTargetDto
import com.zhihuminus.data.zhihu.dto.ZVideoDto
import com.zhihuminus.navigation.Video
import com.zhihuminus.navigation.VideoKind
import io.ktor.client.HttpClient
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ZhihuVideoRepositoryTest {
    private val fhdUrl = "https://example.com/fhd.mp4?expiration=9999999999"
    private val hdUrl = "https://example.com/hd.mp4?expiration=9999999999"

    private class FakeEnvironment : ZhihuApiEnvironment {
        override fun httpClient(): HttpClient = error("not used")

        override fun authenticatedCookies(): Map<String, String> = emptyMap()

        override suspend fun handleFetchFailure(tag: String?, error: Exception) = Unit
    }

    private class FakeVideoApi(
        private val response: ZVideoDto?,
    ) : ZhihuVideoApi(FakeEnvironment()) {
        var calls = 0

        override suspend fun fetchZVideo(videoId: String): ZVideoDto {
            calls++
            return response ?: error("no response")
        }
    }

    private fun videoTarget(): VideoTargetDto = VideoTargetDto(
        id = 7L,
        video = VideoInfoDto(
            playlist = VideoPlaylistDto(
                fhd = VideoPlaylistItemDto(url = fhdUrl, bitrate = 724),
                hd = VideoPlaylistItemDto(url = hdUrl, bitrate = 429),
            ),
        ),
    )

    private fun zvideo(): ZVideoDto = ZVideoDto(
        id = "7",
        video = videoTarget().video,
    )

    @Test
    fun prewarmCachesBestUrl() {
        val cache = MemoryVideoUrlCache(nowMillis = { 0L })
        val repository = ZhihuVideoRepository(FakeVideoApi(zvideo()), cache = cache)

        repository.prewarm(videoTarget())

        assertEquals(fhdUrl, cache.get(VideoKind.ZVIDEO, 7L))
    }

    @Test
    fun resolveUsesCacheBeforeNetwork() = runTest {
        val cache = MemoryVideoUrlCache(nowMillis = { 0L })
        cache.put(VideoKind.ZVIDEO, 7L, fhdUrl)
        val api = FakeVideoApi(zvideo())
        val repository = ZhihuVideoRepository(api, cache = cache)

        assertEquals(fhdUrl, repository.resolve(Video(7L), null))
        assertEquals(0, api.calls)
    }

    @Test
    fun resolveFetchesAndCachesOnMiss() = runTest {
        val cache = MemoryVideoUrlCache(nowMillis = { 0L })
        val api = FakeVideoApi(zvideo())
        val repository = ZhihuVideoRepository(api, cache = cache)

        assertEquals(fhdUrl, repository.resolve(Video(7L, VideoKind.ZVIDEO), null))
        assertEquals(1, api.calls)
        assertEquals(fhdUrl, cache.get(VideoKind.ZVIDEO, 7L))
    }

    @Test
    fun unknownKindUsesAttachmentFetcher() = runTest {
        val cache = MemoryVideoUrlCache(nowMillis = { 0L })
        val repository = ZhihuVideoRepository(
            videoApi = FakeVideoApi(zvideo()),
            attachmentFetcher = { attachment ->
                "https://example.com/${attachment.mediaId}.mp4?expiration=9999999999"
            },
            cache = cache,
        )
        val attachment = VideoAttachment(mediaId = "55", contentId = "66", contentType = "answer")

        assertEquals(
            "https://example.com/55.mp4?expiration=9999999999",
            repository.resolve(Video(55L, VideoKind.UNKNOWN), attachment),
        )
    }

    @Test
    fun unknownKindWithoutAttachmentOrFetcherReturnsNull() = runTest {
        val cache = MemoryVideoUrlCache(nowMillis = { 0L })
        val repository = ZhihuVideoRepository(FakeVideoApi(zvideo()), cache = cache)

        assertNull(repository.resolve(Video(55L, VideoKind.UNKNOWN), null))
    }
}
