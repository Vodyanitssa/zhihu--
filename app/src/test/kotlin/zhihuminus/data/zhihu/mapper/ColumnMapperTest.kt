package com.zhihuminus.data.zhihu.mapper

import com.zhihuminus.data.common.ZhihuJson
import com.zhihuminus.data.zhihu.dto.ColumnDetailDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ColumnMapperTest {
    @Test
    fun testRealColumnDetailResponseDecodesAllFields() {
        val json =
            """
            {"updated":1661317582,"description":"Vulkan 学习笔记","column_type":"normal","is_following":true,
            "id":"c_1542809288766390272","voteup_count":1679,"accept_submission":true,"items_count":26,
            "purchase_count":0,"title":"Vulkan 折磨笔记","url":"http://www.zhihu.com/api/v4/columns/c_1542809288766390272",
            "comment_permission":"all","author":{"is_followed":false,"user_type":"people","is_following":false,
            "url_token":"lllzwj","id":"9417f7bcaf95c74027330ab8fdb86aff","name":"不知名书杯",
            "headline":"熟练的写着XXXCreateInfo","avatar_url":"https://picx.zhimg.com/v2-4ed42c89e5f7dea78057ec5d876c3f3a_l.jpg?source=d16d100b"},
            "image_url":"https://picx.zhimg.com/v2-f111d7ee1c41944859e975a712c0883b_l.jpg?source=d16d100b",
            "followers":107,"articles_count":26,"type":"column"}
            """.trimIndent()

        val dto = ZhihuJson.decodeFromString<ColumnDetailDto>(json)

        assertEquals("c_1542809288766390272", dto.id)
        assertEquals("Vulkan 折磨笔记", dto.title)
        assertEquals("Vulkan 学习笔记", dto.description)
        assertEquals("https://picx.zhimg.com/v2-f111d7ee1c41944859e975a712c0883b_l.jpg?source=d16d100b", dto.imageUrl)
        assertEquals(107, dto.followers)
        assertEquals(26, dto.articlesCount)
        assertEquals(26, dto.itemsCount)
        assertEquals(true, dto.isFollowing)
        assertEquals(1679, dto.voteupCount)
        assertEquals(1661317582L, dto.updated)

        val author = dto.author
        assertEquals("9417f7bcaf95c74027330ab8fdb86aff", author?.id)
        assertEquals("lllzwj", author?.urlToken)
        assertEquals("不知名书杯", author?.name)
        assertEquals("熟练的写着XXXCreateInfo", author?.headline)
        assertEquals("https://picx.zhimg.com/v2-4ed42c89e5f7dea78057ec5d876c3f3a_l.jpg?source=d16d100b", author?.avatarUrl)
    }

    @Test
    fun testColumnDetailMapsToDomain() {
        val dto = ColumnDetailDto(
            id = "c_1",
            title = "测试专栏",
            description = "简介",
            imageUrl = "",
            followers = 107,
            articlesCount = 0,
            itemsCount = 26,
            isFollowing = true,
            author = null,
        )

        val detail = dto.toColumnDetail()

        assertEquals("c_1", detail.id)
        assertEquals("测试专栏", detail.title)
        assertEquals(107, detail.followerCount)
        assertEquals(26, detail.articleCount)
        assertEquals(true, detail.isFollowing)
        assertNull(detail.avatarUrl)
        assertNull(detail.author)
    }
}
