package com.pluxity.aiot.fixture

import com.pluxity.aiot.system.device.profile.DeviceProfile
import com.pluxity.aiot.system.device.profile.DeviceProfileType
import com.pluxity.aiot.system.device.type.DeviceType

object DeviceProfileTypeFixture {
    fun create(
        id: Long? = null,
        deviceType: DeviceType,
        deviceProfile: DeviceProfile,
    ): DeviceProfileType =
        DeviceProfileType(
            id = id,
            deviceType = deviceType,
            deviceProfile = deviceProfile,
        )
}
