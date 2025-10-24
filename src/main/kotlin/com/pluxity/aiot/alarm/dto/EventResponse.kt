package com.pluxity.aiot.alarm.dto

import com.pluxity.aiot.alarm.entity.EventHistory

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
