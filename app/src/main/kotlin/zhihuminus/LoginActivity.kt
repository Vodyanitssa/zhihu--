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

package com.zhihuminus

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.zhihuminus.account.SharedQrLoginPane
import com.zhihuminus.account.ZHIHU_DESKTOP_USER_AGENT
import com.zhihuminus.account.ZHIHU_HOME_URL
import com.zhihuminus.account.ZHIHU_SIGNIN_URL
import com.zhihuminus.account.ZhihuMobileLoginToken
import com.zhihuminus.account.parseCookieAssignments
import com.zhihuminus.data.AccountData
import com.zhihuminus.theme.ZhihuTheme
import com.zhihuminus.ui.components.WebviewComp
import com.zhihuminus.ui.components.setupUpWebviewClient
import com.zhihuminus.util.enableEdgeToEdgeCompat
import com.zhihuminus.util.luoTianYiUrlLauncher
import kotlinx.coroutines.launch

private const val LOGIN_MODE_WEB = 0
private const val LOGIN_MODE_QR = 1
private const val LOGIN_MODE_PHONE = 2

class LoginActivity : ComponentActivity() {
    private var isCompletingLogin = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdgeCompat()

        setContent {
            ZhihuTheme {
                var currentNoticeStep by rememberSaveable { mutableIntStateOf(0) }
                var loginMode by rememberSaveable { mutableIntStateOf(LOGIN_MODE_PHONE) }

                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding(),
                ) {
                    if (currentNoticeStep >= 3) {
                        LoginModeScreen(
                            activity = this@LoginActivity,
                            loginMode = loginMode,
                            onModeChanged = { loginMode = it },
                        )
                    } else {
                        when (currentNoticeStep) {
                            0 ->
                                LoginNoticeScreen(
                                    message = "我清楚，本应用由开源社区开发和维护，不由知乎官方开发并运营，也不受到知乎官方的承认或支持，使用本应用的一切后果由我本人承担。我可以在 https://www.zhihu.com/app/ 下载官方应用。",
                                    secondaryButtonText = "下载官方App",
                                    onSecondaryAction = {
                                        luoTianYiUrlLauncher(
                                            this@LoginActivity,
                                            "https://www.zhihu.com/app/".toUri(),
                                        )
                                    },
                                    onConfirm = {
                                        currentNoticeStep = 1
                                    },
                                )

                            1 ->
                                LoginNoticeScreen(
                                    message = "在使用本应用的过程中，我承诺遵守知乎使用协议 https://www.zhihu.com/term/zhihu-terms 。我保证在使用过程中不侵犯知乎及其他作者的著作权，使用本应用产生的一切输出仅用于个人浏览和备份，不会进行传播等其他影响作者著作权的行为。",
                                    secondaryButtonText = "查看协议",
                                    onSecondaryAction = {
                                        luoTianYiUrlLauncher(
                                            this@LoginActivity,
                                            "https://www.zhihu.com/term/zhihu-terms".toUri(),
                                        )
                                    },
                                    onConfirm = {
                                        currentNoticeStep = 2
                                    },
                                )

                            else ->
                                LoginNoticeScreen(
                                    message = "我知晓，本应用可能会收集部分匿名化的使用信息来确定使用人数，我可以在设置中随时关闭此项遥测。",
                                    secondaryButtonText = "查看设置",
                                    onSecondaryAction = {
                                        startActivity(
                                            Intent(this@LoginActivity, MainActivity::class.java),
                                        )
                                    },
                                    onConfirm = {
                                        currentNoticeStep = 3
                                    },
                                )
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun configureWebLogin(webView: WebView) {
        webView.setupUpWebviewClient()
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest,
            ): Boolean {
                if (request.url.toString() == ZHIHU_HOME_URL) {
                    webView.settings.userAgentString = AccountData.ANDROID_USER_AGENT
                }
                return request.url?.scheme == "zhihu"
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (url == ZHIHU_HOME_URL) {
                    val cookies = parseCookieAssignments(
                        CookieManager.getInstance().getCookie(ZHIHU_HOME_URL).orEmpty(),
                    )
                    lifecycleScope.launch {
                        finalizeLoginFromCookies(cookies)
                    }
                }
            }
        }
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        if (webView.url.isNullOrEmpty()) {
            CookieManager.getInstance().removeAllCookies { }
            webView.loadUrl(ZHIHU_SIGNIN_URL)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun configureRiskControlWebView(
        webView: WebView,
        url: String,
        cookies: Map<String, String>,
        onCookiesChanged: (Map<String, String>) -> Unit,
    ) {
        webView.setupUpWebviewClient()
        webView.settings.javaScriptEnabled = true
        webView.settings.userAgentString = ZHIHU_DESKTOP_USER_AGENT
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptThirdPartyCookies(webView, true)
        cookies.forEach { (name, value) ->
            cookieManager.setCookie(ZHIHU_HOME_URL, "$name=$value; Domain=.zhihu.com; Path=/")
        }
        cookieManager.flush()
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest,
            ): Boolean = request.url?.scheme == "zhihu"

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                onCookiesChanged(
                    readWebViewCookies(url),
                )
            }
        }
        if (webView.url != url) {
            webView.loadUrl(url)
        }
    }

    suspend fun finalizeLoginFromCookies(cookies: Map<String, String>): Boolean =
        finalizeLogin {
            AccountData.verifyLogin(this, cookies)
        }

    suspend fun finalizeMobileLogin(token: ZhihuMobileLoginToken): Boolean =
        finalizeLogin {
            AccountData.verifyMobileLogin(this, token)
        }

    private suspend fun finalizeLogin(verifyLogin: suspend () -> Boolean): Boolean {
        if (isCompletingLogin) {
            return false
        }
        isCompletingLogin = true
        return try {
            if (verifyLogin()) {
                val data = AccountData.loadData(this)
                AlertDialog
                    .Builder(this)
                    .apply {
                        setTitle("登录成功")
                        setMessage("欢迎回来，${data.username}")
                        setPositiveButton("OK") { _, _ -> }
                        setOnDismissListener {
                            finish()
                        }
                    }.create()
                    .show()
                true
            } else {
                AlertDialog
                    .Builder(this)
                    .apply {
                        setTitle("登录失败")
                        setMessage("请重试")
                        setPositiveButton("OK") { _, _ -> }
                    }.create()
                    .show()
                false
            }
        } finally {
            isCompletingLogin = false
        }
    }
}

@Composable
private fun LoginModeScreen(
    activity: LoginActivity,
    loginMode: Int,
    onModeChanged: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LoginModeButton(
                text = "手机号登录",
                selected = loginMode == LOGIN_MODE_PHONE,
                modifier = Modifier.weight(1f),
                onClick = { onModeChanged(LOGIN_MODE_PHONE) },
            )
            LoginModeButton(
                text = "扫码登录",
                selected = loginMode == LOGIN_MODE_QR,
                modifier = Modifier.weight(1f),
                onClick = { onModeChanged(LOGIN_MODE_QR) },
            )
            LoginModeButton(
                text = "备用网页登录",
                selected = loginMode == LOGIN_MODE_WEB,
                modifier = Modifier.weight(1f),
                onClick = { onModeChanged(LOGIN_MODE_WEB) },
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            when (loginMode) {
                LOGIN_MODE_WEB -> {
                    WebviewComp(
                        modifier = Modifier.fillMaxSize(),
                        onLoad = { webView ->
                            activity.configureWebLogin(webView)
                        },
                    )
                }

                LOGIN_MODE_QR -> QrLoginPane(activity = activity)
                LOGIN_MODE_PHONE -> PhoneLoginPane(activity = activity)
            }
        }
    }
}

