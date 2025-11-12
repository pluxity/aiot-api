package com.pluxity.aiot.action.entity

import com.pluxity.aiot.action.ActionHistory
import com.pluxity.aiot.base.entity.withAudit
import com.pluxity.aiot.event.condition.ConditionLevel
import com.pluxity.aiot.event.dto.EventHistoryRow
import com.pluxity.aiot.event.entity.EventHistory
import com.pluxity.aiot.event.entity.EventStatus
import java.time.LocalDateTime

fun dummyActionHistory(
    id: Long = 999L,
    eventHistory: EventHistory = dummyEventHistory(),
    content: String = "content",
) = ActionHistory(
    id = id,
    eventHistory = eventHistory,
    content = content,
).withAudit()

fun dummyEventHistory(
    id: Long = 999L,
    deviceId: String? = null,
    objectId: String? = null,
    sensorDescription: String? = null,
    fieldKey: String? = null,
    value: Double? = null,
    unit: String? = null,
    eventName: String? = null,
    occurredAt: LocalDateTime = LocalDateTime.now(),
    minValue: Double? = null,
    maxValue: Double? = null,
    eventStatus: EventStatus = EventStatus.PENDING,
    guideMessage: String? = null,
    level: ConditionLevel? = ConditionLevel.CAUTION,
) = EventHistory(
    id = id,
    deviceId = deviceId,
    objectId = objectId,
    sensorDescription = sensorDescription,
    fieldKey = fieldKey,
    value = value,
    unit = unit,
    eventName = eventName,
    occurredAt = occurredAt,
    minValue = minValue,
    maxValue = maxValue,
    status = eventStatus,
    guideMessage = guideMessage,
    level = level,
)

fun dummyEventHistoryRow(
    eventId: Long = 999L,
    deviceId: String? = null,
    objectId: String? = null,
    occurredAt: LocalDateTime = LocalDateTime.now(),
    minValue: Double? = null,
    maxValue: Double? = null,
    status: EventStatus = EventStatus.PENDING,
    eventName: String? = null,
    fieldKey: String? = "Temperature",
    guideMessage: String? = null,
    longitude: Double? = null,
    latitude: Double? = null,
    updatedBy: String = "system",
    updatedAt: LocalDateTime = LocalDateTime.now(),
    value: Double? = null,
    level: ConditionLevel? = ConditionLevel.CAUTION,
    siteId: Long = 1,
    siteName: String = "현장",
    sensorDescription: String? = null,
) = EventHistoryRow(
    eventId = eventId,
    deviceId = deviceId,
    objectId = objectId,
    occurredAt = occurredAt,
    minValue = minValue,
    maxValue = maxValue,
    status = status,
    eventName = eventName,
    fieldKey = fieldKey,
    guideMessage = guideMessage,
    longitude = longitude,
    latitude = latitude,
    updatedBy = updatedBy,
    updatedAt = updatedAt,
    value = value,
    level = level,
    siteId = siteId,
    siteName = siteName,
    sensorDescription = sensorDescription,
)
