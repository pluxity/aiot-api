package com.pluxity.aiot.global.messaging.dto

import com.pluxity.aiot.event.dto.EventResponse

data class ConnectionErrorPayload(
    val siteId: Long,
    val deviceId: String,
    val objectId: String,
    val message: String,
)

typealias SensorAlarmPayload = EventResponse

data class ChangeEventStatusPayload(
    val siteId: Long,
    val eventId: Long,
    val status: String,
)
