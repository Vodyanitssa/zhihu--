package com.zhihuminus.data.zhihu.dto

import kotlinx.serialization.Serializable

@Serializable
data class VoteResponse(
    val voteupCount: Int = 0,
)
