package com.zhihuminus.data.zhihu.dto

import com.zhihuminus.data.common.ZhihuJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class FeedDtoSerializationTest {
    @Test
    fun testCommonFeedDtoDeserializationWithLargeMillisecondOffset() {
        val json =
            """
            {
              "id": "0_1792212679113668507_1792204029303_3",
              "type": "feed",
              "offset": 1792204029303,
              "created_time": 1792204029,
              "updated_time": 1792204029,
              "brief": "{\"id\":2094736201873887817,\"source\":\"TS\",\"type\":\"answer\"}",
              "verb": "MEMBER_ANSWER_QUESTION",
              "action_text_tpl": "{}回答了问题",
              "action_text": "Thoughts Memo回答了问题",
              "show_actor_time": true,
              "target": {
                "id": "2094736201873887817",
                "type": "answer",
                "url": "https://api.zhihu.com/answers/2094736201873887817",
                "voteup_count": 70,
                "thanks_count": 4,
                "question": {
                  "id": "2038200491411236835",
                  "title": "阶层为什么会固化？",
                  "url": "https://api.zhihu.com/questions/2038200491411236835",
                  "type": "question",
                  "question_type": "normal",
                  "created": 1778724865,
                  "answer_count": 1979,
                  "comment_count": 18,
                  "follower_count": 6142,
                  "detail": "",
                  "excerpt": "",
                  "bound_topic_ids": [285, 3007, 3324, 13740, 94069],
                  "relationship": { "is_author": false },
                  "is_following": false,
                  "author": {
                    "id": "ebbb38a385442e888b922e28507f7885",
                    "name": "syber",
                    "headline": "",
                    "type": "people",
                    "user_type": "people",
                    "url": "https://www.zhihu.com/people/syber-23-20",
                    "url_token": "syber-23-20",
                    "avatar_url": "https://picx.zhimg.com/v2-avatar.jpg",
                    "gender": 1,
                    "is_following": false,
                    "is_followed": false,
                    "is_org": false
                  }
                },
                "updated_time": 1792204028,
                "created_time": 1792204028,
                "author": {
                  "id": "4c592f496dc33822b560b382907ff1d0",
                  "name": "Thoughts Memo",
                  "headline": "学校≠教育≠技能",
                  "type": "people",
                  "user_type": "people",
                  "url": "https://www.zhihu.com/people/L.M.Sherlock",
                  "url_token": "L.M.Sherlock",
                  "avatar_url": "https://picx.zhimg.com/v2-avatar2.jpg",
                  "gender": 0,
                  "is_following": true,
                  "is_followed": false,
                  "is_org": false
                },
                "comment_permission": "all",
                "is_copyable": true,
                "comment_count": 4
              }
            }
            """.trimIndent()

        val feedDto = ZhihuJson.decodeFromString<FeedDto>(json)
        assertIs<CommonFeedDto>(feedDto)

        assertEquals("0_1792212679113668507_1792204029303_3", feedDto.id)
        assertEquals(1792204029303L, feedDto.offset)
        assertEquals("MEMBER_ANSWER_QUESTION", feedDto.verb)
        assertEquals("Thoughts Memo回答了问题", feedDto.actionText)
        assertEquals("Thoughts Memo回答了问题", feedDto.sourceLabel)

        val target = feedDto.target
        assertNotNull(target)
        assertIs<AnswerTargetDto>(target)
        assertEquals(2094736201873887817L, target.id)
        assertEquals("阶层为什么会固化？", target.title)
        assertEquals("Thoughts Memo", target.author?.name)
    }

    @Test
    fun testAnswerTargetDtoWithIpInfoDeserialization() {
        val json =
            """
            {
              "id": "12345",
              "type": "answer",
              "question": {
                "id": "67890",
                "title": "测试问题"
              },
              "author": {
                "id": "auth1",
                "name": "作者名",
                "ip_info": "IP 属地浙江"
              },
              "ip_info": "IP 属地浙江"
            }
            """.trimIndent()
        val answerTarget = ZhihuJson.decodeFromString<AnswerTargetDto>(json)
        assertEquals("IP 属地浙江", answerTarget.ipInfo)
        assertEquals("IP 属地浙江", answerTarget.author?.ipInfo)
    }
}
