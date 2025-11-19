package com.pluxity.aiot.fixture

import com.pluxity.aiot.event.condition.ConditionLevel
import com.pluxity.aiot.event.condition.ConditionType
import com.pluxity.aiot.event.condition.EventCondition
import com.pluxity.aiot.event.condition.Operator
import com.pluxity.aiot.sensor.type.SensorType

object EventConditionFixture {
    fun create(
        objectId: String = SensorType.TEMPERATURE_HUMIDITY.objectId,
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
