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

fun <T, R> Page<T?>.toPageResponse(transform: (T) -> R): PageResponse<R> =
    PageResponse(
        content = this.content.mapNotNull { it?.let(transform) },
        pageNumber = this.number + 1,
        pageSize = this.size,
        totalElements = this.totalElements,
        last = this.isLast,
        first = this.isFirst,
    )
