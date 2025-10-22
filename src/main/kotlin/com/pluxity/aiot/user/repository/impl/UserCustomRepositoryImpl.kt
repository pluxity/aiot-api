package com.pluxity.aiot.user.repository.impl

import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import com.pluxity.aiot.permission.Permission
import com.pluxity.aiot.permission.PermissionGroup
import com.pluxity.aiot.user.entity.Role
import com.pluxity.aiot.user.entity.RolePermission
import com.pluxity.aiot.user.entity.RoleType
import com.pluxity.aiot.user.entity.User
import com.pluxity.aiot.user.entity.UserRole
import com.pluxity.aiot.user.repository.UserCustomRepository

class UserCustomRepositoryImpl(
    private val kotlinJdslJpqlExecutor: KotlinJdslJpqlExecutor,
) : UserCustomRepository {
    override fun getUserIdsWithSiteAccess(
        resourceType: String,
        resourceId: String,
    ): List<String> =
        kotlinJdslJpqlExecutor
            .findAll {
                selectDistinct(path(User::username))
                    .from(
                        entity(User::class),
                        join(User::userRoles),
                        join(UserRole::role),
                        leftJoin(Role::rolePermissions),
                        leftJoin(RolePermission::permissionGroup),
                        leftJoin(PermissionGroup::permissions),
                    ).where(
                        or(
                            and(
                                path(Permission::resourceName).eq(resourceType),
                                path(Permission::resourceId).eq(resourceId),
                            ),
                            path(Role::auth).eq(RoleType.ADMIN.name),
                        ),
                    )
            }.filterNotNull()
}
