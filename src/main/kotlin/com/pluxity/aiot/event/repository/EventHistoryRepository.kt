package com.pluxity.aiot.event.repository

import com.pluxity.aiot.event.entity.EventHistory
import org.springframework.data.jpa.repository.JpaRepository

interface EventHistoryRepository :
    JpaRepository<EventHistory, Long>,
    EventHistoryRepositoryCustom {
    fun findByDeviceId(deviceId: String): List<EventHistory>
}
