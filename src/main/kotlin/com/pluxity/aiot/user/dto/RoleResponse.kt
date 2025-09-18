package com.pluxity.aiot.user.dto

import com.pluxity.aiot.permission.dto.PermissionGroupResponse
import com.pluxity.aiot.permission.dto.toPermissionGroupResponse
import com.pluxity.aiot.user.entity.Role

data class RoleResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val permissions: List<PermissionGroupResponse>,
)

fun Role.toRoleResponse() =
    RoleResponse(
        this.id!!,
        this.name,
        this.description,
        this.rolePermissions
            .map { it.permissionGroup }
            .map { it.toPermissionGroupResponse() }
            .toList(),
    )
