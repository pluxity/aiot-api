package com.pluxity.aiot.fixture

import com.pluxity.aiot.system.device.type.DeviceType

object DeviceTypeFixture {
    fun create(
        id: Long? = null,
        objectId: String = "SENSOR_001",
        description: String? = "Test sensor device",
        version: String? = "1.0.0",
    ): DeviceType =
        DeviceType(
            id = id,
            objectId = objectId,
            description = description,
            version = version,
        )
}
