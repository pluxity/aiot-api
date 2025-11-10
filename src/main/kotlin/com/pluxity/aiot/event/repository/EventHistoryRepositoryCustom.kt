package com.pluxity.aiot.event.repository

import com.pluxity.aiot.event.condition.ConditionLevel
import com.pluxity.aiot.event.entity.EventHistory
import com.pluxity.aiot.event.entity.EventStatus
import com.pluxity.aiot.sensor.type.SensorType
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

    fun findEventListWithPaging(
        from: String?,
        to: String?,
        siteId: Long? = null,
        result: EventStatus? = null,
        level: ConditionLevel? = null,
        sensorType: SensorType? = null,
        siteIds: List<Long>,
        size: Int,
        lastId: Long? = null,
    ): List<EventHistory>
}
