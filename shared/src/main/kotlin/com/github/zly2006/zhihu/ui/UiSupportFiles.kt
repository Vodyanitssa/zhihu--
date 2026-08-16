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
package com.github.zly2006.zhihu.ui
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavHostController
import com.fleeksoft.ksoup.Ksoup
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.filter.ContentOpenFrom
import com.github.zly2006.zhihu.markdown.RenderMarkdown
import com.github.zly2006.zhihu.navigation.AnswerNavigator
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.navigation.Pin
import com.github.zly2006.zhihu.navigation.Question
import com.github.zly2006.zhihu.navigation.TopLevelDestination
import com.github.zly2006.zhihu.platform.SettingsStore
import com.github.zly2006.zhihu.platform.UserMessageSink
import com.github.zly2006.zhihu.platform.rememberSettingsStore
import com.github.zly2006.zhihu.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.ui.ANSWER_DOUBLE_TAP_ACTION_PREFERENCE_KEY
import com.github.zly2006.zhihu.ui.AnswerDoubleTapAction
import com.github.zly2006.zhihu.ui.article.prepareContentDocument
import com.github.zly2006.zhihu.ui.components.ANSWER_SWITCH_SENSITIVITY_PREFERENCE_KEY
import com.github.zly2006.zhihu.ui.components.DEFAULT_ANSWER_SWITCH_SENSITIVITY
import com.github.zly2006.zhihu.ui.components.WebviewComp
import com.github.zly2006.zhihu.ui.components.normalizedAnswerSwitchSensitivity
import com.github.zly2006.zhihu.ui.components.setupUpWebviewClient
import com.github.zly2006.zhihu.util.EmojiManager
import com.github.zly2006.zhihu.util.Log
import com.github.zly2006.zhihu.util.OpenInBrowser
import com.github.zly2006.zhihu.util.createEmojiInlineContent
import com.github.zly2006.zhihu.viewmodel.ArticleViewModel.CachedAnswerContent
import com.github.zly2006.zhihu.viewmodel.ZhihuApiEnvironment
import com.github.zly2006.zhihu.viewmodel.filter.encodeBlocklistBackup
import com.github.zly2006.zhihu.viewmodel.filter.getContentFilterDatabase
import com.github.zly2006.zhihu.viewmodel.filter.importBlocklistBackupFromJsonText
import com.github.zly2006.zhihu.viewmodel.getOrFetchContentDetail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.jsoup.Jsoup
import java.io.File

data class PinLikeResult(
    val isLiked: Boolean,
    val likeCount: Int,
)

internal suspend fun fetchPinLinkCardPreview(
    linkCard: DataHolder.Pin.ContentLinkCard,
    env: ZhihuApiEnvironment,
): PinLinkCardPreview? {
    val destination = resolveLinkCardDestination(linkCard) ?: return null
    return when (destination) {
        is Article -> {
            when (val detail = env.getOrFetchContentDetail(destination)) {
                is DataHolder.Article -> PinLinkCardPreview(
                    title = compactTitle(detail.title),
                    preview = compactPreview(detail.excerpt.ifBlank { detail.content }),
                )
                is DataHolder.Answer -> PinLinkCardPreview(
                    title = compactTitle(detail.question.title),
                    preview = compactPreview(detail.excerpt.ifBlank { detail.content }),
                )
                else -> null
            }
        }
        is Question -> {
            (env.getOrFetchContentDetail(destination) as? DataHolder.Question)?.let { detail ->
                PinLinkCardPreview(
                    title = compactTitle(detail.title),
                    preview = compactPreview(detail.detail),
                )
            }
        }
        is Pin -> {
            (env.getOrFetchContentDetail(destination) as? DataHolder.Pin)?.let { detail ->
                PinLinkCardPreview(
                    title = "${detail.author.name} 的想法",
                    preview = compactPreview(detail.contentHtml),
                )
            }
        }
        else -> null
    }
}

internal fun JsonObject?.booleanCompat(vararg keys: String): Boolean {
    if (this == null) return false
    return keys.firstNotNullOfOrNull { key ->
        get(key)?.jsonPrimitive?.booleanOrNull
    } ?: false
}

