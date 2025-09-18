package com.pluxity.aiot.global.annotation

import com.pluxity.aiot.permission.ResourceType
import com.pluxity.aiot.user.entity.PermissionCheckType
import com.pluxity.aiot.user.entity.PermissionType

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class CheckPermission(
    val type: PermissionType,
    val target: String = "#returnObject",
    val phase: PermissionCheckType = PermissionCheckType.SINGLE_ITEM,
    val resourceType: ResourceType = ResourceType.NONE,
)
