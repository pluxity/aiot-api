package com.pluxity.aiot.system.event.condition.dto

import com.pluxity.aiot.system.event.condition.ConditionLevel
import com.pluxity.aiot.system.event.condition.ConditionType
import com.pluxity.aiot.system.event.condition.EventCondition
import com.pluxity.aiot.system.event.condition.Operator

data class EventConditionResponse(
    val id: Long?,
    val objectId: String,
    val fieldKey: String,
    val level: ConditionLevel,
    val conditionType: ConditionType?,
    val operator: Operator?,
    val thresholdValue: Double?,
    val leftValue: Double?,
    val rightValue: Double?,
    val booleanValue: Boolean?,
    val isActivate: Boolean,
    val notificationEnabled: Boolean,
)

fun EventCondition.toEventConditionResponse() =
    EventConditionResponse(
        id = this.id,
        objectId = this.objectId,
        fieldKey = this.fieldKey,
        level = this.level,
        conditionType = this.conditionType,
        operator = this.operator,
        thresholdValue = this.thresholdValue,
        leftValue = this.leftValue,
        rightValue = this.rightValue,
        booleanValue = this.booleanValue,
        isActivate = this.isActivate,
        notificationEnabled = this.notificationEnabled,
    )
