package com.pluxity.aiot.permission

import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException

enum class ResourceType(
    val resourceName: String,
    val endpoint: String,
) {
    NONE("NONE", ""),
    SITE("현장", "sites"),
    ;

    companion object {
        fun fromString(resourceName: String): ResourceType =
            entries.firstOrNull { it != NONE && it.name.equals(resourceName, ignoreCase = true) }
                ?: throw CustomException(ErrorCode.INVALID_RESOURCE_TYPE, "Resource type: $resourceName")
    }
}
