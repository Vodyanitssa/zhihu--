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
package com.zhihuminus.ui

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.navigation.NavHostController
import com.zhihuminus.data.AccountData
import com.zhihuminus.navigation.NavDestination
import com.zhihuminus.navigation.TopLevelDestination
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonPrimitive

internal fun JsonObject?.booleanCompat(vararg keys: String): Boolean {
    if (this == null) return false
    return keys.firstNotNullOfOrNull { key ->
        get(key)?.jsonPrimitive?.booleanOrNull
    } ?: false
}

/**
 * 文章页需要从外围应用获取的宿主级服务。
 *
 * 文章会参与历史记录、剪贴板和 deep link 交接。这个接口刻意比 Activity 窄，
 */
interface ArticleHost {
    val articleNavController: NavHostController
    var clipboardDestination: NavDestination?

    fun postHistoryDestination(destination: NavDestination)

    fun consumePendingCommentId(destination: NavDestination): String? = null
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

internal const val PEOPLE_PROFILE_INCLUDE_PATH =
    "allow_message,is_followed,is_following,is_org,is_blocking,badge_v2,answer_count,follower_count,following_count,articles_count,question_count,pins_count"

private const val QR_CODE_SCAN_ACTIVITY_CLASS = "com.zhihuminus.QRCodeScanActivity"
private const val WEBVIEW_ACTIVITY_CLASS = "com.zhihuminus.WebviewActivity"
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
    val buildType = metaData?.getString("com.zhihuminus.BUILD_TYPE")
        ?: if ((applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) "debug" else "release"
    val gitHash = metaData?.getString("com.zhihuminus.GIT_HASH") ?: "unknown"
    return "$versionName $buildType, $gitHash"
}

fun Context.articleHost(): ArticleHost? =
    (this as? ArticleHost) ?: (this as? ContextWrapper)?.baseContext?.takeIf { it !== this }?.articleHost()
