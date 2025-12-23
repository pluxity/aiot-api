package com.pluxity.aiot.event.condition.dto

import com.pluxity.aiot.event.condition.ConditionLevel
import com.pluxity.aiot.event.condition.ConditionType
import com.pluxity.aiot.event.condition.Operator

fun createDummyEventConditionItemRequest(
    fieldKey: String = "Temperature",
    activate: Boolean = true,
    level: ConditionLevel = ConditionLevel.WARNING,
    conditionType: ConditionType? = ConditionType.SINGLE,
    operator: Operator? = Operator.GE,
    thresholdValue: Double? = null,
    leftValue: Double? = null,
    rightValue: Double? = null,
    booleanValue: Boolean? = null,
    notificationEnabled: Boolean = true,
    guideMessage: String? = "guide",
): EventConditionItemRequest =
    EventConditionItemRequest(
        fieldKey = fieldKey,
        activate = activate,
        level = level,
        conditionType = conditionType,
        operator = operator,
        thresholdValue = thresholdValue,
        leftValue = leftValue,
        rightValue = rightValue,
        booleanValue = booleanValue,
        notificationEnabled = notificationEnabled,
        guideMessage = guideMessage,
    )
