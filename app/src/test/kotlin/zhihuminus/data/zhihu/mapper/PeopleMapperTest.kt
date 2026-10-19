package com.zhihuminus.data.zhihu.mapper

import com.zhihuminus.data.zhihu.dto.AnswerDto
import com.zhihuminus.data.zhihu.dto.ArticleDto
import com.zhihuminus.data.zhihu.dto.AuthorDto
import com.zhihuminus.data.zhihu.dto.FollowedQuestionDto
import com.zhihuminus.data.zhihu.dto.FollowedTopicDto
import com.zhihuminus.data.zhihu.dto.FollowedTopicItemDto
import com.zhihuminus.data.zhihu.dto.MemberBadgeItemDto
import com.zhihuminus.data.zhihu.dto.MemberBadgeV2Dto
import com.zhihuminus.data.zhihu.dto.MemberColumnItemDto
import com.zhihuminus.data.zhihu.dto.MemberItemDto
import com.zhihuminus.data.zhihu.dto.MemberProfileDto
import com.zhihuminus.data.zhihu.dto.MemberSocialMediaDto
import com.zhihuminus.data.zhihu.dto.MemberSocialMediaModuleDto
import com.zhihuminus.data.zhihu.dto.PinDto
import com.zhihuminus.data.zhihu.dto.QuestionDto
import com.zhihuminus.feature.post.PostType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class PeopleMapperTest {
    @Test
    fun testMemberSocialMediaToGithubSocialUiState() {
        val dto = MemberSocialMediaDto(
            title = "GitHub · octocat",
            link = "zhihu://link?target=https%3A%2F%2Fgithub.com%2Foctocat",
            modules = listOf(
                MemberSocialMediaModuleDto(title = "stars", value = "1.2k"),
            ),
        )
        val social = dto.toGithubSocialUiState()
        assertNotNull(social)
        assertEquals("GitHub · octocat", social.title)
        assertEquals("1.2k", social.starCount)
        assertEquals("https://github.com/octocat", social.profileUrl)

        val nonGithub = dto.copy(title = "Weibo · octocat")
        assertNull(nonGithub.toGithubSocialUiState())
    }

    @Test
    fun testMemberItemDtoToPeopleMemberItem() {
        val dto = MemberItemDto(
            id = "user_101",
            urlToken = "token_101",
            name = "王五",
            avatarUrl = "https://example.com/avatar.jpg",
            headline = "全栈工程师",
            badgeV2 = MemberBadgeV2Dto(
                title = "优秀答主",
                detailBadges = listOf(MemberBadgeItemDto(title = "优秀答主", description = "认证")),
            ),
            answerCount = 10,
            articlesCount = 5,
            followerCount = 1000,
            isFollowing = true,
        )
        val item = dto.toPeopleMemberItem()
        assertEquals("user_101", item.id)
        assertEquals("token_101", item.urlToken)
        assertEquals("王五", item.name)
        assertEquals("全栈工程师", item.headline)
        assertEquals(10, item.answerCount)
        assertEquals(5, item.articleCount)
        assertEquals(1000, item.followerCount)
        assertEquals(true, item.isFollowing)
    }

    @Test
    fun testPeopleMapperToProfile() {
        val raw = MemberProfileDto(
            id = "user_102",
            urlToken = "token_102",
            name = "赵六",
            avatarUrl = "https://example.com/zhao.jpg",
            headline = "设计总监",
            followerCount = 200,
            followingCount = 50,
            answerCount = 8,
            articlesCount = 2,
            pinsCount = 15,
            questionCount = 3,
            isFollowing = false,
            isFollowed = true,
            isBlocking = false,
            voteupCount = 999,
            thankedCount = 100,
            favoritedCount = 50,
            ipInfo = "IP 属地广东",
            description = "个人简介",
            gender = 1,
        )
        val profile = PeopleMapper.toProfile(raw, null)
        assertEquals("user_102", profile.id)
        assertEquals("赵六", profile.name)
        assertEquals("设计总监", profile.headline)
        assertEquals(200, profile.followerCount)
        assertEquals(999, profile.voteupCount)
        assertEquals("IP 属地广东", profile.ipInfo)
        assertEquals("个人简介", profile.description)
        assertEquals(1, profile.gender)
    }

    @Test
    fun testCreationMappers() {
        val answer = AnswerDto(
            id = 501L,
            question = QuestionDto(title = "如何学习 Compose？"),
            excerpt = "多写代码多看官方文档。",
            voteupCount = 42,
            commentCount = 6,
            author = AuthorDto(name = "小明", headline = "Android", avatarUrl = "https://example.com/m.jpg"),
            createdTime = 1650000000L,
            thumbnail = "https://example.com/thumb.jpg",
        )
        val answerItem = answer.toPeopleCreationItem()
        assertEquals(501L, answerItem.id)
        assertEquals(PostType.Answer, answerItem.type)
        assertEquals("如何学习 Compose？", answerItem.title)
        assertEquals("多写代码多看官方文档。", answerItem.summary)
        assertEquals(1, answerItem.images.size)

        val article = ArticleDto(
            id = 601L,
            title = "Compose 性能优化实战",
            excerpt = "避免不必要的重组。",
            voteupCount = 100,
            commentCount = 15,
            author = AuthorDto(name = "小红", headline = "资深架构师", avatarUrl = "https://example.com/h.jpg"),
            created = 1660000000L,
        )
        val articleItem = article.toPeopleCreationItem()
        assertEquals(601L, articleItem.id)
        assertEquals(PostType.Article, articleItem.type)
        assertEquals("Compose 性能优化实战", articleItem.title)

        val pin = PinDto(
            id = "701",
            contentHtml = "<p>今天天气不错！</p>",
            excerptTitle = "今天天气不错！",
            likeCount = 10,
            commentCount = 2,
            author = AuthorDto(name = "小刚", headline = "学生", avatarUrl = "https://example.com/g.jpg"),
            created = 1670000000L,
        )
        val pinItem = pin.toPeopleCreationItem()
        assertEquals(701L, pinItem.id)
        assertEquals(PostType.Pin, pinItem.type)
        assertEquals("今天天气不错！", pinItem.summary)
    }

    @Test
    fun testColumnAndTopicAndQuestionMappers() {
        val column = MemberColumnItemDto(
            id = "col_1",
            title = "Android 专栏",
            description = "专注移动端",
            articlesCount = 10,
            followerCount = 500,
            followers = 300,
            url = "https://example.com/col",
        )
        val colItem = column.toPeopleColumnItem()
        assertEquals("col_1", colItem.id)
        assertEquals("Android 专栏", colItem.title)
        assertEquals(500, colItem.followerCount)

        val topic = FollowedTopicDto(
            id = "top_1",
            name = "Kotlin",
            topic = FollowedTopicItemDto(id = "top_1", name = "Kotlin"),
        )
        val topItem = topic.toFollowedTopic()
        assertEquals("top_1", topItem.id)
        assertEquals("Kotlin", topItem.name)

        val question = FollowedQuestionDto(
            id = "801",
            title = "什么是纯函数？",
            type = "question",
            url = "https://example.com/q/801",
            questionType = "normal",
            created = 1680000000L,
            updatedTime = 1680001000L,
            answerCount = 5,
            followerCount = 20,
            author = AuthorDto(name = "提问者", avatarUrl = "https://example.com/q.jpg"),
            detail = "<p>在函数式编程中...</p>",
        )
        val qItem = question.toFollowedQuestion()
        assertEquals("801", qItem.id)
        assertEquals("什么是纯函数？", qItem.title)
        assertEquals("在函数式编程中...", qItem.excerpt)
        assertEquals("提问者", qItem.authorName)
    }
}
