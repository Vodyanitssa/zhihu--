package com.zhihuminus.core.error

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
