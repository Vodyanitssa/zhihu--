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

package com.github.zly2006.zhihu.viewmodel

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.zly2006.zhihu.account.ZhihuIdentityClient
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.ContentDetailCache
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.data.FeedDisplayItem
import com.github.zly2006.zhihu.data.HistoryStorage
import com.github.zly2006.zhihu.data.OnlineHistoryDeletePair
import com.github.zly2006.zhihu.data.ZhihuCookieStorage
import com.github.zly2006.zhihu.data.ZhihuJson.decodeJson
import com.github.zly2006.zhihu.data.ZhihuJson.json
import com.github.zly2006.zhihu.data.ZhihuPaging
import com.github.zly2006.zhihu.data.executeZhihuAuthenticatedRequest
import com.github.zly2006.zhihu.data.fetchZhihuAuthenticatedJson
import com.github.zly2006.zhihu.data.fetchZhihuContentDetail
import com.github.zly2006.zhihu.data.getOrFetchContentDetail
import com.github.zly2006.zhihu.data.navDestination
import com.github.zly2006.zhihu.data.target
import com.github.zly2006.zhihu.filter.ContentOpenEventSupport
import com.github.zly2006.zhihu.navigation.AnswerNavigator
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.notification.NotificationSettingsStore
import com.github.zly2006.zhihu.platform.androidSettingsStore
import com.github.zly2006.zhihu.platform.androidUserMessageSink
import com.github.zly2006.zhihu.ui.ArticleAnswerSwitchState
import com.github.zly2006.zhihu.ui.ArticleAnswerTransitionDirection
import com.github.zly2006.zhihu.ui.articleHost
import com.github.zly2006.zhihu.ui.homeFeedStartupCacheFileNames
import com.github.zly2006.zhihu.util.HttpStatusException
import com.github.zly2006.zhihu.util.Log
import com.github.zly2006.zhihu.util.ResolvedCollectionHtmlExportItem
import com.github.zly2006.zhihu.util.ZhihuCredentialRefresher
import com.github.zly2006.zhihu.util.buildArticleExportFileName
import com.github.zly2006.zhihu.util.buildOfflineArticleExportHtml
import com.github.zly2006.zhihu.util.clipboardManager
import com.github.zly2006.zhihu.util.exportCollectionItemsToZip
import com.github.zly2006.zhihu.util.saveBitmapToGallery
import com.github.zly2006.zhihu.util.signZhihuFetchRequest
import com.github.zly2006.zhihu.viewmodel.ArticleViewModel.CachedAnswerContent
import com.github.zly2006.zhihu.viewmodel.filter.BlockedQuestionAuthor
import com.github.zly2006.zhihu.viewmodel.filter.BlockedUser
import com.github.zly2006.zhihu.viewmodel.filter.ContentFilterManager
import com.github.zly2006.zhihu.viewmodel.filter.ContentType
import com.github.zly2006.zhihu.viewmodel.filter.FeedContentFilterPipeline
import com.github.zly2006.zhihu.viewmodel.filter.FeedDisplayFilterPipeline
import com.github.zly2006.zhihu.viewmodel.filter.ForegroundReadFilterPipeline
import com.github.zly2006.zhihu.viewmodel.filter.contentFilterSettings
import com.github.zly2006.zhihu.viewmodel.filter.getContentFilterDatabase
import io.ktor.client.HttpClient
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.appendAll
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.serializer
import java.io.File
import kotlin.reflect.KType
import com.github.zly2006.zhihu.navigation.Article as ArticleDestination
import com.github.zly2006.zhihu.util.buildArticleExportHtml as buildAndroidArticleExportHtml
import io.ktor.http.ContentType as KtorContentType