@Composable
private fun LoginModeButton(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier,
        ) {
            Text(text)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier,
        ) {
            Text(text)
        }
    }
}

@Composable
private fun QrLoginPane(activity: LoginActivity) {
    val context = LocalContext.current
    SharedQrLoginPane(
        createClient = { cookies -> AccountData.httpClient(context, cookies) },
        onLoginSuccess = { cookies -> activity.finalizeLoginFromCookies(cookies) },
        readRiskControlCookies = ::readWebViewCookies,
        riskControlContent = { url, cookies, onCookiesChanged ->
            WebviewComp(
                modifier = Modifier
                    .fillMaxSize(),
                onLoad = { webView ->
                    activity.configureRiskControlWebView(
                        webView = webView,
                        url = url,
                        cookies = cookies,
                        onCookiesChanged = { updatedCookies ->
                            onCookiesChanged(cookies + updatedCookies)
                        },
                    )
                },
            )
        },
    )
}

@Composable
private fun LoginNoticeScreen(
    message: String,
    secondaryButtonText: String,
    onConfirm: () -> Unit,
    onSecondaryAction: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onSecondaryAction,
                    modifier = Modifier
                        .fillMaxWidth(),
                ) {
                    Text(secondaryButtonText)
                }
                Button(
                    onClick = onConfirm,
                    modifier = Modifier
                        .fillMaxWidth(),
                ) {
                    Text("确认")
                }
            }
        }
    }
}

private fun readWebViewCookies(url: String?): Map<String, String> {
    val cookieManager = CookieManager.getInstance()
    val cookies = mutableMapOf<String, String>()
    cookies.putAll(parseCookieAssignments(cookieManager.getCookie(ZHIHU_HOME_URL).orEmpty()))
    if (!url.isNullOrBlank()) {
        cookies.putAll(parseCookieAssignments(cookieManager.getCookie(url).orEmpty()))
    }
    return cookies
}
