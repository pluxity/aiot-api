package com.pluxity.aiot.system.event.setting.dto

import com.pluxity.aiot.system.event.condition.EventCondition

data class EventConditionRequest(
    val id: Long?,
    val deviceEventId: Long,
    val value: String,
    val operator: EventCondition.ConditionOperator?,
    val notificationEnabled: Boolean,
    val locationTrackingEnabled: Boolean,
    val soundEnabled: Boolean,
    val fireEffectEnabled: Boolean,
    val controlType: EventCondition.ControlType?,
    val guideMessage: String?,
    val notificationIntervalMinutes: Int?,
    val order: Int?,
)
