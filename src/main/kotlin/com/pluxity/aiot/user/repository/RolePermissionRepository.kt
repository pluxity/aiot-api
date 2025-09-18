package com.pluxity.aiot.user.repository

import com.pluxity.aiot.permission.PermissionGroup
import com.pluxity.aiot.user.entity.Role
import com.pluxity.aiot.user.entity.RolePermission
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface RolePermissionRepository : JpaRepository<RolePermission, Long> {
    fun deleteAllByPermissionGroup(permissionGroup: PermissionGroup)

    @Modifying
    @Query("DELETE FROM RolePermission rp WHERE rp.role = :role")
    fun deleteAllByRole(
        @Param("role") role: Role,
    )
}
