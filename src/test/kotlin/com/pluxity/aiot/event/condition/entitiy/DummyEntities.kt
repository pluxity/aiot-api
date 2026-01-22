package com.pluxity.aiot.event.condition.entitiy

import com.pluxity.aiot.event.condition.ConditionLevel
import com.pluxity.aiot.event.condition.ConditionType
import com.pluxity.aiot.event.condition.EventCondition
import com.pluxity.aiot.event.condition.Operator

fun dummyEventCondition(
    id: Long,
    objectId: String,
    fieldKey: String,
    thresholdValue: Double,
    level: ConditionLevel = ConditionLevel.WARNING,
): EventCondition =
    EventCondition(
        id = id,
        objectId = objectId,
        fieldKey = fieldKey,
        isActivate = true,
        notificationEnabled = true,
        level = level,
        conditionType = ConditionType.SINGLE,
        operator = Operator.GE,
        thresholdValue = thresholdValue,
        leftValue = null,
        rightValue = null,
        booleanValue = null,
        guideMessage = "guide",
    )
