package com.pluxity.aiot.event.dto

import com.pluxity.aiot.data.dto.ListMetaData
import com.pluxity.aiot.data.dto.ListMetricData
import com.pluxity.aiot.data.dto.MetricDefinition
import com.pluxity.aiot.event.condition.ConditionLevel
import com.pluxity.aiot.event.entity.EventStatus
import com.pluxity.aiot.incident.IncidentSourceType
import com.pluxity.aiot.sensor.type.DeviceProfileEnum
import java.time.LocalDateTime

data class EventResponse(
    val eventId: Long,
    val sourceType: String,
    val title: String,
    val deviceId: String,
    val objectId: String?,
    val occurredAt: String,
    val minValue: Double? = null,
    val maxValue: Double? = null,
    val status: String,
    val eventName: String,
    val fieldKey: String?,
    val guideMessage: String?,
    val longitude: Double?,
    val latitude: Double?,
    val updatedAt: String,
    val updatedBy: String?,
    val value: Double?,
    val level: String,
    val siteId: Long? = null,
    val siteName: String? = null,
    val sensorDescription: String?,
    val profileDescription: String? = null,
)

data class EventTimeSeriesDataResponse(
    val meta: ListMetaData,
    val timestamps: List<String>,
    val metrics: Map<String, ListMetricData>,
)

// 공통 메트릭 정의
object EventMetrics {
    val ACTIVE = MetricDefinition(EventStatus.ACTIVE.metricKey, "건")
    val IN_PROGRESS = MetricDefinition(EventStatus.IN_PROGRESS.metricKey, "건")
    val RESOLVED = MetricDefinition(EventStatus.RESOLVED.metricKey, "건")

    val ALL = listOf(ACTIVE, IN_PROGRESS, RESOLVED)
}

data class IncidentRow(
    val eventId: Long,
    val sourceType: IncidentSourceType,
    val deviceId: String,
    val deviceName: String?,
    val title: String,
    val objectId: String?,
    val occurredAt: LocalDateTime,
    val minValue: Double?,
    val maxValue: Double?,
    val status: EventStatus,
    val eventName: String?,
    val fieldKey: String?,
    val guideMessage: String?,
    val longitude: Double?,
    val latitude: Double?,
    val updatedBy: String?,
    val updatedAt: LocalDateTime,
    val value: Double?,
    val level: ConditionLevel,
    val siteId: Long?,
    val siteName: String?,
)

fun IncidentRow.toEventResponse() =
    EventResponse(
        eventId = this.eventId,
        sourceType = this.sourceType.name,
        title = this.title,
        deviceId = this.deviceId,
        objectId = this.objectId,
        occurredAt = this.occurredAt.toString(),
        minValue = this.minValue,
        maxValue = this.maxValue,
        status = this.status.name,
        eventName = this.eventName ?: "${this.level.name}_${this.title}",
        fieldKey = this.fieldKey,
        guideMessage = this.guideMessage,
        longitude = this.longitude,
        latitude = this.latitude,
        updatedBy = this.updatedBy,
        updatedAt = this.updatedAt.toString(),
        value = this.value,
        level = this.level.name,
        siteId = this.siteId,
        siteName = this.siteName,
        sensorDescription = this.deviceName,
        profileDescription = this.fieldKey?.let { DeviceProfileEnum.getDescriptionByFieldKey(it) } ?: this.title,
    )

data class EventCursorPageResponse(
    val content: List<EventResponse>,
    val nextCursor: Long?,
    val nextStatus: String?,
    val hasNext: Boolean,
)

fun List<EventResponse>.toEventCursorPageResponse(hasNext: Boolean) =
    EventCursorPageResponse(
        content = if (hasNext) this.dropLast(1) else this,
        nextCursor = if (hasNext) this.lastOrNull()?.eventId else null,
        nextStatus = if (hasNext) this.lastOrNull()?.status else null,
        hasNext = hasNext,
    )
