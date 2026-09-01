package com.zhihuminus.feature.question.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.http.SslError
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.zhihuminus.data.AccountData
import com.zhihuminus.util.Log

private const val TAG = "HistoryLogSheet"

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryLogSheet(
    url: String,
    onDismiss: () -> Unit,
) {
    Log.i(TAG, "Opening history log URL: $url")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable(onClick = onDismiss),
            )
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    TopAppBar(
                        title = { Text("问题日志") },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "关闭")
                            }
                        },
                    )
                    AndroidView(
                        factory = { context ->
                            Log.i(TAG, "Creating WebView instance")
                            val cookieManager = CookieManager.getInstance()

                            // 从 AccountData 同步 cookie 到 CookieManager
                            val accountCookies = AccountData.data.cookies
                            Log.i(TAG, "Syncing ${accountCookies.size} cookies from AccountData to CookieManager")
                            accountCookies.forEach { (name, value) ->
                                cookieManager.setCookie("https://www.zhihu.com", "$name=$value; Domain=.zhihu.com; Path=/")
                            }
                            cookieManager.flush()
                            Log.i(TAG, "Cookies after sync: ${cookieManager.getCookie("https://www.zhihu.com")}")

                            WebView(context).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.userAgentString = "Mozilla/5.0 (Linux; Android 10) " +
                                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                                    "Chrome/120.0.0.0 Mobile Safari/537.36"

                                cookieManager.setAcceptThirdPartyCookies(this, true)

                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(
                                        view: WebView?,
                                        request: WebResourceRequest?,
                                    ): Boolean {
                                        val requestUrl = request?.url?.toString() ?: return false
                                        Log.i(TAG, "shouldOverrideUrlLoading: $requestUrl")
                                        if (requestUrl.startsWith("https://www.zhihu.com/") ||
                                            requestUrl.startsWith("https://zhuanlan.zhihu.com/")
                                        ) {
                                            return false
                                        }
                                        return true
                                    }

                                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                        super.onPageStarted(view, url, favicon)
                                        Log.i(TAG, "Page started: $url")
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        Log.i(TAG, "Page finished: $url")
                                    }

                                    override fun onReceivedError(
                                        view: WebView?,
                                        request: WebResourceRequest?,
                                        error: WebResourceError?,
                                    ) {
                                        super.onReceivedError(view, request, error)
                                        Log.e(TAG, "WebView error: ${request?.url} - ${error?.description}")
                                    }

                                    override fun onReceivedSslError(
                                        view: WebView?,
                                        handler: SslErrorHandler?,
                                        error: SslError?,
                                    ) {
                                        Log.e(TAG, "SSL error: ${error?.toString()}")
                                        handler?.cancel()
                                    }
                                }

                                webChromeClient = object : android.webkit.WebChromeClient() {
                                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                        super.onProgressChanged(view, newProgress)
                                        Log.d(TAG, "Page loading progress: $newProgress%")
                                    }

                                    override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                                        consoleMessage?.let {
                                            Log.d(TAG, "Console [${it.messageLevel()}]: ${it.message()}")
                                        }
                                        return true
                                    }
                                }

                                Log.i(TAG, "Loading URL: $url")
                                loadUrl(url)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    )
                }
            }
        }
    }
}
