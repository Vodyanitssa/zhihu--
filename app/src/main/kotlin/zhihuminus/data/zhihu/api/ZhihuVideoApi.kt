package com.zhihuminus.data.zhihu.api

import com.zhihuminus.core.environment.ZhihuApiEnvironment
import com.zhihuminus.data.common.ZhihuJson
import com.zhihuminus.data.zhihu.dto.ZVideoDto

open class ZhihuVideoApi(
    protected val environment: ZhihuApiEnvironment,
) {
    open suspend fun fetchZVideo(videoId: String): ZVideoDto {
        val json = environment.fetchJson("https://www.zhihu.com/api/v4/zvideos/$videoId", "")
            ?: throw IllegalStateException("zvideo 详情响应为空")
        return ZhihuJson.decodeJson(json)
    }
}
