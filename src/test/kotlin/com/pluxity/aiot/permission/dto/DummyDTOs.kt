package com.pluxity.aiot.permission.dto

fun dummyPermissionGroupCreateRequest(
    name: String = "group",
    description: String? = "description",
    permissions: List<PermissionRequest> = listOf(PermissionRequest(resourceType = "SITE", resourceIds = listOf("1"))),
) = PermissionGroupCreateRequest(
    name = name,
    description = description,
    permissions = permissions,
)

fun dummyPermissionGroupUpdateRequest(
    name: String = "group",
    description: String? = null,
    permissions: List<PermissionRequest> = emptyList(),
) = PermissionGroupUpdateRequest(
    name = name,
    description = description,
    permissions = permissions,
)
