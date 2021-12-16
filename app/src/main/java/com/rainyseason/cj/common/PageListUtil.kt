package com.rainyseason.cj.common

import com.airbnb.epoxy.ModelCollector

const val DEFAULT_PER_PAGE = 100

fun <T> createSelectPagedListParam(
    list: List<T>,
    page: Int,
    perPage: Int,
): SelectListParams {
    val maxPage = list.maxPage(perPage)
    val actualPage = page.coerceAtMost(maxPage)
        .coerceAtLeast(0)
    val fromIndex = ((actualPage - 1).coerceAtLeast(0) * perPage)
        .coerceAtLeast(0)
    val toIndex = ((actualPage + 1) * perPage - 1).coerceAtMost(list.lastIndex)
    val showStartLoading = fromIndex > 0
    val showEndLoading = toIndex < list.lastIndex
    return SelectListParams(
        fromIndex = fromIndex,
        toIndex = toIndex,
        showStartLoading = showStartLoading,
        showEndLoading = showEndLoading,
    )
}

fun <T> List<T>.maxPage(perPage: Int): Int {
    var maxPage = (size / perPage - 1).coerceAtLeast(0)
    if (size >= perPage && size % perPage != 0) {
        maxPage += 1
    }
    return maxPage
}

data class SelectListParams(
    val fromIndex: Int,
    val toIndex: Int,
    val showStartLoading: Boolean,
    val showEndLoading: Boolean,
)

fun <T> List<T>.forEachPaged(
    page: Int,
    perPage: Int,
    collector: ModelCollector,
    showMore: () -> Unit,
    block: ((T) -> Unit)
) {
    val params = createSelectPagedListParam(this, page, perPage)
    subList(0, params.toIndex + 1).forEach {
        block.invoke(it)
    }
    if (params.showEndLoading) {
        collector.apply {
            loadingViewWrap {
                id("end_paged_loading")
                onBind { _, _, _ ->
                    showMore()
                }
            }
        }
    }
}
