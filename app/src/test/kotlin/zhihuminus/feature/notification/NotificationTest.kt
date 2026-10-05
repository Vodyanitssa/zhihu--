package com.zhihuminus.feature.notification

import com.zhihuminus.core.content.AstParser
import com.zhihuminus.core.content.EmojiManager
import com.zhihuminus.core.content.InlineNode
import com.zhihuminus.data.FakeZhihuApi
import com.zhihuminus.data.ZhihuJson
import com.zhihuminus.data.zhihu.ZhihuNotificationRepository
import com.zhihuminus.data.zhihu.dto.NotificationAuthorDto
import com.zhihuminus.data.zhihu.dto.NotificationColumnHeadDto
import com.zhihuminus.data.zhihu.dto.NotificationContentDto
import com.zhihuminus.data.zhihu.dto.NotificationHeadDto
import com.zhihuminus.data.zhihu.dto.NotificationHeadEntryDto
import com.zhihuminus.data.zhihu.dto.NotificationImageDto
import com.zhihuminus.data.zhihu.dto.NotificationLabelDto
import com.zhihuminus.data.zhihu.dto.NotificationOverviewDto
import com.zhihuminus.data.zhihu.dto.NotificationTargetDto
import com.zhihuminus.data.zhihu.dto.NotificationTimelineItemDto
import com.zhihuminus.data.zhihu.dto.PrivateMessageDto
import com.zhihuminus.data.zhihu.dto.PrivateMessagePluginDto
import com.zhihuminus.data.zhihu.toDomain
import com.zhihuminus.navigation.Notification
import com.zhihuminus.navigation.Person
import com.zhihuminus.notification.NotificationType
import com.zhihuminus.notification.matchNotificationType
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class NotificationTest {
    @Test
    fun testNotificationCategoryLookup() {
        assertEquals(NotificationCategory.Comment, NotificationCategory.fromEntryName("comment"))
        assertEquals(NotificationCategory.Like, NotificationCategory.fromEntryName("like"))
        assertEquals(NotificationCategory.Favorite, NotificationCategory.fromEntryName("favlist_me"))
        assertEquals(NotificationCategory.Follow, NotificationCategory.fromEntryName("follow"))
        assertNull(NotificationCategory.fromEntryName("unknown"))

        assertEquals(NotificationCategory.Comment, NotificationCategory.fromDetailTitle("评论转发@"))
        assertEquals(NotificationCategory.Like, NotificationCategory.fromDetailTitle("赞同喜欢"))
        assertEquals(NotificationCategory.Favorite, NotificationCategory.fromDetailTitle("收藏了我"))
        assertEquals(NotificationCategory.Follow, NotificationCategory.fromDetailTitle("关注订阅"))
        assertNull(NotificationCategory.fromDetailTitle("未知"))
    }

    @Test
    fun testNotificationTimelineItemHelpers() {
        val item = NotificationTimelineItem(
            id = "123",
            uniqueId = "uniq_123",
            content = NotificationContent(
                title = "张三",
                subTitle = "赞同了你的回答",
                text = "这是一条通知内容",
            ),
            targetSource = NotificationTargetSource(
                text = "原问题标题",
                subText = "我的回答摘要",
            ),
            head = NotificationHead(
                avatarUrl = "https://example.com/avatar.jpg",
            ),
        )

        assertEquals("张三", item.displayTitle())
        assertEquals("原问题标题\n我的回答摘要", item.sourceText())
        assertEquals("https://example.com/avatar.jpg", item.avatarUrl())
        assertEquals("uniq_123", item.stableId)

        val replyCommentItem = NotificationTimelineItem(
            content = NotificationContent(
                title = "李四",
                subTitle = "回复了回答下你的评论",
                abstractText = "<p>赞同你的观点[微笑]</p>",
                text = "某个问题标题",
            ),
        )
        assertEquals("李四", replyCommentItem.displayTitle())
        assertEquals("回复了回答下你的评论", replyCommentItem.content?.subTitle)

        val replyAllItem = NotificationTimelineItem(
            content = NotificationContent(
                title = "王五",
                subTitle = "回复了回答下的所有人",
                abstractText = "大家分析得都很到位",
                text = "某个问题标题",
            ),
        )
        assertEquals("王五", replyAllItem.displayTitle())
        assertEquals("回复了回答下的所有人", replyAllItem.content?.subTitle)
    }

    @Test
    fun testNotificationTimelineItemNavDestination() {
        val messageItem = NotificationTimelineItem(
            id = "1",
            content = NotificationContent(
                targetLink = "https://www.zhihu.com/inbox/peer123",
                title = "私信会话",
            ),
            head = NotificationHead(
                avatarUrl = "https://example.com/p.jpg",
            ),
        )
        val destination = messageItem.navDestination()
        assertNotNull(destination)
        assertIs<Notification.Message>(destination)
        assertEquals("peer123", destination.peerId)
        assertEquals("私信会话", destination.name)
        assertEquals("https://example.com/p.jpg", destination.avatarUrl)

        val personItem = NotificationTimelineItem(
            id = "2",
            target = NotificationTarget(
                type = "people",
                id = "p_456",
                urlToken = "user-token",
                name = "用户A",
            ),
        )
        val personDest = personItem.navDestination()
        assertNotNull(personDest)
        assertIs<Person>(personDest)
        assertEquals("p_456", personDest.id)
        assertEquals("user-token", personDest.urlToken)
        assertEquals("用户A", personDest.name)
    }

    @Test
    fun testNotificationTypeRegexMatching() {
        assertEquals(NotificationType.LIKE_ANSWER, matchNotificationType("喜欢了你的回答"))
        assertEquals(NotificationType.LIKE_COMMENT, matchNotificationType("喜欢了你的评论"))
        assertEquals(NotificationType.REPLY_COMMENT, matchNotificationType("回复了你的评论"))
        assertEquals(NotificationType.REPLY_COMMENT, matchNotificationType("回复了回答下你的评论"))
        assertEquals(NotificationType.REPLY_COMMENT, matchNotificationType("回复了文章下你的评论"))
        assertEquals(NotificationType.REPLY_COMMENT, matchNotificationType("回复了回答下的所有人"))
        assertEquals(NotificationType.REPLY_COMMENT, matchNotificationType("回复了文章下的所有人"))
        assertEquals(NotificationType.INVITE_ANSWER, matchNotificationType("邀请你回答问题"))
        assertNull(matchNotificationType("其他通知"))
    }

    @Test
    fun testPrivateMessageDomainModel() {
        val msg = PrivateMessage(
            id = "msg1",
            content = "Hello",
            createdTime = 123456789L,
            sender = NotificationAuthor(id = "user1", name = "Alice"),
            receiver = NotificationAuthor(id = "user2", name = "Bob"),
        )
        assertEquals("msg1", msg.stableId)

        val fallbackMsg = PrivateMessage(
            id = "",
            content = "Hello",
            createdTime = 123456789L,
            sender = NotificationAuthor(id = "u1"),
            receiver = NotificationAuthor(id = "u2"),
        )
        assertEquals("123456789-u1-u2", fallbackMsg.stableId)
    }

    @Test
    fun testDtoToDomainMapping() {
        val dto = NotificationTimelineItemDto(
            id = "100",
            uniqueId = "u100",
            type = "normal",
            cardType = "card",
            detailTitle = "Detail Title",
            head = NotificationHeadDto(
                author = NotificationAuthorDto(id = "a1", name = "Author 1"),
                avatarUrl = "https://example.com/a.png",
                labels = listOf(NotificationLabelDto(text = "优秀回答者")),
            ),
            content = NotificationContentDto(
                title = "Notification Title",
                subTitle = "Notification SubTitle",
                text = "Body text",
            ),
            target = NotificationTargetDto(
                id = "t1",
                name = "Target Name",
                title = "Question Title",
            ),
        )

        val domain = dto.toDomain()
        assertEquals("100", domain.id)
        assertEquals("u100", domain.uniqueId)
        assertEquals("a1", domain.head?.author?.id)
        assertEquals("Author 1", domain.head?.author?.name)
        assertEquals("https://example.com/a.png", domain.head?.avatarUrl)
        assertEquals(
            "优秀回答者",
            domain.head
                ?.labels
                ?.firstOrNull()
                ?.text,
        )
        assertEquals("Notification Title", domain.content?.title)
        assertEquals("Question Title", domain.target?.title)

        val colDto = NotificationColumnHeadDto(
            id = "col1",
            title = "邀请回答",
            textPrefix = "来自",
            text = "知乎助手",
            avatarUrls = listOf(NotificationImageDto(url = "img1.png", nightUrl = "img1_night.png")),
            unreadCount = 2,
        )
        val colDomain = colDto.toDomain()
        assertEquals("col1", colDomain.id)
        assertEquals("邀请回答", colDomain.title)
        assertEquals(2, colDomain.unreadCount)
        assertEquals("img1.png", colDomain.avatarUrls.first().url)

        val pmDto = PrivateMessageDto(
            id = "pm1",
            content = "PM Content",
            plugin = PrivateMessagePluginDto(excerpt = "Excerpt text"),
        )
        val pmDomain = pmDto.toDomain()
        assertEquals("pm1", pmDomain.id)
        assertEquals("PM Content", pmDomain.content)
        assertEquals("Excerpt text", pmDomain.plugin?.excerpt)
    }

    @Test
    fun testPrivateMessageTimestampCollapsing() {
        assertEquals(emptySet(), calculateTimestampVisibleIds(emptyList()))

        val invalidTimeMessage = PrivateMessage(id = "msg0", createdTime = 0L)
        assertEquals(emptySet(), calculateTimestampVisibleIds(listOf(invalidTimeMessage)))

        val msg1 = PrivateMessage(id = "msg1", createdTime = 1000L)
        assertEquals(setOf("msg1"), calculateTimestampVisibleIds(listOf(msg1)))

        // Note: in messages list, index 0 is newest, index 1 is oldest
        val msg2Within2Min = PrivateMessage(id = "msg2", createdTime = 1080L)
        val listWithin2Min = listOf(msg2Within2Min, msg1)
        assertEquals(setOf("msg1"), calculateTimestampVisibleIds(listWithin2Min))

        val msg3Beyond2Min = PrivateMessage(id = "msg3", createdTime = 1120L)
        val listBeyond2Min = listOf(msg3Beyond2Min, msg1)
        assertEquals(setOf("msg1", "msg3"), calculateTimestampVisibleIds(listBeyond2Min))

        val msg4 = PrivateMessage(id = "msg4", createdTime = 1140L)
        val continuousList = listOf(msg4, msg2Within2Min, msg1)
        assertEquals(setOf("msg1", "msg4"), calculateTimestampVisibleIds(continuousList))
    }

    @Test
    fun testAstParserParseInline() {
        val prevMapping = EmojiManager.mapping
        try {
            EmojiManager.mapping = mapOf("[微笑]" to "smile.png", "[握手]" to "handshake.png")

            // Empty & blank
            assertEquals(emptyList(), AstParser.parseInline(""))
            assertEquals(emptyList(), AstParser.parseInline("   "))

            // Plain text with emojis
            val inlineNodes = AstParser.parseInline("你好[微笑]世界[握手]")
            assertEquals(
                listOf(
                    InlineNode.Text("你好"),
                    InlineNode.Emoji("[微笑]"),
                    InlineNode.Text("世界"),
                    InlineNode.Emoji("[握手]"),
                ),
                inlineNodes,
            )

            // Plain text with unknown emoji brackets
            val unknownNodes = AstParser.parseInline("你好[未知]世界")
            assertEquals(
                listOf(InlineNode.Text("你好[未知]世界")),
                unknownNodes,
            )

            // Plain text with newlines
            val newlineNodes = AstParser.parseInline("第一行\n第二行")
            assertEquals(
                listOf(
                    InlineNode.Text("第一行"),
                    InlineNode.LineBreak,
                    InlineNode.Text("第二行"),
                ),
                newlineNodes,
            )

            // HTML with bold, br, and emojis
            val htmlNodes = AstParser.parseInline("<p>Hello <b>bold</b><br>[微笑]</p>")
            assertEquals(
                listOf(
                    InlineNode.Text("Hello "),
                    InlineNode.Bold(listOf(InlineNode.Text("bold"))),
                    InlineNode.LineBreak,
                    InlineNode.Emoji("[微笑]"),
                ),
                htmlNodes,
            )

            // Multi-paragraph HTML
            val multiParaNodes = AstParser.parseInline("<p>段落1</p><p>段落2</p>")
            assertEquals(
                listOf(
                    InlineNode.Text("段落1"),
                    InlineNode.LineBreak,
                    InlineNode.Text("段落2"),
                ),
                multiParaNodes,
            )
        } finally {
            EmojiManager.mapping = prevMapping
        }
    }

    @Test
    fun testPrivateMessageInlineContentFallback() {
        val blankMsg = PrivateMessage(id = "1", content = "   ")
        val blankParsed = AstParser.parseInline(blankMsg.content).ifEmpty {
            listOf(InlineNode.Text("暂不支持显示这条消息"))
        }
        assertEquals(listOf(InlineNode.Text("暂不支持显示这条消息")), blankParsed)

        val excerptMsg = PrivateMessage(
            id = "2",
            content = "",
            plugin = PrivateMessagePluginDto(excerpt = "分享内容").toDomain(),
        )
        val textToParse = excerptMsg.plugin?.excerpt?.takeIf { it.isNotBlank() } ?: excerptMsg.content
        val parsed = AstParser.parseInline(textToParse)
        assertEquals(listOf(InlineNode.Text("分享内容")), parsed)
    }

    @Test
    fun testNotificationOverviewDtoDecoding() {
        val jsonString =
            """
            {
              "head": [
                {"detail_title": "评论转发@", "unread_count": 3},
                {"entry_name": "like", "detail_title": "赞同喜欢", "unread_count": 5}
              ],
              "column_head": [{
                "id": "invite",
                "title": "邀请回答",
                "text_prefix": "测试用户",
                "text": "等人邀请你回答问题",
                "target_link": "https://www.zhihu.com/compose_answer_tab",
                "avatar_urls": [{"url": "https://pic.example/1.jpg", "night_url": "https://pic.example/1-night.jpg"}],
                "images": [],
                "unread_count": 2
              }],
              "data": [],
              "paging": {
                "is_end": true,
                "next": ""
              }
            }
            """.trimIndent()

        val jsonElement = ZhihuJson.json.parseToJsonElement(jsonString)
        val dto = ZhihuJson.decodeJson<NotificationOverviewDto>(jsonElement)

        assertEquals(2, dto.head.size)
        assertEquals("评论转发@", dto.head[0].detailTitle)
        assertEquals(3, dto.head[0].unreadCount)
        assertEquals("like", dto.head[1].entryName)
        assertEquals("赞同喜欢", dto.head[1].detailTitle)
        assertEquals(5, dto.head[1].unreadCount)

        assertEquals(1, dto.columnHead.size)
        assertEquals("邀请回答", dto.columnHead[0].title)
        assertEquals(2, dto.columnHead[0].unreadCount)
    }

    @Test
    fun testZhihuNotificationRepositoryUnreadCounts() = runBlocking {
        val overviewDto = NotificationOverviewDto(
            head = listOf(
                NotificationHeadEntryDto(detailTitle = "评论转发@", unreadCount = 3),
                NotificationHeadEntryDto(entryName = "like", detailTitle = "", unreadCount = 5),
                NotificationHeadEntryDto(detailTitle = "关注订阅", unreadCount = 1),
            ),
            columnHead = listOf(
                NotificationColumnHeadDto(id = "invite", title = "邀请回答", unreadCount = 2),
            ),
        )

        val fakeApi = object : FakeZhihuApi() {
            override suspend fun getNotificationOverview(nextUrl: String?): NotificationOverviewDto = overviewDto
        }

        val repository = ZhihuNotificationRepository(fakeApi)
        val result = repository.getNotificationOverview()

        assertEquals(3, result.unreadCounts[NotificationCategory.Comment])
        assertEquals(5, result.unreadCounts[NotificationCategory.Like])
        assertEquals(0, result.unreadCounts[NotificationCategory.Favorite])
        assertEquals(1, result.unreadCounts[NotificationCategory.Follow])
        assertEquals(2, result.invitation?.unreadCount)
    }
}
