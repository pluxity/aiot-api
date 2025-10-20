package com.pluxity.aiot.fixture

import com.pluxity.aiot.system.device.event.DeviceEvent
import com.pluxity.aiot.system.event.condition.EventCondition

object EventConditionFixture {
    fun create(
        id: Long? = null,
        deviceEvent: DeviceEvent,
        isActivate: Boolean = true,
        needControl: Boolean = false,
        isBoolean: Boolean = false,
        minValue: String? = null,
        maxValue: String? = null,
        notificationEnabled: Boolean = false,
        notificationIntervalMinutes: Int = 0,
        order: Int? = null,
    ): EventCondition =
        EventCondition(
            id = id,
            deviceEvent = deviceEvent,
            isActivate = isActivate,
            needControl = needControl,
            isBoolean = isBoolean,
            minValue = minValue,
            maxValue = maxValue,
            notificationEnabled = notificationEnabled,
            notificationIntervalMinutes = notificationIntervalMinutes,
            order = order,
        )
}
