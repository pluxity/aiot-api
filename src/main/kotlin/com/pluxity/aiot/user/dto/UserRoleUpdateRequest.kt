package com.pluxity.aiot.user.dto

data class UserRoleUpdateRequest(
    val roleIds: List<Long> = emptyList(),
)
