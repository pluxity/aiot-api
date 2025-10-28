package com.pluxity.aiot.fixture

import com.pluxity.aiot.system.event.condition.ConditionLevel
import com.pluxity.aiot.system.event.condition.ConditionType
import com.pluxity.aiot.system.event.condition.EventCondition
import com.pluxity.aiot.system.event.condition.Operator

object EventConditionFixture {
    fun create(
        objectId: String = "34954",
        fieldKey: String = "Temperature",
        isActivate: Boolean = true,
        notificationEnabled: Boolean = true,
        level: ConditionLevel = ConditionLevel.WARNING,
        conditionType: ConditionType = ConditionType.SINGLE,
        operator: Operator = Operator.GE,
        thresholdValue: Double? = 30.0,
        leftValue: Double? = null,
        rightValue: Double? = null,
        booleanValue: Boolean? = null,
    ) = EventCondition(
        objectId = objectId,
        fieldKey = fieldKey,
        isActivate = isActivate,
        notificationEnabled = notificationEnabled,
        level = level,
        conditionType = conditionType,
        operator = operator,
        thresholdValue = thresholdValue,
        leftValue = leftValue,
        rightValue = rightValue,
        booleanValue = booleanValue,
    )
}
