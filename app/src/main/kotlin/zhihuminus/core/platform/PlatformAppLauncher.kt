package com.zhihuminus.core.platform

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import com.zhihuminus.core.settings.AndroidAppSettingsRepository
import io.ktor.http.Url

fun Context.startLoginActivity() {
    val intent = Intent().setClassName(packageName, "com.zhihuminus.LoginActivity")
    if (this !is Activity) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    startActivity(intent)
}

fun Context.restartApplication() {
    val activity = this as? Activity ?: return
    val launchIntent = activity.packageManager.getLaunchIntentForPackage(activity.packageName)
        ?: error("无法获取应用启动入口")
    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    activity.startActivity(launchIntent)
}

/**
 * 洛天依主题浏览器打开
 */
fun luoTianYiUrlLauncher(context: Context, uri: Uri) {
    if (uri.host == "link.zhihu.com") {
        Url(uri.toString()).parameters["target"]?.let {
            luoTianYiUrlLauncher(context, it.toUri())
            return
        }
    }
    val color = AndroidAppSettingsRepository
        .getInstance(context)
        .current.theme.browserToolbarColor
    val intent = CustomTabsIntent
        .Builder()
        .setDefaultColorSchemeParams(
            CustomTabColorSchemeParams
                .Builder()
                .setToolbarColor(color)
                .build(),
        ).build()
    intent.launchUrl(context, uri)
}

@Composable
fun rememberExternalUrlOpener(): (String) -> Unit {
    val context = LocalContext.current
    return remember(context) { { url -> luoTianYiUrlLauncher(context, url.toUri()) } }
}

@Composable
fun rememberSystemUrlOpener(): (String) -> Unit {
    val context = LocalContext.current
    return remember(context) { { url -> context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) } }
}

@Composable
fun rememberShareText(): (String) -> Unit {
    val context = LocalContext.current
    return remember(context) {
        { text ->
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, text)
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, null)
            context.startActivity(shareIntent)
        }
    }
}

@Composable
fun rememberIsLiteVariant(): Boolean {
    val context = LocalContext.current
    return remember(context) { isAndroidLiteVariantPackageName(context.packageName) }
}

internal fun isAndroidLiteVariantPackageName(packageName: String): Boolean = packageName.endsWith(".lite")
