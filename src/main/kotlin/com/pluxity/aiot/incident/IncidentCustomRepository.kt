package com.pluxity.aiot.incident

import com.pluxity.aiot.event.condition.ConditionLevel
import com.pluxity.aiot.event.dto.IncidentRow
import com.pluxity.aiot.event.entity.EventStatus
import com.pluxity.aiot.sensor.type.SensorType

interface IncidentCustomRepository {
    fun findEventList(
        from: String?,
        to: String?,
        siteId: Long? = null,
        status: EventStatus? = null,
    ): List<IncidentRow>

    fun findEventListWithPaging(
        from: String?,
        to: String?,
        siteId: Long? = null,
        status: EventStatus? = null,
        level: ConditionLevel? = null,
        sensorType: SensorType? = null,
        sourceType: IncidentSourceType? = null,
        size: Int,
        lastId: Long? = null,
        lastStatus: EventStatus? = null,
    ): List<IncidentRow>
}
