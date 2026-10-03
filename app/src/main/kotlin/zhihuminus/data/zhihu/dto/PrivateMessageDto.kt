package com.zhihuminus.data.zhihu.dto

import com.zhihuminus.data.ZhihuPaging
import kotlinx.serialization.Serializable

@Serializable
data class PrivateMessagePageDto(
    val data: List<PrivateMessageDto> = emptyList(),
    val paging: ZhihuPaging = ZhihuPaging(isEnd = true, next = ""),
)

@Serializable
data class PrivateMessageDto(
    val id: String = "",
    val type: String = "",
    val contentType: Int = 0,
    val content: String = "",
    val createdTime: Long = 0,
    val sender: NotificationAuthorDto? = null,
    val receiver: NotificationAuthorDto? = null,
    val plugin: PrivateMessagePluginDto? = null,
)

@Serializable
data class PrivateMessagePluginDto(
    val pluginType: String = "",
    val pluginContent: String = "",
    val excerpt: String = "",
)
