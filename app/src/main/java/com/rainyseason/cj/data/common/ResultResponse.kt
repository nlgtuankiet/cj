package com.rainyseason.cj.data.common

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ResultResponse<T>(
    @Json(name = "result")
    val result: T
)
