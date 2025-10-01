package com.pluxity.aiot.fixture

import com.pluxity.aiot.system.device.profile.DeviceProfile
import com.pluxity.aiot.system.device.profile.DeviceProfile.FieldType

object DeviceProfileFixture {
    fun create(
        id: Long? = null,
        fieldKey: String = "temperature",
        description: String = "Temperature sensor",
        fieldUnit: String? = "°C",
        fieldType: FieldType = FieldType.Float,
    ): DeviceProfile =
        DeviceProfile(
            id = id,
            fieldKey = fieldKey,
            description = description,
            fieldUnit = fieldUnit,
            fieldType = fieldType,
        )

}