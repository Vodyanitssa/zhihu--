package com.zhihuminus.core.environment

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.zhihuminus.core.platform.copyPlainText
import com.zhihuminus.core.platform.hasImageExportPermission
import com.zhihuminus.core.platform.loadExportAssetText
import com.zhihuminus.core.platform.requestImageExportPermission
import com.zhihuminus.core.platform.restartApplication
import com.zhihuminus.core.platform.showDebugErrorDialog
import com.zhihuminus.core.platform.startLoginActivity
import com.zhihuminus.core.platform.tryShowLoginExpiredDialog
import com.zhihuminus.data.AccountData
import com.zhihuminus.data.HistoryStorage
import com.zhihuminus.data.ZhihuCookieStorage
import com.zhihuminus.data.ZhihuJson.json
import com.zhihuminus.navigation.NavDestination
import com.zhihuminus.platform.androidUserMessageSink
import com.zhihuminus.ui.homeFeedStartupCacheFileNames
import com.zhihuminus.util.HttpStatusException
import com.zhihuminus.util.Log
import com.zhihuminus.util.friendlyErrorMessage
import io.ktor.client.HttpClient
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.appendAll
import java.io.File

private val ZHIHU_PP_ANDROID_HEADERS = createClientPlugin("ZhihuPPAndroidHeaders", { }) {
    onRequest { request, _ ->
        request.headers.appendAll(AccountData.ANDROID_HEADERS)
    }
}

open class AndroidPaginationEnvironment(
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

    override fun currentAccountId(): String =
        AccountData.data.self
            ?.id
            .orEmpty()

    override fun restartApplication() {
        context.restartApplication()
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

    override fun mobileHttpClient(): HttpClient = mobileHomeFeedHttpClient()

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
            if (context.tryShowLoginExpiredDialog(error, onRelogin = { requestRelogin() })) {
                return
            }
            context.showDebugErrorDialog(error, userMessageSink)
        }
        Log.e(tag, "Failed to fetch feeds", error)
        context.mainExecutor.execute {
            userMessageSink.showShortMessage("加载失败: ${friendlyErrorMessage(error)}")
        }
    }

    override suspend fun handleMobileHomeFeedFailure(error: Exception) {
        Log.e("AndroidHomeFeedViewModel", "Failed to fetch feeds", error)
        context.mainExecutor.execute {
            userMessageSink.showShortMessage("安卓端推荐加载失败: ${friendlyErrorMessage(error)}")
        }
    }

    override fun localHistory(): List<NavDestination> = HistoryStorage(context).history

    override suspend fun postHistoryDestination(destination: NavDestination) {
        HistoryStorage(context).add(destination)
    }

    override fun setPlainTextClipboard(
        label: String,
        text: String,
    ) {
        context.copyPlainText(label, text)
    }

    override fun hasImageExportPermission(): Boolean = context.hasImageExportPermission()

    override fun requestImageExportPermission() {
        context.requestImageExportPermission()
    }

    override fun loadExportAssetText(fileName: String): String = context.loadExportAssetText(fileName)
}

@Composable
fun rememberPaginationEnvironment(): PaginationEnvironment {
    val context = LocalContext.current
    return remember(context) { AndroidPaginationEnvironment(context) }
}
