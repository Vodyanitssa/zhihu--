package com.zhihuminus.data.zhihu.dto

import com.zhihuminus.data.common.ZhihuJson
import com.zhihuminus.data.zhihu.selectBestVideoUrl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ZVideoDtoTest {
    @Test
    fun parsesZVideoPlaylist() {
        val json =
            """
            {
              "id": "1916898524035019106",
              "title": "价格优势突出的RTX 5060游戏本",
              "description": "本期视频我们一起聊一款RTX 5060游戏本。",
              "video": {
                "width": 1920,
                "height": 1080,
                "duration": 762.6,
                "thumbnail": "https://example.com/video-thumb.jpg",
                "playlist": {
                  "fhd": {
                    "play_url": "https://example.com/fhd.mp4",
                    "url": "https://example.com/fhd.mp4",
                    "bitrate": 724,
                    "size": 69049794
                  },
                  "hd": {
                    "url": "https://example.com/hd.mp4",
                    "bitrate": 429
                  }
                },
                "playlist_v2": {
                  "fhd": {
                    "url": "https://example.com/fhd-h265.mp4",
                    "bitrate": 613
                  }
                }
              },
              "type": "zvideo"
            }
            """.trimIndent()

        val dto = ZhihuJson.decodeJson<ZVideoDto>(ZhihuJson.json.parseToJsonElement(json))
        assertEquals("1916898524035019106", dto.id)
        assertEquals("https://example.com/video-thumb.jpg", dto.video?.thumbnail)
        assertEquals("https://example.com/fhd.mp4", selectBestVideoUrl(dto.video))
    }

    @Test
    fun missingVideoReturnsNullPlayUrl() {
        val dto = ZhihuJson.decodeJson<ZVideoDto>(ZhihuJson.json.parseToJsonElement("""{"id":"1"}"""))
        assertNull(selectBestVideoUrl(dto.video))
    }
}
