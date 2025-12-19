package com.pluxity.aiot.permission.entity

import com.pluxity.aiot.base.entity.withAudit
import com.pluxity.aiot.base.entity.withId
import com.pluxity.aiot.permission.Permission
import com.pluxity.aiot.permission.PermissionGroup
import com.pluxity.aiot.permission.ResourceType

fun dummyPermissionGroup(
    id: Long = 1L,
    name: String = "group",
    description: String? = null,
    permissions: List<Permission> = emptyList(),
): PermissionGroup =
    PermissionGroup(
        name = name,
        description = description,
    ).withId(id).withAudit().apply {
        permissions.forEach { addPermission(it) }
    }

fun dummyPermission(
    id: Long = 1L,
    resourceName: String = ResourceType.SITE.name,
    resourceId: String = "1",
    permissionGroup: PermissionGroup? = dummyPermissionGroup(),
): Permission =
    Permission(
        resourceName = resourceName,
        resourceId = resourceId,
        permissionGroup = permissionGroup,
    ).withId(id).withAudit()
