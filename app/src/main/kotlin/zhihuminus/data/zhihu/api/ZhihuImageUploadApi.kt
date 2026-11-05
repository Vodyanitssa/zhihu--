package com.zhihuminus.data.zhihu.api

import android.graphics.BitmapFactory
import com.zhihuminus.core.environment.ZhihuApiEnvironment
import com.zhihuminus.core.util.HttpStatusException
import com.zhihuminus.core.util.Log
import com.zhihuminus.core.util.raiseForStatus
import com.zhihuminus.data.common.ZhihuJson
import com.zhihuminus.data.zhihu.crypto.ZhihuFetchSignature
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Base64
import java.util.Locale
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * 已上传到知乎图床的图片。
 *
 * [url] 是上传接口返回的 `pic-private.zhihu.com` 草稿地址，评论提交时必须原样使用：
 * 不能自行改写为 `pic1.zhimg.com`（自行拼接的地址没有服务端签名，打开会 403），
 * 由服务端在保存评论时转成公开 CDN 地址。
 */
data class UploadedZhihuImage(
    val url: String,
    val originalUrl: String,
    val width: Int,
    val height: Int,
)

/**
 * 知乎图床上传。
 *
 * 链路（与上游 `editor/ZhihuImageUpload.kt` 一致）：
 * 1. `POST https://api.zhihu.com/images` 申请 `image_id` 与 OSS `upload_token`
 * 2. `PUT https://zhihu-pics-upload.zhimg.com/v2-{md5}` 上传原始字节（GIF 暂不支持）
 * 3. `PUT /images/{id}/uploading_status` 后轮询 `GET /images/{id}` 直到 `status=success`
 */
