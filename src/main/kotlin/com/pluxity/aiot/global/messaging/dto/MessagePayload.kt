package com.pluxity.aiot.global.messaging.dto

import java.time.LocalDateTime

data class ConnectionErrorPayload(
    val siteId: Long,
    val deviceId: String,
    val objectId: String,
    val message: String,
)

data class SensorAlarmPayload(
    val siteId: Long,
    val sensorType: String,
    val message: String,
    val level: String,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val deviceId: String,
    val objectId: String,
    val sensorDescription: String,
    val fieldKey: String,
    val value: Double,
    val unit: String,
    val eventName: String,
    val minValue: Double,
    val maxValue: Double,
    val notificationEnabled: Boolean,
    val actionResult: String,
)
