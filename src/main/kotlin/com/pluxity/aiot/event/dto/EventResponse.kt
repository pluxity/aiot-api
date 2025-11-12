package com.pluxity.aiot.event.dto

import com.pluxity.aiot.data.dto.ListMetaData
import com.pluxity.aiot.data.dto.ListMetricData
import com.pluxity.aiot.data.dto.MetricDefinition
import com.pluxity.aiot.event.condition.ConditionLevel
import com.pluxity.aiot.event.entity.EventStatus
import com.pluxity.aiot.sensor.type.DeviceProfileEnum
import java.time.LocalDateTime

data class EventResponse(
    val eventId: Long,
    val deviceId: String?,
    val objectId: String?,
    val occurredAt: String,
    val minValue: Double? = null,
    val maxValue: Double? = null,
    val status: String,
    val eventName: String?,
    val fieldKey: String?,
    val guideMessage: String?,
    val longitude: Double?,
    val latitude: Double?,
    val updatedAt: String,
    val updatedBy: String?,
    val value: Double? = null,
    val level: String? = null,
    val siteId: Long? = null,
    val siteName: String? = null,
    val sensorDescription: String? = null,
    val profileDescription: String? = null,
)

data class EventTimeSeriesDataResponse(
    val meta: ListMetaData,
    val timestamps: List<String>,
    val metrics: Map<String, ListMetricData>,
)

// 공통 메트릭 정의
object EventMetrics {
    val ACTIVE = MetricDefinition("activeCnt", "건")
    val IN_PROGRESS = MetricDefinition("inProgressCnt", "건")
    val RESOLVED = MetricDefinition("resolvedCnt", "건")

    val ALL = listOf(ACTIVE, IN_PROGRESS, RESOLVED)
}

data class EventHistoryRow(
    val eventId: Long,
    val deviceId: String?,
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
    val updatedBy: String,
    val updatedAt: LocalDateTime,
    val value: Double?,
    val level: ConditionLevel?,
    val siteId: Long,
    val siteName: String,
    val sensorDescription: String?,
)

fun EventHistoryRow.toEventResponse() =
    EventResponse(
        eventId = this.eventId,
        deviceId = this.deviceId,
        objectId = this.objectId,
        occurredAt = this.occurredAt.toString(),
        minValue = this.minValue,
        maxValue = this.maxValue,
        status = this.status.name,
        eventName = this.eventName,
        fieldKey = this.fieldKey,
        guideMessage = this.guideMessage,
        longitude = this.longitude,
        latitude = this.latitude,
        updatedBy = this.updatedBy,
        updatedAt = this.updatedAt.toString(),
        value = this.value,
        level = this.level?.name,
        siteId = this.siteId,
        siteName = this.siteName,
        sensorDescription = this.sensorDescription,
        profileDescription = DeviceProfileEnum.getDescriptionByFieldKey(this.fieldKey!!),
    )
