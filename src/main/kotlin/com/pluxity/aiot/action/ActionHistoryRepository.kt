package com.pluxity.aiot.action

import com.pluxity.aiot.alarm.entity.EventHistory
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository

interface ActionHistoryRepository : JpaRepository<ActionHistory, Long> {
    @EntityGraph(attributePaths = ["eventHistory"])
    override fun findAll(): List<ActionHistory>

    fun findByDeviceId(deviceId: String): List<ActionHistory>

    fun findByEventHistory(eventHistory: EventHistory): List<ActionHistory>
}
