package com.pluxity.aiot.event.repository

import com.pluxity.aiot.event.condition.ConditionLevel
import com.pluxity.aiot.event.dto.EventHistoryRow
import com.pluxity.aiot.event.entity.EventStatus
import com.pluxity.aiot.sensor.type.SensorType

interface EventHistoryRepositoryCustom {
    fun findEventList(
        from: String?,
        to: String?,
        siteId: Long? = null,
        result: EventStatus? = null,
        siteIds: List<Long>,
    ): List<EventHistoryRow>

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
        lastStatus: EventStatus? = null,
    ): List<EventHistoryRow>
}
