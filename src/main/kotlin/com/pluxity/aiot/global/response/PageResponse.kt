package com.pluxity.aiot.global.response

import org.springframework.data.domain.Page

data class PageResponse<T>(
    val content: List<T>,
    val pageNumber: Int,
    val pageSize: Int,
    val totalElements: Long,
    val last: Boolean,
    val first: Boolean,
)

fun <T : Any, R> Page<T>.toPageResponse(transform: (T) -> R): PageResponse<R> =
    PageResponse(
        content = this.content.map(transform),
        pageNumber = this.number + 1,
        pageSize = this.size,
        totalElements = this.totalElements,
        last = this.isLast,
        first = this.isFirst,
    )

fun <T> emptyPageResponse(
    page: Int,
    size: Int,
): PageResponse<T> =
    PageResponse(
        content = emptyList(),
        pageNumber = page,
        pageSize = size,
        totalElements = 0,
        last = true,
        first = true,
    )
