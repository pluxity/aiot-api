package com.pluxity.aiot.action

import com.pluxity.aiot.incident.Incident
import org.springframework.data.jpa.repository.JpaRepository

interface ActionHistoryRepository : JpaRepository<ActionHistory, Long> {
    fun findByIncident(incident: Incident): List<ActionHistory>

    fun findByIdAndIncident(
        id: Long,
        incident: Incident,
    ): ActionHistory?
}
