package com.zhihuminus.feature.notification

import com.zhihuminus.data.zhihu.dto.NotificationAuthorDto
import com.zhihuminus.data.zhihu.dto.NotificationColumnHeadDto
import com.zhihuminus.data.zhihu.dto.NotificationContentDto
import com.zhihuminus.data.zhihu.dto.NotificationHeadDto
import com.zhihuminus.data.zhihu.dto.NotificationImageDto
import com.zhihuminus.data.zhihu.dto.NotificationLabelDto
import com.zhihuminus.data.zhihu.dto.NotificationTargetDto
import com.zhihuminus.data.zhihu.dto.NotificationTimelineItemDto
import com.zhihuminus.data.zhihu.dto.PrivateMessageDto
import com.zhihuminus.data.zhihu.dto.PrivateMessagePluginDto
import com.zhihuminus.data.zhihu.toDomain
import com.zhihuminus.navigation.Notification
import com.zhihuminus.navigation.Person
import com.zhihuminus.notification.NotificationType
import com.zhihuminus.notification.matchNotificationType
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
        assertEquals("赞同了你的回答：", item.displaySubtitle())
        assertEquals("原问题标题\n我的回答摘要", item.sourceText())
        assertEquals("https://example.com/avatar.jpg", item.avatarUrl())
        assertEquals("uniq_123", item.stableId)
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
}
