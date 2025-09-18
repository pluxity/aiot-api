package com.pluxity.aiot.global.utils

import org.springframework.data.domain.Sort

object SortUtils {
    val orderByCreatedAtDesc = Sort.by(Sort.Direction.DESC, "createdAt")
}
