package com.pluxity.aiot.fixture

import com.pluxity.aiot.system.device.event.DeviceEvent
import com.pluxity.aiot.system.event.condition.EventCondition
import com.pluxity.aiot.system.event.setting.EventSetting

object EventConditionFixture {
    fun create(
        id: Long? = null,
        deviceEvent: DeviceEvent,
        operator: EventCondition.ConditionOperator? = EventCondition.ConditionOperator.BETWEEN,
        minValue: Double? = null,
        maxValue: Double? = null,
        value: String = "",
        notificationEnabled: Boolean = false,
        locationTrackingEnabled: Boolean = false,
        soundEnabled: Boolean = false,
        fireEffectEnabled: Boolean = false,
        controlType: EventCondition.ControlType? = EventCondition.ControlType.AUTO,
        guideMessage: String? = null,
        notificationIntervalMinutes: Int = 0,
        order: Int? = null,
        eventSetting: EventSetting? = null,
    ): EventCondition {
        val condition =
            EventCondition(
                id = id,
                deviceEvent = deviceEvent,
                operator = operator,
                minValue = minValue,
                maxValue = maxValue,
                value = value,
                notificationEnabled = notificationEnabled,
                locationTrackingEnabled = locationTrackingEnabled,
                soundEnabled = soundEnabled,
                fireEffectEnabled = fireEffectEnabled,
                controlType = controlType,
                guideMessage = guideMessage,
                notificationIntervalMinutes = notificationIntervalMinutes,
                order = order,
            )
        eventSetting?.let { condition.addEventSetting(it) }
        return condition
    }

}