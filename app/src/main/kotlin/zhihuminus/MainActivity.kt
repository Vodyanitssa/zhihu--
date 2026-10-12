/*
 * Zhihu-- - Free & Ad-Free Zhihu client for Android.
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

package com.zhihuminus

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.request.crossfade
import coil3.svg.SvgDecoder
import com.zhihuminus.account.ZhihuCredentialRefresher
import com.zhihuminus.core.content.EmojiManager
import com.zhihuminus.core.platform.androidUserMessageSink
import com.zhihuminus.core.platform.clearShareImageCache
import com.zhihuminus.core.platform.clipboardManager
import com.zhihuminus.core.platform.startLoginActivity
import com.zhihuminus.core.settings.AndroidAppSettingsRepository
import com.zhihuminus.core.settings.LocalAppSettings
import com.zhihuminus.core.settings.LocalAppSettingsRepository
import com.zhihuminus.core.util.friendlyErrorMessage
import com.zhihuminus.data.AccountData
import com.zhihuminus.data.local.AppRuntimeStore
import com.zhihuminus.data.zhihu.crypto.ZHIHU_WEB_ZSE93
import com.zhihuminus.feature.post.PostType
import com.zhihuminus.navigation.CommentHolder
import com.zhihuminus.navigation.Home
import com.zhihuminus.navigation.MainTabs
import com.zhihuminus.navigation.NavDestination
import com.zhihuminus.navigation.PostDestination
import com.zhihuminus.navigation.Question
import com.zhihuminus.navigation.TopLevelDestination
import com.zhihuminus.navigation.Video
import com.zhihuminus.navigation.router.AppRouter
import com.zhihuminus.navigation.router.RouteResolution
import com.zhihuminus.theme.AndroidThemeSettings
import com.zhihuminus.theme.ZhihuTheme
import com.zhihuminus.ui.AndroidZhihuMain
import com.zhihuminus.ui.ArticleHost
import com.zhihuminus.ui.components.getHighestQualityVideoUrl
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

class MainActivity :
    ComponentActivity(),
    ArticleHost {
    class SharedData : ViewModel() {
        var clipboardDestination: NavDestination? = null
    }

    val sharedData by viewModels<SharedData>()
    override val articleNavController: NavHostController
        get() = navController
    override var clipboardDestination: NavDestination?
        get() = sharedData.clipboardDestination
        set(value) {
            sharedData.clipboardDestination = value
        }
    val httpClient by lazy {
        AccountData.httpClient(this)
    }

    lateinit var navController: NavHostController
    private var pendingCommentHolder: CommentHolder? = null
    var mainTabNavigationTarget by mutableStateOf<TopLevelDestination?>(null)
        private set

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, e ->
            Log.e(TAG, "Uncaught exception", e)
            val isAlreadyHandlingError = intent?.data?.let { uri ->
                (uri.authority == "zhihu-minus.internal" || uri.authority == "zhihu-plus.internal") && uri.path == "/error"
            } ?: false
            if (isAlreadyHandlingError) {
                previousHandler?.uncaughtException(thread, e)
                return@setDefaultUncaughtExceptionHandler
            }
            try {
                val stackString = e.stackTraceToString()
                val crashFile = File(cacheDir, "last_crash.log")
                runCatching { crashFile.writeText(stackString) }

                val safeStackPreview = if (stackString.length > 2000) {
                    stackString.take(2000) + "\n... (更多堆栈信息见崩溃日志)"
                } else {
                    stackString
                }

                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri
                        .Builder()
                        .apply {
                            scheme("https")
                            authority("zhihu-minus.internal")
                            appendPath("error")
                            appendQueryParameter("title", "应用发生异常: ${e.javaClass.simpleName}")
                            appendQueryParameter("message", e.message.orEmpty())
                            appendQueryParameter("stack", safeStackPreview)
                        }.build(),
                    this,
                    MainActivity::class.java,
                )
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            } catch (fatal: Throwable) {
                Log.e(TAG, "Failed to launch crash reporter, falling back to default handler", fatal)
                previousHandler?.uncaughtException(thread, e)
            }
        }
        super.onCreate(savedInstanceState)
        clearShareImageCache(this)
        AccountData.loadData(this)
        AndroidThemeSettings.initialize(this)

        val runtimeStorage = AppRuntimeStore(this)
        val lastLaunchTimestamp = runtimeStorage.lastLaunchTimestamp
        val isFirstLaunch = !runtimeStorage.hasLaunchedBefore
        val now = System.currentTimeMillis()
        if (AccountData.data.login && !isFirstLaunch && now - lastLaunchTimestamp >= TimeUnit.DAYS.toMillis(1)) {
            val client = httpClient
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val refreshToken = ZhihuCredentialRefresher.fetchRefreshToken(client)
                    ZhihuCredentialRefresher.refreshZhihuToken(refreshToken, client)
                    Log.i(TAG, "Zhihu token refreshed successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to refresh Zhihu token", e)
                    withContext(Dispatchers.Main) {
                        androidUserMessageSink(this@MainActivity)
                            .showLongMessage("刷新登录状态失败，如多次看到此提示请重新登录")
                    }
                }
            }
        }
        runtimeStorage.hasLaunchedBefore = true
        runtimeStorage.lastLaunchTimestamp = now

        // 初始化emoji管理器
        lifecycleScope.launch {
            try {
                EmojiManager
                    .initialize(this@MainActivity)
                Log.i(TAG, "Emoji manager initialized")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize emoji manager", e)
            }
        }

        setContent {
            val settingsRepository = remember { AndroidAppSettingsRepository.getInstance(applicationContext) }
            val appSettings by settingsRepository.settingsFlow.collectAsState()

            CompositionLocalProvider(
                LocalAppSettings provides appSettings,
                LocalAppSettingsRepository provides settingsRepository,
            ) {
                navController = rememberNavController()
                ZhihuTheme(themeSettings = appSettings.theme) {
                    Box(Modifier) {
                        AndroidZhihuMain(navController = navController)
                    }
                }
            }
        }
        if (savedInstanceState == null) {
            val errorData = intent?.data
            if ((errorData?.authority == "zhihu-minus.internal" || errorData?.authority == "zhihu-plus.internal") && errorData.path == "/error") {
                val title = errorData.getQueryParameter("title") ?: "应用发生异常"
                val crashFile = File(cacheDir, "last_crash.log")
                val fullStack = if (crashFile.exists()) {
                    runCatching { crashFile.readText() }.getOrNull()
                } else {
                    null
                } ?: errorData.getQueryParameter("stack").orEmpty()
                AlertDialog
                    .Builder(this)
                    .apply {
                        setTitle(title)
                        setMessage(fullStack)
                        setPositiveButton("确定") { _, _ -> }
                        setNeutralButton("复制堆栈") { _, _ ->
                            val clip = ClipData.newPlainText("error", fullStack)
                            clipboardManager.setPrimaryClip(clip)
                        }
                    }.create()
                    .show()
            } else if (isFirstLaunch && !AccountData.data.login) {
                startLoginActivity()
            }
        }

        ImageLoader
            .Builder(this)
            .crossfade(true)
            .components {
                add(SvgDecoder.Factory())
            }.memoryCache {
                MemoryCache
                    .Builder()
                    .maxSizePercent(this, 0.25)
                    .build()
            }.diskCache {
                DiskCache
                    .Builder()
                    .directory(this.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50L * 1024 * 1024) // 50 MB
                    .build()
            }.build()
            .also { loader ->
                SingletonImageLoader.setSafe {
                    loader
                }
            }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        if (hasFocus) {
            if (!handleIntentData(intent)) {
                // read clipboard
                val clip = clipboardManager.primaryClip
                if (clip != null && clip.itemCount > 0) {
                    val text = clip.getItemAt(0).text
                    if (text != null) {
                        val regex = Regex("""(?:https?|zhihu)://[-a-zA-Z0-9@:%_+.~#?&/=]*""")
                        val destination = regex.findAll(text).firstNotNullOfOrNull {
                            resolveScreen(it.value)
                        }
                        if (destination != null && destination != sharedData.clipboardDestination) {
                            sharedData.clipboardDestination = destination
                            navigate(destination, popup = true)
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (::navController.isInitialized) {
            handleIntentData(intent)
        }
    }

    private fun handleIntentData(incomingIntent: Intent): Boolean {
        val data = incomingIntent.data ?: return false
        if (data.authority == "zhihu-minus.internal" || data.authority == "zhihu-plus.internal") return true

        Log.i(TAG, "Intent data: $data")
        when (val resolution = AppRouter.resolveAny(data.toString())) {
            is RouteResolution.Screen -> openScreenDestination(resolution.destination)
            is RouteResolution.Tab -> navigateToMainTabs(resolution.destination)
            null -> {
                AlertDialog
                    .Builder(this)
                    .apply {
                        setTitle("Unsupported URL")
                        setMessage("Unknown URL: $data")
                        setPositiveButton("OK") { _, _ -> }
                    }.create()
                    .show()
            }
        }
        return true
    }

    /** 解析任意 URL（应用协议或知乎 Web 链接）为屏幕目的地；tab 切换 URL 返回 null。 */
    private fun resolveScreen(url: String): NavDestination? =
        (AppRouter.resolveAny(url) as? RouteResolution.Screen)?.destination

    private fun openScreenDestination(destination: NavDestination) {
        if (destination != sharedData.clipboardDestination) {
            sharedData.clipboardDestination = destination
            navigate(destination, popup = true)
        }
    }

    fun navigate(resolution: RouteResolution, popup: Boolean = false) {
        when (resolution) {
            is RouteResolution.Screen -> navigate(resolution.destination, popup)
            is RouteResolution.Tab -> navigateToMainTabs(resolution.destination)
        }
    }

    fun navigate(route: NavDestination, popup: Boolean = false) {
        if (route is CommentHolder) {
            pendingCommentHolder = route
            navigate(route.article, popup)
            return
        }
        if (pendingCommentHolder?.article != route) {
            pendingCommentHolder = null
        }
        if (route is Video) {
            val current = runCatching {
                navController.currentBackStackEntry?.toRoute<PostDestination>()
            }.getOrNull() ?: runCatching {
                navController.currentBackStackEntry?.toRoute<Question>()
            }.getOrNull()
            if (current == null) {
                androidUserMessageSink(this).showShortMessage("无法打开视频：未知的内容类型")
                return
            }
            val (contentId, contentType) = when (current) {
                is PostDestination -> {
                    current.id.toString() to when (current.type) {
                        PostType.Answer -> "answer"
                        PostType.Article -> "article"
                        else -> "unknown"
                    }
                }

                is Question -> {
                    current.questionId.toString() to "question"
                }

                else -> error("Unsupported content type for video: $current")
            }
            lifecycleScope.launch {
                try {
                    val videoUrl = getHighestQualityVideoUrl(
                        this@MainActivity,
                        httpClient,
                        route.id.toString(),
                        contentId,
                        contentType,
                    )
                    if (videoUrl == null) {
                        androidUserMessageSink(this@MainActivity).showShortMessage("获取视频链接失败")
                        return@launch
                    }
                    startActivity(
                        Intent(this@MainActivity, VideoPlayerActivity::class.java).apply {
                            putExtra("video_url", videoUrl)
                            putExtra("video_id", route.id)
                        },
                    )
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    androidUserMessageSink(this@MainActivity).showShortMessage("打开视频失败: ${friendlyErrorMessage(e)}")
                }
            }
            return
        }
        if (route == MainTabs) {
            mainTabNavigationTarget = Home
            navigateToMainTabs()
            return
        }
        navController.navigate(route) {
            if (popup) {
                launchSingleTop = true
                popUpTo(MainTabs) {
                    // clear the back stack and viewModels
                    saveState = true
                }
            }
        }
    }

    override fun consumePendingCommentId(destination: NavDestination): String? {
        val holder = pendingCommentHolder?.takeIf { it.article == destination } ?: return null
        pendingCommentHolder = null
        return holder.commentId
    }

    private fun navigateToMainTabs(target: TopLevelDestination) {
        mainTabNavigationTarget = target
        navigateToMainTabs()
    }

    private fun navigateToMainTabs() {
        navController.navigate(MainTabs) {
            launchSingleTop = true
            restoreState = true
            popUpTo(MainTabs) {
                saveState = true
            }
        }
    }

    fun consumeMainTabNavigationTarget(destination: TopLevelDestination) {
        if (mainTabNavigationTarget == destination) {
            mainTabNavigationTarget = null
        }
    }

    override fun postHistoryDestination(destination: NavDestination) = Unit

    @Suppress("unused")
    companion object {
        const val IOS = "5_2.0"
        const val ANDROID = "4_2.0"
        const val WEB = "3_2.0"
        const val ZSE93 = ZHIHU_WEB_ZSE93
        const val TAG = "MainActivity"
    }
}
