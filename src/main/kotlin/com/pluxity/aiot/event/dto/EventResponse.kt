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
    val actionResult: String,
    val eventName: String?,
    val fieldKey: String?,
)

fun EventHistory.toEventResponse() =
    EventResponse(
        eventId = this.id!!,
        deviceId = this.deviceId,
        objectId = this.objectId,
        occurredAt = this.occurredAt.toString(),
        minValue = this.minValue,
        maxValue = this.maxValue,
        actionResult = this.actionResult.name,
        eventName = this.eventName,
        fieldKey = this.fieldKey,
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
