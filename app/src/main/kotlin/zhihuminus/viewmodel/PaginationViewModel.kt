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

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
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
import com.zhihuminus.account.ZhihuIdentityClient
import com.zhihuminus.data.AccountData
import com.zhihuminus.data.ContentDetailCache
import com.zhihuminus.data.DataHolder
import com.zhihuminus.data.HistoryStorage
import com.zhihuminus.data.OnlineHistoryDeletePair
import com.zhihuminus.data.ZhihuCookieStorage
import com.zhihuminus.data.ZhihuJson.decodeJson
import com.zhihuminus.data.ZhihuJson.json
import com.zhihuminus.data.ZhihuPaging
import com.zhihuminus.data.executeZhihuAuthenticatedRequest
import com.zhihuminus.data.fetchZhihuAuthenticatedJson
import com.zhihuminus.data.fetchZhihuContentDetail
import com.zhihuminus.data.getOrFetchContentDetail
import com.zhihuminus.navigation.AnswerNavigator
import com.zhihuminus.navigation.NavDestination
import com.zhihuminus.notification.NotificationSettingsStore
import com.zhihuminus.platform.androidUserMessageSink
import com.zhihuminus.ui.ArticleAnswerSwitchState
import com.zhihuminus.ui.ArticleAnswerTransitionDirection
import com.zhihuminus.ui.articleHost
import com.zhihuminus.ui.homeFeedStartupCacheFileNames
import com.zhihuminus.util.HttpStatusException
import com.zhihuminus.util.Log
import com.zhihuminus.util.ZhihuCredentialRefresher
import com.zhihuminus.util.clipboardManager
import com.zhihuminus.util.saveBitmapToGallery
import com.zhihuminus.util.signZhihuFetchRequest
import com.zhihuminus.viewmodel.ArticleViewModel.CachedAnswerContent
import io.ktor.client.HttpClient
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.api.createClientPlugin
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.serializer
import java.io.File
import kotlin.reflect.KType
import io.ktor.http.ContentType as KtorContentType

abstract class PaginationViewModel<T : Any>(
    val dataType: KType,
) : ViewModel() {
    val allData = mutableStateListOf<T>()
    val debugData: MutableList<JsonElement> = mutableListOf()
    var isLoading: Boolean by mutableStateOf(false)
        protected set

    /**
     * 正在加载第一页（首次加载或下拉刷新）。此时 [androidx.compose.material3.pulltorefresh.PullToRefreshBox]
     * 会显示顶部的刷新指示器，列表 footer 不应再显示第二个加载圈圈。
     */
    val isRefreshing: Boolean
        get() = isLoading && lastPaging == null
    var errorMessage: String? = null
        protected set
    protected var lastPaging: ZhihuPaging? by mutableStateOf(null)
    open val isEnd: Boolean get() = lastPaging?.isEnd == true

    /**
     * 首页 URL，供基类 [fetchFeeds] 在没有续页游标时使用；
     * 完全自定义取数（重写 [fetchFeeds]）的子类可不覆写。
     */
    protected open val initialUrl: String? = null

    private var currentJob: Job? = null
    protected open val shouldLogDecodeFailures: Boolean = true

    protected open fun resolvePageUrl(): String = lastPaging?.next ?: initialUrl.orEmpty()

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
        allData.clear()
        lastPaging = null // 重置 lastPaging
        loadMore(environment)
    }

    protected open fun processResponse(environment: PaginationEnvironment, data: List<T>, rawData: JsonArray) {
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

    // 导航动画方向
    override var answerTransitionDirection = ArticleAnswerTransitionDirection.DEFAULT

    override fun reset() {
        navigator = pendingNavigator
        pendingNavigator = null
        pendingInitialContent = null
        navigatingFromAnswerSwitch = false
    }
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

interface HistoryEnvironment {
    fun localHistory(): List<NavDestination> = emptyList()

    suspend fun clearAllHistory() = Unit

    suspend fun postHistoryDestination(destination: NavDestination) = Unit
}

interface ClipboardEnvironment {
    fun setPlainTextClipboard(
        label: String,
        text: String,
    ) = Unit
}

interface ArticleExportEnvironment {
    fun hasImageExportPermission(): Boolean = false

    fun requestImageExportPermission() = Unit

    fun saveImageToMediaStore(
        displayName: String,
        bitmap: Any,
    ) = Unit

    fun articleImageExportRenderer(): ArticleImageExportRenderer? = null

    fun loadExportAssetText(fileName: String): String = ""
}

interface ArticleExportContentEnvironment :
    ArticleExportEnvironment,
    ZhihuApiEnvironment

interface ArticleNavigationEnvironment {
    fun articleAnswerSwitchState(): ArticleAnswerSwitchState? = null
}

interface ContentLoadEnvironment :
    ZhihuApiEnvironment,
    HistoryEnvironment

interface ProfileLoadEnvironment : ContentLoadEnvironment

interface ArticleLoadEnvironment :
    ZhihuApiEnvironment,
    ContentLoadEnvironment,
    ArticleNavigationEnvironment

interface PaginationEnvironment :
    ZhihuApiEnvironment,
    AccountEnvironment,
    MobileHomeFeedEnvironment,
    ClipboardEnvironment,
    ProfileLoadEnvironment,
    ArticleLoadEnvironment,
    ArticleExportContentEnvironment

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
) : AndroidContextPaginationEnvironment {
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

    override fun httpClient(): HttpClient = AccountData.httpClient(context)

    override fun mobileHomeFeedHttpClient(): HttpClient =
        HttpClient {
            install(ContentNegotiation) {
                json(json)
            }
            install(UserAgent) {
                agent = AccountData.ANDROID_USER_AGENT
            }
            install(ZHIHU_PP_ANDROID_HEADERS)
            install(HttpCookies) {
                storage = ZhihuCookieStorage(AccountData.data.cookies) {
                    AccountData.saveData(context, AccountData.data)
                }
            }
        }

    override fun authenticatedCookies(): Map<String, String> = AccountData.data.cookies

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

    override fun localHistory(): List<NavDestination> = HistoryStorage(context).history

    override suspend fun postHistoryDestination(destination: NavDestination) {
        HistoryStorage(context).add(destination)
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

    override fun saveImageToMediaStore(
        displayName: String,
        bitmap: Any,
    ) = saveBitmapToGallery(context, displayName, bitmap as android.graphics.Bitmap)

    override fun articleImageExportRenderer(): ArticleImageExportRenderer =
        AndroidArticleExportRenderer(context)

    override fun hasImageExportPermission(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
            PackageManager.PERMISSION_GRANTED

    override fun loadExportAssetText(fileName: String): String =
        context.assets.open(fileName).use { inputStream ->
            inputStream.bufferedReader().use { reader ->
                reader.readText()
            }
        }

    override fun requestImageExportPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(
                context as Activity,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                1001,
            )
        }
    }
}

class SharedAndroidNotificationEnvironment(
    context: Context,
    override val notificationSettingsStore: NotificationSettingsStore,
) : SharedAndroidPaginationEnvironment(context),
    NotificationEnvironment

fun PaginationViewModel<*>.paginationEnvironment(context: Context): AndroidContextPaginationEnvironment =
    SharedAndroidPaginationEnvironment(context)

@Composable
fun rememberPaginationEnvironment(): PaginationEnvironment {
    val context = LocalContext.current
    return remember(context) { SharedAndroidPaginationEnvironment(context) }
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
    val intent = Intent().setClassName(packageName, "com.zhihuminus.LoginActivity")
    if (this !is Activity) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    startActivity(intent)
}
