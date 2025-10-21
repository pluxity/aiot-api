package com.pluxity.aiot.system.event.setting.dto

import com.pluxity.aiot.system.event.condition.EventCondition

data class EventConditionResponse(
    val id: Long?,
    val deviceEventId: Long?,
    val minValue: String?,
    val maxValue: String?,
    val notificationEnabled: Boolean,
    val notificationIntervalMinutes: Int?,
    val order: Int?,
    val deviceEventName: String?,
    val deviceEventLevel: String?,
)

fun EventCondition.toEventConditionResponse() =
    EventConditionResponse(
        id = this.id,
        deviceEventId = this.deviceEvent.id,
        minValue = this.minValue,
        maxValue = this.maxValue,
        notificationEnabled = this.notificationEnabled,
        notificationIntervalMinutes = this.notificationIntervalMinutes,
        order = this.order,
        deviceEventName = this.deviceEvent.name,
        deviceEventLevel = this.deviceEvent.deviceLevel?.toString(),
    )
