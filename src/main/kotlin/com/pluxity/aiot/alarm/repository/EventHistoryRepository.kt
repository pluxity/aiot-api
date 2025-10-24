package com.pluxity.aiot.alarm.repository

import com.pluxity.aiot.alarm.entity.EventHistory
import org.springframework.data.jpa.repository.JpaRepository

interface EventHistoryRepository :
    JpaRepository<EventHistory, Long>,
    EventHistoryRepositoryCustom {
    fun findByDeviceId(deviceId: String): List<EventHistory>

}
