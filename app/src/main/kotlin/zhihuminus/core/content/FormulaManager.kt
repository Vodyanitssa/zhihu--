package com.zhihuminus.core.content

import android.util.Log
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.zhihuminus.data.AccountData
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import java.util.concurrent.ConcurrentHashMap

/**
 * 公式管理器。
 *
 * 知乎公式接口（`/equation?tex=...`）返回的 SVG 根标签带有以 ex 为单位的物理尺寸，
 * Coil 无法解析 ex 相对单位（会退化用 viewBox 原始坐标当固有尺寸），因此这里
 * 自行拉取 SVG 文本、解析根标签得到精确宽高比与相对大小，全局缓存。
 *
 * SVG 原文缓存在内存中，渲染阶段用 base64 data URI 喂给 Coil，绕开知乎的登录鉴权。
 * 所有 fetch 统一通过 [AccountData.httpClient] 携带 Cookie。
 * inline 和 block 公式共享同一套 [resolve] 逻辑。
 */
object FormulaManager {
    private const val TAG = "FormulaManager"

    data class Metrics(
        val widthEx: Float,
        val heightEx: Float,
    ) {
        val aspectRatio: Float get() = widthEx / heightEx
    }

    /** 0.53 为 ex 到 em 的换算系数（MathJax 的 x 高度约定，约半个 em）但会太大，故用 0.4 保证不会撑出行 */
    const val EX_TO_EM = 0.4f

    /** 无度量时的兜底高度（em） */
    private const val FALLBACK_HEIGHT_EM = 1.3f

    /** 无度量时的兜底宽高比（首帧占位用，图片加载后被真实比例替换） */
    private const val FALLBACK_RATIO = 1.5f

    /** Compose 可观察状态：url -> 已解析度量 */
    val metrics = mutableStateMapOf<String, Metrics>()
    val urlToLocal = mutableStateMapOf<String, String>()

    private val svgCache = ConcurrentHashMap<String, String>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val inFlight = ConcurrentHashMap<String, Deferred<Unit>>()
    private val widthRegex = Regex("width=\"([\\d.]+)ex\"")
    private val heightRegex = Regex("height=\"([\\d.]+)ex\"")

    fun collectFormulas(nodes: List<InlineNode>): List<InlineNode.Formula> = nodes.flatMap { node ->
        when (node) {
            is InlineNode.Formula -> listOf(node)
            is InlineNode.Bold -> collectFormulas(node.children)
            is InlineNode.Italic -> collectFormulas(node.children)
            else -> emptyList()
        }
    }

    fun displaySize(
        metrics: Metrics?,
        fallbackRatio: Float?,
        fallbackHeightEm: Float,
        emSize: Dp,
        maxWidth: Dp? = null,
    ): Pair<Dp, Dp> {
        val ratio = metrics?.aspectRatio ?: fallbackRatio ?: FALLBACK_RATIO
        val naturalHeight = emSize * (metrics?.heightEx?.times(EX_TO_EM) ?: fallbackHeightEm)
        var widthDp = naturalHeight * ratio
        var heightDp = naturalHeight
        if (maxWidth != null && ratio > 0f && widthDp > maxWidth) {
            widthDp = maxWidth
            heightDp = maxWidth / ratio
        }
        return widthDp to heightDp
    }

    @Composable
    fun formulaInlineContent(
        nodes: List<InlineNode>,
        emSize: Dp,
        maxWidth: Dp,
    ): Map<String, InlineTextContent> {
        val urls = remember(nodes) { collectFormulas(nodes).map { it.url }.distinct() }
        val context = LocalContext.current
        LaunchedEffect(urls) {
            resolve(urls, AccountData.httpClient(context))
        }
        val fallbackRatios = remember { mutableStateMapOf<String, Float>() }
        val density = LocalDensity.current
        return urls.associateWith { url ->
            val (widthDp, heightDp) = displaySize(
                metrics = metrics[url],
                fallbackRatio = fallbackRatios[url],
                fallbackHeightEm = FALLBACK_HEIGHT_EM,
                emSize = emSize,
                maxWidth = maxWidth,
            )
            val widthPx = with(density) { widthDp.roundToPx() }
            val heightPx = with(density) { heightDp.roundToPx() }
            val localUrl = urlToLocal[url] ?: url
            InlineTextContent(
                placeholder = Placeholder(
                    width = with(density) { widthDp.toSp() },
                    height = with(density) { heightDp.toSp() },
                    placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
                ),
            ) {
                val imageRequest = remember(localUrl, widthPx, heightPx) {
                    ImageRequest
                        .Builder(context)
                        .data(localUrl)
                        .size(widthPx, heightPx)
                        .build()
                }
                AsyncImage(
                    model = imageRequest,
                    contentDescription = "行内公式",
                    modifier = Modifier.size(widthDp, heightDp),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
                    onSuccess = { state ->
                        val size = state.painter.intrinsicSize
                        if (size.isSpecified && size.height > 0f) {
                            fallbackRatios[url] = (size.width / size.height).coerceIn(0.1f, 50f)
                        }
                    },
                )
            }
        }
    }

    @Composable
    fun FormulaResolveEffect(url: String) {
        val context = LocalContext.current
        LaunchedEffect(url) {
            resolve(listOf(url), AccountData.httpClient(context))
        }
    }

    suspend fun resolve(urls: Collection<String>, httpClient: HttpClient) {
        urls
            .filter { it !in metrics && it !in urlToLocal }
            .forEach { url ->
                val deferred = inFlight.getOrPut(url) {
                    scope.async { fetchAndStore(url, httpClient) }
                }
                runCatching { deferred.await() }
                    .onFailure { inFlight.remove(url, deferred) }
            }
    }

    internal fun parseSvgRoot(svgText: String): Metrics? {
        val width = widthRegex
            .find(svgText)
            ?.groupValues
            ?.get(1)
            ?.toFloatOrNull() ?: return null
        val height = heightRegex
            .find(svgText)
            ?.groupValues
            ?.get(1)
            ?.toFloatOrNull() ?: return null
        if (width <= 0f || height <= 0f) return null
        return Metrics(width, height)
    }

    private suspend fun fetchAndStore(url: String, httpClient: HttpClient) {
        svgCache[url]?.let { cached ->
            urlToLocal[url] = toDataUri(cached)
            parseSvgRoot(cached)?.let { metrics[url] = it }
            return
        }
        try {
            val text = httpClient.get(url).bodyAsText()
            svgCache[url] = text
            urlToLocal[url] = toDataUri(text)
            parseSvgRoot(text)?.let { metrics[url] = it }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch formula: $url", e)
        }
    }

    private fun toDataUri(svgText: String): String {
        val encoded = android.util.Base64.encodeToString(
            svgText.toByteArray(Charsets.UTF_8),
            android.util.Base64.NO_WRAP,
        )
        return "data:image/svg+xml;base64,$encoded"
    }
}
