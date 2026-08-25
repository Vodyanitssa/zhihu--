package com.zhihuminus.core.content

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
import com.zhihuminus.core.content.FormulaManager.metrics
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import java.util.concurrent.ConcurrentHashMap

/**
 * 行内公式（eeimg 的 img）的尺寸管理器。
 *
 * 知乎公式接口返回的 SVG 根标签带有以 ex 为单位的物理尺寸：
 * `<svg width="8.976ex" height="2.676ex" style="font-size: 15px" viewBox="...">`。
 * Coil 无法解析 ex 相对单位（会退化用 viewBox 原始坐标当固有尺寸），因此这里
 * 自行拉取 SVG 文本、解析根标签得到精确宽高比与相对大小，全局缓存。
 *
 * 渲染端读取 [metrics] 构建占位符；解析失败或未完成时由渲染端自行兜底。
 */
object FormulaManager {
    /** 公式度量：SVG 根标签声明的物理尺寸（ex 单位） */
    data class Metrics(
        val widthEx: Float,
        val heightEx: Float,
    ) {
        /** 宽高比，可直接用于布局 */
        val aspectRatio: Float get() = widthEx / heightEx
    }

    /** 0.53 为 ex 到 em 的换算系数（MathJax 的 x 高度约定，约半个 em） */
    const val EX_TO_EM = 0.35f

    /** 无度量时的兜底高度（em） */
    private const val FALLBACK_HEIGHT_EM = 1.3f

    /** 无度量时的兜底宽高比（首帧占位用，图片加载后被真实比例替换） */
    private const val FALLBACK_RATIO = 1.5f

    /** Compose 可观察状态：url -> 已解析度量（进程级缓存） */
    val metrics = mutableStateMapOf<String, Metrics>()

    private val client by lazy {
        HttpClient {
            install(HttpTimeout) {
                requestTimeoutMillis = 5000
            }
        }
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val inFlight = ConcurrentHashMap<String, Deferred<Unit>>()

    private val widthRegex = Regex("width=\"([\\d.]+)ex\"")
    private val heightRegex = Regex("height=\"([\\d.]+)ex\"")

    /** 从节点树中收集全部公式节点（含粗体/斜体嵌套） */
    fun collectFormulas(nodes: List<InlineNode>): List<InlineNode.Formula> = nodes.flatMap { node ->
        when (node) {
            is InlineNode.Formula -> listOf(node)
            is InlineNode.Bold -> collectFormulas(node.children)
            is InlineNode.Italic -> collectFormulas(node.children)
            else -> emptyList()
        }
    }

    /**
     * 计算公式的显示尺寸（宽, 高）。
     * 有度量时按 ex 物理换算；否则用兜底高度 + 兜底比例。
     * [maxWidth] 非 null 时，宽度超出可用宽度则强制占满并等比缩放高度。
     */
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

    /**
     * 构建 [nodes] 中行内公式的 InlineTextContent 表（key 为图片 URL），
     * 同时触发缺失度量的解析。渲染端与 [EmojiManager.inlineContent] 合并使用。
     * 超出 [maxWidth] 的公式强制占满可用宽度、高度等比缩放。
     */
    @Composable
    fun formulaInlineContent(
        nodes: List<InlineNode>,
        emSize: Dp,
        maxWidth: Dp,
    ): Map<String, InlineTextContent> {
        val urls = remember(nodes) { collectFormulas(nodes).map { it.url }.distinct() }
        LaunchedEffect(urls) { resolve(urls) }
        val fallbackRatios = remember { mutableStateMapOf<String, Float>() }
        val density = LocalDensity.current
        val context = LocalContext.current
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
            InlineTextContent(
                placeholder = Placeholder(
                    width = with(density) { widthDp.toSp() },
                    height = with(density) { heightDp.toSp() },
                    placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
                ),
            ) {
                // 显式按占位符像素尺寸请求，让 SVG 以显示分辨率光栅化；
                // 否则 Compose 会对行内内容做绘制时缩放，长公式会被放大而模糊
                val imageRequest = remember(url, widthPx, heightPx) {
                    ImageRequest
                        .Builder(context)
                        .data(url)
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

    /**
     * 批量补齐 [urls] 中缺失的度量。同一 URL 并发去重；
     * 拉取失败静默跳过（允许后续重试），渲染端走兜底路径。
     */
    suspend fun resolve(urls: Collection<String>) {
        urls
            .filter { it !in metrics }
            .forEach { url ->
                val deferred = inFlight.getOrPut(url) {
                    scope.async { fetchAndStore(url) }
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

    private suspend fun fetchAndStore(url: String) {
        val text = client.get(url).bodyAsText()
        val parsed = parseSvgRoot(text) ?: return
        metrics[url] = parsed
    }
}
