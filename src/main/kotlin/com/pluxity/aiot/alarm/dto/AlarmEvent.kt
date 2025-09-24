package com.pluxity.aiot.alarm.dto

import java.time.LocalDateTime

data class AlarmEvent(
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
    val locationTrackingEnabled: Boolean,
    val soundEnabled: Boolean,
    val guideMessage: String,
    val actionResult: String,
)
