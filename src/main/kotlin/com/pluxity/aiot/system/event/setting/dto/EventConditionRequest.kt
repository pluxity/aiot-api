package com.pluxity.aiot.system.event.setting.dto

import com.pluxity.aiot.system.event.condition.ConditionLevel
import com.pluxity.aiot.system.event.condition.DataType
import com.pluxity.aiot.system.event.condition.Operator

data class EventConditionRequest(
    val id: Long?,
    val isActivate: Boolean,
    val needControl: Boolean,
    val level: ConditionLevel,
    val dataType: DataType,
    val operator: Operator,
    val numericValue1: Double?,
    val numericValue2: Double?,
    val booleanValue: Boolean?,
    val notificationEnabled: Boolean,
    val notificationIntervalMinutes: Int?,
    val order: Int?,
)
