package com.pluxity.aiot.event.repository

import com.pluxity.aiot.event.entity.EventHistory
import com.pluxity.aiot.event.entity.EventStatus
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
        siteId: Long? = null,
        result: EventStatus? = null,
        siteIds: List<Long>,
    ): List<EventHistory>
}
