package com.zhihuminus.feature.question

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuminus.core.content.AstParser
import com.zhihuminus.core.content.ContentNode
import com.zhihuminus.data.FeedDisplayItem
import com.zhihuminus.data.navDestination
import com.zhihuminus.feature.post.PostType
import com.zhihuminus.navigation.PostDestination
import com.zhihuminus.navigation.QuestionAnswerNavigator
import com.zhihuminus.util.Log
import com.zhihuminus.viewmodel.ZhihuApiEnvironment
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jsoup.Jsoup
import kotlin.coroutines.coroutineContext

data class QuestionUiState(
    val title: String = "",
    val detail: QuestionDetail? = null,
    /** 详情 AST 只解析一次，渲染与图片收集共用同一份节点。 */
    val contentNodes: List<ContentNode> = emptyList(),
    val allowDetailCollapse: Boolean = false,
    val answers: List<FeedDisplayItem> = emptyList(),
    val sort: QuestionSort = QuestionSort.DEFAULT,
    val isLoadingQuestion: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isAnswersEnded: Boolean = false,
)

class QuestionViewModel(
    private val questionId: Long,
    initialTitle: String,
    private val repository: QuestionRepository,
    private val apiEnvironment: ZhihuApiEnvironment,
) : ViewModel() {
    var uiState by mutableStateOf(QuestionUiState(title = initialTitle))
        private set

    private val _effect = Channel<QuestionEffect>(capacity = Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var answersJob: Job? = null
    private var answersCursor: String? = null

    init {
        loadFirstAnswersPage()
    }

    /**
     * 加载问题详情。每次进入页面（含从回答页返回）时由 Route 层调用，与旧行为一致地刷新统计数据。
     */
    fun loadMeta() {
        loadQuestion()
    }

    fun onEvent(event: QuestionEvent) {
        when (event) {
            is QuestionEvent.Refresh -> refreshAnswers()
            is QuestionEvent.LoadMore -> loadMoreAnswers()
            is QuestionEvent.ChangeSort -> changeSort(event.sort)
            is QuestionEvent.ToggleFollow -> toggleFollow()
            is QuestionEvent.Navigate -> sendEffect(QuestionEffect.Navigate(event.destination))
            is QuestionEvent.OpenHistoryLog ->
                sendEffect(QuestionEffect.OpenExternalUrl("https://www.zhihu.com/question/$questionId/log"))
        }
    }

    /**
     * 构建从 [item] 开始的回答切换导航器；非回答条目返回 null。
     * 导航器携带当前列表中该回答之前/之后的队列与续页游标，供文章页无缝切换。
     */
    fun createAnswerNavigatorFor(item: FeedDisplayItem): QuestionAnswerNavigator? {
        val clicked = item.navDestination as? PostDestination ?: return null
        if (clicked.type != PostType.Answer) return null
        val index = uiState.answers.indexOfFirst { it.stableKey == item.stableKey }
        if (index < 0) return null
        return QuestionAnswerNavigator(
            questionId = questionId,
            initialNextAnswers = uiState.answers
                .drop(index + 1)
                .mapNotNull { it.navDestination as? PostDestination },
            initialPreviousAnswers = uiState.answers
                .take(index)
                .asReversed()
                .mapNotNull { it.navDestination as? PostDestination },
            initialNextUrl = answersCursor.orEmpty(),
            order = uiState.sort.apiValue,
            environment = apiEnvironment,
        )
    }

    private fun loadQuestion() {
        viewModelScope.launch {
            try {
                val detail = repository.getQuestion(questionId)
                viewModelScope.launch {
                    runCatching { repository.recordRead(questionId) }
                        .onFailure { Log.w(TAG, "Failed to record read history", it) }
                }
                val nodes = AstParser.parseContent(detail.detailHtml)
                val plainTextLength = Jsoup
                    .parse(detail.detailHtml)
                    .text()
                    .trim()
                    .length
                uiState = uiState.copy(
                    title = detail.title,
                    detail = detail,
                    contentNodes = nodes,
                    allowDetailCollapse = plainTextLength >= DETAIL_COLLAPSE_TEXT_THRESHOLD ||
                        detail.detailHtml.contains("<img"),
                    isLoadingQuestion = false,
                )
                sendEffect(QuestionEffect.ContentOpened(detail.title))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load question $questionId", e)
                uiState = uiState.copy(isLoadingQuestion = false)
                sendEffect(QuestionEffect.ShowMessage("获取问题详情失败"))
            }
        }
    }

    private fun loadFirstAnswersPage() {
        if (uiState.isLoadingMore || uiState.isRefreshing) return
        answersCursor = null
        uiState = uiState.copy(isLoadingMore = true)
        answersJob = requestAnswersPage(reset = true)
    }

    private fun refreshAnswers() {
        if (uiState.isRefreshing || uiState.isLoadingMore) return
        answersCursor = null
        uiState = uiState.copy(isRefreshing = true)
        answersJob = requestAnswersPage(reset = true)
    }

    private fun loadMoreAnswers() {
        if (uiState.isLoadingMore || uiState.isRefreshing || uiState.isAnswersEnded) return
        uiState = uiState.copy(isLoadingMore = true)
        answersJob = requestAnswersPage(reset = false)
    }

    private fun changeSort(sort: QuestionSort) {
        if (uiState.sort == sort) return
        answersJob?.cancel()
        answersCursor = null
        uiState = uiState.copy(sort = sort, isRefreshing = true)
        answersJob = requestAnswersPage(reset = true)
    }

    private fun requestAnswersPage(reset: Boolean): Job =
        viewModelScope.launch {
            try {
                val cursor = answersCursor
                val page = if (cursor != null) {
                    repository.loadAnswers(cursor)
                } else {
                    repository.loadAnswers(questionId, uiState.sort)
                }
                answersCursor = page.nextUrl
                uiState = if (reset) {
                    uiState.copy(answers = page.items.distinctBy { it.stableKey }, isAnswersEnded = page.isEnd)
                } else {
                    uiState.copy(
                        answers = appendWithoutDuplicates(uiState.answers, page.items),
                        isAnswersEnded = page.isEnd,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load question feeds", e)
                sendEffect(QuestionEffect.ShowMessage("加载失败: ${e.message}"))
            } finally {
                // 被取消说明已有新任务接管加载标志，不能在这里清掉
                if (coroutineContext.isActive) {
                    uiState = uiState.copy(isRefreshing = false, isLoadingMore = false)
                }
            }
        }

    private fun toggleFollow() {
        val detail = uiState.detail ?: return
        val nextFollowing = !detail.isFollowing
        val previousCount = detail.followerCount
        val optimistic = detail.copy(
            isFollowing = nextFollowing,
            followerCount = (previousCount + if (nextFollowing) 1 else -1).coerceAtLeast(0),
        )
        uiState = uiState.copy(detail = optimistic)

        viewModelScope.launch {
            try {
                repository.followQuestion(questionId, nextFollowing)
                sendEffect(QuestionEffect.ShowMessage(if (nextFollowing) "已关注问题" else "已取消关注问题"))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to toggle follow for question $questionId", e)
                // 回滚乐观更新
                uiState = uiState.copy(detail = detail)
                sendEffect(QuestionEffect.ShowMessage("关注操作失败"))
            }
        }
    }

    private fun appendWithoutDuplicates(
        existing: List<FeedDisplayItem>,
        incoming: List<FeedDisplayItem>,
    ): List<FeedDisplayItem> {
        val seen = existing.mapTo(HashSet()) { it.stableKey }
        return existing + incoming.filter { seen.add(it.stableKey) }
    }

    private fun sendEffect(effect: QuestionEffect?) {
        if (effect == null) return
        viewModelScope.launch {
            _effect.send(effect)
        }
    }

    companion object {
        private const val TAG = "QuestionViewModel"
        private const val DETAIL_COLLAPSE_TEXT_THRESHOLD = 100
    }
}
