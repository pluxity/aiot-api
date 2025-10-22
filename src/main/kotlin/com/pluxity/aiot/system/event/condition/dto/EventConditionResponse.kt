package com.pluxity.aiot.system.event.condition.dto

import com.pluxity.aiot.system.event.condition.ConditionLevel
import com.pluxity.aiot.system.event.condition.DataType
import com.pluxity.aiot.system.event.condition.EventCondition
import com.pluxity.aiot.system.event.condition.Operator

data class EventConditionResponse(
    val id: Long?,
    val objectId: String,
    val level: ConditionLevel,
    val dataType: DataType,
    val operator: Operator,
    val numericValue1: Double?,
    val numericValue2: Double?,
    val booleanValue: Boolean?,
    val isActivate: Boolean,
    val needControl: Boolean,
    val notificationEnabled: Boolean,
    val notificationIntervalMinutes: Int?,
    val order: Int?,
)

fun EventCondition.toEventConditionResponse() =
    EventConditionResponse(
        id = this.id,
        objectId = this.objectId,
        level = this.level,
        dataType = this.dataType,
        operator = this.operator,
        numericValue1 = this.numericValue1,
        numericValue2 = this.numericValue2,
        booleanValue = this.booleanValue,
        isActivate = this.isActivate,
        needControl = this.needControl,
        notificationEnabled = this.notificationEnabled,
        notificationIntervalMinutes = this.notificationIntervalMinutes,
        order = this.order,
    )