abstract class PaginationViewModel<T : Any>(
    val dataType: KType,
) : ViewModel() {
    val allData = mutableStateListOf<T>()
    val debugData: MutableList<JsonElement> = mutableListOf()
    var isLoading: Boolean by mutableStateOf(false)
        protected set
    var errorMessage: String? = null
        protected set
    var allowGuestAccess = false
    protected var lastPaging: ZhihuPaging? by mutableStateOf(null)
    open val isEnd: Boolean get() = lastPaging?.isEnd == true
    protected abstract val initialUrl: String
    private var currentJob: Job? = null
    protected open val shouldLogDecodeFailures: Boolean = true

    protected open fun resolvePageUrl(): String = lastPaging?.next ?: initialUrl

    /**
     * Generally used fields to include in the API request.
     * This can be overridden in subclasses to include more specific fields.
     */
    open val include = "data[*].content,excerpt,headline,target.author.badge_v2"

    open fun refresh(environment: PaginationEnvironment) {
        currentJob?.cancel()
        currentJob = null
        isLoading = false
        errorMessage = null
        debugData.clear()
        allData.clear()
        lastPaging = null // 重置 lastPaging
        loadMore(environment)
    }

    protected open fun processResponse(environment: PaginationEnvironment, data: List<T>, rawData: JsonArray) {
        debugData.addAll(rawData) // 保存原始JSON
        allData.addAll(data) // 保存未flatten的数据
    }

    protected open suspend fun fetchFeeds(environment: PaginationEnvironment) {
        try {
            val url = resolvePageUrl()

            @Suppress("HttpUrlsUsage")
            val json = environment.fetchJson(url.replace("http://", "https://"), include)
                ?: throw RuntimeException("您可能已被风控，请重新登录。", Exception("cause: not json object."))

            val jsonArray = json["data"] as? JsonArray
                ?: throw RuntimeException("您可能已被风控，请重新登录。", Exception("cause: no $.data"))
            processResponse(
                environment,
                jsonArray.mapNotNull {
                    if ("type" in it.jsonObject &&
                        it.jsonObject["type"]?.jsonPrimitive?.content in listOf(
                            "invited_answer", // invalid
                            "tab_list", // invalid
                            "feed_item_index_group", // todo
                        )
                    ) {
                        return@mapNotNull null
                    }
                    try {
                        @Suppress("UNCHECKED_CAST")
                        decodeJson(serializer(dataType) as KSerializer<T>, it)
                    } catch (e: Exception) {
                        if (shouldLogDecodeFailures) {
                            environment.logDecodeFailure(this::class.simpleName, it, e)
                        }
                        null
                    }
                },
                jsonArray,
            )
            if ("paging" in json) {
                lastPaging = decodeJson(json["paging"]!!)
            }
        } catch (e: Exception) {
            if (e is kotlin.coroutines.cancellation.CancellationException) throw e
            environment.handleFetchFailure(this::class.simpleName, e)
        } finally {
            isLoading = false
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    open fun loadMore(environment: PaginationEnvironment) {
        if (isLoading || isEnd) return // 使用新的isEnd getter
        isLoading = true
        currentJob = viewModelScope.launch {
            try {
                fetchFeeds(environment)
            } catch (e: Exception) {
                errorHandle(e)
            }
        }
    }

    protected fun errorHandle(e: Exception) {
        if (e !is CancellationException) {
            errorMessage = e.message
            isLoading = false
        }
    }
}

open class ArticleAnswerSwitchData :
    ViewModel(),
    ArticleAnswerSwitchState {
    /** 活跃的导航器：管理来源、历史记录和预取 */
    override var navigator: AnswerNavigator? by mutableStateOf(null)

    /**
     * 导航前由来源界面设置（如 CollectionContentScreen）。
     * [reset] 时会将其应用到 [navigator]。
     */
    override var pendingNavigator: AnswerNavigator? = null

    // 用于消除切换闪动：导航前设置，新页面用它初始化
    override var pendingInitialContent: CachedAnswerContent? = null

    // 标记是否从回答切换导航进入（避免被 LaunchedEffect 重置方向后误判）
    @kotlin.concurrent.Volatile
    override var navigatingFromAnswerSwitch = false

    // 由 DisposableEffect.onDispose 消费，不受 LaunchedEffect 时序影响
    override var answerSwitchDisposeInProgress = false

    // 导航动画方向
    override var answerTransitionDirection = ArticleAnswerTransitionDirection.DEFAULT

    // 沉浸式阅读模式
    override var isImmersiveMode by mutableStateOf(false)

    override fun reset() {
        navigator = pendingNavigator
        pendingNavigator = null
        pendingInitialContent = null
        navigatingFromAnswerSwitch = false
        isImmersiveMode = false
    }

    override fun promoteForNavigation(direction: ArticleAnswerTransitionDirection) = Unit
}

interface PreparedArticleExportContent

interface ArticleImageExportRenderer {
    suspend fun prepareExportWebView(htmlContent: String, timeoutMs: Long): PreparedArticleExportContent

    suspend fun captureExportBitmap(preparedWebView: PreparedArticleExportContent): Any

    suspend fun destroyExportWebView(preparedWebView: PreparedArticleExportContent)

    fun recycleExportBitmap(bitmap: Any)
}

interface ZhihuApiEnvironment {
    fun httpClient(): HttpClient

    fun authenticatedCookies(): Map<String, String>

    suspend fun <T> withAuthenticatedClient(
        block: suspend (client: HttpClient, cookies: Map<String, String>) -> T,
    ): T = block(httpClient(), authenticatedCookies())

    suspend fun fetchJson(
        url: String,
        include: String,
    ): JsonObject? = withAuthenticatedClient { client, cookies ->
        fetchZhihuAuthenticatedJson(client, url) {
            method = HttpMethod.Get
            url {
                protocol = URLProtocol.HTTPS
                if (include.isNotEmpty()) {
                    parameters["include"] = include
                }
            }
            signZhihuFetchRequest(cookies)
        }
    }

    suspend fun signedGetText(url: String): String = withAuthenticatedClient { client, cookies ->
        executeZhihuAuthenticatedRequest(client, url) {
            method = HttpMethod.Get
            signZhihuFetchRequest(cookies)
        }.bodyAsText()
    }

    suspend fun refreshToken() {
        val client = httpClient()
        ZhihuCredentialRefresher.refreshZhihuToken(
            ZhihuCredentialRefresher.fetchRefreshToken(client),
            client,
        )
    }

    suspend fun handleFetchFailure(
        tag: String?,
        error: Exception,
    )

    fun xsrfToken(): String = ""

    fun logDecodeFailure(
        tag: String?,
        item: JsonElement,
        error: Exception,
    ) {
        Log.e(tag ?: "PaginationViewModel", "Failed to decode item: $item", error)
    }
}

interface AccountEnvironment {
    suspend fun refreshAccountProfile() = Unit

    fun requestLogin(): Boolean = false

    fun clearAccountSession() = Unit

    fun currentAccountId(): String = ""

    fun identityClient(): ZhihuIdentityClient? = null

    fun restartApplication() = Unit

    suspend fun verifyLogin(cookies: Map<String, String>): Boolean = false

    fun saveCookies(cookies: Map<String, String>) = Unit

    fun logout() = clearAccountSession()

    fun requestRelogin(): Boolean {
        clearAccountSession()
        return requestLogin()
    }
}

suspend fun ZhihuApiEnvironment.fetchContentDetail(destination: NavDestination): DataHolder.Content? =
    runCatching {
        fetchZhihuContentDetail(destination) { url, include ->
            fetchJson(url, include)
        }
    }.getOrElse { error ->
        if (error !is CancellationException) {
            Log.e("ZhihuApiEnvironment", "Failed to fetch content detail for $destination", error)
        }
        null
    }

suspend fun ZhihuApiEnvironment.getOrFetchContentDetail(destination: NavDestination): DataHolder.Content? =
    runCatching {
        ContentDetailCache.getOrFetchContentDetail(destination) { url, include ->
            fetchJson(url, include)
        }
    }.getOrElse { error ->
        if (error !is CancellationException) {
            Log.e("ZhihuApiEnvironment", "Failed to fetch content detail for $destination", error)
        }
        null
    }

suspend fun ZhihuApiEnvironment.addReadHistory(
    contentToken: String,
    contentTypeName: String,
) {
    if (authenticatedCookies()["d_c0"] == null) return
    runCatching {
        postSigned("https://www.zhihu.com/api/v4/read_history/add") {
            contentType(KtorContentType.Application.Json)
            setBody(
                buildJsonObject {
                    put("content_token", contentToken)
                    put("content_type", contentTypeName)
                }.toString(),
            )
        }
    }
}

internal suspend fun ZhihuApiEnvironment.deleteOnlineHistoryItem(item: OnlineHistoryDeletePair) {
    val response = postSigned("https://api.zhihu.com/read_history/batch_del") {
        contentType(KtorContentType.Application.Json)
        setBody(
            buildJsonObject {
                put(
                    "pairs",
                    JsonArray(
                        listOf(
                            buildJsonObject {
                                put("content_token", item.contentToken)
                                put("content_type", item.contentType)
                            },
                        ),
                    ),
                )
                put("clear", false)
            }.toString(),
        )
    }
    check(response.status.isSuccess()) { "删除在线历史记录失败: ${response.status}" }
}

suspend fun ZhihuApiEnvironment.postSigned(
    url: String,
    block: HttpRequestBuilder.() -> Unit = {},
): HttpResponse = withAuthenticatedClient { client, cookies ->
    client.post(url) {
        block()
        signZhihuFetchRequest(cookies)
    }
}

suspend fun ZhihuApiEnvironment.deleteSigned(
    url: String,
    block: HttpRequestBuilder.() -> Unit = {},
): HttpResponse = withAuthenticatedClient { client, cookies ->
    client.delete(url) {
        block()
        signZhihuFetchRequest(cookies)
    }
}

interface MobileHomeFeedEnvironment : ZhihuApiEnvironment {
    fun mobileHomeFeedHttpClient(): HttpClient = httpClient()

    suspend fun handleMobileHomeFeedFailure(error: Exception) {
        handleFetchFailure("AndroidHomeFeedViewModel", error)
    }
}

interface FeedDisplayEnvironment {
    fun feedDisplaySettings(): FeedDisplaySettings = FeedDisplaySettings()

    suspend fun applyHomeFeedFilters(items: List<FeedDisplayItem>): HomeFeedFilterResult =
        HomeFeedFilterResult(
            foregroundItems = items,
            filteredItems = items,
        )
}

interface HistoryEnvironment {
    fun localHistory(): List<NavDestination> = emptyList()

    suspend fun clearAllHistory() = Unit

    suspend fun postHistoryDestination(destination: NavDestination) = Unit
}

interface ContentInteractionEnvironment : ZhihuApiEnvironment {
    suspend fun recordContentInteraction(feed: Feed) = Unit
}

interface ContentOpenEnvironment {
    suspend fun recordContentOpenEvent(
        destination: NavDestination,
        questionId: Long? = null,
        openFrom: String = "",
    ) = Unit

    suspend fun recordOpenEvent(
        destination: Article,
        questionId: Long?,
    ) = Unit
}

interface ContentBlocklistEnvironment {
    suspend fun isUserBlocked(userId: String): Boolean = false

    suspend fun isQuestionAuthorBlocked(userId: String): Boolean = false

    fun blockedUserIds(): Set<String> = emptySet()

    suspend fun addBlockedUser(
        userId: String,
        userName: String,
        urlToken: String? = null,
        avatarUrl: String? = null,
    ) = Unit

    suspend fun addBlockedQuestionAuthor(
        userId: String,
        userName: String,
        urlToken: String? = null,
        avatarUrl: String? = null,
    ) = Unit

    suspend fun removeBlockedUser(userId: String) = Unit

    suspend fun removeBlockedQuestionAuthor(userId: String) = Unit
}

interface ClipboardEnvironment {
    fun setPlainTextClipboard(
        label: String,
        text: String,
    ) = Unit
}

interface ArticleExportEnvironment {
    fun hasImageExportPermission(): Boolean = false

    fun requiresHtmlExportPermission(): Boolean = false

    fun requestImageExportPermission() = Unit

    fun loadExportAssetText(fileName: String): String = ""

    fun buildArticleExportHtml(
        content: DataHolder.Content,
        includeAppAttribution: Boolean,
        extraSectionsHtml: String,
    ): String = ""

    suspend fun buildOfflineArticleExportHtml(
        content: DataHolder.Content,
        includeAppAttribution: Boolean,
        httpClient: HttpClient,
    ): String = ""

    fun saveHtmlToDownloads(
        displayName: String,
        htmlContent: String,
    ): String = ""

    fun saveImageToMediaStore(
        displayName: String,
        bitmap: Any,
    ) = Unit

    fun articleImageExportRenderer(loadAssetText: (String) -> String): ArticleImageExportRenderer? = null
}

interface ArticleExportContentEnvironment :
    ArticleExportEnvironment,
    ZhihuApiEnvironment

interface ArticleNavigationEnvironment {
    fun articleAnswerSwitchState(): ArticleAnswerSwitchState? = null
}

interface ContentLoadEnvironment :
    ZhihuApiEnvironment,
    HistoryEnvironment,
    ContentOpenEnvironment

interface ProfileLoadEnvironment :
    ContentLoadEnvironment,
    ContentBlocklistEnvironment

interface ArticleLoadEnvironment :
    ZhihuApiEnvironment,
    ContentLoadEnvironment,
    ArticleNavigationEnvironment

interface PaginationEnvironment :
    ZhihuApiEnvironment,
    AccountEnvironment,
    MobileHomeFeedEnvironment,
    FeedDisplayEnvironment,
    ContentInteractionEnvironment,
    ClipboardEnvironment,
    ProfileLoadEnvironment,
    ArticleLoadEnvironment,
    ArticleExportContentEnvironment

data class FeedDisplaySettings(
    val enableQualityFilter: Boolean = true,
)

data class HomeFeedFilterResult(
    val foregroundItems: List<FeedDisplayItem>,
    val filteredItems: List<FeedDisplayItem>,
)

interface AndroidContextPaginationEnvironment : PaginationEnvironment {
    val context: Context
}

private val ZHIHU_PP_ANDROID_HEADERS = createClientPlugin("ZhihuPPAndroidHeaders", { }) {
    onRequest { request, _ ->
        request.headers.appendAll(AccountData.ANDROID_HEADERS)
    }
}

open class SharedAndroidPaginationEnvironment(
    override val context: Context,
    private val allowGuestAccess: Boolean,
) : AndroidContextPaginationEnvironment,
    CollectionContentEnvironment {
    private val settingsStore by lazy { androidSettingsStore(context) }
    private val userMessageSink by lazy { androidUserMessageSink(context) }

    override suspend fun refreshAccountProfile() {
        AccountData.refreshProfile(context)
    }

    override fun requestLogin(): Boolean {
        context.startLoginActivity()
        return true
    }

    override fun clearAccountSession() {
        AccountData.delete(context)
    }

    override fun currentAccountId(): String = AccountData.data.self
        ?.id
        .orEmpty()

    override fun identityClient() = AccountData.identityClient(context.applicationContext)

    override fun restartApplication() {
        val activity = context as? Activity ?: return
        val launchIntent = activity.packageManager
            .getLaunchIntentForPackage(activity.packageName)
            ?: error("无法获取应用启动入口")
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        activity.startActivity(launchIntent)
    }

    override suspend fun verifyLogin(cookies: Map<String, String>): Boolean =
        AccountData.verifyLogin(context, cookies)

    override fun saveCookies(cookies: Map<String, String>) {
        AccountData.saveData(
            context,
            AccountData.data.copy(cookies = cookies.toMutableMap(), login = true),
        )
    }

    override fun logout() {
        homeFeedStartupCacheFileNames().forEach { fileName ->
            File(context.filesDir, fileName).delete()
        }
        clearAccountSession()
    }

    override fun httpClient(): HttpClient {
        val loginForRecommendation = settingsStore.getBoolean("loginForRecommendation", true)
        if (allowGuestAccess && !loginForRecommendation) {
            return HttpClient {
                install(HttpCache)
                install(ContentNegotiation) {
                    json(json)
                }
                install(UserAgent) {
                    agent = AccountData.data.userAgent
                }
            }
        }
        return AccountData.httpClient(context)
    }

    override fun mobileHomeFeedHttpClient(): HttpClient {
        val loginForRecommendation = settingsStore.getBoolean("loginForRecommendation", true)

        return HttpClient {
            install(ContentNegotiation) {
                json(json)
            }
            install(UserAgent) {
                agent = AccountData.ANDROID_USER_AGENT
            }
            install(ZHIHU_PP_ANDROID_HEADERS)
            if (loginForRecommendation) {
                install(HttpCookies) {
                    storage = ZhihuCookieStorage(AccountData.data.cookies) {
                        AccountData.saveData(context, AccountData.data)
                    }
                }
            }
        }
    }

    override fun authenticatedCookies(): Map<String, String> {
        val loginForRecommendation = settingsStore.getBoolean("loginForRecommendation", true)
        return if (allowGuestAccess && !loginForRecommendation) {
            emptyMap()
        } else {
            AccountData.data.cookies
        }
    }

    override suspend fun handleFetchFailure(
        tag: String?,
        error: Exception,
    ) {
        if (error is HttpStatusException) {
            Log.e(tag, "Response: ${error.bodyText}", error)
            if (tryShowLoginExpiredDialog(error)) {
                return
            }
            showDebugErrorDialog(error)
        }
        Log.e(tag, "Failed to fetch feeds", error)
        context.mainExecutor.execute {
            userMessageSink.showShortMessage("加载失败: ${error.message}")
        }
    }

    override suspend fun handleMobileHomeFeedFailure(error: Exception) {
        Log.e("AndroidHomeFeedViewModel", "Failed to fetch feeds", error)
        context.mainExecutor.execute {
            userMessageSink.showShortMessage("安卓端推荐加载失败: ${error.message}")
        }
    }

    override fun feedDisplaySettings(): FeedDisplaySettings = FeedDisplaySettings(
        enableQualityFilter = settingsStore.getBoolean("enableQualityFilter", true),
    )

    override fun localHistory(): List<NavDestination> = HistoryStorage(context).history

    override suspend fun postHistoryDestination(destination: NavDestination) {
        HistoryStorage(context).add(destination)
    }

    override suspend fun isUserBlocked(userId: String): Boolean =
        getContentFilterDatabase(context).let { database ->
            database.blockedUserDao().isUserBlocked(userId)
        }

    override suspend fun isQuestionAuthorBlocked(userId: String): Boolean =
        getContentFilterDatabase(context).let { database ->
            database.blockedQuestionAuthorDao().isUserBlocked(userId)
        }

    override fun blockedUserIds(): Set<String> =
        kotlinx.coroutines.runBlocking {
            val database = getContentFilterDatabase(context)
            database
                .blockedUserDao()
                .getAllUsers()
                .map { it.userId }
                .toSet()
        }

    override suspend fun addBlockedUser(
        userId: String,
        userName: String,
        urlToken: String?,
        avatarUrl: String?,
    ) {
        val database = getContentFilterDatabase(context)
        database.blockedUserDao().insertUser(
            BlockedUser(
                userId = userId,
                userName = userName,
                urlToken = urlToken,
                avatarUrl = avatarUrl,
            ),
        )
    }

    override suspend fun addBlockedQuestionAuthor(
        userId: String,
        userName: String,
        urlToken: String?,
        avatarUrl: String?,
    ) {
        val database = getContentFilterDatabase(context)
        database.blockedQuestionAuthorDao().insertUser(
            BlockedQuestionAuthor(
                userId = userId,
                userName = userName,
                urlToken = urlToken,
                avatarUrl = avatarUrl,
            ),
        )
    }

    override suspend fun removeBlockedUser(userId: String) {
        val database = getContentFilterDatabase(context)
        database.blockedUserDao().deleteUserById(userId)
    }

    override suspend fun removeBlockedQuestionAuthor(userId: String) {
        val database = getContentFilterDatabase(context)
        database.blockedQuestionAuthorDao().deleteUserById(userId)
    }

    override suspend fun recordContentOpenEvent(
        destination: NavDestination,
        questionId: Long?,
        openFrom: String,
    ) {
        val resolvedOpenFrom = openFrom.ifBlank {
            context.articleHost()?.consumePendingContentOpenFrom(destination) ?: ""
        }
        ContentOpenEventSupport.recordOpenEvent(
            database = getContentFilterDatabase(context),
            destination = destination,
            questionId = questionId,
            openFrom = resolvedOpenFrom.ifBlank { "unknown" },
        )
    }

    override suspend fun recordOpenEvent(
        destination: ArticleDestination,
        questionId: Long?,
    ) {
        recordContentOpenEvent(destination, questionId)
    }

    override suspend fun applyHomeFeedFilters(items: List<FeedDisplayItem>): HomeFeedFilterResult {
        val settings = feedDisplaySettings()
        val filterSettings = context.contentFilterSettings()
        val filterDatabase = getContentFilterDatabase(context)
        val foregroundItems = ForegroundReadFilterPipeline(
            settings = filterSettings,
            contentFilterManager = ContentFilterManager(filterDatabase.contentFilterDao()),
            blockedFeedRecordDao = filterDatabase.blockedFeedRecordDao(),
        ).filter(items)
        val filteredItems = FeedDisplayFilterPipeline(
            settings = filterSettings,
            contentDetailProvider = this::getOrFetchContentDetail,
            contentFilterPipeline = FeedContentFilterPipeline(
                settings = filterSettings,
                blockedKeywordDao = filterDatabase.blockedKeywordDao(),
                blockedUserDao = filterDatabase.blockedUserDao(),
                blockedQuestionAuthorDao = filterDatabase.blockedQuestionAuthorDao(),
                blockedTopicDao = filterDatabase.blockedTopicDao(),
            ),
            blockedFeedRecordDao = filterDatabase.blockedFeedRecordDao(),
            onDetailFetchFailed = { item ->
                Log.w("ContentFilterExtensions", "Failed to fetch content details for item '${item.title}'. Using dummy content for filtering.")
            },
            onDetailsKeywordFiltered = { item, keyword ->
                Log.e("ContentFilterExtensions", "Filtered item '${item.title}' due to keyword '$keyword' in details: ${item.content}")
            },
        ).filter(foregroundItems)
        return HomeFeedFilterResult(
            foregroundItems = foregroundItems,
            filteredItems = filteredItems,
        )
    }

    override suspend fun recordContentInteraction(feed: Feed) {
        val settings = context.contentFilterSettings()
        if (!settings.enableContentFilter) return
        val database = getContentFilterDatabase(context)
        val target = feed.target ?: return
        val (targetType, targetId) = when (target) {
            is Feed.AnswerTarget -> ContentType.ANSWER to target.id.toString()
            is Feed.ArticleTarget -> ContentType.ARTICLE to target.id.toString()
            is Feed.QuestionTarget -> ContentType.QUESTION to target.id.toString()
            is Feed.PinTarget -> ContentType.PIN to target.id.toString()
            else -> return
        }
        ContentFilterManager(database.contentFilterDao()).recordContentInteraction(targetType, targetId)
    }

    override suspend fun clearAllHistory() {
        HistoryStorage(context).clearAndSave()
        postSigned("https://api.zhihu.com/read_history/batch_del") {
            contentType(KtorContentType.Application.Json)
            setBody(
                buildJsonObject {
                    put("pairs", JsonArray(emptyList()))
                    put("clear", true)
                }.toString(),
            )
        }
    }

    override fun articleAnswerSwitchState() = context.articleHost()?.articleAnswerSwitchState

    override suspend fun exportCollectionItemsToHtmlZip(
        collectionTitle: String,
        items: List<CollectionItem>,
        includeImages: Boolean,
        onProgress: suspend (CollectionHtmlExportProgress) -> Unit,
    ): CollectionHtmlExportResult {
        val outputDir = context.getExternalFilesDir(null)
            ?: throw IllegalStateException("外部文件目录不可用")
        val exportHttpClient = httpClient()
        val result = withContext(Dispatchers.IO) {
            exportCollectionItemsToZip(
                collectionTitle = collectionTitle,
                items = items,
                cacheDir = context.cacheDir,
                outputDir = outputDir,
                displayTitle = { item ->
                    item.content.title.ifBlank { item.content.description() }
                },
                resolveItem = { item ->
                    resolveCollectionItemForHtmlExport(
                        item = item,
                        exportHttpClient = exportHttpClient,
                        includeImages = includeImages,
                    )
                },
                onProgress = { progress ->
                    withContext(Dispatchers.Main) {
                        onProgress(
                            CollectionHtmlExportProgress(
                                totalCount = progress.totalCount,
                                processedCount = progress.processedCount,
                                successCount = progress.successCount,
                                skippedCount = progress.skippedCount,
                                failedCount = progress.failedCount,
                                currentTitle = progress.currentTitle,
                            ),
                        )
                    }
                },
            )
        }
        return CollectionHtmlExportResult(
            totalCount = result.totalCount,
            successCount = result.successCount,
            skippedCount = result.skippedCount,
            failedCount = result.failedCount,
            zipFilePath = result.zipFile?.absolutePath,
        )
    }

    override suspend fun handleCollectionExportFailure(error: Exception) {
        Log.e("CollectionContentViewModel", "Failed to export collection HTML zip", error)
        context.mainExecutor.execute {
            userMessageSink.showShortMessage("导出失败: ${error.message}")
        }
    }

    private suspend fun resolveCollectionItemForHtmlExport(
        item: CollectionItem,
        exportHttpClient: HttpClient,
        includeImages: Boolean,
    ): ResolvedCollectionHtmlExportItem? {
        val navDestination = item.content.navDestination as? ArticleDestination ?: return null
        val content = getOrFetchContentDetail(navDestination)
            ?: throw IllegalStateException("无法加载「${item.content.title}」详情")
        if (content !is DataHolder.Answer && content !is DataHolder.Article) {
            return null
        }

        return ResolvedCollectionHtmlExportItem(
            htmlFileName = buildArticleExportFileName(content, "html"),
            htmlContent = buildOfflineArticleExportHtml(
                context = context,
                content = content,
                includeAppAttribution = true,
                httpClient = exportHttpClient,
                includeImages = includeImages,
            ),
        )
    }

    private fun tryShowLoginExpiredDialog(error: HttpStatusException): Boolean {
        try {
            val body = json.parseToJsonElement(error.bodyText).jsonObject
            val errorBody = body["error"]?.jsonObject ?: return false
            if (errorBody["code"]?.jsonPrimitive?.int == 100 &&
                errorBody["message"]?.jsonPrimitive?.content == "ERR_TICKET_NOT_EXIST"
            ) {
                context.mainExecutor.execute {
                    if (context.canSafelyShowDialog()) {
                        AlertDialog
                            .Builder(context)
                            .setTitle("登录已过期")
                            .setMessage("请重新登录以继续使用完整功能。")
                            .setPositiveButton("重新登录") { _, _ ->
                                requestRelogin()
                            }.setNegativeButton("取消", null)
                            .show()
                    }
                }
                return true
            }
        } catch (_: Exception) {
        }
        return false
    }

    private fun showDebugErrorDialog(error: HttpStatusException) {
        context.mainExecutor.execute {
            if (context.canSafelyShowDialog()) {
                AlertDialog
                    .Builder(context)
                    .setTitle("错误 ${error.status}")
                    .setMessage(error.bodyText)
                    .setNeutralButton("复制curl") { _, _ ->
                        val curl = error.dumpedCurlRequest
                        context.clipboardManager
                            .setPrimaryClip(
                                ClipData.newPlainText(
                                    "curl",
                                    curl,
                                ),
                            )
                        userMessageSink.showShortMessage("已复制到剪贴板")
                    }.show()
            }
        }
    }

    // Export methods
    override fun setPlainTextClipboard(
        label: String,
        text: String,
    ) {
        context.clipboardManager.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    override fun buildArticleExportHtml(
        content: DataHolder.Content,
        includeAppAttribution: Boolean,
        extraSectionsHtml: String,
    ): String = buildAndroidArticleExportHtml(
        context = context,
        content = content,
        includeAppAttribution = includeAppAttribution,
        extraSectionsHtml = extraSectionsHtml,
    )

    override suspend fun buildOfflineArticleExportHtml(
        content: DataHolder.Content,
        includeAppAttribution: Boolean,
        httpClient: HttpClient,
    ): String = buildOfflineArticleExportHtml(
        context = context,
        content = content,
        includeAppAttribution = includeAppAttribution,
        httpClient = httpClient,
    )

    override fun saveImageToMediaStore(
        displayName: String,
        bitmap: Any,
    ) = saveBitmapToGallery(context, displayName, bitmap as android.graphics.Bitmap)

    override fun saveHtmlToDownloads(
        displayName: String,
        htmlContent: String,
    ): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/html")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Zhihu++")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                ?: throw IllegalStateException("无法创建下载文件")

            return try {
                resolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { writer ->
                    writer.write(htmlContent)
                } ?: throw IllegalStateException("无法打开下载文件")

                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
                "Zhihu++/$displayName"
            } catch (e: Exception) {
                resolver.delete(uri, null, null)
                throw e
            }
        }

        @Suppress("DEPRECATION")
        val downloadsDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "Zhihu++",
        )
        if (!downloadsDir.exists() && !downloadsDir.mkdirs()) {
            throw IllegalStateException("无法创建下载目录")
        }
        val file = File(downloadsDir, displayName)
        file.writeText(htmlContent)
        return file.absolutePath
    }

    override fun articleImageExportRenderer(loadAssetText: (String) -> String): ArticleImageExportRenderer =
        AndroidArticleExportRenderer(context, loadAssetText)

    override fun hasImageExportPermission(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
            PackageManager.PERMISSION_GRANTED

    override fun requiresHtmlExportPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q

    override fun requestImageExportPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(
                context as Activity,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                1001,
            )
        }
    }

    override fun loadExportAssetText(fileName: String): String =
        context.assets.open(fileName).use { inputStream ->
            inputStream.bufferedReader().use { reader ->
                reader.readText()
            }
        }
}

