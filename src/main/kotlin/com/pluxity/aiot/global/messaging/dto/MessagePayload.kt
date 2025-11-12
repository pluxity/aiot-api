package com.pluxity.aiot.global.messaging.dto

data class ConnectionErrorPayload(
    val siteId: Long,
    val deviceId: String,
    val objectId: String,
    val message: String,
)

data class SensorAlarmPayload(
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
