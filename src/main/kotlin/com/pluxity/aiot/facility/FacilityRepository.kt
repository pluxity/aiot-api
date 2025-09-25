package com.pluxity.aiot.facility

import com.pluxity.aiot.global.annotation.CheckPermission
import com.pluxity.aiot.user.entity.PermissionCheckType
import com.pluxity.aiot.user.entity.PermissionType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface FacilityRepository : JpaRepository<Facility, Long> {
    @CheckPermission(type = PermissionType.ID, phase = PermissionCheckType.ITEM_LIST)
    fun findAllByOrderByCreatedAtDesc(): List<Facility>

    @Query(
        value = "SELECT * FROM facility WHERE ST_Contains(location, ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)) LIMIT 1",
        nativeQuery = true,
    )
    fun findFirstByPointInPolygon(
        @Param("lon") lon: Double,
        @Param("lat") lat: Double,
    ): Facility?
}
