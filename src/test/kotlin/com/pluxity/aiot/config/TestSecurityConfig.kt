package com.pluxity.aiot.config

import com.pluxity.aiot.user.entity.Role
import com.pluxity.aiot.user.entity.RoleType
import com.pluxity.aiot.user.entity.User
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder

object TestSecurityConfig {
    fun setAdminAuthentication() {
        val adminRole = Role(id = 1L, name = RoleType.ADMIN.roleName, description = "Admin Role")
        val adminUser =
            User(
                id = 1L,
                username = "admin",
                password = "password",
                name = "Admin User",
                code = "ADMIN",
            )
        adminUser.addRole(adminRole)

        val authorities = listOf(SimpleGrantedAuthority("ROLE_${RoleType.ADMIN.roleName}"))
        val authentication = UsernamePasswordAuthenticationToken(adminUser, null, authorities)
        SecurityContextHolder.getContext().authentication = authentication
    }

    fun clearAuthentication() {
        SecurityContextHolder.clearContext()
    }
}
