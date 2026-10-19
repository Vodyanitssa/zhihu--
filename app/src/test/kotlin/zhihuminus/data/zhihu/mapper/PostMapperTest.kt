package com.zhihuminus.data.zhihu.mapper

import com.zhihuminus.core.content.ContentNode
import com.zhihuminus.data.zhihu.dto.AnswerDto
import com.zhihuminus.data.zhihu.dto.AnswerTargetDto
import com.zhihuminus.data.zhihu.dto.ArticleDto
import com.zhihuminus.data.zhihu.dto.ArticleTargetDto
import com.zhihuminus.data.zhihu.dto.AuthorDto
import com.zhihuminus.data.zhihu.dto.BadgeItemDto
import com.zhihuminus.data.zhihu.dto.BadgeV2Dto
import com.zhihuminus.data.zhihu.dto.FeedAuthorDto
import com.zhihuminus.data.zhihu.dto.FeedBadgeDto
import com.zhihuminus.data.zhihu.dto.FeedRelationshipDto
import com.zhihuminus.data.zhihu.dto.PinContentItemDto
import com.zhihuminus.data.zhihu.dto.PinDto
import com.zhihuminus.data.zhihu.dto.PinTargetDto
import com.zhihuminus.data.zhihu.dto.QuestionDto
import com.zhihuminus.data.zhihu.dto.QuestionTargetDto
import com.zhihuminus.data.zhihu.dto.ReactionDto
import com.zhihuminus.data.zhihu.dto.RelationDto
import com.zhihuminus.feature.post.PostType
import com.zhihuminus.feature.post.VoteUpState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class PostMapperTest {
    @Test
    fun testAuthorDtoToAuthor() {
        val dto = AuthorDto(
            id = "author_1",
            name = "张三",
            headline = "Android 工程师",
            avatarUrl = "https://example.com/avatar.jpg",
            urlToken = "zhang-san",
            badgeV2 = BadgeV2Dto(
                detailBadges = listOf(BadgeItemDto(type = "best", description = "优秀答主认证")),
            ),
            isFollowing = true,
            ipInfo = "IP 属地北京",
        )
        val author = dto.toAuthor()
        assertEquals("author_1", author.id)
        assertEquals("张三", author.name)
        assertEquals("Android 工程师", author.headline)
        assertEquals("https://example.com/avatar.jpg", author.avatarUrl)
        assertEquals("zhang-san", author.urlToken)
        assertEquals("优秀答主认证", author.badgeText)
        assertEquals(true, author.isFollowing)
        assertEquals("IP 属地北京", author.ipInfo)
    }

    @Test
    fun testFeedAuthorDtoToAuthor() {
        val nullAuthor: FeedAuthorDto? = null
        val defaultAuthor = nullAuthor.toAuthor()
        assertEquals("知乎用户", defaultAuthor.name)
        assertEquals("", defaultAuthor.id)

        val feedAuthor = FeedAuthorDto(
            id = "feed_author_1",
            name = "李四",
            headline = "Kotlin 爱好者",
            avatarUrl = "https://example.com/avatar2.jpg",
            urlToken = "li-si",
            badge = listOf(FeedBadgeDto(type = "star", description = "新星答主")),
            isFollowing = false,
            ipInfo = "IP 属地上海",
        )
        val author = feedAuthor.toAuthor()
        assertEquals("feed_author_1", author.id)
        assertEquals("李四", author.name)
        assertEquals("新星答主", author.badgeText)
        assertEquals("IP 属地上海", author.ipInfo)
    }

    @Test
    fun testAnswerDtoToPost() {
        val dto = AnswerDto(
            id = 1001L,
            question = QuestionDto(id = 2001L, title = "如何评价 Kotlin 2.0？"),
            author = AuthorDto(id = "user1", name = "答主A", avatarUrl = "https://example.com/a.jpg"),
            content = "<p>Kotlin 2.0 带来了全新的 K2 编译器。</p>",
            voteupCount = 50,
            commentCount = 12,
            reaction = ReactionDto(
                relation = RelationDto(vote = "up", faved = true),
            ),
            createdTime = 1600000000L,
            updatedTime = 1600001000L,
            ipInfo = "IP 属地浙江",
            excerpt = "Kotlin 2.0 带来了全新的 K2 编译器。",
        )
        val post = dto.toPost()
        assertEquals(1001L, post.id)
        assertEquals(PostType.Answer, post.type)
        assertEquals("如何评价 Kotlin 2.0？", post.title)
        assertEquals(2001L, post.questionId)
        assertEquals("答主A", post.author.name)
        assertEquals(50, post.voteCount)
        assertEquals(12, post.commentCount)
        assertEquals(VoteUpState.Up, post.voteState)
        assertEquals(true, post.isFaved)
        assertEquals("IP 属地浙江", post.ipInfo)
    }

    @Test
    fun testArticleDtoToPost() {
        val dto = ArticleDto(
            id = 3001L,
            title = "深入探索 Jetpack Compose",
            author = AuthorDto(id = "user2", name = "作者B", avatarUrl = "https://example.com/b.jpg"),
            content = "<p>Compose 重构了 Android UI 开发范式。</p>",
            voteupCount = 88,
            commentCount = 20,
            reaction = ReactionDto(
                relation = RelationDto(vote = "neutral", faved = false),
            ),
            created = 1610000000L,
            updated = 1610001000L,
            excerpt = "Compose 重构了 Android UI 开发范式。",
        )
        val post = dto.toPost()
        assertEquals(3001L, post.id)
        assertEquals(PostType.Article, post.type)
        assertEquals("深入探索 Jetpack Compose", post.title)
        assertEquals("作者B", post.author.name)
        assertEquals(88, post.voteCount)
        assertEquals(20, post.commentCount)
        assertEquals(VoteUpState.Neutral, post.voteState)
        assertEquals(false, post.isFaved)
    }

    @Test
    fun testFeedTargetDtoToPost() {
        val answerTarget = AnswerTargetDto(
            id = 4001L,
            question = QuestionTargetDto(id = 5001L, _title = "问题标题"),
            author = FeedAuthorDto(id = "u3", name = "作者C", avatarUrl = "https://example.com/c.jpg"),
            content = "<p>信息流回答内容</p>",
            voteupCount = 30,
            commentCount = 5,
            relationship = FeedRelationshipDto(voting = 1, isFollowing = true),
            createdTime = 1620000000L,
            updatedTime = 1620001000L,
            excerpt = "信息流回答内容",
        )
        val post = answerTarget.toPost()
        assertNotNull(post)
        assertEquals(4001L, post.id)
        assertEquals(PostType.Answer, post.type)
        assertEquals("问题标题", post.title)
        assertEquals(VoteUpState.Up, post.voteState)
        assertEquals(true, post.isFaved)

        val emptyAnswer = answerTarget.copy(content = "")
        assertNull(emptyAnswer.toPost())

        val articleTarget = ArticleTargetDto(
            id = 6001L,
            title = "信息流文章标题",
            author = FeedAuthorDto(id = "u4", name = "作者D", avatarUrl = "https://example.com/d.jpg"),
            content = "<p>信息流文章正文</p>",
            voteupCount = 15,
            commentCount = 3,
            voting = -1,
            created = 1630000000L,
            updated = 1630001000L,
            rawExcerpt = "信息流文章正文",
        )
        val articlePost = articleTarget.toPost()
        assertNotNull(articlePost)
        assertEquals(6001L, articlePost.id)
        assertEquals(PostType.Article, articlePost.type)
        assertEquals(VoteUpState.Down, articlePost.voteState)
    }

    @Test
    fun testExtractPinTitleWithTrailingBr() {
        val html = "<div>你们还穿着开裆裤，玩泥巴，玩 AI Code， Anthropic 已经开始来骗“研究级数据”了<br/><p>可以看得出来，OpenAI 和 Anthropic...</p></div>"
        val (title, bodyHtml) = PostMapper.extractTitleAndBodyHtml(html)
        assertEquals("你们还穿着开裆裤，玩泥巴，玩 AI Code， Anthropic 已经开始来骗“研究级数据”了", title)
        assertEquals("<p>可以看得出来，OpenAI 和 Anthropic...</p></div>", bodyHtml)
    }

    @Test
    fun testExtractPinTitleWithPipeAndSpace() {
        val html = "<div>1/3 | <p>百合救世界</p></div>"
        val (title, bodyHtml) = PostMapper.extractTitleAndBodyHtml(html)
        assertEquals("1/3", title)
        assertEquals("<p>百合救世界</p></div>", bodyHtml)
    }

    @Test
    fun testExtractPinTitleWithBrVariations() {
        val variations = listOf(
            "<div>标题一<br><p>正文</p></div>",
            "<div>标题一<br /><p>正文</p></div>",
            "<div>标题一<br/> <p>正文</p></div>",
            "<div>标题一 | <br/><p>正文</p></div>",
            "<div>标题一<br/><br/><p>正文</p></div>",
        )
        for (variant in variations) {
            val (title, bodyHtml) = PostMapper.extractTitleAndBodyHtml(variant)
            assertEquals("标题一", title, "Failed for variant: $variant")
            assertEquals("<p>正文</p></div>", bodyHtml)
        }
    }

    @Test
    fun testExtractPinTitleWithHtmlEntitiesAndTags() {
        val html = "<div>关于 <b>AI</b> &amp; 算法<br/><p>正文内容</p></div>"
        val (title, _) = PostMapper.extractTitleAndBodyHtml(html)
        assertEquals("关于 AI & 算法", title)
    }

    @Test
    fun testExtractPinTitleWithoutTitle() {
        val html = "<div><p>直接就是段落正文</p></div>"
        val (title, bodyHtml) = PostMapper.extractTitleAndBodyHtml(html)
        assertNull(title)
        assertEquals(html, bodyHtml)
    }

    @Test
    fun testExtractPinTitleOnlyBrBeforeP() {
        val html = "<div><br/><p>正文</p></div>"
        val (title, bodyHtml) = PostMapper.extractTitleAndBodyHtml(html)
        assertNull(title)
        assertEquals(html, bodyHtml)
    }

    @Test
    fun testPinTargetDtoToPost() {
        val target = PinTargetDto(
            id = 12345L,
            contentHtml = "<div>想法标题<br/><p>正文内容</p></div>",
            author = FeedAuthorDto(id = "user1", name = "张三", avatarUrl = "https://example.com/a.jpg"),
            likeCount = 10,
            commentCount = 2,
            content = listOf(
                PinContentItemDto(
                    type = "link_card",
                    url = "https://example.com/article",
                    dataContentId = "c1",
                    dataContentType = "link",
                ),
            ),
        )

        val post = target.toPost()
        assertNotNull(post)
        assertEquals(12345L, post.id)
        assertEquals(PostType.Pin, post.type)
        assertEquals("想法标题", post.title)
        assertEquals("张三", post.author.name)
        assertEquals(1, post.content.size)
        assertIs<ContentNode.Paragraph>(post.content.first())
        assertEquals(1, post.linkCards.size)
        assertEquals("https://example.com/article", post.linkCards.first().url)
    }

    @Test
    fun testPinDtoToPost() {
        val dto = PinDto(
            id = "67890",
            contentHtml = "<div>Pin 标题<br/><p>正文段落</p></div>",
            author = AuthorDto(id = "user2", name = "李四", avatarUrl = "https://example.com/b.jpg"),
            likeCount = 20,
            commentCount = 5,
        )

        val post = dto.toPost()
        assertEquals(67890L, post.id)
        assertEquals(PostType.Pin, post.type)
        assertEquals("Pin 标题", post.title)
        assertEquals("李四", post.author.name)
        assertEquals(1, post.content.size)
        assertIs<ContentNode.Paragraph>(post.content.first())
    }
}
