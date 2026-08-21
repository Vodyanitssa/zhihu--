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

package com.zhihuminus.markdown

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import coil3.compose.LocalPlatformContext
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import com.hrm.latex.renderer.font.MathFont
import com.zhihuminus.data.AccountData
import com.zhihuminus.data.toCookieHeaderString

/**
 * 获取用于代码块的等宽字体（优先使用下载的 KaTeX Typewriter 字体，回退到系统字体）。
 * 该字体包含完整的框线字符（┌┐└┘├┤┬┴┼│─等），适合渲染字符画/流程图。
 */

@Composable
fun rememberMarkdownImageModel(url: String): Any {
    val context = LocalPlatformContext.current
    val headerData = rememberMarkdownImageRequestHeaders()
    val headers = remember(headerData.cookieHeader, headerData.userAgent) {
        NetworkHeaders
            .Builder()
            .set("Cookie", headerData.cookieHeader)
            .set("User-Agent", headerData.userAgent)
            .build()
    }
    return remember(context, url, headers) {
        ImageRequest
            .Builder(context)
            .data(url)
            .httpHeaders(headers)
            .build()
    }
}

data class MarkdownImageRequestHeaders(
    val cookieHeader: String,
    val userAgent: String,
)

@Composable
fun rememberMarkdownMathFont(): MathFont? {
    val context = LocalContext.current
    val httpClient = AccountData.httpClient(context)
    return rememberLatexFonts(context, httpClient).downloaded?.mathFont
}

@Composable
fun rememberMarkdownImageRequestHeaders(): MarkdownImageRequestHeaders {
    val userAgent = AccountData.data.userAgent
    return MarkdownImageRequestHeaders(
        cookieHeader = AccountData.data.cookies.toCookieHeaderString(),
        userAgent = userAgent,
    )
}

@Composable
fun rememberMarkdownCodeFontFamily(): FontFamily {
    val context = LocalContext.current
    val httpClient = AccountData.httpClient(context)
    val fontResult = rememberLatexFonts(context, httpClient)
    val downloaded = fontResult.downloaded
    return if (downloaded != null) {
        downloaded.katexFamilies.monospace
    } else {
        FontFamily.Monospace
    }
}
