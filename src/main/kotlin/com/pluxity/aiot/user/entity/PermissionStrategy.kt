package com.pluxity.aiot.user.entity

interface PermissionStrategy {
    fun check(
        user: User,
        resource: Any,
    ): Boolean
}
