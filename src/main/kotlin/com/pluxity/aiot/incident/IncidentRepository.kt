package com.pluxity.aiot.incident

import org.springframework.data.jpa.repository.JpaRepository

interface IncidentRepository :
    JpaRepository<Incident, Long>,
    IncidentCustomRepository {
    fun findBySourceTypeAndSourceId(
        sourceType: IncidentSourceType,
        sourceId: Long,
    ): Incident?

    fun findAllByDeviceId(deviceId: String): List<Incident>
}
