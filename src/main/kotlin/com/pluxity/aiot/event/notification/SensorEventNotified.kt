package com.pluxity.aiot.event.notification

import com.pluxity.aiot.event.condition.ConditionLevel
import com.pluxity.aiot.sensor.type.SensorType
import java.time.LocalDateTime

/** 수신 대상 조회와 발송은 구독자가 맡는다 */
data class SensorEventNotified(
    val eventId: Long,
    val siteId: Long,
    val siteName: String,
    val deviceId: String,
    val sensorType: SensorType,
    val level: ConditionLevel,
    val fieldDescription: String,
    val value: Double,
    val unit: String,
    val guideMessage: String?,
    val occurredAt: LocalDateTime,
)
