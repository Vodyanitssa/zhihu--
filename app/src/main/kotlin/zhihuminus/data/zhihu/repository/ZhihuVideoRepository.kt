package com.zhihuminus.data.zhihu.repository

import com.zhihuminus.data.cache.VideoUrlCache
import com.zhihuminus.data.zhihu.api.ZhihuVideoApi
import com.zhihuminus.data.zhihu.dto.FeedTargetDto
import com.zhihuminus.data.zhihu.dto.VideoTargetDto
import com.zhihuminus.data.zhihu.selectBestVideoUrl
import com.zhihuminus.navigation.Video
import com.zhihuminus.navigation.VideoKind

data class VideoAttachment(
    val mediaId: String,
    val contentId: String,
    val contentType: String,
)

interface VideoRepository {
    fun prewarm(target: FeedTargetDto)

    suspend fun resolve(video: Video, attachment: VideoAttachment?): String?
}

class ZhihuVideoRepository(
    private val videoApi: ZhihuVideoApi,
    private val attachmentFetcher: (suspend (VideoAttachment) -> String?)? = null,
    private val cache: VideoUrlCache = VideoUrlCache,
) : VideoRepository {
    override fun prewarm(target: FeedTargetDto) {
        val videoTarget = target as? VideoTargetDto ?: return
        val url = selectBestVideoUrl(videoTarget.video) ?: return
        cache.put(VideoKind.ZVIDEO, videoTarget.id, url)
    }

    override suspend fun resolve(video: Video, attachment: VideoAttachment?): String? {
        cache.get(video.kind, video.id)?.let { return it }
        val url = when (video.kind) {
            VideoKind.ZVIDEO -> selectBestVideoUrl(videoApi.fetchZVideo(video.id.toString()).video)
            VideoKind.UNKNOWN -> if (attachment != null) attachmentFetcher?.invoke(attachment) else null
        }
        if (url != null) {
            cache.put(video.kind, video.id, url)
        }
        return url
    }
}
