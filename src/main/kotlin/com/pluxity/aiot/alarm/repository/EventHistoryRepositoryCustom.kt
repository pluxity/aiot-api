package com.pluxity.aiot.alarm.repository

import com.pluxity.aiot.alarm.entity.EventHistory
import com.pluxity.aiot.alarm.entity.HistoryResult
import java.time.LocalDateTime

interface EventHistoryRepositoryCustom {
    fun findByOccurredAtBetween(
        sensorDescription: String?,
        keyword: String?,
        startTime: LocalDateTime?,
        endTime: LocalDateTime?,
    ): List<EventHistory>

    fun findEventList(
        from: String?,
        to: String?,
        siteId: Long?,
        result: HistoryResult?,
        siteIds: List<Long>,
    ): List<EventHistory>
}
