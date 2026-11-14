package com.zhihuminus.data.zhihu

import com.zhihuminus.data.zhihu.dto.VideoInfoDto
import com.zhihuminus.data.zhihu.dto.VideoPlaylistDto
import com.zhihuminus.data.zhihu.dto.VideoPlaylistItemDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ZhihuVideoHelperTest {
    @Test
    fun selectBestVideoUrlPrefersHighestBitrate() {
        val video = VideoInfoDto(
            playlist = VideoPlaylistDto(
                fhd = VideoPlaylistItemDto(url = "https://example.com/fhd.mp4", bitrate = 724),
                hd = VideoPlaylistItemDto(url = "https://example.com/hd.mp4", bitrate = 429),
                ld = VideoPlaylistItemDto(url = "https://example.com/ld.mp4", bitrate = 273),
            ),
        )

        assertEquals("https://example.com/fhd.mp4", selectBestVideoUrl(video))
    }

    @Test
    fun selectBestVideoUrlSkipsBlankUrl() {
        val video = VideoInfoDto(
            playlist = VideoPlaylistDto(
                fhd = VideoPlaylistItemDto(url = "", bitrate = 724),
                hd = VideoPlaylistItemDto(url = "https://example.com/hd.mp4", bitrate = 429),
            ),
        )

        assertEquals("https://example.com/hd.mp4", selectBestVideoUrl(video))
    }

    @Test
    fun selectBestVideoUrlReturnsNullWithoutPlaylist() {
        assertNull(selectBestVideoUrl(null))
        assertNull(selectBestVideoUrl(VideoInfoDto()))
        assertNull(selectBestVideoUrl(VideoInfoDto(playlist = VideoPlaylistDto())))
    }
}
