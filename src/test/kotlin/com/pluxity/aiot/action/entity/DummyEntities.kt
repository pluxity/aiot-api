package com.pluxity.aiot.action.entity

import com.pluxity.aiot.action.ActionHistory
import com.pluxity.aiot.alarm.entity.EventHistory
import com.pluxity.aiot.alarm.entity.HistoryResult
import com.pluxity.aiot.base.entity.withAudit
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
    actionResult: HistoryResult = HistoryResult.PENDING,
    guideMessage: String? = null,
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
    actionResult = actionResult,
    guideMessage = guideMessage,
)