/**
 * 想法正文的 HTML 渲染入口。
 *
 * 根据当前 WebView 设置选择平台 WebView 或 Compose Markdown 渲染。这样想法页、问题详情和文章页可以共享同一条“正文渲染模式”
 * 语义，避免用户打开 WebView 后只有部分内容类型生效。
 */
@Composable
fun PinHtmlContent(html: String) {
    if (rememberSettingsStore().getBoolean(ARTICLE_USE_WEBVIEW_PREFERENCE_KEY, false) &&
        supportsZhihuHtmlWebView()
    ) {
        ZhihuHtmlWebViewContent(html)
    } else {
        Spacer(Modifier.height(10.dp))
        RenderMarkdown(
            html = html,
            modifier = Modifier.questionSelectionWorkaround(),
            selectable = true,
            enableScroll = false,
        )
    }
}

/**
 * 文章页 Compose UI 使用的运行时设置视图。
 *
 * 这些值保存在可变 state 中，是因为大多数阅读设置都应该在用户已经打开文章时即时生效：标题/底栏自动隐藏、回答切换、
 * WebView 渲染和双击正文动作都不应要求重建页面。持久化仍由 [SettingsStore] 负责；这个类只镜像会影响当前 UI 的值，
 * 并暴露文章页内弹窗会用到的显式保存入口。
 */
class ArticleScreenSettingsState(
    isTitleAutoHide: Boolean,
    autoHideArticleBottomBar: Boolean,
    answerSwitchMode: String,
    answerSwitchSensitivity: Float,
    pinAnswerDate: Boolean,
    useDuo3ArticleActions: Boolean,
    buttonSkipAnswer: Boolean,
    autoHideSkipAnswerButton: Boolean,
    answerDoubleTapAction: AnswerDoubleTapAction,
    useWebView: Boolean,
    private val saveAnswerDoubleTapActionPreference: (AnswerDoubleTapAction) -> Unit,
) {
    var isTitleAutoHide by mutableStateOf(isTitleAutoHide)
    var autoHideArticleBottomBar by mutableStateOf(autoHideArticleBottomBar)
    var answerSwitchMode by mutableStateOf(answerSwitchMode)
    var answerSwitchSensitivity by mutableFloatStateOf(answerSwitchSensitivity)
    var pinAnswerDate by mutableStateOf(pinAnswerDate)
    var useDuo3ArticleActions by mutableStateOf(useDuo3ArticleActions)
    var buttonSkipAnswer by mutableStateOf(buttonSkipAnswer)
    var autoHideSkipAnswerButton by mutableStateOf(autoHideSkipAnswerButton)
    var answerDoubleTapAction by mutableStateOf(answerDoubleTapAction)
    var useWebView by mutableStateOf(useWebView)

    fun saveAnswerDoubleTapAction(action: AnswerDoubleTapAction) {
        answerDoubleTapAction = action
        saveAnswerDoubleTapActionPreference(action)
    }
}

/**
 * 订阅会改变文章页可见阅读行为的设置项。
 *
 * 设置页和文章页内弹窗可能修改同一批 key。这里通过监听这些 key 并原地更新 [ArticleScreenSettingsState]，
 * 让文章 UI 在保留滚动位置、已加载内容和 ViewModel 状态的同时应用新设置。
 */
