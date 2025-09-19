package com.pluxity.aiot.system.event.setting.dto

import com.pluxity.aiot.system.event.condition.EventCondition

class EventConditionResponse(
    val id: Long?,
    val deviceEventId: Long?,
    val value: String?,
    val minValue: Double?,
    val maxValue: Double?,
    val operator: EventCondition.ConditionOperator?,
    val notificationEnabled: Boolean,
    val locationTrackingEnabled: Boolean,
    val soundEnabled: Boolean,
    val fireEffectEnabled: Boolean,
    val controlType: EventCondition.ControlType?,
    val guideMessage: String?,
    val notificationIntervalMinutes: Int?,
    val order: Int?,
    val deviceEventName: String?,
    val deviceEventLevel: String?,
)

fun EventCondition.toEventConditionResponse() =
    EventConditionResponse(
        id = this.id,
        deviceEventId = this.deviceEvent?.id,
        value = this.value,
        minValue = this.minValue,
        maxValue = this.maxValue,
        operator = this.operator,
        notificationEnabled = this.notificationEnabled,
        locationTrackingEnabled = this.locationTrackingEnabled,
        soundEnabled = this.soundEnabled,
        fireEffectEnabled = this.fireEffectEnabled,
        controlType = this.controlType,
        guideMessage = this.guideMessage,
        notificationIntervalMinutes = this.notificationIntervalMinutes,
        order = this.order,
        deviceEventName = this.deviceEvent?.name,
        deviceEventLevel = this.deviceEvent?.deviceLevel?.toString(),
    )
