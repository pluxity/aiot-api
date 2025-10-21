package com.pluxity.aiot.fixture

import com.pluxity.aiot.system.device.event.DeviceEvent
import com.pluxity.aiot.system.event.condition.ConditionLevel
import com.pluxity.aiot.system.event.condition.DataType
import com.pluxity.aiot.system.event.condition.EventCondition
import com.pluxity.aiot.system.event.condition.Operator

object EventConditionFixture {
    fun create(
        id: Long? = null,
        deviceEvent: DeviceEvent,
        isActivate: Boolean = true,
        needControl: Boolean = false,
        level: ConditionLevel = ConditionLevel.NORMAL,
        dataType: DataType = DataType.NUMERIC,
        operator: Operator = Operator.GREATER_OR_EQUAL,
        numericValue1: Double? = null,
        numericValue2: Double? = null,
        booleanValue: Boolean? = null,
        notificationEnabled: Boolean = false,
        notificationIntervalMinutes: Int = 0,
        order: Int? = null,
    ): EventCondition =
        EventCondition(
            id = id,
            deviceEvent = deviceEvent,
            isActivate = isActivate,
            needControl = needControl,
            level = level,
            dataType = dataType,
            operator = operator,
            numericValue1 = numericValue1,
            numericValue2 = numericValue2,
            booleanValue = booleanValue,
            notificationEnabled = notificationEnabled,
            notificationIntervalMinutes = notificationIntervalMinutes,
            order = order,
        )
}