@Composable
fun rememberArticleScreenSettingsState(): ArticleScreenSettingsState {
    val settings = rememberSettingsStore()
    val state = remember(settings) {
        ArticleScreenSettingsState(
            isTitleAutoHide = settings.getBoolean("titleAutoHide", false),
            autoHideArticleBottomBar = settings.getBoolean("autoHideArticleBottomBar", false),
            answerSwitchMode = settings.getString("answerSwitchMode", "vertical"),
            answerSwitchSensitivity = normalizedAnswerSwitchSensitivity(
                settings.getFloat(
                    ANSWER_SWITCH_SENSITIVITY_PREFERENCE_KEY,
                    DEFAULT_ANSWER_SWITCH_SENSITIVITY,
                ),
            ),
            pinAnswerDate = settings.getBoolean("pinAnswerDate", false),
            useDuo3ArticleActions = settings.getBoolean("duo3_article_actions", false),
            buttonSkipAnswer = settings.getBoolean("buttonSkipAnswer", true),
            autoHideSkipAnswerButton = settings.getBoolean("autoHideSkipAnswerButton", true),
            answerDoubleTapAction = settings.answerDoubleTapAction(),
            useWebView = settings.getBoolean(ARTICLE_USE_WEBVIEW_PREFERENCE_KEY, false),
            saveAnswerDoubleTapActionPreference = { action ->
                settings.putString(
                    ANSWER_DOUBLE_TAP_ACTION_PREFERENCE_KEY,
                    action.preferenceValue,
                )
            },
        )
    }

    DisposableEffect(settings, state) {
        val unregister = settings.observeKeyChanges { key ->
            when (key) {
                "titleAutoHide" -> state.isTitleAutoHide = settings.getBoolean(key, false)
                "autoHideArticleBottomBar" -> {
                    state.autoHideArticleBottomBar = settings.getBoolean(key, false)
                }

                "buttonSkipAnswer" -> state.buttonSkipAnswer = settings.getBoolean(key, true)
                "autoHideSkipAnswerButton" -> state.autoHideSkipAnswerButton = settings.getBoolean(key, true)
                "answerSwitchMode" -> {
                    state.answerSwitchMode = settings.getString(key, "vertical")
                }

                ANSWER_SWITCH_SENSITIVITY_PREFERENCE_KEY -> {
                    state.answerSwitchSensitivity = normalizedAnswerSwitchSensitivity(
                        settings.getFloat(key, DEFAULT_ANSWER_SWITCH_SENSITIVITY),
                    )
                }

                "pinAnswerDate" -> state.pinAnswerDate = settings.getBoolean(key, false)
                "duo3_article_actions" -> state.useDuo3ArticleActions = settings.getBoolean(key, false)
                ARTICLE_USE_WEBVIEW_PREFERENCE_KEY -> state.useWebView = settings.getBoolean(key, false)
                ANSWER_DOUBLE_TAP_ACTION_PREFERENCE_KEY -> {
                    state.answerDoubleTapAction = settings.answerDoubleTapAction()
                }
            }
        }
        onDispose(unregister)
    }

    return state
}

private fun SettingsStore.answerDoubleTapAction(): AnswerDoubleTapAction =
    AnswerDoubleTapAction.fromPreference(
        getString(
            ANSWER_DOUBLE_TAP_ACTION_PREFERENCE_KEY,
            AnswerDoubleTapAction.Ask.preferenceValue,
        ),
    )

/** 过滤部分设备文本选择菜单中的非预期系统项。 */

/**
 * 问题描述正文的渲染入口。
 *
 * 与文章和想法一致，优先遵循用户选择的 WebView/Markdown 渲染模式；当前平台不支持问题详情 WebView 时回落到 Compose Markdown。
 */
@Composable
fun QuestionDetailContent(
    questionId: Long,
    html: String,
) {
    if (rememberSettingsStore().getBoolean(ARTICLE_USE_WEBVIEW_PREFERENCE_KEY, false) &&
        supportsQuestionDetailWebView()
    ) {
        QuestionDetailWebViewContent(
            questionId = questionId,
            html = html,
        )
    } else {
        RenderMarkdown(
            html = html,
            modifier = Modifier.questionSelectionWorkaround(),
            selectable = true,
            enableScroll = false,
        )
    }
}

fun articleActionText(
    article: Article,
    questionId: Long,
    title: String,
    authorName: String,
): String =
    when (article.type) {
        ArticleType.Answer -> {
            "https://www.zhihu.com/question/$questionId/answer/${article.id}\n【$title - $authorName 的回答】"
        }
        ArticleType.Article -> {
            "https://zhuanlan.zhihu.com/p/${article.id}\n【$title - $authorName 的文章】"
        }
    }

fun articleWebUrl(article: Article): String =
    when (article.type) {
        ArticleType.Answer -> "https://www.zhihu.com/answer/${article.id}"
        ArticleType.Article -> "https://zhuanlan.zhihu.com/p/${article.id}"
    }

fun articleSpeechText(
    title: String,
    content: String,
    maxContentLength: Int = 50_000,
): String =
    buildString {
        append(title)
        append("。")
        if (content.isNotEmpty()) {
            val contentToProcess =
                if (content.length > maxContentLength) {
                    content.substring(0, maxContentLength) + "..."
                } else {
                    content
                }
            append(Ksoup.parse(contentToProcess).text())
        }
    }

