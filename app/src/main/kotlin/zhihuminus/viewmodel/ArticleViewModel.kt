/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.zhihuminus.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuminus.core.content.AstParser
import com.zhihuminus.core.renderer.HtmlRenderer
import com.zhihuminus.data.Collection
import com.zhihuminus.data.CollectionResponse
import com.zhihuminus.data.DataHolder
import com.zhihuminus.data.OfficialBadge
import com.zhihuminus.data.VoteUpState
import com.zhihuminus.data.ZhihuJson
import com.zhihuminus.data.decodeZhihuCommentData
import com.zhihuminus.data.officialBadge
import com.zhihuminus.navigation.Article
import com.zhihuminus.navigation.ArticleType
import com.zhihuminus.navigation.CollectionAnswerNavigator
import com.zhihuminus.navigation.PaginationInfoNavigator
import com.zhihuminus.navigation.QuestionAnswerNavigator
import com.zhihuminus.platform.UserMessageSink
import com.zhihuminus.util.ArticleExportComment
import com.zhihuminus.util.Log
import com.zhihuminus.util.applySegmentInfosToHtml
import com.zhihuminus.util.buildArticleExportCommentsHtml
import com.zhihuminus.util.buildArticleExportFileName
import com.zhihuminus.util.escapeArticleExportHtml
import com.zhihuminus.util.prepareArticleExportComment
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ArticleViewModel(
    private val article: Article,
    val httpClient: HttpClient?,
    private val userMessages: UserMessageSink = UserMessageSink(showShortMessage = {}),
    registerOnPause: (((() -> Unit) -> Unit))? = null,
) : ViewModel() {
    var permissionRequestCount by mutableIntStateOf(0)
    var title by mutableStateOf("")
    var authorId by mutableStateOf("")
    var authorUrlToken by mutableStateOf("")
    var authorName by mutableStateOf("")
    var authorBio by mutableStateOf("")
    var authorAvatarSrc by mutableStateOf("")
    var authorBadge by mutableStateOf<OfficialBadge?>(null)
    var content by mutableStateOf("")
    var attachment by mutableStateOf<JsonElement?>(null)
    var voteUpCount by mutableIntStateOf(0)
    var commentCount by mutableIntStateOf(0)
    var voteUpState by mutableStateOf(VoteUpState.Neutral)
    var votersTotal by mutableIntStateOf(0)
        private set
    var votersNextUrl by mutableStateOf<String?>(null)
        private set
    var votersLoading by mutableStateOf(false)
        private set
    var votersError by mutableStateOf<String?>(null)
        private set
    var votersSocialText by mutableStateOf("")
        private set
    val voters = mutableStateListOf<DataHolder.Author>()
    var questionId by mutableLongStateOf(0L)
    var answerNextIds by mutableStateOf<List<Long>>(emptyList())
        private set
    var collections = mutableStateListOf<Collection>()
    var updatedAt by mutableLongStateOf(0L)
    var createdAt by mutableLongStateOf(0L)
    var ipInfo by mutableStateOf<String?>(null)
    var endorsements by mutableStateOf<List<DataHolder.AnswerEndorsementDisplay>>(emptyList())
    var topics by mutableStateOf<List<DataHolder.Topic>>(emptyList())
    var endorsementTexts: List<String>
        get() = endorsements.map { endorsement -> endorsement.text }
        set(value) {
            endorsements = value.map { text -> DataHolder.AnswerEndorsementDisplay(text = text) }
        }
    private var exportSourceContent: DataHolder.Content? = null

    // scroll fix
    var rememberedScrollY by mutableIntStateOf(0)
    var rememberedScrollYSync = true

    /**
     * 缓存的回答完整内容，用于水平滑动预览。
     */
    data class CachedAnswerContent(
        val article: Article,
        val title: String,
        val authorName: String,
        val authorBio: String,
        val authorAvatarUrl: String,
        val authorBadge: OfficialBadge? = null,
        val content: String,
        val voteUpCount: Int,
        val commentCount: Int,
        val createdAt: Long = 0L,
        val updatedAt: Long = 0L,
        val ipInfo: String? = null,
        val endorsements: List<DataHolder.AnswerEndorsementDisplay> = emptyList(),
        /** 来源标签，用于 UI 显示，例如 "此问题"、"「收藏夹名称」" */
        val sourceLabel: String = "此问题",
    ) {
        val endorsementTexts: List<String>
            get() = endorsements.map { endorsement -> endorsement.text }
    }

    fun toCachedContent(sourceLabel: String = "此问题"): CachedAnswerContent = CachedAnswerContent(
        article = article,
        title = title,
        authorName = authorName,
        authorBio = authorBio,
        authorAvatarUrl = authorAvatarSrc,
        authorBadge = authorBadge,
        content = content,
        voteUpCount = voteUpCount,
        commentCount = commentCount,
        createdAt = createdAt,
        updatedAt = updatedAt,
        ipInfo = ipInfo,
        endorsements = endorsements,
        sourceLabel = sourceLabel,
    )

    init {
        Log.i("zhihu-scroll", "me is $this")
        registerOnPause?.invoke {
            rememberedScrollYSync = false
        }
    }

    val isFavorited: Boolean
        get() = collections.any { it.isFavorited }

    @OptIn(ExperimentalStdlibApi::class)
    fun loadArticle(environment: ArticleLoadEnvironment) {
        if (httpClient == null) return
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                try {
                    when (article.type) {
                        ArticleType.Answer -> loadAnswerContent(environment)
                        ArticleType.Article -> loadArticleContent(environment)
                    }
                } catch (e: Exception) {
                    Log.e("ArticleViewModel", "Failed to load content", e)
                }
            }
        }
    }

    private suspend fun loadAnswerContent(environment: ArticleLoadEnvironment) {
        val sharedData = environment.articleAnswerSwitchState()
        val answer = environment.fetchContentDetail(article) as? DataHolder.Answer
        if (answer == null) {
            content = "<h1>你似乎来到了没有知识存在的荒原</h1>"
            endorsements = emptyList()
            Log.e("ArticleViewModel", "Answer not found")
            return
        }
        exportSourceContent = answer
        title = answer.question.title
        authorName = answer.author.name
        authorId = answer.author.id
        authorUrlToken = answer.author.urlToken
        content = applySegmentInfosToHtml(
            content = answer.content,
            segmentInfos = answer.segmentInfos,
            sourceUrl = "https://www.zhihu.com/question/${answer.question.id}/answer/${answer.id}",
            contentId = answer.id.toString(),
            contentType = "answer",
        )
        attachment = answer.attachment
        authorBio = answer.author.headline
        authorAvatarSrc = answer.author.avatarUrl
        authorBadge = answer.author.badgeV2.officialBadge()
        voteUpCount = answer.voteupCount
        votersTotal = answer.voteupCount
        commentCount = answer.commentCount
        questionId = answer.question.id
        answerNextIds = answer.paginationInfo?.nextAnswerIds.orEmpty()
        voteUpState = VoteUpState.from(answer.reaction?.relation?.vote)
        updatedAt = answer.updatedTime
        createdAt = answer.createdTime
        ipInfo = answer.ipInfo
        endorsements = answer.endorsementItems
        topics = emptyList()

        environment.postHistoryDestination(
            Article(
                id = answer.id,
                type = ArticleType.Answer,
                title = answer.question.title,
                authorName = answer.author.name,
                authorBio = answer.author.headline,
                avatarSrc = answer.author.avatarUrl,
                excerpt = answer.excerpt,
            ),
        )
        environment.recordOpenEvent(article, answer.question.id)
        withContext(Dispatchers.Main.immediate) {
            // 设置问题回答导航器（如果当前不是收藏夹导航器）
            if (sharedData?.navigator !is CollectionAnswerNavigator) {
                val existingNav = sharedData?.navigator
                val isSameQuestion = when (existingNav) {
                    is QuestionAnswerNavigator -> existingNav.questionId == questionId
                    is PaginationInfoNavigator -> existingNav.questionId == questionId
                    else -> false
                }
                if (!isSameQuestion) {
                    sharedData?.navigator = QuestionAnswerNavigator(
                        questionId = questionId,
                        environment = environment,
                    )
                }
            }
            sharedData?.navigator?.pushAnswer(
                toCachedContent(sourceLabel = sharedData.navigator?.sourceName ?: "此问题"),
            )
        }
        loadAnswerRelationshipEndorsement(environment)
        loadMoreVoters(environment, reset = true)

        // 仅在无前向历史时预取下一个回答
        withContext(Dispatchers.Main.immediate) {
            sharedData?.navigator?.let { nav ->
                if (nav.currentAnswerIndex >= nav.answerHistory.size - 1) {
                    nav.prefetchNext(article.id)
                }
                nav.prefetchPrevious(article.id)
            }
        }
    }

    private suspend fun loadArticleContent(environment: ArticleLoadEnvironment) {
        val articleDetail = environment.fetchContentDetail(article) as? DataHolder.Article
        if (articleDetail == null) {
            content = "<h1>你似乎来到了没有知识存在的荒原</h1>"
            Log.e("ArticleViewModel", "Article not found")
            return
        }
        endorsements = emptyList()
        exportSourceContent = articleDetail
        title = articleDetail.title
        content = applySegmentInfosToHtml(
            content = articleDetail.content,
            segmentInfos = articleDetail.segmentInfos,
            sourceUrl = "https://zhuanlan.zhihu.com/p/${articleDetail.id}",
            contentId = articleDetail.id.toString(),
            contentType = "article",
        )
        voteUpCount = articleDetail.voteupCount
        votersTotal = articleDetail.voteupCount
        commentCount = articleDetail.commentCount
        authorId = articleDetail.author.id
        authorUrlToken = articleDetail.author.urlToken
        authorName = articleDetail.author.name
        authorBio = articleDetail.author.headline
        authorAvatarSrc = articleDetail.author.avatarUrl
        authorBadge = articleDetail.author.badgeV2.officialBadge()
        voteUpState = VoteUpState.from(articleDetail.reaction?.relation?.vote)
        updatedAt = articleDetail.updated
        createdAt = articleDetail.created
        ipInfo = articleDetail.ipInfo
        topics = articleDetail.topics.orEmpty()

        environment.postHistoryDestination(
            Article(
                id = articleDetail.id,
                type = ArticleType.Article,
                title = articleDetail.title,
                authorName = articleDetail.author.name,
                authorBio = articleDetail.author.headline,
                avatarSrc = articleDetail.author.avatarUrl,
                excerpt = articleDetail.excerpt,
            ),
        )
        environment.recordOpenEvent(this.article, null)
    }

    fun toggleFavorite(collectionId: String, remove: Boolean, environment: ZhihuApiEnvironment) {
        if (httpClient == null) return
        CoroutineScope(Dispatchers.Main.immediate).launch {
            try {
                val contentType = when (article.type) {
                    ArticleType.Answer -> "answer"
                    ArticleType.Article -> "article"
                }
                val action = if (remove) "remove" else "add"
                val url = "https://api.zhihu.com/collections/contents/$contentType/${article.id}"
                val body = "${action}_collections=$collectionId"

                val response = httpClient.put(url) {
                    contentType(ContentType.Application.FormUrlEncoded)
                    setBody(body)
                }

                if (response.status.isSuccess()) {
                    loadCollections(environment)
                    userMessages.showShortMessage(if (remove) "取消收藏成功" else "收藏成功")
                } else {
                    userMessages.showShortMessage("收藏操作失败")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("ArticleViewModel", "Favorite toggle failed", e)
                userMessages.showShortMessage("收藏操作失败: ${e.message}")
            }
        }
    }

    private val collectionOrder = mutableListOf<String>()

    fun loadCollections(environment: ZhihuApiEnvironment) {
        if (httpClient == null) return
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                try {
                    val contentType = when (article.type) {
                        ArticleType.Answer -> "answer"
                        ArticleType.Article -> "article"
                    }
                    val collectionsUrl =
                        "https://api.zhihu.com/collections/contents/$contentType/${article.id}?limit=50"
                    val jojo = environment.fetchJson(collectionsUrl, "") ?: return@withContext
                    val collectionsData = ZhihuJson.decodeJson<CollectionResponse>(jojo)
                    collections.clear()
                    collections.addAll(
                        collectionsData.data
                            .sortedWith { a, b ->
                                val indexA = collectionOrder.indexOf(a.id)
                                val indexB = collectionOrder.indexOf(b.id)
                                when {
                                    indexA == -1 && indexB == -1 -> 0
                                    // 把新的放前面
                                    indexA == -1 -> -1
                                    indexB == -1 -> 1
                                    else -> indexA.compareTo(indexB)
                                }
                            },
                    )
                    collectionOrder.clear()
                    collectionOrder.addAll(collections.map { it.id })
                } catch (e: Exception) {
                    Log.e("ArticleViewModel", "Failed to load collections", e)
                }
            }
        }
    }

    fun createNewCollection(
        environment: ZhihuApiEnvironment,
        title: String,
        description: String = "",
        isPublic: Boolean = false,
    ) {
        if (httpClient == null) return
        viewModelScope.launch {
            environment.postSigned("https://www.zhihu.com/api/v4/collections") {
                contentType(ContentType.Application.Json)
                setBody(
                    buildJsonObject {
                        put("title", title)
                        put("description", description)
                        put("is_public", isPublic)
                    },
                )
            }
            loadCollections(environment)
        }
    }

    fun toggleVoteUp(environment: ZhihuApiEnvironment, newState: VoteUpState) {
        viewModelScope.launch {
            try {
                val endpoint = when (article.type) {
                    ArticleType.Answer -> "https://www.zhihu.com/api/v4/answers/${article.id}/voters"
                    ArticleType.Article -> "https://www.zhihu.com/api/v4/articles/${article.id}/voters"
                }

                val response = environment
                    .postSigned(endpoint) {
                        when (article.type) {
                            ArticleType.Answer -> setBody(mapOf("type" to newState.key))
                            ArticleType.Article -> setBody(mapOf("voting" to if (newState == VoteUpState.Up) 1 else 0))
                        }
                        contentType(ContentType.Application.Json)
                    }.body<JsonObject>()

                voteUpState = newState
                voteUpCount = response["voteup_count"]!!.jsonPrimitive.int
                votersTotal = voteUpCount
                if (article.type == ArticleType.Answer) {
                    loadAnswerRelationshipEndorsement(environment)
                    loadMoreVoters(environment, reset = true)
                }
            } catch (e: Exception) {
                Log.e("ArticleViewModel", "Vote up failed", e)
                userMessages.showShortMessage("点赞失败: ${e.message}")
            }
        }
    }

    fun loadMoreVoters(environment: ZhihuApiEnvironment, reset: Boolean = false) {
        if (article.type != ArticleType.Answer || votersLoading) return
        viewModelScope.launch {
            votersLoading = true
            votersError = null
            try {
                val page = loadVotersPage(
                    environment = environment,
                    initialUrl = "https://www.zhihu.com/api/v4/answers/${article.id}/upvoters?limit=10&offset=0",
                    nextUrl = votersNextUrl,
                    reset = reset,
                )
                voters.replaceOrAppendUniqueVoters(page.data, reset)
                votersTotal = page.paging.totals.takeIf { it > 0 } ?: voteUpCount
                votersNextUrl = page.nextUrlOrNull()
            } catch (e: Exception) {
                Log.e("ArticleViewModel", "Failed to load answer voters", e)
                votersError = e.message ?: "加载赞同者失败"
            } finally {
                votersLoading = false
            }
        }
    }

    fun loadAnswerRelationshipEndorsement(environment: ZhihuApiEnvironment) {
        if (article.type != ArticleType.Answer) return
        viewModelScope.launch {
            try {
                val response = environment.fetchJson(
                    "https://www.zhihu.com/api/v4/answers/${article.id}/relationship?desktop=true",
                    "",
                )
                    ?: return@launch
                val endorsement = ZhihuJson.decodeJson<AnswerRelationshipEndorsement>(response)
                votersSocialText = endorsement.text
            } catch (e: Exception) {
                Log.e("ArticleViewModel", "Failed to load answer relationship endorsement", e)
                votersSocialText = ""
            }
        }
    }

    suspend fun exportImage(
        environment: ArticleExportContentEnvironment,
        includeComments: Boolean,
        onComplete: (Boolean) -> Unit,
    ) {
        runCatching { requireExportSourceContent() }.onFailure { error ->
            withContext(Dispatchers.Main) {
                userMessages.showShortMessage(error.message ?: "内容未加载完成")
                onComplete(false)
            }
            return
        }

        if (!environment.hasImageExportPermission()) {
            withContext(Dispatchers.Main) {
                environment.requestImageExportPermission()
                permissionRequestCount++
                userMessages.showShortMessage("需要存储权限才能导出图片，正在请求权限")
                onComplete(false)
            }
            return
        }

        var preparedWebView: PreparedArticleExportContent? = null
        var bitmap: Any? = null
        val renderer = environment.articleImageExportRenderer()!!
        try {
            preparedWebView = renderer.prepareExportWebView(
                htmlContent = createHtmlContent(
                    environment = environment,
                    includeComments = includeComments,
                ),
                timeoutMs = if (includeComments) 18_000L else 15_000L,
            )
            val capturedBitmap = renderer.captureExportBitmap(preparedWebView)
            bitmap = capturedBitmap
            withContext(Dispatchers.Default) {
                environment.saveImageToMediaStore(
                    displayName = buildArticleExportFileName(
                        content = requireExportSourceContent(),
                        extension = "jpg",
                    ),
                    bitmap = capturedBitmap,
                )
            }
            withContext(Dispatchers.Main) {
                userMessages.showLongMessage("图片已保存到相册")
                onComplete(true)
            }
        } catch (e: Exception) {
            Log.e("ArticleViewModel", "Image export failed", e)
            withContext(Dispatchers.Main) {
                userMessages.showShortMessage("图片导出失败: ${e.message}")
                onComplete(false)
            }
        } finally {
            bitmap?.let { renderer.recycleExportBitmap(it) }
            preparedWebView?.let { renderer.destroyExportWebView(it) }
        }
    }

    // 创建HTML内容：article → AST(ContentNode) → HtmlRenderer → HTML
    private suspend fun createHtmlContent(
        environment: ArticleExportContentEnvironment,
        includeComments: Boolean,
    ): String {
        val commentsHtml = if (includeComments) {
            buildArticleExportCommentsHtml(
                comments = fetchExportComments(environment, DEFAULT_EXPORT_COMMENT_COUNT),
                requestedCount = DEFAULT_EXPORT_COMMENT_COUNT,
            )
        } else {
            ""
        }

        val contentHtml = when (val exportSource = requireExportSourceContent()) {
            is DataHolder.Answer -> exportSource.content
            is DataHolder.Article -> exportSource.content
            else -> throw IllegalArgumentException("Unsupported content type")
        }
        val nodes = AstParser.parseContent(contentHtml)
        val bodyHtml = HtmlRenderer.render(nodes)

        return """
            |<!DOCTYPE html>
            |<html>
            |<head>
            |<meta charset="UTF-8"/>
            |<meta name="viewport" content="width=device-width,initial-scale=1"/>
            |<style>
            |body{max-width:680px;margin:0 auto;padding:24px 16px;font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,"Helvetica Neue",Arial,sans-serif;color:#1a1a1a;line-height:1.8;font-size:16px}
            |.article-header{margin-bottom:24px;border-bottom:1px solid #eee;padding-bottom:16px}
            |.article-title{font-size:24px;font-weight:700;margin:0 0 12px;line-height:1.4}
            |.article-meta{font-size:14px;color:#8590a6}
            |.article-meta strong{color:#1a1a1a}
            |img{max-width:100%;height:auto}
|.emoji{display:inline-block;width:1.3em;height:1.3em;vertical-align:text-bottom}
            |pre{background:#f6f8fa;padding:16px;border-radius:6px;overflow-x:auto}
            |code{font-family:"SFMono-Regular",Consolas,"Liberation Mono",Menlo,monospace;font-size:14px}
            |blockquote{border-left:3px solid #ddd;margin:0;padding:8px 16px;color:#666}
            |table{border-collapse:collapse;width:100%}
            |th,td{border:1px solid #ddd;padding:8px;text-align:left}
            |th{background:#f6f8fa}
            |figure{margin:16px 0;text-align:center}
            |figcaption{font-size:13px;color:#999;margin-top:4px}
            |a{color:#175199;text-decoration:none}
            |a:hover{text-decoration:underline}
            |.comment{margin:12px 0;padding:12px;background:#fafafa;border-radius:8px}
            |.comment-author{font-weight:600;font-size:14px;margin-bottom:4px}
            |.comment-content{font-size:14px;line-height:1.6}
            |.comment-time{font-size:12px;color:#999;margin-top:4px}
                        |.comments-title{font-size:18px;font-weight:600;margin:24px 0 12px;padding-top:16px;border-top:1px solid #eee}
            |</style>
            |</head>
            |<body>
            |<div class="article-header">
            |<h1 class="article-title">${escapeArticleExportHtml(title)}</h1>
            |<div class="article-meta"><strong>${escapeArticleExportHtml(authorName)}</strong> · $voteUpCount 赞 · $commentCount 评论</div>
            |</div>
            |$bodyHtml
            |$commentsHtml
            |</body>
            |</html>
            """.trimMargin()
    }

    private suspend fun fetchExportComments(
        environment: ArticleExportContentEnvironment,
        requestedCount: Int,
    ): List<ArticleExportComment> {
        val safeRequestedCount = requestedCount.coerceAtLeast(0)
        if (safeRequestedCount == 0) return emptyList()

        val url = when (article.type) {
            ArticleType.Answer -> "https://www.zhihu.com/api/v4/comment_v5/answers/${article.id}/root_comment"
            ArticleType.Article -> "https://www.zhihu.com/api/v4/comment_v5/articles/${article.id}/root_comment"
        }
        val json = environment.fetchJson(
            url = "$url?order=score&limit=${safeRequestedCount.coerceAtMost(20)}",
            include = "data[*].content,excerpt,headline",
        ) ?: return emptyList()
        return decodeZhihuCommentData(json, safeRequestedCount)
            .map { comment ->
                prepareArticleExportComment(
                    authorName = comment.author.name,
                    content = comment.content,
                    createdTimeText = formatArticleDateTime(comment.createdTime).dropLast(3),
                )
            }
    }

    private fun requireExportSourceContent(): DataHolder.Content = exportSourceContent
        ?: throw IllegalStateException("内容未加载完成")
}

private const val DEFAULT_EXPORT_COMMENT_COUNT = 3

private val articleDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

fun formatArticleDateTime(seconds: Long): String =
    Instant
        .ofEpochSecond(seconds)
        .atZone(ZoneId.systemDefault())
        .format(articleDateFormatter)

@Serializable
private data class AnswerRelationshipEndorsement(
    val type: String = "",
    val text: String = "",
)
