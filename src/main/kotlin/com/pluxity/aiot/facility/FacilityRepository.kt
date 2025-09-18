package com.pluxity.aiot.facility

import com.pluxity.aiot.global.annotation.CheckPermission
import com.pluxity.aiot.user.entity.PermissionCheckType
import com.pluxity.aiot.user.entity.PermissionType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface FacilityRepository : JpaRepository<Facility, Long> {
    @CheckPermission(type = PermissionType.ID, phase = PermissionCheckType.ITEM_LIST)
    fun findAllByOrderByCreatedAtDesc(): List<Facility>
}