/**
 * 文章页需要从外围应用获取的宿主级服务。
 *
 * 文章会参与历史记录、回答间导航、内容打开来源归因、TTS、剪贴板和 deep link 交接。这个接口刻意比 Activity 窄，
 * 让 common 文章 UI 能同时运行在 Android、Desktop 和测试环境里，而不依赖平台类。
 */
interface ArticleHost {
    val articleNavController: NavHostController
    val articleAnswerSwitchState: ArticleAnswerSwitchState
    val articleTtsState: TtsState
    var clipboardDestination: NavDestination?

    fun postHistoryDestination(destination: NavDestination)

    fun consumePendingContentOpenFrom(destination: NavDestination): String = ContentOpenFrom.UNKNOWN

    fun consumePendingCommentId(destination: NavDestination): String? = null

    fun speakArticleText(
        text: String,
        title: String,
    )

    fun stopArticleSpeaking()
}

/**
 * 同一问题下不同回答之间导航时使用的共享状态。
 *
 * 手势处理器会在导航前更新这里的状态，让平台适配层选择正确的入场/出场转场方向，并避免 route 切换时丢失待交接的
 * navigator 或内容。它不能放在单个文章 composable 内，因为离开页和进入页都需要通过它协调。
 */
interface ArticleAnswerSwitchState {
    var navigator: AnswerNavigator?
    var pendingNavigator: AnswerNavigator?
    var pendingInitialContent: CachedAnswerContent?
    var navigatingFromAnswerSwitch: Boolean
    var answerSwitchDisposeInProgress: Boolean
    var answerTransitionDirection: ArticleAnswerTransitionDirection
    var isImmersiveMode: Boolean

    fun reset()

    fun promoteForNavigation(direction: ArticleAnswerTransitionDirection)
}

enum class ArticleAnswerTransitionDirection {
    DEFAULT,
    VERTICAL_NEXT,
    VERTICAL_PREVIOUS,
    HORIZONTAL_NEXT,
    HORIZONTAL_PREVIOUS,
}

enum class TtsState(
    val isSpeaking: Boolean = false,
) {
    Uninitialized,
    Initializing,
    Ready,
    Error,
    LoadingText,
    Speaking(true),
    Paused,
    SwitchingChunk(true),
}

/**
 * 影响应用主壳形态的不可变设置快照。
 *
 * 这些值决定底部栏有哪些入口、主 pager 从哪个页面开始、重选 tab 是否回到顶部/刷新，以及顶栏/底栏是否自动隐藏。
 * [ZhihuMain] 按快照读取它们，避免把更新到一半的导航设置应用到主界面。
 */
data class ZhihuMainPreferenceSnapshot(
    val duo3HomeAccount: Boolean,
    val tapToScrollToTopEnabled: Boolean,
    val autoHideBottomBar: Boolean,
    val collectionDirectBrowseEnabled: Boolean,
    val selectedBottomBarItemKeys: List<String>,
    val startDestination: TopLevelDestination,
)

/**
 * 长生命周期主壳使用的 [ZhihuMainPreferenceSnapshot] 可变持有者。
 *
 * 用户每次修改外观设置时不应该重建 NavHost。设置页退出时调用 [reload] 即可；主壳会原地更新底部栏和 pager 状态，
 * 同时保持已加载 tab、返回栈和滚动位置稳定。
 */
class ZhihuMainPreferenceState(
    private val readSnapshot: () -> ZhihuMainPreferenceSnapshot,
) {
    private var snapshot by mutableStateOf(readSnapshot())

    val duo3HomeAccount: Boolean get() = snapshot.duo3HomeAccount
    val tapToScrollToTopEnabled: Boolean get() = snapshot.tapToScrollToTopEnabled
    val autoHideBottomBar: Boolean get() = snapshot.autoHideBottomBar
    val collectionDirectBrowseEnabled: Boolean get() = snapshot.collectionDirectBrowseEnabled
    val selectedBottomBarItemKeys: List<String> get() = snapshot.selectedBottomBarItemKeys
    val startDestination: TopLevelDestination get() = snapshot.startDestination

    fun reload() {
        snapshot = readSnapshot()
    }
}

