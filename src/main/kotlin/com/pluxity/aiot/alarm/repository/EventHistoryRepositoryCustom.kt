package com.pluxity.aiot.alarm.repository

import com.pluxity.aiot.alarm.entity.EventHistory
import java.time.LocalDateTime

interface EventHistoryRepositoryCustom {
    fun findByOccurredAtBetween(
        sensorDescription: String?,
        keyword: String?,
        startTime: LocalDateTime?,
        endTime: LocalDateTime?,
    ): List<EventHistory>
}
