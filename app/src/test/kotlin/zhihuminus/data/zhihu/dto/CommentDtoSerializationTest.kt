package com.zhihuminus.data.zhihu.dto

import com.zhihuminus.data.common.ZhihuJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CommentDtoSerializationTest {
    @Test
    fun testSnakeCaseCommentResponseDeserialization() {
        val json =
            """
            {
              "id": "1002345678",
              "type": "comment",
              "url": "https://www.zhihu.com/api/v4/comment_v5/comment/1002345678",
              "resource_type": "answer",
              "content": "<p>测试评论内容</p>",
              "created_time": 1728000000,
              "is_delete": false,
              "collapsed": false,
              "reviewing": false,
              "reply_comment_id": null,
              "reply_root_comment_id": "0",
              "liked": false,
              "like_count": 42,
              "is_author": true,
              "can_delete": true,
              "is_author_top": false,
              "author": {
                "id": "author_123",
                "name": "知友",
                "headline": "测试头衔",
                "avatar_url": "https://pic1.zhimg.com/v2-avatar.jpg",
                "url_token": "zhiyou-token",
                "user_type": "people",
                "is_following": false
              },
              "reply_to_author": null,
              "author_tag": [],
              "comment_tag": [],
              "child_comment_count": 3,
              "child_comments": []
            }
            """.trimIndent()

        val dto = ZhihuJson.decodeFromString<CommentDto>(json)

        assertEquals("1002345678", dto.id)
        assertEquals("answer", dto.resourceType)
        assertEquals("<p>测试评论内容</p>", dto.content)
        assertEquals(1728000000L, dto.createdTime)
        assertEquals(42, dto.likeCount)
        assertTrue(dto.canDelete)
        assertFalse(dto.liked)
        assertEquals("0", dto.replyRootCommentId)
        assertEquals("author_123", dto.author.id)
        assertEquals("知友", dto.author.name)
        assertEquals("https://pic1.zhimg.com/v2-avatar.jpg", dto.author.avatarUrl)
        assertEquals("zhiyou-token", dto.author.urlToken)
        assertEquals(3, dto.childCommentCount)
    }

    @Test
    fun testMissingOptionalFieldsDoesNotThrowSerializationException() {
        val minimalJson =
            """
            {
              "id": "minimal_1",
              "content": "简单评论",
              "author": {
                "id": "anon"
              }
            }
            """.trimIndent()

        val dto = ZhihuJson.decodeFromString<CommentDto>(minimalJson)

        assertEquals("minimal_1", dto.id)
        assertEquals("简单评论", dto.content)
        assertEquals("anon", dto.author.id)
        assertEquals("", dto.author.avatarUrl)
        assertEquals("", dto.author.name)
    }
}
