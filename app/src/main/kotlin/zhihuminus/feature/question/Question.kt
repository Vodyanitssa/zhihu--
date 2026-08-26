package com.zhihuminus.feature.question

import com.zhihuminus.data.FeedDisplayItem

/** 问题回答流的排序方式。 */
enum class QuestionSort(
    val apiValue: String,
) {
    DEFAULT("default"),
    LATEST("updated"),
}

data class QuestionTopic(
    val id: String,
    val name: String,
)

/**
 * 问题详情页的展示模型。
 */
data class QuestionDetail(
    val title: String,
    val detailHtml: String,
    val excerpt: String,
    val visitCount: Int,
    val commentCount: Int,
    val followerCount: Int,
    val answerCount: Int,
    val voteupCount: Int,
    val isFollowing: Boolean,
    val topics: List<QuestionTopic>,
)

/**
 * 一页回答流。[nextUrl] 为 null 表示没有下一页。
 */
data class QuestionAnswersPage(
    val items: List<FeedDisplayItem>,
    val nextUrl: String?,
    val isEnd: Boolean,
)
