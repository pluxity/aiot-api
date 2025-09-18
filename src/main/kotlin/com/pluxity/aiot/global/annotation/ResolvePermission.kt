package com.pluxity.aiot.global.annotation

import com.pluxity.aiot.user.entity.PermissionType
import org.springframework.stereotype.Component

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Component
annotation class ResolvePermission(
    val value: PermissionType,
)
