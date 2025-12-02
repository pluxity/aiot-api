package com.pluxity.aiot.user.dto

import com.pluxity.aiot.user.entity.RoleType
import jakarta.validation.constraints.NotBlank

data class RoleCreateRequest(
    @field:NotBlank(message = "Role name cannot be empty")
    val name: String,
    val description: String?,
    val permissionGroupIds: List<Long> = emptyList(),
    val auth: RoleType = RoleType.USER,
)
