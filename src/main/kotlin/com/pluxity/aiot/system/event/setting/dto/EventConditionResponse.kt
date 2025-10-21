package com.pluxity.aiot.system.event.setting.dto

import com.pluxity.aiot.system.event.condition.ConditionLevel
import com.pluxity.aiot.system.event.condition.DataType
import com.pluxity.aiot.system.event.condition.EventCondition
import com.pluxity.aiot.system.event.condition.Operator

data class EventConditionResponse(
    val id: Long?,
    val deviceEventId: Long?,
    val level: ConditionLevel,
    val dataType: DataType,
    val operator: Operator,
    val numericValue1: Double?,
    val numericValue2: Double?,
    val booleanValue: Boolean?,
    val notificationEnabled: Boolean,
    val notificationIntervalMinutes: Int?,
    val order: Int?,
    val deviceEventName: String?,
)

fun EventCondition.toEventConditionResponse() =
    EventConditionResponse(
        id = this.id,
        deviceEventId = this.deviceEvent.id,
        level = this.level,
        dataType = this.dataType,
        operator = this.operator,
        numericValue1 = this.numericValue1,
        numericValue2 = this.numericValue2,
        booleanValue = this.booleanValue,
        notificationEnabled = this.notificationEnabled,
        notificationIntervalMinutes = this.notificationIntervalMinutes,
        order = this.order,
        deviceEventName = this.deviceEvent.name,
    )
