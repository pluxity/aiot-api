package com.pluxity.aiot.permission.dto

data class PermissionResponse(
    val resourceType: String,
    val resourceIds: List<String>,
)
