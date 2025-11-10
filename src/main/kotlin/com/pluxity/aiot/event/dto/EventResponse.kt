package com.pluxity.aiot.event.dto

import com.pluxity.aiot.data.dto.ListMetaData
import com.pluxity.aiot.data.dto.ListMetricData
import com.pluxity.aiot.data.dto.MetricDefinition
import com.pluxity.aiot.event.entity.EventHistory

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
)

fun EventHistory.toEventResponse() =
    EventResponse(
        eventId = this.id!!,
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
        updatedAt = this.updatedAt.toString(),
        updatedBy = this.updatedBy,
        value = this.value,
        level = this.level?.name,
    )

data class EventTimeSeriesDataResponse(
    val meta: ListMetaData,
    val timestamps: List<String>,
    val metrics: Map<String, ListMetricData>,
)

// 공통 메트릭 정의
object EventMetrics {
    val PENDING = MetricDefinition("pendingCnt", "건")
    val WORKING = MetricDefinition("workingCnt", "건")
    val COMPLETED = MetricDefinition("completedCnt", "건")

    val ALL = listOf(PENDING, WORKING, COMPLETED)
}
