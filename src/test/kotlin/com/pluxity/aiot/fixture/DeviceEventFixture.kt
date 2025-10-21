package com.pluxity.aiot.fixture

import com.pluxity.aiot.system.device.event.DeviceEvent
import com.pluxity.aiot.system.device.type.DeviceType

object DeviceEventFixture {
    fun create(
        id: Long? = null,
        name: String = "Normal",
        deviceType: DeviceType? = null,
    ): DeviceEvent =
        DeviceEvent(
            id = id,
            name = name,
            deviceType = deviceType,
        )
}
