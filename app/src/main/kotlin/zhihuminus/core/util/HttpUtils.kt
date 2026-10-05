package com.zhihuminus.core.util

import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url

class HttpStatusException(
    val status: HttpStatusCode,
    val requestUrl: Url,
    val bodyText: String,
) : Exception() {
    override val message: String
        get() = "HTTP error: ${status.value} ${status.description} on $requestUrl: \n $bodyText"

    var dumpedCurlRequest: String? = null

    constructor(
        status: HttpStatusCode,
        requestUrl: Url,
        bodyText: String,
        dumpedCurlRequest: String?,
    ) : this(status, requestUrl, bodyText) {
        this.dumpedCurlRequest = dumpedCurlRequest
    }
}

suspend fun HttpResponse.raiseForStatus(dumpRequest: Boolean = false): HttpResponse {
    if (status.value >= 400) {
        throw HttpStatusException(
            status = status,
            requestUrl = request.url,
            bodyText = bodyAsText(),
            dumpedCurlRequest = if (dumpRequest) dumpCurlRequest() else null,
        )
    }
    return this
}

fun HttpResponse.dumpCurlRequest(): String {
    val sb = StringBuilder()
    sb.append("curl -X ${request.method.value} '${request.url}' ")
    request.headers.forEach { key, values ->
        values.forEach { value ->
            sb.append("\\\n  -H '$key: $value' ")
        }
    }
    return sb.toString()
}
