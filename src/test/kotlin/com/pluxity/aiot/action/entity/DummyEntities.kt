package com.pluxity.aiot.action.entity

import com.pluxity.aiot.action.ActionHistory
import com.pluxity.aiot.base.entity.withAudit
import com.pluxity.aiot.base.entity.withId
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
    eventHistory = eventHistory,
    content = content,
).withAudit().withId(id)

fun dummyEventHistory(
    id: Long = 999L,
    deviceId: String = "SNIOT-P-THM-001",
    objectId: String = "34954",
    sensorDescription: String = "온습도계",
    fieldKey: String = "Temperature",
    value: Double = 4.9,
    unit: String = "°C",
    eventName: String = "CAUTION_Temperature",
    occurredAt: LocalDateTime = LocalDateTime.now(),
    minValue: Double? = null,
    maxValue: Double? = null,
    eventStatus: EventStatus = EventStatus.ACTIVE,
    guideMessage: String? = null,
    level: ConditionLevel? = ConditionLevel.CAUTION,
) = EventHistory(
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
    guideMessage = guideMessage,
    level = level,
).apply { status = eventStatus }.withId(id)

fun dummyEventHistoryRow(
    eventId: Long = 999L,
    deviceId: String = "SNIOT-P-THM-001",
    objectId: String = "34954",
    occurredAt: LocalDateTime = LocalDateTime.now(),
    minValue: Double? = null,
    maxValue: Double? = null,
    status: EventStatus = EventStatus.ACTIVE,
    eventName: String = "CAUTION_Temperature",
    fieldKey: String = "Temperature",
    guideMessage: String? = null,
    longitude: Double? = null,
    latitude: Double? = null,
    updatedBy: String = "system",
    updatedAt: LocalDateTime = LocalDateTime.now(),
    value: Double = 4.9,
    level: ConditionLevel = ConditionLevel.CAUTION,
    siteId: Long = 1,
    siteName: String = "현장",
    sensorDescription: String = "온습도계",
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