@Composable
fun rememberZhihuMainPreferenceState(
    readSnapshot: () -> ZhihuMainPreferenceSnapshot,
): ZhihuMainPreferenceState = remember { ZhihuMainPreferenceState(readSnapshot) }

data class AccountSettingsAccountState(
    val login: Boolean = false,
    val hasRequiredCookie: Boolean = true,
    val username: String = "",
    val avatarUrl: String? = null,
    val id: String = "",
    val urlToken: String? = null,
    val identityManagementSupported: Boolean = false,
)

fun noopSettingsStore(): SettingsStore = SettingsStore(
    getBoolean = { _, defaultValue -> defaultValue },
    putBoolean = { _, _ -> },
    getString = { _, defaultValue -> defaultValue },
    putString = { _, _ -> },
    getStringOrNull = { _ -> null },
    putStringSet = { _, _ -> },
    getStringSet = { _, defaultValue -> defaultValue },
    getInt = { _, defaultValue -> defaultValue },
    putInt = { _, _ -> },
    getLong = { _, defaultValue -> defaultValue },
    putLong = { _, _ -> },
    getFloat = { _, defaultValue -> defaultValue },
    putFloat = { _, _ -> },
    remove = { _ -> },
)

internal const val PEOPLE_PROFILE_INCLUDE_PATH =
    "allow_message,is_followed,is_following,is_org,is_blocking,badge_v2,answer_count,follower_count,following_count,articles_count,question_count,pins_count"

data class CommentEmoji(
    val placeholder: String,
    val inlineKey: String,
)

/**
 * 沉浸式阅读时控制系统栏（状态栏/导航栏）的显隐。
 * Android 会隐藏状态栏并允许滑动唤出；Desktop/iOS 为空操作。
 */

/**
 * 离开沉浸式阅读时恢复系统状态栏。
 * 调用时机：导航目的地从 Article 切换到非 Article 时。
 * Android 会显示状态栏；Desktop/iOS 为空操作。
 */

private const val QR_CODE_SCAN_ACTIVITY_CLASS = "com.github.zly2006.zhihu.QRCodeScanActivity"
private const val WEBVIEW_ACTIVITY_CLASS = "com.github.zly2006.zhihu.WebviewActivity"
private const val QR_SCAN_RESULT_EXTRA = "scan_result"

@Composable
fun rememberAccountSettingsAccountState(): androidx.compose.runtime.State<AccountSettingsAccountState> {
    val accountDataState = AccountData.asState()
    return remember(accountDataState.value) {
        androidx.compose.runtime.derivedStateOf {
            accountDataState.value.toAccountSettingsAccountState()
        }
    }
}

@Composable
fun rememberAccountQrLoginRequester(): () -> Unit {
    val context = LocalContext.current
    val scanActivityLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) scan@{ result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val scanResult = result.data?.getStringExtra(QR_SCAN_RESULT_EXTRA) ?: return@scan
            context.startActivity(
                Intent().apply {
                    setClassName(context.packageName, WEBVIEW_ACTIVITY_CLASS)
                    data = scanResult.toUri()
                },
            )
        }
    }
    return remember(context, scanActivityLauncher) {
        { scanActivityLauncher.launch(Intent().setClassName(context.packageName, QR_CODE_SCAN_ACTIVITY_CLASS)) }
    }
}

@Composable
fun rememberAppVersionInfo(): String = LocalContext.current.zhihuVersionInfo()

fun AccountData.Data.toAccountSettingsAccountState(): AccountSettingsAccountState = AccountSettingsAccountState(
    login = login,
    hasRequiredCookie = cookies["d_c0"].isNullOrBlank().not(),
    username = username,
    avatarUrl = self?.avatarUrl,
    id = self?.id ?: "",
    urlToken = self?.urlToken,
    identityManagementSupported = true,
)

