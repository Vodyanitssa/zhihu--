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
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.navigation.NavHostController
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
import com.github.zly2006.zhihu.ui.components.ANSWER_SWITCH_SENSITIVITY_PREFERENCE_KEY
import com.github.zly2006.zhihu.ui.components.DEFAULT_ANSWER_SWITCH_SENSITIVITY
import com.github.zly2006.zhihu.ui.components.normalizedAnswerSwitchSensitivity
import com.github.zly2006.zhihu.util.EmojiManager
import com.github.zly2006.zhihu.util.Log
import com.github.zly2006.zhihu.util.createEmojiInlineContent
import com.github.zly2006.zhihu.viewmodel.ArticleViewModel.CachedAnswerContent
import com.github.zly2006.zhihu.viewmodel.ZhihuApiEnvironment
import com.github.zly2006.zhihu.viewmodel.filter.encodeBlocklistBackup
import com.github.zly2006.zhihu.viewmodel.filter.getContentFilterDatabase
import com.github.zly2006.zhihu.viewmodel.filter.importBlocklistBackupFromJsonText
import com.github.zly2006.zhihu.viewmodel.getOrFetchContentDetail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonPrimitive
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
    Spacer(Modifier.height(10.dp))
    RenderMarkdown(
        html = html,
        modifier = Modifier.questionSelectionWorkaround(),
        selectable = true,
        enableScroll = false,
    )
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
    buttonSkipAnswer: Boolean,
    autoHideSkipAnswerButton: Boolean,
) {
    var isTitleAutoHide by mutableStateOf(isTitleAutoHide)
    var autoHideArticleBottomBar by mutableStateOf(autoHideArticleBottomBar)
    var answerSwitchMode by mutableStateOf(answerSwitchMode)
    var answerSwitchSensitivity by mutableFloatStateOf(answerSwitchSensitivity)
    var pinAnswerDate by mutableStateOf(pinAnswerDate)
    var buttonSkipAnswer by mutableStateOf(buttonSkipAnswer)
    var autoHideSkipAnswerButton by mutableStateOf(autoHideSkipAnswerButton)
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
            buttonSkipAnswer = settings.getBoolean("buttonSkipAnswer", true),
            autoHideSkipAnswerButton = settings.getBoolean("autoHideSkipAnswerButton", true),
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
            }
        }
        onDispose(unregister)
    }

    return state
}

/** 过滤部分设备文本选择菜单中的非预期系统项。 */

/**
 * 问题描述正文的渲染入口。
 *
 * 与文章和想法一致，Compose Markdown。
 */
@Composable
fun QuestionDetailContent(
    questionId: Long,
    html: String,
) {
    RenderMarkdown(
        html = html,
        modifier = Modifier.questionSelectionWorkaround(),
        selectable = true,
        enableScroll = false,
    )
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

/**
 * 文章页需要从外围应用获取的宿主级服务。
 *
 * 文章会参与历史记录、回答间导航、内容打开来源归因、剪贴板和 deep link 交接。这个接口刻意比 Activity 窄，
 */
interface ArticleHost {
    val articleNavController: NavHostController
    val articleAnswerSwitchState: ArticleAnswerSwitchState
    var clipboardDestination: NavDestination?

    fun postHistoryDestination(destination: NavDestination)

    fun consumePendingContentOpenFrom(destination: NavDestination): String = ContentOpenFrom.UNKNOWN

    fun consumePendingCommentId(destination: NavDestination): String? = null
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

/**
 * 影响应用主壳形态的不可变设置快照。
 *
 * 这些值决定底部栏有哪些入口、主 pager 从哪个页面开始、重选 tab 是否回到顶部/刷新，以及顶栏/底栏是否自动隐藏。
 * [ZhihuMain] 按快照读取它们，避免把更新到一半的导航设置应用到主界面。
 */
data class ZhihuMainPreferenceSnapshot(
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
fun rememberArticleHost(): ArticleHost? = LocalContext.current.articleHost()

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
fun rememberCommentEmojiInlineContent(
    emojiKeys: Set<String>,
    context: Context = LocalContext.current,
): Map<String, InlineTextContent> =
    remember(emojiKeys) { createEmojiInlineContent(context, emojiKeys) }

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

fun Modifier.questionSelectionWorkaround(): Modifier = this

