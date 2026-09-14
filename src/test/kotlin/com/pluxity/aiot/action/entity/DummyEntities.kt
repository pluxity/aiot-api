package com.pluxity.aiot.action.entity

import com.pluxity.aiot.action.ActionHistory
import com.pluxity.aiot.base.entity.withAudit
import com.pluxity.aiot.base.entity.withId
import com.pluxity.aiot.event.condition.ConditionLevel
import com.pluxity.aiot.event.dto.IncidentRow
import com.pluxity.aiot.event.entity.EventStatus
import com.pluxity.aiot.incident.Incident
import com.pluxity.aiot.incident.IncidentSourceType
import com.pluxity.aiot.site.Site
import java.time.LocalDateTime

fun dummyActionHistory(
    id: Long = 999L,
    incident: Incident = dummyIncident(),
    content: String = "content",
) = ActionHistory(
    incident = incident,
    content = content,
).withAudit().withId(id)

fun dummyIncident(
    id: Long = 999L,
    sourceType: IncidentSourceType = IncidentSourceType.SENSOR,
    sourceId: Long = 1L,
    site: Site? = null,
    deviceId: String = "SNIOT-P-THM-001",
    deviceName: String? = "온습도계",
    title: String = "온도",
    level: ConditionLevel = ConditionLevel.CAUTION,
    occurredAt: LocalDateTime = LocalDateTime.now(),
    latitude: Double? = 37.0,
    longitude: Double? = 127.0,
    guideMessage: String? = null,
    status: EventStatus = EventStatus.ACTIVE,
) = Incident(
    sourceType = sourceType,
    sourceId = sourceId,
    site = site,
    deviceId = deviceId,
    deviceName = deviceName,
    title = title,
    level = level,
    occurredAt = occurredAt,
    latitude = latitude,
    longitude = longitude,
    guideMessage = guideMessage,
).apply { changeStatus(status) }.withAudit().withId(id)

fun dummyIncidentRow(
    eventId: Long = 999L,
    sourceType: IncidentSourceType = IncidentSourceType.SENSOR,
    deviceId: String = "SNIOT-P-THM-001",
    deviceName: String? = "온습도계",
    title: String = "온도",
    objectId: String? = "34954",
    occurredAt: LocalDateTime = LocalDateTime.now(),
    minValue: Double? = null,
    maxValue: Double? = null,
    status: EventStatus = EventStatus.ACTIVE,
    eventName: String? = "CAUTION_Temperature",
    fieldKey: String? = "Temperature",
    guideMessage: String? = null,
    longitude: Double? = null,
    latitude: Double? = null,
    updatedBy: String? = "system",
    updatedAt: LocalDateTime = LocalDateTime.now(),
    value: Double? = 4.9,
    level: ConditionLevel = ConditionLevel.CAUTION,
    siteId: Long? = 1,
    siteName: String? = "현장",
) = IncidentRow(
    eventId = eventId,
    sourceType = sourceType,
    deviceId = deviceId,
    deviceName = deviceName,
    title = title,
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
)