class ZhihuImageUploadApi(
    private val environment: ZhihuApiEnvironment,
) {
    suspend fun upload(
        bytes: ByteArray,
        mimeType: String? = null,
    ): UploadedZhihuImage {
        val contentType = resolveContentType(bytes, mimeType)
        val md5 = ZhihuFetchSignature.md5Hex(bytes)
        val applyResponse = environment.withAuthenticatedClient { client, _ ->
            requestApplyWithFallback(client, md5, COMMENT_SOURCE)
        }
        val imageId = applyResponse.uploadFile.imageId
        if (applyResponse.uploadFile.state == UPLOAD_REQUIRED_STATE) {
            val token = applyResponse.uploadToken
                ?: throw IllegalStateException("图片需要上传但缺少 upload_token (image_id=$imageId)")
            environment.withAuthenticatedClient { client, _ ->
                uploadObject(client, md5, bytes, contentType.toString(), token)
                notifyUploadingStatus(client, imageId)
            }
        }
        val status = environment.withAuthenticatedClient { client, _ ->
            pollImageStatus(client, imageId)
        }
        val url = status.src
            ?: status.originalSrc
            ?: "https://pic-private.zhihu.com/v2-$md5"
        val (width, height) = readImageSize(bytes)
        Log.i(TAG, "uploaded image $imageId -> $url (${width}x$height)")
        return UploadedZhihuImage(
            url = url,
            originalUrl = status.originalSrc ?: url,
            width = width,
            height = height,
        )
    }

    private suspend fun requestApply(
        client: HttpClient,
        md5: String,
        source: String,
    ): ApplyImageUploadResponse {
        val response = client
            .post(APPLY_URL) {
                contentType(ContentType.Application.Json)
                setBody("""{"image_hash":"$md5","source":"$source"}""")
            }.raiseForStatus()
        return ZhihuJson.decodeFromString(response.bodyAsText())
    }

    private suspend fun requestApplyWithFallback(
        client: HttpClient,
        md5: String,
        source: String,
    ): ApplyImageUploadResponse = try {
        requestApply(client, md5, source)
    } catch (e: HttpStatusException) {
        if (source == COMMENT_SOURCE && e.status.value in 400..499) {
            Log.w(TAG, "source=$source rejected with ${e.status.value}, retry with article")
            requestApply(client, md5, ARTICLE_SOURCE)
        } else {
            throw e
        }
    }

    private suspend fun uploadObject(
        client: HttpClient,
        md5: String,
        bytes: ByteArray,
        mimeType: String,
        token: UploadToken,
    ) {
        val date = rfc1123Now()
        client
            .put("$UPLOAD_HOST/v2-$md5") {
                contentType(ContentType.parse(mimeType))
                header("x-oss-date", date)
                header("x-oss-user-agent", OSS_USER_AGENT)
                header("x-oss-security-token", token.accessToken)
                header(HttpHeaders.Authorization, ossAuthorization(token, "PUT", mimeType, date, "/zhihu-pics/v2-$md5"))
                setBody(bytes)
            }.raiseForStatus()
    }

    private suspend fun notifyUploadingStatus(
        client: HttpClient,
        imageId: String,
    ) {
        client
            .put("$IMAGE_URL_PREFIX/$imageId/uploading_status") {
                contentType(ContentType.Application.Json)
                setBody("""{"upload_result":"success"}""")
            }.raiseForStatus()
    }

    private suspend fun pollImageStatus(
        client: HttpClient,
        imageId: String,
    ): ImageStatus {
        var status = fetchImageStatus(client, imageId)
        repeat(POLL_ATTEMPTS - 1) {
            if (status.status == "success") return status
            delay(POLL_INTERVAL_MILLIS)
            status = fetchImageStatus(client, imageId)
        }
        return status
    }

    private suspend fun fetchImageStatus(
        client: HttpClient,
        imageId: String,
    ): ImageStatus {
        val response = client.get("$IMAGE_URL_PREFIX/$imageId").raiseForStatus()
        return parseStatus(response.bodyAsText())
    }

    private fun parseStatus(body: String): ImageStatus = runCatching {
        ZhihuJson.decodeFromString<ImageStatus>(body)
    }.getOrElse {
        Log.w(TAG, "unexpected image status payload: $body")
        ImageStatus()
    }

    private fun ossAuthorization(
        token: UploadToken,
        method: String,
        mimeType: String,
        date: String,
        resource: String,
    ): String {
        val stringToSign = buildString {
            append(method).append('\n')
            append('\n')
            append(mimeType).append('\n')
            append(date).append('\n')
            append("x-oss-date:").append(date).append('\n')
            append("x-oss-security-token:").append(token.accessToken).append('\n')
            append("x-oss-user-agent:").append(OSS_USER_AGENT).append('\n')
            append(resource)
        }
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(token.accessKey.toByteArray(), "HmacSHA1"))
        val signature = Base64.getEncoder().encodeToString(mac.doFinal(stringToSign.toByteArray()))
        return "OSS ${token.accessId}:$signature"
    }

    private fun resolveContentType(bytes: ByteArray, provided: String?): ContentType {
        val mime = provided
            ?.takeIf { it.startsWith("image/") }
            ?: sniffMimeType(bytes)
            ?: throw IllegalArgumentException("无法识别图片格式，请选择 JPG/PNG/WebP 图片")
        if (mime !in SUPPORTED_MIME_TYPES) {
            throw IllegalArgumentException("暂不支持该图片格式，请选择 JPG/PNG/WebP 图片")
        }
        return ContentType.parse(mime)
    }

    private fun sniffMimeType(bytes: ByteArray): String? = when {
        bytes.size >= 8 &&
            bytes[0] == 0x89.toByte() &&
            bytes[1] == 'P'.code.toByte() &&
            bytes[2] == 'N'.code.toByte() &&
            bytes[3] == 'G'.code.toByte() -> "image/png"

        bytes.size >= 3 &&
            bytes[0] == 0xff.toByte() &&
            bytes[1] == 0xd8.toByte() -> "image/jpeg"

        bytes.size >= 12 &&
            bytes[0] == 'R'.code.toByte() &&
            bytes[1] == 'I'.code.toByte() &&
            bytes[2] == 'F'.code.toByte() &&
            bytes[3] == 'F'.code.toByte() &&
            bytes[8] == 'W'.code.toByte() &&
            bytes[9] == 'E'.code.toByte() &&
            bytes[10] == 'B'.code.toByte() &&
            bytes[11] == 'P'.code.toByte() -> "image/webp"

        else -> null
    }

    private fun readImageSize(bytes: ByteArray): Pair<Int, Int> {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        return options.outWidth to options.outHeight
    }

    private fun rfc1123Now(): String =
        ZonedDateTime.now(ZoneOffset.UTC).format(RFC1123_FORMATTER)

    @Serializable
    private data class ApplyImageUploadResponse(
        val uploadFile: UploadFile = UploadFile(),
        val uploadToken: UploadToken? = null,
    )

    @Serializable
    private data class UploadFile(
        val imageId: String = "",
        val state: Int = 0,
    )

    @Serializable
    private data class UploadToken(
        val accessId: String = "",
        val accessKey: String = "",
        val accessToken: String = "",
    )

    @Serializable
    private data class ImageStatus(
        val status: String? = null,
        val src: String? = null,
        val originalSrc: String? = null,
    )

    private companion object {
        const val TAG = "ZhihuImageUpload"
        const val APPLY_URL = "https://api.zhihu.com/images"
        const val IMAGE_URL_PREFIX = "https://api.zhihu.com/images"
        const val UPLOAD_HOST = "https://zhihu-pics-upload.zhimg.com"
        const val OSS_USER_AGENT = "aliyun-sdk-js/6.8.0 Chrome 99.0.4844.84 on Windows 10 64-bit"
        const val COMMENT_SOURCE = "comment"
        const val ARTICLE_SOURCE = "article"
        const val UPLOAD_REQUIRED_STATE = 2
        const val POLL_ATTEMPTS = 10
        const val POLL_INTERVAL_MILLIS = 2_000L
        val SUPPORTED_MIME_TYPES = setOf("image/jpeg", "image/png", "image/webp")
        val RFC1123_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US)
    }
}
