package com.zhihuminus.core.platform

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.roundToInt

/**
 * 用于将 HTML 渲染为图片的离屏 WebView 包装。
 *
 * 参考 [com.zhihuminus.viewmodel.AndroidArticleExportRenderer] 的实现，
 * 但作为独立的平台工具类，不依赖 Article 的 ViewModel 层。
 */
class PictureExportWebView(
    private val context: Context,
) {
    companion object {
        /** 导出图片的 DPI，值越高图片越清晰 */
        private const val EXPORT_DPI = 400f
    }

    /**
     * 准备一个离屏 WebView 并加载 HTML 内容，等待内容高度稳定后返回。
     *
     * @param htmlContent 完整的 HTML 文档字符串
     * @param timeoutMs 加载超时时间（毫秒）
     * @return 包含 WebView 实例、视口宽度和内容高度的 [PreparedWebView]
     */
    suspend fun prepare(
        htmlContent: String,
        timeoutMs: Long = 15_000L,
    ): PreparedWebView = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            val webView = createWebView()
            val mainHandler = Handler(Looper.getMainLooper())
            val viewportWidthPx = context.resources.displayMetrics.widthPixels
                .coerceAtLeast(1)
            var isFinished = false
            var timeoutRunnable = Runnable {}

            fun fail(error: Throwable) {
                if (isFinished) return
                isFinished = true
                mainHandler.removeCallbacks(timeoutRunnable)
                runCatching { webView.stopLoading() }
                runCatching { webView.destroy() }
                if (continuation.isActive) {
                    continuation.resumeWithException(error)
                }
            }

            fun finish(contentHeightPx: Int) {
                if (isFinished) return
                isFinished = true
                mainHandler.removeCallbacks(timeoutRunnable)
                measureAndLayout(
                    webView = webView,
                    widthPx = viewportWidthPx,
                    heightPx = contentHeightPx.coerceAtLeast(1),
                )
                if (continuation.isActive) {
                    continuation.resume(
                        PreparedWebView(
                            webView = webView,
                            viewportWidthPx = viewportWidthPx,
                            contentHeightPx = contentHeightPx,
                        ),
                    )
                }
            }

            fun scheduleReadinessCheck(
                attempt: Int = 0,
                lastHeightPx: Int = -1,
                stablePasses: Int = 0,
            ) {
                mainHandler.postDelayed({
                    if (isFinished) return@postDelayed

                    val density = webView.resources.displayMetrics.density
                    val contentHeightPx = maxOf(
                        (webView.contentHeight * density).roundToInt(),
                        webView.measuredHeight,
                        webView.height,
                        1,
                    )
                    if (contentHeightPx <= 1 && attempt >= 24) {
                        fail(IllegalStateException("内容为空"))
                        return@postDelayed
                    }

                    measureAndLayout(
                        webView = webView,
                        widthPx = viewportWidthPx,
                        heightPx = contentHeightPx.coerceAtLeast(1),
                    )

                    val nextStablePasses = if (contentHeightPx == lastHeightPx) stablePasses + 1 else 0
                    if (contentHeightPx > 1 && (nextStablePasses >= 2 || attempt >= 24)) {
                        finish(contentHeightPx)
                    } else {
                        scheduleReadinessCheck(
                            attempt = attempt + 1,
                            lastHeightPx = contentHeightPx,
                            stablePasses = nextStablePasses,
                        )
                    }
                }, if (attempt == 0) 450L else 180L)
            }

            timeoutRunnable = Runnable {
                fail(IllegalStateException("超时"))
            }

            webView.webViewClient = object : android.webkit.WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    if (!isFinished) {
                        scheduleReadinessCheck()
                    }
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: android.webkit.WebResourceError?,
                ) {
                    super.onReceivedError(view, request, error)
                    if (request?.isForMainFrame != false) {
                        fail(IllegalStateException("加载错误"))
                    }
                }
            }

            measureAndLayout(
                webView = webView,
                widthPx = viewportWidthPx,
                heightPx = 1,
            )
            mainHandler.postDelayed(timeoutRunnable, timeoutMs)
            webView.loadDataWithBaseURL(
                "https://www.zhihu.com",
                htmlContent,
                "text/html",
                "UTF-8",
                null,
            )

            continuation.invokeOnCancellation {
                if (!isFinished) {
                    isFinished = true
                    mainHandler.removeCallbacks(timeoutRunnable)
                    runCatching { webView.stopLoading() }
                    runCatching { webView.destroy() }
                }
            }
        }
    }

    /**
     * 将已准备好的 WebView 渲染为高分辨率 Bitmap。
     */
    suspend fun captureBitmap(prepared: PreparedWebView): Bitmap =
        withContext(Dispatchers.Main) {
            val rawWidth = prepared.viewportWidthPx.coerceAtLeast(1)
            val rawHeight = prepared.contentHeightPx.coerceAtLeast(1)
            val scale = EXPORT_DPI / context.resources.displayMetrics.densityDpi
                .coerceAtLeast(1)
                .toFloat()
            val bitmapWidth = (rawWidth * scale).roundToInt().coerceAtLeast(1)
            val bitmapHeight = (rawHeight * scale).roundToInt().coerceAtLeast(1)

            Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888).also { bitmap ->
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                canvas.scale(
                    bitmapWidth.toFloat() / rawWidth.toFloat(),
                    bitmapHeight.toFloat() / rawHeight.toFloat(),
                )
                prepared.webView.draw(canvas)
            }
        }

    /**
     * 销毁已准备好的 WebView，释放资源。
     */
    suspend fun destroy(prepared: PreparedWebView) {
        withContext(Dispatchers.Main) {
            runCatching {
                prepared.webView.stopLoading()
                prepared.webView.destroy()
            }
        }
    }

    private fun createWebView(): WebView = WebView(context).apply {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = false
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = false
        settings.builtInZoomControls = false
        settings.displayZoomControls = false
        setBackgroundColor(Color.WHITE)
        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false
    }

    private fun measureAndLayout(webView: WebView, widthPx: Int, heightPx: Int) {
        val safeHeight = heightPx.coerceAtLeast(1)
        webView.measure(
            View.MeasureSpec.makeMeasureSpec(widthPx.coerceAtLeast(1), View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(safeHeight, View.MeasureSpec.EXACTLY),
        )
        webView.layout(0, 0, widthPx.coerceAtLeast(1), safeHeight)
    }
}

/**
 * 已加载完成的离屏 WebView 状态。
 */
data class PreparedWebView(
    val webView: WebView,
    val viewportWidthPx: Int,
    val contentHeightPx: Int,
)
