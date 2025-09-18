package com.pluxity.aiot.user.entity

import com.pluxity.aiot.permission.ResourceType

class ResourceAllPermissible(
    val type: ResourceType,
) : Permissible {
    override val resourceId: String
        get() = "ALL"
    override val resourceType: ResourceType
        get() = type
}
