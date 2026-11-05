package com.zhihuminus.core.util

import com.zhihuminus.core.error.HttpStatusException
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request

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
