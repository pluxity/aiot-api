package com.pluxity.aiot.site

import com.pluxity.aiot.global.annotation.CheckPermission
import com.pluxity.aiot.user.entity.PermissionCheckType
import com.pluxity.aiot.user.entity.PermissionType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface SiteRepository : JpaRepository<Site, Long> {
    @CheckPermission(type = PermissionType.ID, phase = PermissionCheckType.ITEM_LIST)
    fun findAllByOrderByCreatedAtDesc(): List<Site>

    @Query(
        value = "SELECT * FROM site WHERE ST_Contains(location, ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)) LIMIT 1",
        nativeQuery = true,
    )
    fun findFirstByPointInPolygon(
        @Param("lon") lon: Double,
        @Param("lat") lat: Double,
    ): Site?
}
