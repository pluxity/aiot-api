package com.pluxity.aiot.user.entity

import com.pluxity.aiot.permission.ResourceType

interface Permissible {
    val resourceId: String

    val resourceType: ResourceType
}
