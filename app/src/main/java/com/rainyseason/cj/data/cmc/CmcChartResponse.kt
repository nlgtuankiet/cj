package com.rainyseason.cj.data.cmc

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CmcChartResponse(
    val data: Data,
) {
    @JsonClass(generateAdapter = true)
    data class Data(
        val points: Map<Double, Point>
    ) {
        @JsonClass(generateAdapter = true)
        data class Point(
            val v: List<Double>,
            val c: List<Double>?
        )
    }
}