private fun Context.zhihuVersionInfo(): String {
    val versionName = runCatching {
        packageManager.getPackageInfo(packageName, 0).versionName
    }.getOrNull() ?: "unknown"
    val appInfo = runCatching {
        packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
    }.getOrNull()
    val metaData = appInfo?.metaData
    val buildType = metaData?.getString("com.github.zly2006.zhihu.BUILD_TYPE")
        ?: if ((applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) "debug" else "release"
    val gitHash = metaData?.getString("com.github.zly2006.zhihu.GIT_HASH") ?: "unknown"
    return "$versionName $buildType, $gitHash"
}

@Composable
fun rememberArticleTtsState(): TtsState {
    val articleHost = LocalContext.current.articleHost()
    return articleHost?.articleTtsState ?: TtsState.Uninitialized
}

@Composable
fun rememberArticleSpeechToggler(): (title: String, content: String) -> Unit {
    val activityContext = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val userMessages = rememberUserMessageSink()
    val articleHost = activityContext.articleHost()
    val ttsState = articleHost?.articleTtsState ?: TtsState.Uninitialized
    return remember(coroutineScope, userMessages, articleHost, ttsState) {
        { title, content ->
            if (ttsState.isSpeaking) {
                articleHost?.stopArticleSpeaking()
            } else if (ttsState !in listOf(TtsState.Error, TtsState.Uninitialized, TtsState.Initializing)) {
                coroutineScope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            val textToRead = articleSpeechText(title, content)
                            withContext(Dispatchers.Main) {
                                if (textToRead.isNotBlank()) {
                                    articleHost?.speakArticleText(textToRead, title)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            userMessages.showMessage("朗读失败：${e.message}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun rememberArticleBrowserOpener(): (Article) -> Unit {
    val context = LocalContext.current.applicationContext
    val coroutineScope = rememberCoroutineScope()
    val userMessages = rememberUserMessageSink()
    return remember(context, coroutineScope, userMessages) {
        { article ->
            coroutineScope.launch {
                OpenInBrowser.openUrlInBrowser(context, article)
                userMessages.showMessage("已发送到浏览器")
            }
        }
    }
}

@Composable
fun rememberArticleHost(): ArticleHost? = LocalContext.current.articleHost()

@Composable
fun ArticleWebViewContent(
    article: Article,
    html: String,
    title: String,
    scrollState: ScrollState,
    rememberedScrollY: Int,
    rememberedScrollYSync: Boolean,
    onRememberedScrollYSyncChange: (Boolean) -> Unit,
    onImageLoadFailed: () -> Unit,
    onDoubleTap: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    WebviewComp(
        onDoubleTap = onDoubleTap,
        scrollState = scrollState,
    ) {
        it.isVerticalScrollBarEnabled = false
        it.setupUpWebviewClient {
            if (!rememberedScrollYSync) {
                coroutineScope.launch {
                    while (scrollState.maxValue < rememberedScrollY) {
                        delay(100)
                    }
                    Log.i("zhihu-scroll", "scroll to $rememberedScrollY, max= ${scrollState.maxValue}, sync on")
                    scrollState.animateScrollTo(rememberedScrollY)
                    onRememberedScrollYSyncChange(true)
                }
            }
        }
        it.contentId = article.id.toString()
        it.loadZhihu(
            "https://www.zhihu.com/${article.type}/${article.id}",
            prepareContentDocument(html, onImageLoadFailed),
            title,
        )
    }
}

fun Modifier.articleMarkdownSelectionWorkaround(): Modifier = this

@Composable
fun rememberHomeIsDebuggable(): Boolean {
    val context = LocalContext.current
    return (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
}

@Composable
fun rememberBlocklistRuleImporter(
    userMessages: UserMessageSink,
): (((String) -> Unit) -> Unit) {
    val context = LocalContext.current
    val database = remember(context) { getContentFilterDatabase(context) }
    val coroutineScope = rememberCoroutineScope()
    var importCallback by remember { mutableStateOf<((String) -> Unit)?>(null) }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val summary = withContext(Dispatchers.IO) {
                        val text = context.contentResolver
                            .openInputStream(uri)
                            ?.bufferedReader()
                            ?.readText()
                            ?: return@withContext "读取文件失败"
                        importBlocklistBackupFromJsonText(
                            keywordDao = database.blockedKeywordDao(),
                            userDao = database.blockedUserDao(),
                            questionAuthorDao = database.blockedQuestionAuthorDao(),
                            topicDao = database.blockedTopicDao(),
                            text = text,
                        )
                    }
                    importCallback?.invoke(summary)
                } catch (e: Exception) {
                    Log.e("BlocklistSettings", "Failed to import blocklist", e)
                    userMessages.showShortMessage("导入失败: ${e.message}")
                }
            }
        }
    }
    return remember(context, database, userMessages, importLauncher) {
        { onImported ->
            importCallback = onImported
            importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
        }
    }
}

@Composable
fun rememberBlocklistRuleExporter(): suspend () -> String {
    val context = LocalContext.current
    val database = remember(context) { getContentFilterDatabase(context) }
    return remember(context, database) {
        suspend {
            val file = withContext(Dispatchers.IO) {
                val dir = context.getExternalFilesDir(null) ?: context.filesDir
                val file = File(dir, "zhihupp_blocklist.json")
                file.writeText(
                    encodeBlocklistBackup(
                        keywordDao = database.blockedKeywordDao(),
                        userDao = database.blockedUserDao(),
                        questionAuthorDao = database.blockedQuestionAuthorDao(),
                        topicDao = database.blockedTopicDao(),
                    ),
                )
                file
            }
            val intent = Intent().apply {
                action = Intent.ACTION_VIEW
                setDataAndType(
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.provider",
                        file,
                    ),
                    "application/json",
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "查看屏蔽规则"))
            "已导出到 ${file.absolutePath}"
        }
    }
}

@Composable
fun ZhihuHtmlWebViewContent(html: String) {
    WebviewComp {
        it.isVerticalScrollBarEnabled = false
        it.setupUpWebviewClient()
        it.loadZhihu(
            "https://www.zhihu.com",
            Jsoup.parse(html),
        )
    }
}

fun supportsZhihuHtmlWebView(): Boolean = true

@Composable
fun rememberCommentEmojiInlineContent(emojiKeys: Set<String>): Map<String, InlineTextContent> =
    remember(emojiKeys) { createEmojiInlineContent(emojiKeys) }

@Composable
fun rememberCommentEmojis(): List<CommentEmoji> {
    val placeholders by EmojiManager.placeholders.collectAsState()
    return remember(placeholders) {
        placeholders.mapNotNull { placeholder ->
            commentEmojiInlineKey(placeholder)?.let { inlineKey ->
                CommentEmoji(placeholder = placeholder, inlineKey = inlineKey)
            }
        }
    }
}

fun commentEmojiInlineKey(placeholder: String): String? {
    val emojiPath = EmojiManager.getEmojiPath(placeholder) ?: return null
    val emojiFileName = emojiPath.substringAfterLast('/')
    return "emoji_$emojiFileName"
}

fun Modifier.commentSelectionWorkaround(): Modifier = this

fun Context.articleHost(): ArticleHost? =
    (this as? ArticleHost) ?: (this as? ContextWrapper)?.baseContext?.takeIf { it !== this }?.articleHost()

@Composable
fun QuestionDetailWebViewContent(
    questionId: Long,
    html: String,
) {
    WebviewComp {
        it.loadZhihu(
            "https://www.zhihu.com/question/$questionId",
            Jsoup.parse(html),
        )
    }
}

fun supportsQuestionDetailWebView(): Boolean = true

fun Modifier.questionSelectionWorkaround(): Modifier = this

@Composable
fun ArticleImmersiveModeEffect(immersive: Boolean) {
    val context = LocalContext.current
    val window = remember(context) { (context as? Activity)?.window }
    LaunchedEffect(window, immersive) {
        window?.let { w ->
            val ctrl = WindowInsetsControllerCompat(w, w.decorView)
            if (immersive) {
                ctrl.hide(WindowInsetsCompat.Type.statusBars())
                ctrl.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                ctrl.show(WindowInsetsCompat.Type.statusBars())
            }
        }
    }
}

@Composable
fun LeaveImmersiveModeCleanup() {
    val context = LocalContext.current
    val window = remember(context) { (context as? Activity)?.window }
    LaunchedEffect(window) {
        window?.let { w ->
            WindowInsetsControllerCompat(w, w.decorView)
                .show(WindowInsetsCompat.Type.statusBars())
        }
    }
}
