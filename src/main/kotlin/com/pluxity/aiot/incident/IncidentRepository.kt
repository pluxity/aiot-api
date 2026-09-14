package com.pluxity.aiot.incident

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface IncidentRepository :
    JpaRepository<Incident, Long>,
    IncidentCustomRepository {
    fun findBySourceTypeAndSourceId(
        sourceType: IncidentSourceType,
        sourceId: Long,
    ): Incident?

    fun findAllByDeviceId(deviceId: String): List<Incident>

    // 호출 전에 쌓인 삭제(site_sensor_manager 등)를 먼저 flush하지 않으면 clear가 그 삭제를 버린다
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Incident i set i.site = null where i.site.id = :siteId")
    fun detachSite(
        @Param("siteId") siteId: Long,
    ): Int
}
