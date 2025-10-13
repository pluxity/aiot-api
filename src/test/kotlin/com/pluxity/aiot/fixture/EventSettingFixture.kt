package com.pluxity.aiot.fixture

import com.pluxity.aiot.system.device.profile.DeviceProfileType
import com.pluxity.aiot.system.event.setting.EventSetting

object EventSettingFixture {
    fun create(
        id: Long? = null,
        deviceProfileType: DeviceProfileType? = null,
        eventEnabled: Boolean = false,
        isPeriodic: Boolean = false,
        isOriginal: Boolean = false,
        months: MutableSet<Int>? = mutableSetOf(),
    ): EventSetting =
        EventSetting(
            id = id,
            deviceProfileType = deviceProfileType,
            eventEnabled = eventEnabled,
            isPeriodic = isPeriodic,
            isOriginal = isOriginal,
            months = months,
        )
}