class SharedAndroidNotificationEnvironment(
    context: Context,
    allowGuestAccess: Boolean,
    override val notificationSettingsStore: NotificationSettingsStore,
) : SharedAndroidPaginationEnvironment(context, allowGuestAccess),
    NotificationEnvironment

fun PaginationViewModel<*>.paginationEnvironment(context: Context): AndroidContextPaginationEnvironment =
    SharedAndroidPaginationEnvironment(context, allowGuestAccess)

@Composable
fun rememberPaginationEnvironment(allowGuestAccess: Boolean): PaginationEnvironment {
    val context = LocalContext.current
    return remember(context, allowGuestAccess) { SharedAndroidPaginationEnvironment(context, allowGuestAccess) }
}

fun PaginationViewModel<*>.refresh(context: Context) {
    refresh(paginationEnvironment(context))
}

fun PaginationViewModel<*>.loadMore(context: Context) {
    loadMore(paginationEnvironment(context))
}

fun PaginationViewModel<*>.httpClient(context: Context): HttpClient =
    paginationEnvironment(context).httpClient()

private fun Context.canSafelyShowDialog(): Boolean {
    val activity = this as? Activity ?: return false
    if (activity.isFinishing || activity.isDestroyed) return false
    val lifecycleOwner = activity as? LifecycleOwner ?: return true
    return lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
}

private fun Context.startLoginActivity() {
    val intent = Intent().setClassName(packageName, "com.github.zly2006.zhihu.LoginActivity")
    if (this !is Activity) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    startActivity(intent)
}
