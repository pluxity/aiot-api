package com.pluxity.aiot.fixture

import com.pluxity.aiot.system.event.condition.ConditionLevel
import com.pluxity.aiot.system.event.condition.DataType
import com.pluxity.aiot.system.event.condition.EventCondition
import com.pluxity.aiot.system.event.condition.Operator

object EventConditionFixture {
    fun create(
        objectId: String = "34954",
        isActivate: Boolean = true,
        notificationEnabled: Boolean = true,
        order: Int? = null,
        level: ConditionLevel = ConditionLevel.WARNING,
        dataType: DataType = DataType.NUMERIC,
        operator: Operator = Operator.GREATER_THAN,
        numericValue1: Double? = 30.0,
        numericValue2: Double? = null,
        booleanValue: Boolean? = null,
    ) = EventCondition(
        objectId = objectId,
        isActivate = isActivate,
        notificationEnabled = notificationEnabled,
        order = order,
        level = level,
        dataType = dataType,
        operator = operator,
        numericValue1 = numericValue1,
        numericValue2 = numericValue2,
        booleanValue = booleanValue,
    )
}
