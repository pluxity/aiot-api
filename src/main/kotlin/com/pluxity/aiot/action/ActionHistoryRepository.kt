package com.pluxity.aiot.action

import com.pluxity.aiot.alarm.entity.EventHistory
import org.springframework.data.jpa.repository.JpaRepository

interface ActionHistoryRepository : JpaRepository<ActionHistory, Long> {
    fun findByEventHistory(eventHistory: EventHistory): List<ActionHistory>

    fun findByIdAndEventHistory(
        id: Long,
        eventHistory: EventHistory,
    ): ActionHistory?
}
